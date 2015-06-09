package io.github.infolis.infolink.luceneIndexing;


/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.github.infolis.algorithm.BaseAlgorithm;
import io.github.infolis.algorithm.IllegalAlgorithmArgumentException;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LimitTokenCountAnalyzer;
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
 * @author	katarina.boland@gesis.org
 * @author kba
 * @version	2014-01-27
 */
public class Indexer extends BaseAlgorithm
{
	
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
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_35, createAnalyzer());
		indexWriterConfig.setOpenMode(OpenMode.CREATE);
		File indexDir = new File(getExecution().getIndexDirectory());
		IndexWriter writer = new IndexWriter(FSDirectory.open(indexDir), indexWriterConfig);
		List<InfolisFile> files = new ArrayList<>();

		for (String fileUri : getExecution().getInputFiles()) {
			files.add(this.getDataStoreClient().get(InfolisFile.class, fileUri));
		}

		Date start = new Date();
		log.debug("Starting to index");
		for (InfolisFile file : files) {
			
//			log.debug("Indexing file " + file);
			try {
				writer.addDocument(FileDocument.toLuceneDocument(getFileResolver(), file));
			} catch (FileNotFoundException fnfe) {
				// NOTE: at least on windows, some temporary files raise this
				// exception with an "access denied" message checking if the
				// file can be read doesn't help
				writer.close();
				throw new RuntimeException("Could not write index entry for " + file);
			}
		}
		Date end = new Date();
		log.debug(String.format("Indexing %s documents took %s ms", files.size(), end.getTime() - start.getTime()));
		log.debug("Merging all Lucene segments ...");
		writer.forceMerge(1);
		writer.close();
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		Execution exec = this.getExecution();
		if (null == exec.getInputFiles() || exec.getInputFiles().isEmpty())
			throw new IllegalAlgorithmArgumentException(getClass(), "inputFiles", "missing or empty");
		if (null == exec.getIndexDirectory())
			throw new IllegalAlgorithmArgumentException(getClass(), "inputFiles", "missing or empty");
		if (! Files.exists(Paths.get(getExecution().getIndexDirectory())))
			throw new IllegalAlgorithmArgumentException(getClass(), "indexDirectory", "doesn't exist");
	}
  
	
//	/** 
//	 * Selects either all subdirectories in the specified directory (recursive mode) or only the root 
//	 * directory, specifies a path for the indexes to be created and starts the indexing process. 
//	 * 
//	 * @param	args	args[0]: path to the corpus root directory; args[1]: path to the index directory; args[2]: "r" to set recursive mode
//	 */
//	public static void main(String[] args) {
//		if (args.length < 2) {
//			System.out.println("Usage: Indexer <corpusPath> <indexPath> <recursiveFlag>");
//			System.out.println("	corpusPath	path to the corpus root directory");
//			System.out.println("	indexPath	path to the index directory");
//			System.out.println("	<recursiveFlag>	\"r\" (without quotes) to select directories recursively [OPTIONAL]");
//			System.exit(1);
//		}
//		HashMap<File, File> toIndex = new HashMap<File, File>();
//		File root_corpus = new File(args[0]);
//		String root_index = new File (Paths.get(args[1]).normalize().toString()).getAbsolutePath();
//		boolean recursive;
//		try { recursive = args[2].toLowerCase().equals("r"); }
//		catch (ArrayIndexOutOfBoundsException e) { recursive = false; }
//		if (recursive) 
//		{
//			for (File file : root_corpus.listFiles()) 
//			{
//				if (file.isDirectory())
//				{
//					toIndex.put(new File(root_index + "_" + file.getName()), new File(root_corpus + File.separator + file.getName()));
//				}
//			}
//		}
//		else { toIndex.put(new File(root_index), root_corpus); }
//
//		Indexer indexer = new Indexer();
//		indexer.indexAllFiles(toIndex);
//	}
	
}
