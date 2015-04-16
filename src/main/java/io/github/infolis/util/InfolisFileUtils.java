package io.github.infolis.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

/**
 * Class containing various utility functions and definitions. 
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
public class InfolisFileUtils
{
	
	/**
	 * Writes the given content to file using the given encoding
	 * 
	 * @param file		the output file
	 * @param encoding	encoding to use for writing the file
	 * @param content	content to write to file
	 * @param append	if set, content will be appended to existing file, else existing file will be overwritten
	 * @throws IOException
	 */
	public static void writeToFile(File file, String encoding, String content, boolean append) throws IOException
	{
		FileUtils.write(file, content + System.getProperty("line.separator"), encoding, append);
	}
	
	/**
	 * Reads a file and return its content as a string
	 * 
	 * @param file		the input file
	 * @param encoding	encoding of the input file
	 * @return			a string representing the content of the file
	 * @throws IOException
	 */
	public static String readFile(File file, String encoding) throws IOException
	{
		return FileUtils.readFileToString(file, encoding);
	}
	
	/**
	   * Adds header and start tag to studyRefFinder output XML file.
	   * 
	   * @throws IOException
	   */
	  public static void prepareOutputFile(String filename) throws IOException
	  {
		  FileWriter writer = new FileWriter(filename);
		  BufferedWriter buf = new BufferedWriter(writer);
		  buf.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.getProperty("line.separator") + "<contexts>" + System.getProperty("line.separator"));
		  buf.close();
	  }

	  /**
	   * Adds end tag to studyRefFinder output XML file.
	   * 
	   * @throws IOException
	   */
	  public static void completeOutputFile(String filename) throws IOException
	  {
		  FileWriter writer = new FileWriter(filename, true);
		  BufferedWriter buf = new BufferedWriter(writer);
		  buf.write(System.getProperty("line.separator") + "</contexts>" + System.getProperty("line.separator"));
		  buf.close();
	  }
	  
		/**
		 * ...
		 * 
		 * @param f	...
		 * @return	...
		 * @throws IOException
		 */
		public static Set<String> getDisctinctPatterns(File f) throws IOException
		{
			ArrayList<String> patList = (ArrayList<String>)FileUtils.readLines(f, "UTF-8");
			Set<String> patternSet = new HashSet<String>();
			patternSet.addAll(patList);
	    	return patternSet;
		}
		
		/**
		 * ...
		 * 
		 * @param f_in	...
		 * @param f_out	...
		 * @throws IOException
		 */
		public static void getDistinct(File f_in, File f_out) throws IOException
		{
			Set<String> contextSet = getDisctinctPatterns(f_in);
		    for (String context : contextSet) 
		    { 
		    	FileUtils.write(f_out, context + System.getProperty("line.separator"), "UTF-8", true);
		    }
		}
		
		/**
		 * ...
		 * 
		 * @param f_in	...
		 * @param f_out	...
		 * @throws IOException
		 */
		public static void getDistinctContexts(File f_in, File f_out) throws IOException
		{
			Set<String> contextSet = new HashSet<String>();
		    boolean inContext = false;
		    String newContext = "";
			InputStreamReader isr = new InputStreamReader(new FileInputStream(f_in), "UTF-8");
	    	BufferedReader reader = new BufferedReader(isr);
	    	String line = null;
	    	while ((line = reader.readLine()) != null) 
	    	{
			    if (line.startsWith("\t<context")) { inContext=true; }
			    else if (line.startsWith("\t</context>")) 
			    { 
			    	inContext=false; 
			    	newContext += line;
			    	contextSet.add(newContext);
			    	newContext = "";
			    }
			    if (inContext == true) { newContext += line + System.getProperty("line.separator"); }
			}
	    	reader.close();
	    	
			OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(f_out), "UTF-8");
		    BufferedWriter out = new BufferedWriter(fstream);
		    for (String context : contextSet)
		    {
		    	out.write(context + System.getProperty("line.separator")); 
		    }
		    out.close();
		}
		
		/**
		 * ...
		 * 
		 * @param f_in	...
		 * @param f_out	...
		 * @throws IOException
		 */
		public static void getDistinctFilenames( File f_in, File f_out ) throws IOException
		{
			Set<String> filenameSet = getDisctinctPatterns(f_in);
			Set<String> filenameSetDistinct = new HashSet<String>();
			OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(f_out), "UTF-8");
		    BufferedWriter out = new BufferedWriter(fstream);
		    for (String filename : filenameSet) 
		    { 
		    	filenameSetDistinct.add(new File(filename).getAbsolutePath());
		    }
		    for (String distinctFilename: filenameSetDistinct) { out.write(distinctFilename + System.getProperty("line.separator")); } 
		    out.close();
		}
	
}
