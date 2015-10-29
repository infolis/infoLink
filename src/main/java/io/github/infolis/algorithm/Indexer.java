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
import io.github.infolis.infolink.luceneIndexing.CaseSensitiveStandardAnalyzer;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LimitTokenCountAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
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

    /*
     * kba: That was the old value of {@link IndexWriter.MaxFieldLength.LIMITED}
     */
    private static final int MAX_TOKEN_COUNT = 10000;

    public static Analyzer createAnalyzer() {
        return new LimitTokenCountAnalyzer(new CaseSensitiveStandardAnalyzer(), MAX_TOKEN_COUNT);
    }

    @Override
    public void execute() throws IOException {
        File indexDir;
        if (null != getExecution().getIndexDirectory() && ! getExecution().getIndexDirectory().isEmpty()) {
            indexDir = new File(getExecution().getIndexDirectory());
        } else {
            indexDir = new File(Files.createTempDirectory(InfolisConfig.getTmpFilePath().toAbsolutePath(), INDEX_DIR_PREFIX).toString());
            FileUtils.forceDeleteOnExit(indexDir);
        }
        log.debug("Indexing to: " + indexDir.getAbsolutePath());
        getExecution().setOutputDirectory(indexDir.getAbsolutePath().toString());

        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_35, createAnalyzer());
        indexWriterConfig.setOpenMode(OpenMode.CREATE);
        FSDirectory fsIndexDir = FSDirectory.open(indexDir);
        IndexWriter writer = new IndexWriter(fsIndexDir, indexWriterConfig);

        List<InfolisFile> files = new ArrayList<>();
        for (String fileUri : getExecution().getInputFiles()) {   
            files.add(this.getInputDataStoreClient().get(InfolisFile.class, fileUri));
        }

        Date start = new Date();
        log.debug("Starting to index");
        try {
            for (InfolisFile file : files) {
                log.trace("Indexing file " + file);
                writer.addDocument(toLuceneDocument(getInputFileResolver(), file));
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
        }
        fsIndexDir.close();
        log.debug(String.format("Indexing %s documents took %s ms", files.size(), new Date().getTime() - start.getTime()));
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        Execution exec = this.getExecution();
        if (null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) {
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
     * indexed, but not stored. Note that FileReader expects the file to be in
     * the system's default encoding. If that's not the case searching for
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
	  //use code below to process pdfs instead of text (requires pdfBox)
	  /*FileInputStream fi = new FileInputStream(new File(f.getPath()));   
         PDFParser parser = new PDFParser(fi);   
         parser.parse();   
         COSDocument cd = parser.getDocument();   
         PDFTextStripper stripper = new PDFTextStripper();   
         String text = stripper.getText(new PDDocument(cd));  */
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

        // make a new, empty document
        Document doc = new Document();

      // Add the path of the file as a field named "path".  Use a field that is 
        // indexed (i.e. searchable), but don't tokenize the field into words.
        doc.add(new Field("path", f.getUri(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("fileName", f.getFileName(), Field.Store.YES, Field.Index.ANALYZED));

      // Add the last modified date of the file a field named "modified".  Use 
        // a field that is indexed (i.e. searchable), but don't tokenize the field
        // into words.
        // TODO kba: Add modified to InfolisFile
//      doc.add( new Field( "modified", 
//    		  DateTools.timeToString( f.lastModified(), DateTools.Resolution.MINUTE ),
//    		  Field.Store.YES, Field.Index.NOT_ANALYZED ) );
	  // save the content (text files) in the index
        // Add the contents of the file to a field named "contents".  Specify a Reader,
        // so that the text of the file is tokenized and indexed, but not stored.
        // Note that FileReader expects the file to be in the system's default encoding.
        // If that's not the case searching for special characters will fail.
        //Store both position and offset information 
        // TextFilesContent = readTextFiles(f.getPath()) + " ";
        doc.add(new Field("contents", text, Field.Store.YES, Field.Index.ANALYZED,
                Field.TermVector.WITH_POSITIONS_OFFSETS));

      // return the document
        //cd.close();
        return doc;
    }
}
