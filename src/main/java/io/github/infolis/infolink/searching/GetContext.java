package io.github.infolis.infolink.searching;

import io.github.infolis.infolink.patternLearner.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Class for retrieving the context ( = surrounding words) of a term in a document. 
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 * 
 */
public class GetContext
{

	/**
	 * Returns the words surrounding a given position in the text
	 * 
	 * @param filename		name of the text file
	 * @param term			the term whose contexts to retrieve
	 * @param position		position of the term
	 * @param windowsize	number of neighboring words to retrieve 
	 * @param contextFile	name of the output file
	 */
	GetContext(String filename, String term, int position, int windowsize, File contextFile) throws IOException
	{
		try 
		{
			File fi = new File(filename);
			// call different method here if document is not encoded in utf-8
			ArrayList<ArrayList<String>> contexts = getContexts_utf8(fi, term);
			OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(contextFile,true), "UTF-8");
		    BufferedWriter out = new BufferedWriter(fstream);
		    
		    ArrayList<String> leftContexts = contexts.get(0);
		    ArrayList<String> rightContexts = contexts.get(1);
		    // assert: leftContexts and rightContexts must always have the same size
		    for (int i = 0; i < leftContexts.size(); i ++)
		    {
		    	String leftContext = io.github.infolis.infolink.patternLearner.Util.escapeXML(leftContexts.get(i));
		    	String rightContext = io.github.infolis.infolink.patternLearner.Util.escapeXML(rightContexts.get(i));
		    	out.write("\t<context term=\"" + io.github.infolis.infolink.patternLearner.Util.escapeXML(term) + "\" document=\"" + filename + "\">\n\t\t<leftContext>" + leftContext +"</leftContext>\n\t\t<rightContext>" + rightContext + "</rightContext>\n\t</context>\n");
		    }
		    out.close();
		}
		catch (FileNotFoundException e) {e.printStackTrace();}
		catch(IOException ioe) {ioe.printStackTrace();}
	}
	
	/**
	 * Retrieves the contexts of a word at a given position in a unicode document - random access files 
	 * are not applicable here. Instead, input text is split into a word list. Only works in 
	 * conjunction with WhitespaceAnalyzer!
	 * 
	 * @param f				input text file
	 * @param query			term whose contexts to extract
	 * @param position		position of the term for which the context to retrieve
	 * @param windowsize	number of surrounding words to retrieve as context
	 * @return 				array of context (2 elements: [0]: left context, [1]: right context)
	 * @throws IOException
	 */
	public String[] getContexts_utf8_whitespaceAnalyzer(File f, String query, int position, int windowsize) throws IOException
	{
		InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF8");
		BufferedReader reader = new BufferedReader(isr);
	    StringBuffer contents = new StringBuffer();
	    String text = null;
	    while ((text = reader.readLine()) != null) {
	    	  contents.append(text).append(System.getProperty("line.separator"));
	    }
	    reader.close();
	    text = new String(contents);
	    
	    // substitutes all kinds of whitespace by " "!
	    // in order to get correct positions, input must be tokenized using the same tokenizer as for the indexing...
	    // code below can be used only if whitespace analyzer is used for building the index and searching!
	    // (else there will be a mismatch in positions)
	    
		String[] wordArray = text.split("\\s+");
		List<String> wordList = Arrays.asList(wordArray); 
		// handle phrase queries here
		String[] queryWordList = query.split("\\s+");
		int numWordsInQuery = queryWordList.length;
		for (int i = 0; i < numWordsInQuery; i++)
		{
			// check whether term at given position equals query term
			String term = wordArray[position + i];
			if (!term.equals(queryWordList[i]))
			{
				System.err.println("Warning: could not find term at given position!");
				System.err.println(term);
				System.err.println(query);
			}
		}
		List<String> leftContextList = wordList.subList(position-windowsize/2, position);
		List<String> rightContextList = wordList.subList(position+numWordsInQuery, position+numWordsInQuery+windowsize/2);
		Iterator<String> wordIter = leftContextList.iterator();
		String leftContext = "";
		String rightContext = "";
		while (wordIter.hasNext())
		{
			String curWord = wordIter.next();
			leftContext += " " + curWord;
		}
		leftContext = leftContext.trim();
		wordIter = rightContextList.iterator();
		while (wordIter.hasNext())
		{
			String curWord = wordIter.next();
			rightContext += " " + curWord;
		}
		rightContext = rightContext.trim();
		String[] contexts = new String[2];
		contexts[0] = leftContext;
		contexts[1] = rightContext;
		return contexts;
	}
	   
	
	/**
	 * Retrieves the contexts of words in unicode documents - random access files are not applicable here.
	 * Instead, regular expressions are used for retrieving the contexts.
	 * 
	 * @param f		input text file
	 * @param query	term whose contexts to extract
	 * @return 		a list of contexts (format: ...)
	 * @throws IOException
	 */
	public ArrayList<ArrayList<String>> getContexts_utf8(File f, String query) throws IOException
	{
		InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF8");
		BufferedReader reader = new BufferedReader(isr);
	    StringBuffer contents = new StringBuffer();
	    String text = null;
	    while ((text = reader.readLine()) != null) {
	    	  contents.append(text).append(System.getProperty("line.separator"));
	    }
	    reader.close();
	    text = new String(contents);
	 
	    // search for phrase using regex
	    // first group: left context (consisting of 5 words)
	    // second group: right context (consisting of 5 words)
	    // contexts may or may not be separated from the query by whitespace!
	    // e.g. "Eurobarometer-Daten" with "Eurobarometer" as query term
	    String leftContextPat = "(" + Util.wordRegex + "\\s+" + Util.wordRegex + "\\s+" + Util.wordRegex + "\\s+" + Util.wordRegex + "\\s+" + Util.wordRegex + "\\s*?" + ")";
	    String rightContextPat = "(\\s*?" + Util.wordRegex + "\\s+" + Util.wordRegex + "\\s+" + Util.wordRegex + "\\s+" + Util.wordRegex + "\\s+" + Util.lastWordRegex + ")";
	    // pattern should be case-sensitive! Else, e.g. the study "ESS" would be found in "vergessen"...
	    // Pattern pat = Pattern.compile( leftContextPat + query + rightContextPat, Pattern.CASE_INSENSITIVE );
	    Pattern pat = Pattern.compile(leftContextPat + Pattern.quote(query) + rightContextPat);
	    Matcher m = pat.matcher(text);
	    ArrayList<String> leftContexts = new ArrayList<String>();
	    ArrayList<String> rightContexts = new ArrayList<String>();
	    boolean matchFound = m.find();
	    if (matchFound == false) 
	    
	    {	//TODO: this checks for more characters than actually replaced by currently used analyzer - not neccessary 
	    	query = query.replace("-", " ").replace("–", " ").replace(".", " ").replace("(", " ").replace(")", " ").replace(":", " ").replace(",", " ").replace(";", " ").replace("/", " ").replace("\\", " ").replace("&", " ").replace("_", "");
	    	System.out.println(Pattern.quote(query.replace(" ", "[\\s\\-–\\\\/:.,;()&_]")));
	    	pat = Pattern.compile( leftContextPat + Pattern.quote(query.replace(" ", "[\\s\\-–\\\\/:.,;()&_]")) + rightContextPat, Pattern.CASE_INSENSITIVE );
	    	m = pat.matcher(text);
	    	matchFound = m.find();
	    }
	    while (matchFound)
	    {
	    	String leftContext = m.group(1).trim();
	    	String rightContext = m.group(2).trim();
	    	leftContexts.add(leftContext);
	    	rightContexts.add(rightContext);
	    	matchFound = m.find();
	    }
	    ArrayList<ArrayList<String>> contextList = new ArrayList<ArrayList<String>>();
	    contextList.add(leftContexts);
	    contextList.add(rightContexts);
	    return contextList;
	}
	
	/**
	 * Retrieves the left context of a term using a RandomAccessFile.
	 * 
	 * @param f				RandomAccessFile to retrieve the contexts from
	 * @param position		start position of term
	 * @param windowsize	size of the context to retrieve (number of words)
	 * @return				the left context (words preceding term at position position) as String
	 * @throws IOException
	 */
	public String getLeftContext(RandomAccessFile f, int position, int windowsize) throws IOException
	{
		boolean lastCharWhitespace = false;
		boolean firstChar = true;
		String content = new String();
		f.seek(position-1);
		int numWords = 0;
		while (true)
		{
			char c = (char) f.readByte();
			if (Character.isWhitespace(c))
			{
				if (firstChar)
				{
					firstChar = false;
					lastCharWhitespace = true;
					long currentPos = f.getFilePointer();
					f.seek(currentPos-2);
					continue;
				}
				if (lastCharWhitespace == false)
				{
					numWords += 1;
					if (numWords >= windowsize) { break; }
					lastCharWhitespace = true;
				}
				else
				{
					// ignore current character and proceed
					long currentPos = f.getFilePointer();
					f.seek(currentPos-2);
					continue;
				}
			}
			else { lastCharWhitespace = false; }
			// insert current character BEFORE last read chars
			content = c + content;
			long currentPos = f.getFilePointer();
			f.seek(currentPos-2);
			
		}
		return content;
	}
	
	/**
	 * Retrieves the right context of a term using a RandomAccessFile.
	 * 
	 * @param f				RandomAccessFile to retrieve the contexts from
	 * @param term			term whose right context to retrieve
	 * @param position		start position of term
	 * @param windowsize	size of the context to retrieve (number of words)
	 * @return				the right context (words following term at position position) as String
	 * @throws IOException
	 */
	public String getRightContext(RandomAccessFile f, String term, int position, int windowsize) throws IOException
	{
		boolean lastCharWhitespace = false;
		boolean firstChar = true;
		String content = new String();
		f.seek(position + term.length());
		int numWords = 0;
		while (true)
		{
			char c = (char) f.readByte();
			
			if (Character.isWhitespace(c))
			{
				if (firstChar)
				{
					firstChar = false;
					lastCharWhitespace = true;
					continue;
				}
				if (lastCharWhitespace == false)
				{
					numWords += 1;
					if (numWords >= windowsize) { break; }
					lastCharWhitespace = true;
				}
				// ignore current character and proceed
				else { continue; }
			}
			else { lastCharWhitespace = false; }
			content += String.valueOf(c);
		}
		return content;
	}
	
	
	/**
	 * Writes the context (surrounding words) of a term at a given position in the text to an XML file. 
	 * 
	 * @param	filename	name of the text file
	 * @param	term		term whose context shall be retrieved
	 * @param	position	position of the term whose context shall be retrieved
	 * @param	windowsize	size of bytes to read (including term)
	 * @param	contextFile	output XML file
	 */
	public void GetContext_bytes(String filename, String term, int position, int windowsize, File contextFile)
	{
		try 
		{
			RandomAccessFile f = new RandomAccessFile(filename,"r");
			final byte[] content = new byte[windowsize];
			int startpos = position - windowsize / 2;
			if (startpos < 0) {	startpos = 0; }
			f.seek(startpos);
			f.read(content, 0, windowsize);
			f.close();
			
			OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(contextFile,true), "UTF-8");
		    BufferedWriter out = new BufferedWriter(fstream);
		    out.write("\t<context term=\"" + term + "\" document=\"" + filename + "\">" + new String(content,"UTF8") +"</context>\n");
		    out.close();
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch(IOException ioe) { ioe.printStackTrace(); }
	}
}
