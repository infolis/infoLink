package io.github.infolis.infolink.preprocessing;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.file.Paths;


/**
 * Cleans messy text files (text automatically extracted from pdf documents).
 * 
 * @author katarina.boland@gesis.org
 * @author farag.saad@gesis.org (remove_line_break method)
 * @version 2014-01-29
 */
public class Cleaner
{
	File file;
	String outDirName;
	
	/**
	 * Class constructor specifying the text file to clean and the output directory for cleaned 
	 * files.
	 * 
	 * @param f					the text file to be cleaned
	 * @param outDirectoryName	output directory for cleaned files
	 */
	Cleaner(File f, String outDirName) 
	{
		this.file = f;
		this.outDirName = outDirName;
	}
	
	/**
	 * Applies methods to resolve hyphenation and remove control sequences.
	 *  
	 * @throws IOException
	 */
	public void clean() throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.file), "UTF-8"));
	    StringBuffer contents = new StringBuffer();
	    String text = null;
	    while ((text = reader.readLine()) != null) 
	    {
	    	contents.append(text).append(System.getProperty("line.separator"));
	    }
	    reader.close();
	    text = new String(contents);
	    System.out.println("Cleaning " + this.file);
	    String cleaned_text = removeLineBreaks(text);
	    // save cleaned files in the specified output directory, mark their origin by adding 
	    // the source directory name as prefix
	    String trace = Paths.get(this.outDirName).relativize(Paths.get(this.file.getParent())).normalize().toString();
	    String cleanFilePath = new String( this.outDirName + File.separator + trace.replaceAll(Pattern.quote(".." + File.separator), "").replaceAll(Pattern.quote(File.separator), "_") + "_" );
	    // append the suffix "_clean" to the cleaned copy of the file
	    String cleanFilename = new String( this.file.getName().replaceAll( ".txt","_clean.txt" ) );
	    String cleanFile = new String( cleanFilePath + cleanFilename );
	    System.out.println("Writing " + cleanFile);
	    OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(cleanFile), "UTF8");
	    BufferedWriter out = new BufferedWriter(w);
	    out.write(removeControlSequences(cleaned_text));
	    out.close();
	}
	
	/**
	 * Removes control sequences that may have been inserted during automatic text extraction.
	 * 
	 * @param text	the text to clean
	 * @return	the text without control sequences
	 */
	public static String removeControlSequences(String text)
	{
		return text.replaceAll("[^\\P{Cc}\\P{Cf}\\P{Co}\\P{Cs}\\P{Cn}\\s]", "");	
	}
	
	/**
	 * Resolves hyphenation and re-assembles words at line breaks.
	 * Tries to not concatenate words connected by "-" coincidentally occurring at a line break.
	 * 
	 * @param content input text to be processed
	 * @return input text without hyphenation at line-breaks
	 * @throws IOException
	 */
	public static String removeLineBreaks(String content) throws IOException {

		// start cleaning the text file
		StringTokenizer loop_content = new StringTokenizer(content, " ");
		String text_content = "", text_content1 = "", text_content2 = "";
		String parsed_content = "";
		while (loop_content.hasMoreTokens()) {

			text_content = loop_content.nextToken();
			int count = text_content.split("\\-", -1).length - 1; int line_break_pos=text_content.indexOf(System.getProperty("line.separator"));
			int Bindestrich =text_content.indexOf("-");
 
			// tackle the line break problem
			if (text_content.contains("¬")) {
				// replace the symbol "¬" with space
				text_content1 = text_content.replace("¬", " ");
				// remove the produced space by the first step and also remove
				// the line break to concatenate the 2 parts of the broken word
				text_content2 = text_content1.trim().replaceAll("\\s+", "");
				// start concatenate the two parts of the broken word and add
				// the line break in the correct place
				parsed_content = parsed_content + text_content2 + " "
						+ System.getProperty("line.separator");
			} else

			if (text_content.contains("-")
					&& text_content.contains(System
							.getProperty("line.separator")) && count <= 1
					&& !Character.isDigit(text_content.charAt(0))  && line_break_pos>Bindestrich) {
				// replace the symbol "-" with space
				text_content1 = text_content.replace('-', ' ');
				// remove the produced space by the first step and also remove
				// the line break to concatenate the 2 parts of the broken word
				text_content2 = text_content1.trim().replaceAll("\\s+", "");
				// start concatenate the two parts of the broken word and add
				// the line break in the correct place
				parsed_content = parsed_content + text_content2 + " "
						+ System.getProperty("line.separator");
			}
			else
			// case of more than one "-" and in same time with linebreak
			if (text_content.contains("-")
					&& text_content.contains(System
							.getProperty("line.separator")) && count > 1
					&& !Character.isDigit(text_content.charAt(0))  && line_break_pos>Bindestrich) { 
				 
				int k=text_content.lastIndexOf("-");
				text_content1 = text_content.substring(0,k)+' '+text_content.substring(k+1);  
				// remove the produced space by the first step and also remove
				// the line break to concatenate the 2 parts of the broken word
				text_content2 = text_content1.trim().replaceAll("\\s+", "");
				// start concatenate the two parts of the broken word and add
				// the line break in the correct place
				parsed_content = parsed_content + text_content2 + " "
						+ System.getProperty("line.separator");
			}
			
			else
				// if no broken word just
				parsed_content = parsed_content + text_content + " ";

		}
		return parsed_content;
	} 

	/**
	 * Applies the clean method recursively to all files in the specified directory and all 
	 * subdirectories. 
	 * 
	 * @param inDirectoryName	name of the parent directory containing all files to be cleaned
	 * @throws IOException
	 */
	public static void cleanFiles (String inDirectoryName, String outDirectoryName) throws IOException
	{
		File inDirectory = new File(inDirectoryName);
		if (inDirectory.isDirectory()) 
		{
	        String[] filenames = inDirectory.list();
	        for (String filename: filenames)
	        {
	        	cleanFiles (inDirectoryName + File.separator + filename + File.separator, outDirectoryName);
	        }
		}
	    else
	    {
	        new Cleaner(inDirectory, outDirectoryName).clean();
	    }	        	
	}

	/**
	 * Applies the cleanFiles method to clean all files in the specified directory.
	 * 
	 * @param argv	argv[0]: path of input directory; argv[1]: path of output directory
	 * @throws IOException
	 */
	public static void main (String[] argv) throws IOException 
	{
		String inDirectoryName = argv[0];
		String outDirectoryName = argv[1];
		cleanFiles(inDirectoryName, outDirectoryName);
		System.out.println("Done cleaning files.\n");
	}
}
