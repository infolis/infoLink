package patternLearner;

import java.net.URL;
import java.net.URLConnection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
//import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.io.*;
import java.util.HashMap;
import java.math.BigInteger;

/**
 * Class for matching dataset reference strings to entries in the dara dataset repository.
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
public class StudyMatcher 
{
	String searchInterface; 

	/**
	 * Class constructor specifying the repository's search interface base URL.
	 * 
	 * @param searchInterface	the repository's search interface base URL
	 */
	StudyMatcher(String searchInterface)
	{
		this.searchInterface = searchInterface;
	}
	
	/**
	 * Returns the hex representation of the input string <emph>arg</emph>.
	 * 
	 * @param arg	string to be represented in hex value
	 * @return		the hex representation of the input string <emph>arg</emph>
	 * @throws UnsupportedEncodingException
	 */
	public String toHex(String arg) throws UnsupportedEncodingException {
		  return String.format("%x", new BigInteger(1, arg.getBytes("UTF-8")));
		}
	
	/*
	The MIT License

	Copyright (c) 2013 Mashape (http://mashape.com)

	Permission is hereby granted, free of charge, to any person obtaining
	a copy of this software and associated documentation files (the
	"Software"), to deal in the Software without restriction, including
	without limitation the rights to use, copy, modify, merge, publish,
	distribute, sublicense, and/or sell copies of the Software, and to
	permit persons to whom the Software is furnished to do so, subject to
	the following conditions:

	The above copyright notice and this permission notice shall be
	included in all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
	EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
	MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
	NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
	LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
	OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
	WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
	
	package com.mashape.unirest.http.utils;*/

	//TODO: move to separate file with above copyright note
	/**
	 * Class for transforming all parameters in <emph>input</emph> URL into valid URL parameters.
	 * (URLEncoder transforms plain text into the application/x-www-form-urlencoded MIME format 
	 * as described in the HTML specification (GET-style URLs or POST forms) but this 
	 * does not work with the new dara search function)
	 * 
	 * @author Mashape (http://mashape.com)
	 * @version 2013
	 */
	public static class URLParamEncoder {

	    public static String encode(String input) throws UnsupportedEncodingException {
	        StringBuilder resultStr = new StringBuilder();
	        for (char ch : input.toCharArray()) {
	            if (isUnsafe(ch)) {
	                resultStr.append('%');
	                resultStr.append(toHex(ch / 16));
	                resultStr.append(toHex(ch % 16));
	            } else {
	                resultStr.append(ch);
	            }
	        }
	        return resultStr.toString();
	    }

	    private static char toHex(int ch) {
	        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
	    }

	    private static boolean isUnsafe(char ch) {
	        if (ch > 128 || ch < 0)
	            return true;
	        return " %$&+,/:;=?@<>#%".indexOf(ch) >= 0;
	    }

	}

	/**
	 * Constructs a search URL from the base URL and the query.
	 * 
	 * @param searchTerm	the query term
	 * @param maxNumber		the maximum number of hits to be displayed
	 * @return				the search URL
	 * @throws MalformedURLException
	 */
	public URL constructURL(String searchTerm, int maxNumber) throws MalformedURLException
	{
		try	
		{ 
			return new URL(String.format("%s?title=%s&max=%s&lang=de", 
							this.searchInterface, 
							// URLEncoder transforms plain text into the	application/x-www-form-urlencoded MIME format 
							// as described in the HTML specification (GET-style URLs or POST forms)
							// does not work with the new dara search function 
							//URLEncoder.encode(searchTerm, "UTF-8"),
							URLParamEncoder.encode(searchTerm),
							String.valueOf(maxNumber)));
		} 
		catch (UnsupportedEncodingException e) 
		{ 
			e.printStackTrace(); 
			return new URL(String.format("%s?title=%s&max=%s&lang=de", this.searchInterface,searchTerm,String.valueOf(maxNumber)));
		}
	}
	
	/**
	 * Reads and returns the content from the query response page.
	 * 
	 * @param url	the URL to read from
	 * @return		the contents of the page
	 * @throws IOException
	 */
	public String readFromURL(URL url) throws IOException
	{
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        //connection.setConnectTimeout(6000);
        System.out.println("Reading from url...");
        // make sure that all data is read
        byte[] resultBuff = new byte[0];
        byte[] buff = new byte[1024];
        int k = -1;
        while((k = connection.getInputStream().read(buff, 0, buff.length)) > -1) {
            byte[] tbuff = new byte[resultBuff.length + k]; // temp buffer size = bytes already read + bytes last read
            System.arraycopy(resultBuff, 0, tbuff, 0, resultBuff.length); // copy previous bytes
            System.arraycopy(buff, 0, tbuff, resultBuff.length, k);  // copy current lot
            resultBuff = tbuff; // call the temp buffer as your result buff
        }
        System.out.println(resultBuff.length + " bytes read.");
        String content = new String(resultBuff);
        System.out.println("Done reading from url.");
        return content;
    }
	
	/**
	 * Parses the HTML output of the dara search function and returns a map with dataset DOIs (keys) and 
	 * names (values).
	 * 
	 * @param html	dara HTML output
	 * @return		a map containing dataset DOIs (keys) and dataset names (values)
	 */
	public HashMap<String,String> parseHTML(String html)
	{
		HashMap<String,String> matchingStudyMap = new HashMap<String,String>();
		Document doc = Jsoup.parseBodyFragment(html);
		Elements hitlist = doc.getElementsByTag("li");
		for (Element hit : hitlist)
		{
			String studyName = "";
			String studyDoi = "";
			//TODO: search for tag "a" first to limit elements to search by attribute value?
			Elements names = hit.getElementsByAttributeValueMatching("href", "/dara/study/web_show?.*");
			Elements dois = hit.getElementsByAttributeValueContaining("href", "http://dx.doi.org");
			// each entry has exactly one name and one doi element
			//TODO: except for some datasets that are not registered but only referenced in dara!
			// e.g. "OECD Employment Outlook" -> no doi listed here -> ignored
			for (Element name : names) { studyName = name.text().trim(); }
			for (Element doi : dois) { studyDoi = doi.text().trim(); }
			if (studyName != "")
			{
				if (studyDoi != "")
				{
					System.out.println("name: " + studyName);
					System.out.println("doi: " + studyDoi);
					matchingStudyMap.put(studyDoi, studyName);
				}
			}
		}
		return matchingStudyMap;
	}
	
	/**
	 * Reads the cache to find DOIs and names of previously queried dataset names.
	 *  
	 * @param url		the query to find the dataset entries
	 * @param filename	path of the cache file
	 * @return			a map containing dataset DOIs (key) and names (value)
	 */
	public HashMap<String,String> readFromCache(String url, String filename)
	{
		try
		{
    		File f = new File( filename );
    		InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF8");
    		BufferedReader reader = new BufferedReader(isr);
    	    String text = null;
    	    while ((text = reader.readLine()) != null) 
    	    {
    	    	if (text.contains(url.toString())) 
    	    	{
    	    		HashMap<String,String> res = new HashMap<String,String>();
    	    		String[] data = text.split(Util.delimiter_cacheFile);
    	    		// query is in cache but no data can be found in dara - return empty hashmap
    	    		if (data.length < 3) { res.put("", ""); return res; }
    	    		// each query has n dataset names with n dois
    	    		// data[0] = the query, therefore start at index 1
    	    		// end at data.length -2 because data[i+1] is accessed in each iteration
    	    		for ( int i = 1; i < data.length-1; i+=2 )
    	    		{
    	    			// every first entry of pair: study name
    	    			// every second entry of pair: study doi
    	    			res.put(data[i+1], data[i]);
    	    		}
    	    		reader.close(); return res; 
    	    	}
    	    }
    	    reader.close();
    	    return new HashMap<String,String>();
		}
		catch (IOException e) { e.printStackTrace(); return new HashMap<String,String>();}
	}
	
	/**
	 * Searches for matching (= similar to <emph>studyname</emph>) dataset names in 
	 * <emph>externalDatasetPathsFile</emph> listing datasets along with URLs to their landing pages.
	 * 
	 * @param studyname					name of the dataset to be matched
	 * @param externalDatasetPathsFile	path of file listing dataset names and URLs
	 * @return							string representation of a URL pointing to the matching dataset record
	 */
	public String match_external(String studyname, String externalDatasetPathsFile)
	{
		System.out.println(studyname);
		String link = null;
		try
		{
    		File f = new File(externalDatasetPathsFile);
    		InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF8");
    		BufferedReader reader = new BufferedReader(isr);
    	    String text = null;
    	    while ((text = reader.readLine()) != null) 
    	    {
    	    	String[] nameUrl =  text.split(";");
    	    	// studyname might contain additional info, e.g. year specifications
    	    	// therefore search for listed title inside of studyname instead of checking whether both are equal
    	    	if (studyname.contains(nameUrl[0])) { return nameUrl[1]; }
    	    }
    	    reader.close();
		}
		catch (IOException e) { e.printStackTrace(); }
		return link;
	}
	
	/**
	 * Splits <emph>searchTerm</emph> into several terms if enumeration markers are present. 
	 * 
	 * @param searchTerm	string that might represent an enumeration of different terms
	 * @return				array of terms if enumeration markers are present or empty array otherwise
	 */
	String[] getEnumeratedTerms(String searchTerm)
	{
		for (String enumerator : Util.enumeratorList)
		{
			String[] newTerms = searchTerm.split(enumerator);
			if (newTerms.length > 1) {	return newTerms; }
		}
		return new String[0];
	}
	
	/**
	 * Matches the assumed dataset name <emph>searchTerm</emph> to records in dara having a similar name. 
	 * The computation of string similarity is done by dara's search function. Querying dara is 
	 * carried out using the dara web interface to ensure accessibility from outside of GESIS. However, 
	 * this leads to high processing times both for querying and parsing the results.
	 * 
	 * @param searchTerm	assumed dataset name to be matched to dara records
	 * @param cacheFile		path to the query cache
	 * @return				a map containing matching dataset DOIs (keys) and names (values)
	 */
	public HashMap<String,String> match(String searchTerm, String cacheFile)
	{
		URL url;
		try { url = constructURL(searchTerm, 600); System.out.println(url); System.out.println("\n" + searchTerm); }
		catch (MalformedURLException e) { e.printStackTrace(); return new HashMap<String,String>(); }
		// read file queryCache - use saved results instead of querying whenever possible
		HashMap<String,String> res = readFromCache(url.toString(), cacheFile);
		// if no entry is found in the cache, query dara
		if (res.isEmpty())
		{
			try { res = parseHTML(readFromURL(url)); }
			catch (IOException ioe) { ioe.printStackTrace(); return new HashMap<String,String>(); }
		}
		else 
		{ 
			System.out.println("Found query in cache for term: " + searchTerm); 
			// query was in cache but no data was specified i.e. study is not registered in dara
			if (res.keySet().contains("")) { return new HashMap<String, String>(); }
			System.out.println(res.toString());
			return res;
		}
		// if result is empty, check if studytitle maybe is an enumeration and search for parts!
		if (res.isEmpty())
		{
			String[] newTerms = getEnumeratedTerms(searchTerm);
			for (String term : newTerms)
			{
				// ignore terms consisting of digits only
				if (!term.trim().matches("\\d+")) {	res.putAll(match(term.trim(), cacheFile)); }
			}
		}
		// write results to cache
		// empty results in the cache are valuable too -> prevents repeated searching for non-registered studies
		writeToCache(url.toString(), res, cacheFile);
		return res; 
	}
	
	/**
	 * Writes the results of a dara query to the cache found in specified cacheFilename path.
	 *  
	 * @param url		the dara query url
	 * @param res		the parsed dara response for the specified query url
	 * @param cacheFilename	path of the cache file
	 */
	private void writeToCache(String url, HashMap<String, String> res, String cacheFilename)
	{
		String delimiter = Util.delimiter_cacheFile;
		String newLine = url;

		for (String key : res.keySet())	{ newLine = newLine + delimiter + res.get(key) + delimiter + key; }
		try 
		{
			System.out.println("Writing query to cache: " + newLine);
			File f = new File(cacheFilename);
			OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(f, true), "UTF-8");
		    BufferedWriter out = new BufferedWriter(fstream);
		    out.write(newLine + System.getProperty("line.separator"));
		    out.close();
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	/**
	 * Matches the assumed dataset name <emph>searchTerm</emph> to records in dara having a similar name. 
	 * The computation of string similarity is done by dara's search function. Querying dara is 
	 * carried out using the dara web interface to ensure accessibility from outside of GESIS. However, 
	 * this leads to high processing times both for querying and parsing the results.
	 * 
	 * @param searchTerm	assumed dataset name to be matched to dara records
	 * @return				a map containing matching dataset DOIs (keys) and names (values)
	 */
	public HashMap<String,String> match(String searchTerm)
	{
		URL url;
		try { url = constructURL(searchTerm, 600); System.out.println(url); System.out.println("\n" + searchTerm); }
		catch (MalformedURLException e) { e.printStackTrace(); return new HashMap<String,String>(); }
		HashMap<String,String> res = new HashMap<String,String>();
		try { res = parseHTML(readFromURL(url)); }
		catch (IOException ioe) { ioe.printStackTrace(); return new HashMap<String,String>(); }
		
		// check if studytitle maybe is an enumeration and search for parts as well
		String[] newTerms = getEnumeratedTerms(searchTerm);
		for (String term : newTerms) 
		{ 
			// ignore terms consisting of digits only
			if (!term.trim().matches("\\d+")) {	res.putAll(match(term.trim())); }
		}		
		return res; 
	}
	
	/**
	 * Queries "http://www.da-ra.de/dara/study/web_search_show" for the specified dataset name. Optionally 
	 * uses cache file if specified. 
	 * 
	 * @param args	args[0]: dataset name(s); args[1]: path of cache file (optional)
	 */
	public static void main(String[] args)
	{
		StudyMatcher matcher = new StudyMatcher("http://www.da-ra.de/dara/study/web_search_show");
		if (args.length == 1) {	System.out.println(matcher.match(args[0])); }
		else { System.out.println(matcher.match(args[0], args[1])); }
	}
}