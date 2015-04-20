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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LimitTokenCountAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;


/** 
 * Class for adding text files to a Lucene index. 
 * 
 * @author	katarina.boland@gesis.org
 * @version	2014-01-27
 */
public class Indexer 
{

	/*
	 * kba: That was the old value of {@link IndexWriter.MaxFieldLength.LIMITED}
	 */
	private static final int MAX_TOKEN_COUNT = 10000;
	
	private Logger log = LoggerFactory.getLogger(Indexer.class);
	
	private static final Analyzer analyzer =  new LimitTokenCountAnalyzer(new CaseSensitiveStandardAnalyzer(), MAX_TOKEN_COUNT);

	public static Analyzer getAnalyzer() {
		return analyzer;
	}

	/**
	 * Converts all text files in a directory and all subdirectories to lucene documents and 
	 * adds them to a lucene index.
	 * 
	 * @param writer	lucene IndexWriter instance to add the document(s) to
	 * @param file		the location of the text document(s) to be added to the index
	 * @throws IOException
	 */
	public void indexDocs(IndexWriter writer, File file) throws IOException 
	{
		// do not try to index files that cannot be read
		if (file.canRead()) 
		{
			//call indexDocs recursively to index all documents in all subdirectories
			if (file.isDirectory()) 
			{
				String[] files = file.list();
				// an IO error could occur
				if (files != null) 
				{
					for (int i = 0; i < files.length; i++) { indexDocs(writer, new File(file, files[i])); }
				}
			} 
			else 
			{
				System.out.println("adding " + file);
				try { writer.addDocument(FileDocument.Document(file)); }
				// at least on windows, some temporary files raise this exception with an "access denied" message
				// checking if the file can be read doesn't help
				catch (FileNotFoundException fnfe) { ;}
			}
		}
	}
	
	/**
	 * Initializes the Lucene IndexWriter instance, starts the indexing process and prints some status 
	 * information. 
	 * 
	 * @param fileMap	A map listing index output locations (keys) and input directories (values) containing the documents to index	
	 */
	public void indexAllFiles(HashMap<File, File> fileMap)
	{
		for (File indexDir : fileMap.keySet())
		{
			File docDir = fileMap.get(indexDir);

			System.out.println("start");
			if (indexDir.exists()) 
			{
				System.out.println("Cannot save index to '" + indexDir + "' directory, please delete it first");
				continue;
			}
			IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_35, getAnalyzer());
			indexWriterConfig.setOpenMode(OpenMode.CREATE);

			Date start = new Date();
			try 
			{
				IndexWriter writer = new IndexWriter(FSDirectory.open(indexDir), indexWriterConfig);
				log.debug("Indexing to directory '{}'...", indexDir);
				indexDocs(writer, docDir);
				log.debug("Merging all Lucene segments ...");
				writer.forceMerge(1);
				writer.close();
				Date end = new Date();
				log.debug(end.getTime() - start.getTime() + " total milliseconds");
			} 
			catch (IOException e) 
			{
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
			System.out.println("end");
		}
	}
  
	
	/** 
	 * Selects either all subdirectories in the specified directory (recursive mode) or only the root 
	 * directory, specifies a path for the indexes to be created and starts the indexing process. 
	 * 
	 * @param	args	args[0]: path to the corpus root directory; args[1]: path to the index directory; args[2]: "r" to set recursive mode
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: Indexer <corpusPath> <indexPath> <recursiveFlag>");
			System.out.println("	corpusPath	path to the corpus root directory");
			System.out.println("	indexPath	path to the index directory");
			System.out.println("	<recursiveFlag>	\"r\" (without quotes) to select directories recursively [OPTIONAL]");
			System.exit(1);
		}
		HashMap<File, File> toIndex = new HashMap<File, File>();
		File root_corpus = new File(args[0]);
		String root_index = new File (Paths.get(args[1]).normalize().toString()).getAbsolutePath();
		boolean recursive;
		try { recursive = args[2].toLowerCase().equals("r"); }
		catch (ArrayIndexOutOfBoundsException e) { recursive = false; }
		if (recursive) 
		{
			for (File file : root_corpus.listFiles()) 
			{
				if (file.isDirectory())
				{
					toIndex.put(new File(root_index + "_" + file.getName()), new File(root_corpus + File.separator + file.getName()));
				}
			}
		}
		else { toIndex.put(new File(root_index), root_corpus); }

		Indexer indexer = new Indexer();
		indexer.indexAllFiles(toIndex);
	}
}
