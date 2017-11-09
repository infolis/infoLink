package io.github.infolis.algorithm;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import io.github.infolis.InfolisConfig;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.algorithm.TagSearcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for adding text files to a Lucene index.
 *
 * @author kata
 * @author kba
 */
public class Indexer extends BaseAlgorithm {
//

    private final static String INDEX_DIR_PREFIX = "infolis-index-";

    public Indexer(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private Logger log = LoggerFactory.getLogger(Indexer.class);

    public static Analyzer createAnalyzer() {
    	return new WhitespaceAnalyzer();
    }

    @Override
    public void execute() throws IOException {
        String indexPath;
        if (null != getExecution().getIndexDirectory() && !getExecution().getIndexDirectory().isEmpty()) {
            indexPath = getExecution().getIndexDirectory();
        } else {
            indexPath = Files.createTempDirectory(InfolisConfig.getTmpFilePath().toAbsolutePath(), INDEX_DIR_PREFIX).toString();
            FileUtils.forceDeleteOnExit(new File(indexPath));
        }
        log.debug("Indexing to: " + indexPath);
        getExecution().setOutputDirectory(indexPath);

        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(createAnalyzer());
        indexWriterConfig.setOpenMode(OpenMode.CREATE);
        // An FSDirectory implementation that uses java.nio's FileChannel's positional read, which allows multiple threads to read from the same file without synchronizing. 
        // NIOFSDirectory is not recommended on Windows because of a bug in how FileChannel.read is implemented in Sun's JRE. Inside of the implementation the position is apparently synchronized.
        Directory fsIndexDir = NIOFSDirectory.open(Paths.get(indexPath));

        List<InfolisFile> files = new ArrayList<>();
		if (null != getExecution().getInfolisFileTags() && !getExecution().getInfolisFileTags().isEmpty()) {
			Execution tagExec = getExecution().createSubExecution(TagSearcher.class);
			tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
			tagExec.instantiateAlgorithm(this).run();
			getExecution().getInputFiles().addAll(tagExec.getInputFiles());
		}
        for (String fileUri : getExecution().getInputFiles()) {
            try {
                files.add(this.getInputDataStoreClient().get(InfolisFile.class, fileUri));
            } catch (BadRequestException | ProcessingException e) {
                error(log, "Could not retrieve file " + fileUri + ": " + e.getMessage());
                getExecution().setStatus(ExecutionStatus.FAILED);
                fsIndexDir.close();
                return;
            }
        }

        Date start = new Date();
        log.debug("Starting to index");
        IndexWriter writer = new IndexWriter(fsIndexDir, indexWriterConfig);
        try {
            int counter = 0;
            for (InfolisFile file : files) {
                counter++;
                log.trace("Indexing file " + file);
                writer.addDocument(toLuceneDocument(getInputFileResolver(), file));
                updateProgress(counter, files.size());

            }
        } catch (FileNotFoundException fnfe) {
            // NOTE: at least on windows, some temporary files raise this
            // exception with an "access denied" message checking if the
            // file can be read doesn't help
            throw new RuntimeException("Could not write index entry: " + fnfe);
        } finally {
            log.debug("Merging all Lucene segments ...");
            writer.forceMerge(1);
            writer.close();
            fsIndexDir.close();
        }
        long duration = new Date().getTime() - start.getTime();
        getExecution().setStatus(ExecutionStatus.FINISHED);
        log.debug(String.format("Indexing %s documents took %s ms = %s hours (index: %s)", files.size(), duration, ((duration / 1000) / 60) / 60, indexPath));
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        Execution exec = this.getExecution();
        if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) && (null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())){
            throw new IllegalAlgorithmArgumentException(getClass(), "inputFiles", "missing or empty");
        }
    }

    /**
     * Files a lucene document. Documents are created as follows:
     * <ol>
     * <li>The path of the file is added as a field named "path". The field is
     * indexed (i.e. searchable), but not tokenized into words.</li>
     * <li>The last modified date of the file is added as a field named
     * "modified". The field is indexed (i.e. searchable), not tokenized into
     * words.</li>
     * <li>The contents of the file are added to a field named "contents". A
     * reader is specified so that the text of the file is tokenized and
     * indexed, but not stored. Note that the file is expected to be in
     * UTF-8 encoding. If that's not the case, searching for
     * special characters will fail.</li>
     * <li>Content (text files) is saved in the index along with position and
     * offset information.</li>
     * </ol>
     *
     * @param f	a txt-file to be included in the lucene index
     * @return a	lucene document
     * @throws IOException
     */
    public static Document toLuceneDocument(FileResolver fileResolver, InfolisFile f) throws IOException {
        InputStreamReader isr = new InputStreamReader(fileResolver.openInputStream(f), "UTF8");
        BufferedReader reader = new BufferedReader(isr);
        StringBuffer contents = new StringBuffer();
        String text = null;
        while ((text = reader.readLine()) != null) {
            contents.append(text).append(System.getProperty("line.separator"));
        }
        reader.close();
        isr.close();
        text = new String(contents);
        
        Document doc = new Document();
        doc.add(new StringField("path", f.getUri(), Field.Store.YES));
        doc.add(new StringField("fileName", f.getFileName(), Field.Store.YES));
        // TODO kba: Add modified to InfolisFile
        //doc.add(new LongField("modified", lastModified, Field.Store.NO));
        
        // file is expected to be in UTF-8 encoding.
        // If that's not the case searching for special characters will fail.
        FieldType offsetsType = new FieldType(TextField.TYPE_STORED);
        offsetsType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        Field contentField = new Field("contents", text, offsetsType);
        doc.add(contentField);
        return doc;
    }
}
