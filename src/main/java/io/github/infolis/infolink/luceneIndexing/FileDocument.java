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

 
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.InfolisFile;

/*import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
//import java.util.StringTokenizer;

/** 
 * Creates a Lucene Document from a File.
 * <p>
 * The document has three fields:
 * <ul>
 * <li><code>path</code>--containing the pathname of the file, as a stored, untokenized field;</li>
 * <li><code>fileName</code>--containing the file name of the file, as a stored, tokenized field;</li>
 * <li><code>modified</code>--containing the last modified date of the file as a field as created by <a
 * href="lucene.document.DateTools.html">DateTools</a>; and</li>
 * <li><code>contents</code>--containing the full contents of the file, as a Reader field;</li>
 * </ul>
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 */
public class FileDocument {

	/**
	 * Class constructor.
	 */
	private FileDocument() {}
	
	/**
	 * Files a lucene document.
	 * Documents are created as follows:
	 * <ol>
	 * <li>The path of the file is added as a field named "path". The field is indexed (i.e. searchable), 
	 * but not tokenized into words.</li>
	 * <li>The last modified date of the file is added as a field named "modified". The field is indexed 
	 * (i.e. searchable), not tokenized into words.</li>
	 * <li>The contents of the file are added to a field named "contents". A reader is specified so that 
	 * the text of the file is tokenized and indexed, but not stored. Note that FileReader expects the file 
	 * to be in the system's default encoding. If that's not the case searching for special characters will 
	 * fail.</li>
	 * <li>Content (text files) is saved in the index along with position and offset information.</li>
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
	  
	  InputStreamReader isr = new InputStreamReader(fileResolver.openInputStream(f) , "UTF8" );
	  BufferedReader reader = new BufferedReader( isr );
      StringBuffer contents = new StringBuffer();
      String text = null;
      while ( (text = reader.readLine() ) != null) 
      { 
    	  contents.append( text ).append( System.getProperty( "line.separator" ) );
      }
      reader.close();
      isr.close();
      text = new String( contents );
	   
      // make a new, empty document
      Document doc = new Document();

      // Add the path of the file as a field named "path".  Use a field that is 
      // indexed (i.e. searchable), but don't tokenize the field into words.
      doc.add( new Field( "path", f.getUri(), Field.Store.YES, Field.Index.NOT_ANALYZED ) );  
      doc.add( new Field( "fileName",  f.getFileName(), Field.Store.YES, Field.Index.ANALYZED ) );
    
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
      doc.add( new Field( "contents", text, Field.Store.YES, Field.Index.ANALYZED, 
    		Field.TermVector.WITH_POSITIONS_OFFSETS ) ); 

      // return the document
      //cd.close();
      return doc;
  }

}
