package patternLearner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;  
import java.util.List;  
import java.util.ArrayList;
import static java.nio.file.StandardCopyOption.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import searching.Search_Term_Position;

/**
 * Class containing various utility functions and definitions. 
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
public class Util
{
	public static String yearRegex = new String("(\\d{4})");
	public static String percentRegex = new String("\\d+[.,]?\\d*%");
	public static String numberRegex = new String("\\d+[.,]?\\d*");
	public static Pattern[] patterns = getContextMinerYearPatterns();
	public static String delimiter_csv = "|";
	public static String delimiter_internal = "--@--";
	public static String[] enumeratorList = {",", ";", "/", "\\\\"};
	public static String urlPatString = "((\\w+?://)|(www.*?\\.)).+\\.[a-zA-Z][a-zA-Z][a-zA-Z]*";
	public static Pattern urlPat = Pattern.compile(urlPatString);
	// restricts study names to contain at most 3 words (and at least 3 characters)
	//public static String studyRegex_ngram = new String("(\\S+?\\s?\\S+?\\s?\\S+?)");
	// restricts study names to contain at most 5 words (and at least 3 characters)
	public static String studyRegex_ngram = new String("(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)");
	//public static String studyRegex_ngram = new String("(\\S*?\\s?\\S*?\\s?\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?\\s?\\S*?\\s?\\S*?)");
	// restricts study names to contain at most 6 words (and at least 3 characters)
	// slower and does not yield better results: 5 words seem to be optimal
	//public static String studyRegex_ngram = new String("(\\S*?\\s?\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)");
	// word = any char sequence not containing whitespace (punctuation is seen as part of the word here)
	public static String studyRegex = new String("(\\S+?)");
	// use atomic grouping where possible to prevent catastrophic backtracking
	public static String wordRegex = new String("\\S+?");
	public static String wordRegex_atomic = new String("\\S++");
	// use greedy variant for last word - normal wordRegex would only extract first character of last word
	public static String lastWordRegex = new String("\\S+");
	
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
		  buf.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<contexts>\n");
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
		  buf.write("\n</contexts>\n");
		  buf.close();
	  }
	
	/**
	 * Returns a list of patterns for extracting numerical information. 
	 * 
	 * Patterns should be sorted by priority / reliability (highest priority first), first match is accepted 
	 * by calling method. This way, you can give year specifications a higher weight than other 
	 * number specifications, for example. Currently, only one pattern is used.
	 * 
	 * @return	a list of patterns
	 */
	public static Pattern[] getContextMinerYearPatterns() 
	{
		Pattern[] patterns = new Pattern[1];

		String enumRegex = "(([,;/&\\\\])|(and)|(und))";
		String yearRegex = "(\\d{4})";
		String yearAbbrRegex = "('\\d\\d)";
		String numberRegex = "(\\d+[.,]?\\d*)"; //this includes yearRegex
		String rangeRegex = "(([-–])|(bis)|(to)|(till)|(until))";
		
		String numericInfoRegex = "(" + yearRegex + "|" + yearAbbrRegex + "|" + numberRegex + ")";
		String enumRangeRegex = "(" + enumRegex + "|" + rangeRegex + ")";
		String complexNumericInfoRegex = "(" + numericInfoRegex + "(\\s*" + enumRangeRegex + "\\s*" + numericInfoRegex + ")*)";

		patterns[0] = Pattern.compile(complexNumericInfoRegex);
		return patterns;
	}
	
	/**
	 * Returns a selection of words to be treated as stopwords by the reference extraction algorithm
	 * 
	 * @return	a list of stopwords (English and German)
	 */
	public static HashSet<String> stopwordList()
	{
		HashSet<String> stopwords = new HashSet<String>();
		stopwords.add("the");
		stopwords.add("and");
		stopwords.add("on");
		stopwords.add("in");
		stopwords.add("at");
		stopwords.add("from");
		stopwords.add("to");
		stopwords.add("with");
		stopwords.add("for");
		stopwords.add("as");
		stopwords.add("by");
		stopwords.add("of");
		stopwords.add("that");
		stopwords.add("which");
		stopwords.add("who");
		stopwords.add("than");
		
		stopwords.add("als");
		stopwords.add("durch");
		stopwords.add("wegen");
		stopwords.add("der");
		stopwords.add("des");
		stopwords.add("den");
		stopwords.add("deren");
		stopwords.add("dessen");
		stopwords.add("dem");
		stopwords.add("die");
		stopwords.add("das");
		stopwords.add("fuer");
		stopwords.add("für");
		stopwords.add("und");
		stopwords.add("in");
		stopwords.add("im");
		stopwords.add("zu");
		stopwords.add("zur");
		stopwords.add("zum");
		stopwords.add("um");
		stopwords.add("und");
		stopwords.add("mit");
		stopwords.add("ohne");
		stopwords.add("aus");
		stopwords.add("von");
		stopwords.add("vom");
		stopwords.add("nach");
		stopwords.add("zwischen");
		stopwords.add("über");
		stopwords.add("unter");
		stopwords.add("neben");
		stopwords.add("vor");
		stopwords.add("hinter");
		// todo: use list, e.g.  http://de.wiktionary.org/wiki/Wiktionary:Deutsch/Liste_der_Pr%C3%A4positionen
		// + Konjunktionen	
		// placeholders inserted into contexts
		stopwords.add("<year>");
		stopwords.add("<percent>");
		stopwords.add("<number>");
		return stopwords;
	}
	
	/**
	 * Removes all characters that are not allowed in filenames (on windows filesystems).
	 * 
	 * @param seed	the string to be escaped
	 * @return		the escaped string
	 */
	public static String escapeSeed( String seed )
	{
		return seed.replace(":", "_").replace("\\", "_").replace("/", "_").replace("?",  "_").replace(">",  "_").replace("<",  "_");
	}
	
	
	/**
	 * Escapes a string for integration into XML files.
	 * 
	 * @param string	the string to be escaped
	 * @return			the escaped string
	 */
	public static String escapeXML(String string)
	{
	    String xml10pattern = "[^"
                    + "\u0009\r\n"
                    + "\u0020-\uD7FF"
                    + "\uE000-\uFFFD"
                    + "\ud800\udc00-\udbff\udfff"
                    + "]";
		return StringEscapeUtils.escapeXml(string).replaceAll(xml10pattern,"");
	}
	
	/**
	 * Returns an escaped XML string into its normal string representation.
	 * 
	 * @param string	the string to be transformed
	 * @return			the transformed string
	 */
	public static String unescapeXML(String string)
	{
		return StringEscapeUtils.unescapeXml(string);
	}
	
	//TODO: change name to denormalizeRegex or something similar...
	/**
	 * Replaces previously inserted placeholders for years, numbers and percent specifications with their 
	 * regular expressions.
	 * 
	 * @param string	input text where placeholders shall be replaced
	 * @return			string with placeholders replaced by regular expressions
	 */
	public static String escapeRegex(String string)
	{	
		String yearNorm = new String("<YEAR>");
		String percentNorm = new String("<PERCENT>");
		String numberNorm = new String("<NUMBER>");
		string = string.replace(yearNorm, yearRegex).replace(percentNorm, percentRegex).replace(numberNorm, numberRegex);
		//return Pattern.quote(string);
		return string;
	}
	
	public static String normalizeRegex(String term)
	{
		Pattern yearPat = Pattern.compile(yearRegex);
		Pattern percentPat = Pattern.compile(percentRegex);
		Pattern numberPat = Pattern.compile(numberRegex);
		
		String yearNorm = new String("<YEAR>");
		String percentNorm = new String("<PERCENT>");
		String numberNorm = new String("<NUMBER>");
		
		// do not change order of replacements
		Matcher percentMatcher = percentPat.matcher(term);
		term = percentMatcher.replaceAll(percentNorm);
		
		Matcher yearMatcher = yearPat.matcher(term);
		term = yearMatcher.replaceAll(yearNorm);
		
		Matcher numberMatcher = numberPat.matcher(term);
		term = numberMatcher.replaceAll(numberNorm);
		
		return term;
	}
	
	/**
	 * Replaces previously inserted placeholders for years, numbers and percent specifications with their 
	 * regular expressions and quotes all parts of the regular expression that are to be treated as 
	 * strings (all but character classes).
	 * 
	 * @param string	input text where placeholders shall be replaced and all literals quoted
	 * @return			quoted regular expression string
	 */
	public static String normalizeAndEscapeRegex(String string)
	{	
		String yearNorm = new String("<YEAR>");
		String percentNorm = new String("<PERCENT>");
		String numberNorm = new String("<NUMBER>");
		string = Pattern.quote(string).replace(yearNorm, "\\E" + yearRegex + "\\Q").replace(percentNorm, "\\E" + percentRegex + "\\Q").replace(numberNorm, "\\E" + numberRegex + "\\Q");
		return string;
	}
	
	/**
	 * Normalizes and escapes strings for usage as Lucene queries. 
	 * Replaces placeholders by wildcards, removes characters with special meanings in Lucene and 
	 * normalizes the query using the Lucene Analyzer used for building the Lucene index.
	 * 
	 * @param string	input string to be used as Lucene query
	 * @return			a Lucene query string
	 */
	public static String normalizeAndEscapeRegex_lucene(String string)
	{
		string = string.trim();
		String yearPat = new String("<YEAR>");
		String percentPat = new String("<PERCENT>");
		String numberPat = new String("<NUMBER>");
		
		string = string.replaceAll(yearPat, "*").replaceAll(percentPat, "*").replaceAll(numberPat, "*");
		String[] specialChars = {"+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":", "\\"};
		for (String c : specialChars) { string = string.replace(c, "\\" + c) ; }
		string = Search_Term_Position.normalizeQuery(string);
		return string;
	}

	/**
	 * Merge all context files in directory to filename.
	 * 
	 * Used for frequency-based bootstrapping baseline 1.
	 * 
	 * @param directory	input directory
	 * @param filename	ouput filename (in directory)
	 */
	public static void mergeContexts(String directory, String filename, String prefix) throws IOException
	{
		String content = getMergedContexts(directory, prefix);
		OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(new File (directory + File.separator + filename),true), "UTF-8");
	    BufferedWriter out = new BufferedWriter(fstream);
	    out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.getProperty("line.separator") + "<contexts>" + System.getProperty("line.separator"));
	    out.write(content);
	    out.write(System.getProperty("line.separator") + "</contexts>");
	    out.close();
	}

	
	/**
	 * Merges all new contexts in directory to filename. 
	 * 
	 * @param directory	input directory
	 * @param filename 	output filename (in directory)
	 */
	public static void mergeNewContexts(String directory, String filename, String excludePrefix) throws IOException
	{
		String content = getMergedNewContexts(directory, excludePrefix);
		OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(new File(directory + File.separator + filename),true), "UTF-8");
		BufferedWriter out = new BufferedWriter(fstream);
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.getProperty("line.separator") + "<contexts>" + System.getProperty("line.separator"));
		out.write(content);
		out.write(System.getProperty("line.separator") + "</contexts>");
		out.close();
	}
	
	/**
	 * Merges all new contexts in directory with all previously found contexts (removes duplicates) and 
	 * writes to filename. 
	 * 
	 * Used for frequency-based bootstrapping baseline 3.
	 * 
	 * @param directory	input directory
	 * @param filename 	output filename (in directory)
	 */
	public static void mergeAllContexts(String directory, String filename, String prefix) throws IOException
	{
		moveSeenContexts(directory, prefix);
		String content = getMergedContexts(directory, prefix);
		OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(new File(directory + File.separator + filename),true), "UTF-8");
		BufferedWriter out = new BufferedWriter(fstream);
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.getProperty("line.separator") + "<contexts>" + System.getProperty("line.separator"));
		out.write(content);
		out.write(System.getProperty("line.separator") + "</contexts>");
		out.close();
	}
	
	/**
	 * Moves all seen contexts from previous iteration to folder of current iteration. 
	 * 
	 * @param directory	...
	 */
	public static void moveSeenContexts(String directory, String prefix) throws IOException
	{
    	File contextFileCorpus = new File(directory);
    	String[] contextFiles = contextFileCorpus.list();
    	List<String> contextFileList = Arrays.asList(contextFiles);
    	File oldContextFileCorpus = contextFileCorpus.getParentFile();
    	String[] oldContextFiles = oldContextFileCorpus.list();
    	if (contextFiles == null) {
    	    // Either dir does not exist or is not a directory
    	} else 
    	{
    	    for (int i=0; i<oldContextFiles.length; i++) 
    	    {
    	        // Get filename of file or directory
    	    	if (oldContextFiles[i].endsWith(".xml") & !(oldContextFiles[i].startsWith(prefix)) & !(oldContextFiles[i].startsWith("all.xml")) & !(oldContextFiles[i].startsWith("allNew.xml")))
    	    	{
    	    		// if context has not been found again in new iteration
    	    		if (!contextFileList.contains(oldContextFiles[i]))
    	    		{	
    	    			// move context to current directory
    	    	    	Path source = Paths.get(contextFileCorpus.getParent() + File.separator + oldContextFiles[i]);
    	    	    	Path destination = Paths.get(directory);
    	    	    	Files.move(source, destination.resolve(source.getFileName()), REPLACE_EXISTING); 
    	    	    }
    	    	}
    	    }
    	}
	}
	
	/**
	 * ...
	 * 
	 * @param directory	...
	 * @return			...
	 */
	public static String getMergedNewContexts(String directory, String excludedPrefix) throws IOException
	{
    	File contextFileCorpus = new File(directory);
    	String[] contextFiles = contextFileCorpus.list();
    	File oldContextFileCorpus = contextFileCorpus.getParentFile();
    	String[] oldContextFiles = oldContextFileCorpus.list();
    	List<String> oldContextFileList = Arrays.asList(oldContextFiles);
    	List<String> seenFileList = new ArrayList<String>();
    	String xmlContents = "";
    	if (contextFiles == null) {
    	    // Either dir does not exist or is not a directory
    	} else 
    	{
    	    for (int i=0; i<contextFiles.length; i++) {
    	        // Get filename of file or directory
    	    	if (contextFiles[i].endsWith(".xml") & !(contextFiles[i].startsWith(excludedPrefix)))
    	    	{
    	    		//if seed has not already been processed in previous iterations
    	    		if (! oldContextFileList.contains(contextFiles[i]))
    	    		{	
	    	    		File f = new File(directory + File.separator + contextFiles[i]);
		    	    	InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF8");
		    	    	BufferedReader reader = new BufferedReader(isr);
		    	    	StringBuffer contents = new StringBuffer();
		    	    	String text = null;
		    	    	// dis.available() returns 0 if the file does not have more lines.
		    	    	while ((text = reader.readLine()) != null) {
		    	    	   	if (! (text.startsWith("</contexts>") | text.startsWith("<?xml") | text.startsWith("<contexts>")))
		    	    	   	{
		    	    	   		contents.append(text).append(System.getProperty("line.separator"));
		    	    	   	}
		    	    	}
		    	    	reader.close();
		    	    	xmlContents = xmlContents + new String(contents) + System.getProperty("line.separator");
    	    		}
    	    		else { seenFileList.add(contextFiles[i]); }
    	    	}
    	    }
    	}
    	// move seen files to current directory
    	Path source;
    	Path destination;
    	for ( String seenFilename : seenFileList )
    	{
    		source = Paths.get(contextFileCorpus.getParent() + File.separator + seenFilename);
    		destination = Paths.get(directory);
    		Files.move(source, destination.resolve(source.getFileName()), REPLACE_EXISTING); 
    	}
    	return xmlContents;
	}
	

	/**
	 * bl1, 
	 * 
	 * @param directory	...
	 * @return			...
	 */
	public static String getMergedContexts(String directory, String prefix) throws IOException
	{
    	File contextFileCorpus = new File(directory);
    	String[] contextFiles = contextFileCorpus.list();
    	String xmlContents = "";
    	if (contextFiles == null) {
    	    // Either dir does not exist or is not a directory
    	} else 
    	{
    	    for (int i=0; i<contextFiles.length; i++) {
    	        // Get filename of file or directory
    	    	if (contextFiles[i].endsWith(".xml") & !(contextFiles[i].startsWith(prefix)) & !(contextFiles[i].startsWith("all.xml")) & !(contextFiles[i].startsWith("allNew.xml")))
    	    	{
    	    		File f = new File(directory + File.separator + contextFiles[i]);
	    	    	InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF-8");
	    	    	BufferedReader reader = new BufferedReader(isr);
	    	    	StringBuffer contents = new StringBuffer();
	    	    	String text = null;
	    	    	// dis.available() returns 0 if the file does not have more lines.
	    	    	while ((text = reader.readLine()) != null) {
	    	    		if (! (text.startsWith("</contexts>") | text.startsWith("<?xml") | text.startsWith("<contexts>")))
	    	    	    {
	    	    	    	contents.append(text).append(System.getProperty("line.separator"));
	    	    	    }
	    	    	}
	    	    	reader.close();
	    	    	xmlContents = xmlContents + new String(contents) + System.getProperty("line.separator");
    	    	}
    	    }
    	}
    	return xmlContents;
	}
	
	/**
	 * ...
	 * 
	 * @param f	...
	 * @return	...
	 * @throws IOException
	 */
	public static HashSet<String> getDisctinctPatterns(File f) throws IOException
	{
		ArrayList<String> patList = (ArrayList<String>)FileUtils.readLines(f, "UTF-8");
		HashSet<String> patternSet = new HashSet<String>();
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
		HashSet<String> contextSet = getDisctinctPatterns(f_in);
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
		HashSet<String> contextSet = new HashSet<String>();
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
		HashSet<String> filenameSet = getDisctinctPatterns(f_in);
		HashSet<String> filenameSetDistinct = new HashSet<String>();
		OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(f_out), "UTF-8");
	    BufferedWriter out = new BufferedWriter(fstream);
	    for (String filename : filenameSet) 
	    { 
	    	filenameSetDistinct.add(new File(filename).getAbsolutePath());
	    }
	    for (String distinctFilename: filenameSetDistinct) { out.write(distinctFilename + System.getProperty("line.separator")); } 
	    out.close();
	}
	
	/**
	 * ...
	 * 
	 * @param args	...
	 */
	public static void main(String[] args)
	{
		//mergeContexts(args[0]);
		//mergeProcessedContexts(args[0]);
		try{
			getDistinct(new File(args[0]),
						new File(args[1]));
		}
		catch (IOException ioe) { ioe.printStackTrace(); }
		/*try{
			getDistinctContexts(new File(args[0]),
						new File(args[1]));
		}
		catch (IOException ioe) { ioe.printStackTrace(); }*/
		/*try {
		getDistinctFilenames(new File(args[0]),
					new File(args[1]));
		}
		catch (IOException ioe) { ioe.printStackTrace(); }
		*/
	}

}
