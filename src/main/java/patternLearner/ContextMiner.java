package patternLearner;

import patternLearner.bootstrapping.ExampleReader;
import studyMatching.StudyMatcher;
import java.io.BufferedWriter;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import patternLearner.bootstrapping.Term;

import searching.SearchTermPosition;


/**
 * Class for mapping dataset references listed in InfoLink reference extraction context files 
 * to dataset records.
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
public class ContextMiner 
{
	Set<String> documentSet;
	Map<String, Term> termMap;
	Map<String,Set<String[]>> documentMap;
	String corpusName;
	
	/**
	 * Class constructor specifying the name of the text corpus from which the contexts were extracted. 
	 * Needed to apply corpus-specific transformations to document names before look-up in URN dictionary, 
	 * if necessary. 
	 * Use this constructor for mining known study references (output of term search rather than pattern-based 
	 * extraction).
	 * 
	 * @param corpusName	name of the text corpus from which the contexts were extracted
	 */
	ContextMiner (String corpusName)
	{
		this.corpusName = corpusName;
	}
	
	/**
	 * Class constructor specifying the name of the XML context file to be processed and the name of the 
	 * text corpus from which the contexts were extracted. The latter is needed to apply corpus-specific 
	 * transformations to document names before look-up in the URN dictionary, if necessary. 
	 * 
	 * @param filename		name of the xml content file to be processed
	 * @param corpusName	name of the text corpus from which the contexts were extracted
	 */
	ContextMiner (String filename, String corpusName)
	{
		ExampleReader exReader = new ExampleReader(new File(filename));
		this.documentSet = exReader.getDocuments();
		this.termMap = exReader.getTermMap();
		this.documentMap = exReader.getDocumentMap();
		this.corpusName = corpusName;
	}
	
	/**
	 * Class constructor specifying the XML context file to be processed and the name of the 
	 * text corpus from which the contexts were extracted. The latter is needed to apply corpus-specific 
	 * transformations to document names before look-up in the URN dictionary, if necessary. 
	 * 
	 * @param file			the XML content file to be processed
	 * @param corpusName	name of the text corpus from which the contexts were extracted
	 */
	ContextMiner (File file, String corpusName)
	{
		ExampleReader exReader = new ExampleReader(file);
		this.documentSet = exReader.getDocuments();
		this.termMap = exReader.getTermMap();
		this.documentMap = exReader.getDocumentMap();
		this.corpusName = corpusName;
	}
	
	/**
	 * Returns this set of documents consisting of the filenames of all documents occurring in the context file. 
	 * 
	 * @return	set of documents found in the context file.
	 */
	public Set<String> getDocuments ()
	{
		return this.documentSet;
	}
	
	/**
	 * Types of InfoLink links. 
	 * <ul>
	 * <li>DOI: study names that could be matched to a record in the repository, identified by a DOI</li>
	 * <li>URL: study names that are in fact a URL</li>
	 * <li>STRING: study names that could not be matched to any record and that are not a URL</li>
	 * </ul>
	 * 
	 * @author katarina.boland@gesis.org
	 * @version 2014-01-27
	 *
	 */
	private static enum StudyType { DOI, URL, STRING; }  
	
	/**
	 * Searches for year, number or version specifications in the input string and returns an array 
	 * containing the left context, the right context and the extracted specification as members.
	 * 
	 * @param patterns	array of patterns for year, number or version specifications. Patterns should be ordered by priority (high priority first), first match is always accepted. Each pattern must have its first group reserved for characters directly preceeding the numeric specification 
	 * @param string	snippet to extract numerical information from
	 * @param useFirstNumber	if set, first number being found is accepted. Else, the last number is.
	 * @return			an array containing the left context, the right context and the extracted specification as members (in this order)
	 */
	private String[] separateYearAndVersion(Pattern[] patterns, String string, boolean useFirstNumber)
	{
		Matcher matcher;
		String[] res = new String[3];
		for (Pattern pat : patterns)
		{
			matcher = pat.matcher(string);
			while (matcher.find())
			{
				String version = matcher.group();
				// version may contain dots, therefore needs to be escaped
				// split string into left context of version, version, right context of version
				// remove chars directly adjacent to version (e.g. brackets)
				String[] stringSplit = string.trim().split("\\S*\\Q" + version + "\\E\\S*", 2);
				res[0] = stringSplit[0];
				try	{ res[1] = stringSplit[1]; }
				catch (ArrayIndexOutOfBoundsException e) { res[1] = null; }
				res[2] = version;
				if (useFirstNumber) { return res; }
			}
			if (res[2] != null) { return res; }
		}
		return res;
	}
	
	/**
	 * Extract URLs from string, if present.
	 * 
	 * @param string input string
	 * @return		identified URL string or null
	 */
	public String extractUrl(String string)
	{
		Pattern urlPat = Util.urlPat; 
		Matcher m = urlPat.matcher(string);
		if (m.find()) { return m.group(); }
		else { return null; }
	}
	
	/**
	 * Extracts URL references from the specified context and returns them along with their titles. 
	 * 
	 * @param completeContext	context of the dataset reference that may include a URL. Format: completeContext[0]: left context; completeContext[1]: dataset title; completeContext[2]: right context
	 * @return					array with url and dataset title as members if URL is found, null otherwise
	 */
	public String[] getHtmlRef(String[] completeContext, String strippedTitle)
	{
		// search for url in right context
		String url = extractUrl(completeContext[2]);
		if (url!=null) { String[] ref = { url, strippedTitle }; return ref; }
		// if url could not be found in right context, search for url in study name
		// (e.g. for study names like "SHARE (http://www.share-project.org/)")
		else { url = extractUrl(completeContext[1]); }
		if (url!=null) 
		{ 
			// remove url from dataset title
			// also remove any directly attached characters, e.g. brackets
			String[] ref = { url, strippedTitle.replaceAll("\\S*" + url + "\\S*", "").trim() }; 
			// if title without url only contains punctuation, whitespace and digits:
			// there was no title given except the url itself plus maybe a version
			// in this case: use url as dataset name 
			if (ref[1].matches("[\\p{Punct}\\s\\d]*")) { ref[1] = strippedTitle; }
			return ref; 
		}
		// if url could neither be found in right context nor in dataset title, search for url in left context
		else { url = extractUrl(completeContext[0]); }
		if (url!=null) { String[] ref = { url, strippedTitle }; return ref; }
		// no url could be found anywhere in context
		return null;
	}
	
	/**
	 * Normalizes document names of different subcorpora in the path <emph>docName</emph> 
	 * to allow indirect mapping to entries in URN dictionary.
	 * 
	 * This method currently does not contain any mapping rules, add your rules here if needed.
	 * 
	 * @param docName	document filename to be normalized and later looked-up in URN dictionary
	 * @return			normalized document name ready to be looked-up in URN dictionary
	 */
	public String mapToDictName(String docName)
	{
		docName = docName.replace("\\", "/");
		String patternStr = "";
		String replaceStr = "";
		// add your rules here, e.g.:
		/*
		if (this.corpusName.equals("dgs"))
		{
			patternStr = ".*?dgs/(.*?-.*?)_(.*?)_clean.*?\\.txt";
			replaceStr = "dgs/$1/$2.pdf";
		}*/
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(docName);
		return matcher.replaceAll(replaceStr);
	}
	
	/**
	 * Inserts extracted information into docMap storing information on documents and corresponding 
	 * parsed references. 
	 * 
	 * A parsed reference contains information on dataset title, dataset number specification, dataset url 
	 * and the complete reference snippet.
	 * 
	 * @param docMap			the map to insert the information to 
	 * @param document			id or name of the document (used as key in the map)
	 * @param htmlRefNtitle		extracted dataset url and dataset title with stripped url
	 * @param title				title of the referenced dataset
	 * @param version			number specification of the referenced dataset
	 * @param completeContext	the complete reference snippet found for the dataset
	 * @return					the updated map
	 */
	public HashMap<String,HashSet<String[]>> addInfoToLinkMap(HashMap<String,HashSet<String[]>> docMap, String document, String[] htmlRefNtitle, String title, String version, String completeContext)
	{
		String[] titleVersion = new String[4];
		// url has higher priority than name and version
		if ( htmlRefNtitle != null ) 
		{ 
			titleVersion[0] = htmlRefNtitle[1]; // title without url and without version
			titleVersion[2] = htmlRefNtitle[0]; // url
		} 
		// if no url was found in the study reference, use other title and set url to null
		else { titleVersion[0] = title; titleVersion[2] = null; } 
		titleVersion[1] = version; 
		titleVersion[3] = completeContext;
		HashSet<String[]> studySet;
		if (docMap.containsKey(document)) { studySet = docMap.get(document); }
		else { studySet = new HashSet<String[]>(); }
		studySet.add(titleVersion);
		docMap.put(document, studySet);
		return docMap;
		
	}
	
	/**
	 * Extracts titles and versions from all contexts found in a document and maps document filenames 
	 * to URNs. If no URN can be determined for a filename, the filename is used as ID.
	 * 
	 * @param idMmap	map containing document names as keys and IDs as values
	 * @return			list containing two maps: first map contains all contexts where any version 
	 * 					(= number specification) could be found, second map contains all other contexts. 
	 * 					IDs of documents are keys, a set of contexts belonging to a 
	 * 					document are values.
	 */
	public ArrayList<HashMap<String,HashSet<String[]>>> getTitleAndVersion(HashMap<String,String> idMap)
	{
		// store data for all references containing numerical information (on reference years or versions etc.) in docStudyMap
		HashMap<String,HashSet<String[]>> docStudyMap = new HashMap<String,HashSet<String[]>>();
		// store data for all references not containing any numerical information in separate map 
		// -> it might be favorable to treat such references differently
		HashMap<String,HashSet<String[]>> docNoVersionMap = new HashMap<String,HashSet<String[]>>();
		// retrieve patterns of valid year / number / version specifications
		Pattern[] patterns = Util.getContextMinerYearPatterns();

		for (String document : this.documentMap.keySet())
		{
			for (String[] contextComplete : documentMap.get(document))
			{
				// retrieve ID of document
				document = getID(mapToDictName(document), idMap);

				// try to find year inside of dataset title (e.g. "ALLBUS 2000" identified as dataset title)
				String[] separatedTitle = separateYearAndVersion(patterns, contextComplete[1], true);
				// separatedTitle[0]: left context of numeric specification, separatedTitle[1]: right context of numeric specification, separatedTitle[2]: numeric specification without directly adjacent characters
				// find URLs if present in reference snippet
				String[] htmlRefNtitle;
				if (separatedTitle[2] != null) { htmlRefNtitle = getHtmlRef(contextComplete, (separatedTitle[0] + " " + separatedTitle[1]).trim()); }
				else { htmlRefNtitle = getHtmlRef(contextComplete, contextComplete[1]); }

				// found version inside of dataset title
				if (separatedTitle[2] != null)
				{
					//docStudyMap = addInfoToLinkMap(docStudyMap, document, htmlRefNtitle, separatedTitle[0], separatedTitle[2], contextComplete[0] + " " + contextComplete[1] + " " + contextComplete[2]);
					// title here is the original title without the extracted numeric information
					docStudyMap = addInfoToLinkMap(docStudyMap, document, htmlRefNtitle, (separatedTitle[0]+" "+separatedTitle[1]).trim(), separatedTitle[2], contextComplete[0] + " " + contextComplete[1] + " " + contextComplete[2]);
					continue;
				}
				
				// try to find year inside of right context (e.g. "ALLBUS" survey in 2003...)
				else { separatedTitle = separateYearAndVersion(patterns, contextComplete[2], true); }
				// found version inside of right context
				if (separatedTitle[2] != null)
				{
					// title here is the unchanged extracted dataset title
					docStudyMap = addInfoToLinkMap(docStudyMap, document, htmlRefNtitle, contextComplete[1], separatedTitle[2], contextComplete[0] + " " + contextComplete[1] + " " + contextComplete[2]);
					continue;
				}
				// try to find year inside of left context (e.g. the 2003 "ALLBUS" survey...)
				// in this case, accept right-most number as most valuable one
				else { separatedTitle = separateYearAndVersion(patterns, contextComplete[0], false); }
				// found version inside of left context
				if (separatedTitle[2] != null)
				{
					docStudyMap = addInfoToLinkMap(docStudyMap, document, htmlRefNtitle, contextComplete[1], separatedTitle[2], contextComplete[0] + " " + contextComplete[1] + " " + contextComplete[2]);
					continue;
				}
				// no version or year could be found anywhere in the context
				else { docNoVersionMap = addInfoToLinkMap(docNoVersionMap, document, htmlRefNtitle, contextComplete[1], separatedTitle[2], contextComplete[0] + " " + contextComplete[1] + " " + contextComplete[2]); }
			}
		}
		ArrayList<HashMap<String,HashSet<String[]>>> res = new ArrayList<HashMap<String,HashSet<String[]>>>();
		res.add(docStudyMap);
		res.add(docNoVersionMap);
		return res;
	}
	
	/**
	 * Replaces umlauts with their alternative representations. 
	 * 
	 * Useful for conducting searches using dara's search interface which does not handly umlauts well.
	 * 
	 * @param name	the string to be cleaned
	 * @return		the string with alternative representations of any umlauts
	 */
	public static String cleanStudyName(String name)
	{
		return name.replace("ö", "oe").replace("ä", "ae").replace("ü", "ue").replace(
											"Ö", "Oe").replace("Ä", "Ae").replace("Ü", "Ue");
	}
	
	/**
	 * Reads a file listing filenames and their URNs separated by ";" and constructs a map with
	 * filenames as keys and URNs as values.
	 * 
	 * @param filename	name of the file containing a list of filenames and corresponding URNs
	 * @return			a map with filenames as keys and URNs as values
	 */
	public HashMap<String, String> readIDmap(String filename)
	{
		HashMap<String, String> urnMap = new HashMap<String, String>();
		try
		{
    		File f = new File( filename );
    		InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF8");
    		BufferedReader reader = new BufferedReader(isr);
    	    String line = null;
    	    while ((line = reader.readLine()) != null) 
    	    {
    	    	for (String entry : line.split("\n"))
    			{
    				// this should always yield exactly 2 items as neither filenames nor URNs should contain ";"
    				String[] nameNurn= entry.split(";");
    				if (nameNurn.length != 2) { System.err.println("Warning: could not properly separate urn and filename for " + line); }
    				urnMap.put(nameNurn[0], nameNurn[1]);
    			}
    	    }
    	    reader.close();
		}
		catch (IOException e) { e.printStackTrace(); }
		return urnMap;
	}
	
	/**
	 * Searches for the ID of the filename in the specified map and returns it. If no ID can be found, the 
	 * filename is returned instead.
	 * 
	 * @param filename		name of the file to be looked up
	 * @param ssoarURNmap	map with filenames as keys and IDs as values
	 * @return				the ID of <emph>filename</emph> if found in the map or <emph>filename</emph> otherwise.
	 */
	public String getID(String filename, HashMap<String,String> ssoarURNmap)
	{
		String pubId = ssoarURNmap.get(new File(filename).getName());
		// if text file cannot be mapped to urn: use filename
		if ( pubId == null ) 
		{ 
			System.err.println( "Warning: could not determine urn for " + filename ); 
			pubId = filename;
		}
		return pubId;
	}

	/**
	 * Mines the dataset references listed in <emph>docMap</emph> and finds matching dataset records in 
	 * the dara repository. 
	 * 
	 * Note: any dataset records with matching dataset titles are accepted, no filtering with regard to 
	 * corresponding numbers, years or versions is applied here.
	 * 
	 * @param docMap				map with document IDs as keys and found references contexts as values
	 * @param idMap					map listing document filenames and their IDs
	 * @param matcher				StudyMatcher instance to be used to find matching dataset records
	 * @param links_doiUrlString	list for storing and returning the resulting DOI, URL and string link maps
	 * @param method				method used for extracting the references in docMap (pattern-based search vs. term-based search)
	 * @return
	 */
	public ArrayList<HashMap<String, Collection<StudyLink>>> mineStudyRefs(HashMap<String,HashSet<String[]>> docMap, HashMap<String,String> idMap, StudyMatcher matcher, ArrayList<HashMap<String, Collection<StudyLink>>> links_doiUrlString, ExtractionMethod method)
	{
		HashMap<String, Collection<StudyLink>> links_doi = links_doiUrlString.get(0);
		HashMap<String, Collection<StudyLink>> links_url = links_doiUrlString.get(1);
		HashMap<String, Collection<StudyLink>> links_str = links_doiUrlString.get(2);

		for (String filename : docMap.keySet())
		{
			String pubId = getID(filename, idMap);
			for (String[] citation : docMap.get(filename))
			{
				// filter out some predefined dataset titles
				if (ignoreStudy(citation[0]) == true)
				{
					System.out.println("Ignoring study \"" + citation[0] + "\"");
					continue;
				}	
				
				Collection<StudyLink> foundStudies_doi = new HashSet<StudyLink>();
				Collection<StudyLink> foundStudies_url = new HashSet<StudyLink>();
				Collection<StudyLink> foundStudies_str = new HashSet<StudyLink>();
				HashMap<String,String> matchingStudiesMap = new HashMap<String,String>();
				
				matchingStudiesMap = matcher.match(citation[0]);
				
				// if no match was found, clean the dataset title and try again
				if (matchingStudiesMap.isEmpty()) { matchingStudiesMap = matcher.match(cleanStudyName(citation[0])); }
				
				// if no match was found: append name for manual matching or whatever plus url if specified
				if (matchingStudiesMap.isEmpty()) 
				{	
					// html ref found in context
					// high confidence, type url
					if (citation[2] != null) 
					{ 
						float confidence = 0.7f;
						// if no version is specified, mark entry with a lower confidence value
						if (citation[1]==null) { confidence = 0.5f; citation[1] = "";} 
						if (links_url.containsKey(pubId)) { foundStudies_url = links_url.get(pubId); }
						foundStudies_url.add(new StudyLink("", citation[1], citation[0], citation[2], StudyType.URL, confidence, citation[3], method));
						links_url.put(pubId, foundStudies_url);
					}
					else 
					{
						// look up study in external link index
						String link = matcher.match_external(citation[0]);
						// high confidence, type url
						if (link != null) 
						{ 
							float confidence = 0.7f;
							// if no version is specified, mark entry with a lower confidence value
							if (citation[1]==null) { confidence = 0.4f; citation[1] = "";} 
							if ( links_url.containsKey(pubId) ) { foundStudies_url = links_url.get(pubId); }
							foundStudies_url.add(new StudyLink ("", citation[1], citation[0], link, StudyType.URL, confidence, citation[3], method));
							links_url.put(pubId, foundStudies_url);
						}
						// no match was found, neither in repository nor in external index
						// type string
						else 
						{ 
							float confidence = 0.6f;
							// if no version is specified, mark entry with a lower confidence value
							if (citation[1]==null) { confidence = 0.2f; citation[1] = ""; } 
							if ( links_str.containsKey(pubId) ) { foundStudies_str = links_str.get(pubId); }
							foundStudies_str.add(new StudyLink("", citation[1], citation[0], "", StudyType.STRING, confidence, citation[3], method));
							links_str.put(pubId, foundStudies_str);
						}
					}
				}
				// matching record was found in repository
				else 
				{ 
					for ( String key : matchingStudiesMap.keySet() )
					{
						float confidence = 0.7f;
						// if no version is specified, mark entry with a lower confidence value
						if (citation[1]==null) { confidence = 0.3f; citation[1] = ""; }
						if (links_doi.containsKey(pubId)) { foundStudies_doi = links_doi.get(pubId); }
						foundStudies_doi.add(new StudyLink(matchingStudiesMap.get(key), citation[1], citation[0], key, StudyType.DOI, confidence, citation[3], method));
					}
					links_doi.put(pubId, foundStudies_doi);
				}
			}
		}
		ArrayList<HashMap<String, Collection<StudyLink>>> res_doiUrlStr = new ArrayList<HashMap<String, Collection<StudyLink>>>();
		res_doiUrlStr.add(links_doi);
		res_doiUrlStr.add(links_url);
		res_doiUrlStr.add(links_str);
		return res_doiUrlStr;
	}
	
	//TODO: disable when evaluating! use only for productive version (set as param...)
	/**
	 * Checks whether <emph>studyname</emph> is contained in a list of study names to ignore. 
	 * 
	 * @param studyname	string to be searched in the ignore list
	 * @return			<emph>true</emph>, if <emph>studyname</emph> is in ignore list, <emph>false</emph> otherwise
	 */
	public static boolean ignoreStudy(String studyname)
	{
		return ( studyname.contains("Eigene Erhebung") | studyname.contains("eigene Erhebung")
				| studyname.contains("eigene Darstellung") | studyname.contains("Eigene Darstellung") 
				| studyname.contains("eigene Abbildung") | studyname.contains("Eigene Abbildung") 
				| studyname.matches("[\\d\\s]*"));
	}
	
	/**
	 * Reads previously saved results of term search and maps filenames of documents included in the 
	 * file to corresponding IDs. 
	 * 
	 * @param filename		path of the filename containing the search results (= extracted contexts)
	 * @param corpusName	name of the used text corpus (for possible transformations prior to ID look-up)
	 * @param idMap			path of the file listing document filenames and their IDs
	 * @return				a map with document IDs as keys and extracted dataset references as values
	 */
	public HashMap<String,HashSet<String[]>> getKnownStudyRefs(String filename, String corpusName, HashMap<String,String> idMap)
	{
		HashMap<String,HashSet<String[]>> resKnownStudies = new HashMap<String,HashSet<String[]>>();
		try
		{
			File file = new File(filename);  
			FileInputStream f = new FileInputStream(file);  
			ObjectInputStream s = new ObjectInputStream(f);  
			HashMap<String,HashSet<String[]>> resKnownStudies_ = (HashMap<String,HashSet<String[]>>)s.readObject();         
			s.close();
			
			for (String key : resKnownStudies_.keySet())
			{
				String keyNormalized = getID(mapToDictName(key), idMap);
				resKnownStudies.put(keyNormalized, resKnownStudies_.get(key));
			}
		}
		catch (IOException ioe) { ioe.printStackTrace(); }
		catch (ClassNotFoundException cnfe) { cnfe.printStackTrace(); }
		return resKnownStudies;
	}
	
	/**
	 * Enumeration class for distinguishing pattern-based search and term-based search for dataset 
	 * references. 
	 *
	 */
	private static enum ExtractionMethod {PATTERN, TERM};
	
	/**
	 * Class representing the link between a publication and a dataset.
	 * 
	 * Instances of <emph>StudyLink</emph> have the following fields:
	 * <ul>
	 * 	<li>name: title of the linked dataset</li>
	 * 	<li>alt_name: title of the referenced dataset</li>
	 * 	<li>version: year or number of the referenced dataset</li>
	 * 	<li>link: the linked dataset</li>
	 * 	<li>type: a StudyType instance denoting whether <emph>link</emph> contains a DOI, a URL or a string</li>
	 * 	<li>snippet: text snippet from which the dataset reference was extracted</li>
	 * 	<li>confidence: probability that the StudyLink instance is correct</li>
	 * </ul>
	 * 
	 * @author katarina.boland@gesis.org
	 * @version 2014-01-27
	 *
	 */
	private class StudyLink 
	{
		StudyType type;  
		float confidence; // 1.0 denotes highest confidence, 0 lowest...
		String name;
		String version;
		String link;
		String alt_name;
		String snippet;
		ExtractionMethod method;


		/**
		 * Class constructor specifying the name, version, alt_name, link, type, confidence, snippet and extraction method of a 
		 * <emph>StudyLink</emph> instance. 
		 * 
		 * @param name			title of the linked dataset
		 * @param version		year or number of the referenced dataset
		 * @param alt_name		title of the referenced dataset
		 * @param link			the linked dataset
		 * @param type			StudyType instance denoting whether the linked dataset is identified by a DOI, a URL or a string
		 * @param confidence	probability that the StudyLink instance is correct
		 * @param snippet		text snippet from which the dataset reference was extracted
		 * @param method		method used for extraction (pattern-based vs. term-based search)
		 */
		StudyLink(String name, String version, String alt_name, String link, StudyType type, float confidence, String snippet, ExtractionMethod method)
		{
			this.name = name;
			this.type = type;
			this.confidence = confidence;
			this.version = version;
			this.link = link;
			this.alt_name = alt_name;
			this.snippet = snippet;
			this.method = method;
		}

		@ Override public String toString()
		{
			String delimiter = Util.delimiter_csv;
			String _alt_name;
			String _name;
			String _version;
			String _link;
			// some fields may be null, trying to replace chars will provoke NullPointerException
			try { _alt_name = this.alt_name.replace(delimiter, "").trim(); }
			catch (NullPointerException npe) { _alt_name = ""; }
			try { _name = this.name.replace(delimiter, "").trim(); }
			catch (NullPointerException npe) { _name = ""; }
			try { _version = this.version.replace(delimiter, "").trim(); }
			catch (NullPointerException npe) { _version = ""; }
			try { _link = this.link.replace(delimiter, "").trim(); }
			catch (NullPointerException npe) { _link = ""; }
			return _name  + delimiter + (_alt_name + " " + _version).trim() + delimiter + _link + delimiter + "Study" + delimiter + this.type.toString() + delimiter + String.valueOf(this.confidence) + delimiter + this.method.toString();
		}
		
		String getName()
		{
			return this.name;
		}
		
		String getVersion()
		{
			return this.version;
		}
		
		StudyType getType()
		{
			return this.type;
		}
		
		float getConfidence()
		{
			return confidence;
		}
		
		void setConfidence(int confidence)
		{
			this.confidence = confidence;
		}
	}
	
	/**
	 * Checks whether the document identified by <emph>pubId</emph> is already linked to dataset 
	 * <emph>link</emph>. Performs look-up in <emph>refMap</emph> for this purpose. 
	 * 
	 * 
	 * @param pubId		ID of the document
	 * @param link		link to a dataset
	 * @param refMap	map listing all datasets (values) linked to all documents (keys)
	 * @return			<emph>True</emph>, if document was already linked to dataset, <emph>False</emph> 
	 * 					otherwise.
	 */
	public boolean isDuplicate(String pubId, String link, HashMap<String, HashSet<String>> refMap)
	{
		try { return refMap.get(pubId).contains(link); }
		catch (NullPointerException npe) { return false; }
	}
	
	//...
		
	/**
	 * Searches for contexts of term in the specified document <emph>docName</emph> using <emph>docMap</emph>. 
	 * 
	 * @param term			the search term
	 * @param docName		name or ID of the document to search the term in
	 * @param docMap		map having documents as keys and a term map as values. Term maps have terms as keys and all contexts of these terms in the current documen as values
	 * @param logFilename	path to the logfile
	 * @return				a list of all contexts found for term in docName
	 */
	public HashSet<String> getAllSnippetsForTermInDoc(String term, String docName, HashMap<String,HashMap<String,HashSet<String>>> docMap, String logFilename)
	{
		HashMap<String,HashSet<String>> docSnippets = docMap.get(docName); 
		// useful when comparing filenames
		if (docSnippets == null) { docSnippets = docMap.get(docName.replace("/", "\\")); }
		if (docSnippets == null) { docSnippets = docMap.get(docName.replace("\\", "/")); }
		
		if (docSnippets == null) 
		{
			try
			{
				FileWriter writer = new FileWriter(new File(logFilename), true);
				BufferedWriter buf = new BufferedWriter(writer);
				buf.write("\nno entry for " + docName + " (term " + term + ") in " + docMap.keySet());
				buf.close();
				return new HashSet<String>();
			}
			catch (IOException ioe)
			{
				ioe.printStackTrace();
				System.out.println("no entry for " + docName + " (term " + term + ") in " + docMap.keySet()); 
				return new HashSet<String>();
			}
		}
		HashSet<String> snippets = docSnippets.get(term.trim());
		if (snippets != null) {	return snippets; }
		else { return new HashSet<String>();}
	}
	
	/**
	 * Constructs an inverted index of document filenames and their corresponding IDs (keys: IDs, values: document filenames).
	 * 
	 * @param snippetFilename	path to the file containing all contexts and document names for which an index is needed
	 * @param idMap				map listing document names and their corresponding IDs
	 * @return					a map having IDs as keys and corresponding document filenames as values
	 */
	public HashMap<String,HashMap<String,HashSet<String>>> getLinkDocMap(String snippetFilename, HashMap<String,String> idMap)
	{
		ExampleReader exReader = new ExampleReader(new File(snippetFilename));
		Map<String,Set<String[]>> docMap = exReader.getDocumentMap();
		HashMap<String,HashMap<String,HashSet<String>>> urnDocMap = new HashMap<String,HashMap<String,HashSet<String>>>();
		// docMap has filenames as keys, transform them into IDs
		for (String key : docMap.keySet())
		{
			String urnKey = getID(mapToDictName(key.replace("\\", "/")), idMap);
			Set<String[]> links = docMap.get(key);
			HashMap<String,HashSet<String>> termMap = new HashMap<String,HashSet<String>>();
			// for better performance: sort by term (=context[1])
			Iterator<String[]> linkIter = links.iterator();
			while (linkIter.hasNext())
			{
				String[] contextComplete = linkIter.next();
				String term = contextComplete[1].trim();
				String context = contextComplete[0].trim() + " " + term + " " + contextComplete[2].trim();
				context = context.replace("\n", " ");
				HashSet<String> termLinks = new HashSet<String>();
				if (termMap.containsKey(term)) { termLinks = termMap.get(term); }
				termLinks.add(context);
				termMap.put(term, termLinks);
			}
			urnDocMap.put(urnKey, termMap);
		}
		return urnDocMap;
	}

	  /**
	   * Searches for the terms in termList using the lucene index <emph>indexDir</emph> and saves their 
	   * contexts to <emph>snippetFilename</emph>. 
	   * 
	   * @param indexDir		path of the lucene index
	   * @param termList		list of search terms
	   * @param snippetFilename	path of output file
	   */
	public void saveAllSnippets(String indexDir, Collection<String> termList, String snippetFilename) throws IOException
	{
		try { Util.prepareOutputFile(snippetFilename); }
		catch (IOException e) { e.printStackTrace(); }  
		for (String term : termList)
		{
			SearchTermPosition search = new SearchTermPosition(indexDir, snippetFilename, term.trim(), "\"" + SearchTermPosition.normalizeQuery(term) + "\"");
			try { search.complexSearch(new File(snippetFilename), true);	}
			catch (IOException ioe) { ioe.printStackTrace(); }
			catch (Exception e) { e.printStackTrace(); }
		}
		try { Util.completeOutputFile(snippetFilename); }
		catch (IOException e) { e.printStackTrace(); } 
	}

	/**
	 * Scans the specified link map for all distinct dataset reference names.
	 * 
	 * @param linkMap	map containing StudyLink instances as values
	 * @return			list of distince dataset reference names
	 */
	public HashSet<String> getAllRefNames(HashMap<String, Collection<StudyLink>> linkMap)
	{
		HashSet<String> refNameList = new HashSet<String>();
		for (String key : linkMap.keySet())
		{
			Collection<StudyLink> linkList = linkMap.get(key);
			for (StudyLink curLink : linkList) { refNameList.add(curLink.alt_name);	}
		}
		return refNameList;
	}
	
	/**
	 * Finds links that are identical in all fields but the snippet and merges them.
	 * 
	 * @param linkSet	set containing all links
	 * @return			a set of all links now being merged (only one entry per link with all snippets merged)
	 */
	public HashSet<StudyLink> mergeSnippets(Collection<StudyLink> linkSet)
	{
		String delimiter = Util.delimiter_csv;
		// unique key for links
		String studyNameVersionLinkConf = "";
		// snippetMap: contains all different snippets for a link
		HashMap<String, HashSet<String>> snippetMap = new HashMap<String, HashSet<String>>();
		// first pass: merge snippets
		for (StudyLink ref : linkSet)
		{
			HashSet<String> knownSnippets = new HashSet<String>();
			studyNameVersionLinkConf = ref.name + ref.version + ref.link + ref.confidence + ref.method;
			if (snippetMap.containsKey(studyNameVersionLinkConf)) { knownSnippets = snippetMap.get(studyNameVersionLinkConf); }
			knownSnippets.add(ref.snippet);
			snippetMap.put(studyNameVersionLinkConf, knownSnippets);
		}
		// second pass: construct new linkSet with merged snippets
		HashSet<StudyLink> newLinkSet = new HashSet<StudyLink>();
		for (StudyLink ref : linkSet)
		{
			studyNameVersionLinkConf = ref.name + ref.version + ref.link + ref.confidence + ref.method;
			HashSet<String> snippets = snippetMap.get(studyNameVersionLinkConf);
			Iterator<String> snippetIter = snippets.iterator();
			String allSnippets = "";
			while (snippetIter.hasNext())
			{
				allSnippets += Util.delimiter_internal + snippetIter.next().replace(delimiter, "_");
			}
			// replace first delimiter symbol
			if (allSnippets.length() > 0) { allSnippets = allSnippets.substring(Util.delimiter_internal.length()); }
			StudyLink newLink = new StudyLink(ref.name, ref.version, ref.alt_name, ref.link, ref.type, ref.confidence, allSnippets, ref.method);
			newLinkSet.add(newLink);
		}
		return newLinkSet;
	}
	
	/**
	 * Writes the contents of the links in linkMap to file filename.
	 * 
	 * @param linkMap	map containing all links
	 * @param filename	output filename
	 */
	public void outputLinkMap(HashMap<String, Collection<StudyLink>> linkMap, String filename)
	{
		String delimiter = Util.delimiter_csv;
		String pubStudyNameVersionLinkConf = "";
		try 
		{
			//TODO: check: changed to true here to append to existing files when both term-based and pattern-based searches are used
			OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(filename, true), "UTF-8");
			BufferedWriter out = new BufferedWriter(fstream);
			OutputStreamWriter fstream2 = new OutputStreamWriter(new FileOutputStream(filename.replace(".csv", "_unknownURN.csv")), "UTF-8");
			BufferedWriter out2 = new BufferedWriter(fstream2);
			for (String key : linkMap.keySet())
			{
				System.out.println("Processing " + key);
				// refMap: contains all previously processed distinct references (distinct = link having same refString, same doi, same confidence
				HashMap<String, HashSet<String>> refMap = new HashMap<String, HashSet<String>>();
				
				// output only one link when multiple references are mapped to same study but merge and include all snippets in this link
				HashSet<StudyLink> mergedLinks = mergeSnippets(linkMap.get(key));
				for (StudyLink ref : mergedLinks)
				{
					HashSet<String> studyRefs = new HashSet<String>();
					// ignore duplicates
					pubStudyNameVersionLinkConf = key + ref.name + ref.version + ref.link + ref.confidence + ref.method;
					if (isDuplicate(key, pubStudyNameVersionLinkConf, refMap)) { continue; }
					
					String outStr = "" + delimiter + key.replace(delimiter, "") + delimiter + "Publication" + 
							delimiter + "URN" + delimiter + ref.toString() + delimiter + ref.snippet + delimiter + 
							"LitStudy_automatic" + System.getProperty("line.separator");
					// export links for publications with unknown urn to separate files
					if (key.endsWith(".pdf") | key.endsWith(".txt")) { out2.write(outStr.replace("URN", "Filename")); }
					//if (key.endsWith(".pdf") | key.endsWith(".txt")) { Util.writeToFile(new File(filename.replace(".csv", "_unknownURN.csv")), "utf-8", outStr.replace("URN", "Filename"), true); }
					else {out.write(outStr); }
					//else { Util.writeToFile(new File(filename), "utf-8", outStr, true); }
					if (refMap.containsKey(key)) { studyRefs = refMap.get(key); }
					studyRefs.add(pubStudyNameVersionLinkConf);
					refMap.put(key, studyRefs);
				}
			}
			out.close();
			out2.close();
		}
		catch (IOException e) { e.printStackTrace(); } 
	}
	
	/**
	 * Writes the contents of the links in linkMap to file filename.
	 * 
	 * When generating snippets, not only add snippets for found references, also output snippets of 
	 * all other occurrences of the identified dataset titles.
	 * 
	 * @param linkMap			the map containing all links
	 * @param filename			output file
	 * @param indexDir			location of the lucene index
	 * @param snippetFilename	name of the file containing all snippets for all found dataset names
	 * @param idMap				map listing document filenames and their IDs
	 * @param logFilename		name of the log file
	 * @param useExistingSnippetCache	if true use existing snippetFilename, else create
	 */
	public void outputLinkMap(HashMap<String, Collection<StudyLink>> linkMap, String filename, String indexDir, String snippetFilename, HashMap<String,String> idMap, String logFilename, boolean useExistingSnippetCache) throws IOException
	{
		String delimiter = Util.delimiter_csv;
		String pubStudyNameVersionLinkConf = "";
		
		HashSet<String> allRefNames = getAllRefNames(linkMap);
		
		// search for all occurrences of ref names in the documents and output snippet map
		// alternatively, use cache of previous searches
		if (!useExistingSnippetCache) {	saveAllSnippets(indexDir, allRefNames, snippetFilename); }
		HashMap<String,HashMap<String,HashSet<String>>> linkDocMap = getLinkDocMap(snippetFilename, idMap);
		try 
		{
			OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(filename), "UTF-8");
			BufferedWriter out = new BufferedWriter(fstream);
			OutputStreamWriter fstream2 = new OutputStreamWriter(new FileOutputStream(filename.replace(".csv", "_unknownURN.csv")), "UTF-8");
			BufferedWriter out2 = new BufferedWriter(fstream2);
			for (String key : linkMap.keySet())
			{
				System.out.println("Processing " + key);
				// refMap: contains all previously processed distinct references (distinct = link having same refString, same doi, same confidence
				HashMap<String, HashSet<String>> refMap = new HashMap<String, HashSet<String>>();
				
				// output only one link when multiple references are mapped to same study but merge and include all snippets for this link
				HashSet<StudyLink> mergedLinks = mergeSnippets(linkMap.get(key));
				for (StudyLink ref : mergedLinks)
				{
					HashSet<String> studyRefs = new HashSet<String>();
					// ignore duplicates
					pubStudyNameVersionLinkConf = key + ref.name + ref.version + ref.link + ref.confidence + ref.method;
					if (isDuplicate(key, pubStudyNameVersionLinkConf, refMap)) { continue; }
					
					// search for all occurences of reference study in the document and output contexts as snippets
					// instead of only the one snippet that was mapped to study
					HashSet<String> termBasedSnippets = getAllSnippetsForTermInDoc(ref.alt_name, key, linkDocMap, logFilename);
					Iterator<String> termSnippetIter = termBasedSnippets.iterator();
					String termSnippets = "";
					while (termSnippetIter.hasNext())
					{
						termSnippets += Util.delimiter_internal + termSnippetIter.next().replace(delimiter, "_");
					}
					System.out.println("term snippets: " + termSnippets); 
					System.out.println("ref snippets: " + ref.snippet);

					String outStr = "" + delimiter + key.replace(delimiter, "") + delimiter + "Publication" + 
							delimiter + "URN" + delimiter + ref.toString() + delimiter + ref.snippet + termSnippets + delimiter + 
							"LitStudy_automatic" + System.getProperty("line.separator");
					// export links for publications with unknown urn to separate files
					if (key.endsWith(".pdf") | key.endsWith(".txt")) { out2.write(outStr.replace("URN", "Filename")); }
					else {out.write(outStr); }
					if (refMap.containsKey(key)) { studyRefs = refMap.get(key); }
					studyRefs.add(pubStudyNameVersionLinkConf);
					refMap.put(key, studyRefs);
				}
			}
			out.close();
			out2.close();
		}
		catch (IOException e) { e.printStackTrace(); } 
	}
	
	
	/**
	 * Constructs a list of subcorpora in a specified basePath (for each subcorpus being stored in 
	 * a subfolder of the basePath)
	 */
	public static HashSet<File> getSubcorpora(String basePath, String prefix)
	{
		File baseDir = new File(basePath);
		HashSet<File> subcorpora = new HashSet<File>();
		for ( File subDir : baseDir.listFiles() )
		{
			if (subDir.getName().startsWith(prefix)) { subcorpora.add(subDir); }
		}
		return subcorpora;
	}
	
	//TODO: remove prefix and corresponding methods
	/**
	 * Maps all references found in the specified context files to records in the dara repository. 
	 * Snnipets... if indexPath is not null, ...
	 * 
	 * @param basePath			root path of the text corpus
	 * @param prefix			prefix of all subcorpora inside of basePath to process
	 * @param patterns			InfoLink context file generated using pattern-based search
	 * @param terms				InfoLink context file generated using term-based search
	 * @param outPath			output path
	 * @param indexPath			path of the lucene index
	 * @param urnDictPath		path to the map containing document filenames and corresponding IDs
	 * @param language			language of the documents / patterns
	 * @param searchInterface	data search interface URL
	 * @param queryCache		path of the query cache file
	 * @param externalURLs		path of the URL list for external datasets
	 */
	public static void mineContexts(String basePath, String prefix, String patterns, String terms, String outPath, String indexPath, String urnDictPath, String searchInterface, String queryCache, String externalURLs) throws IOException
	{
		StudyMatcher matcher = new StudyMatcher(searchInterface, queryCache, externalURLs);
		if (indexPath!=null)
		{
			if (!patterns.equals(" ")) { for (File subcorpus : getSubcorpora(basePath, prefix) ) { getLinks(subcorpus, outPath, indexPath, matcher, patterns, urnDictPath); }}
			if (!terms.equals(" ")) { for (File subcorpus : getSubcorpora(basePath, prefix)) { getLinks_termSearch(subcorpus.getName(), outPath, matcher, terms, urnDictPath); }}
		}
		else
		{
			if (!patterns.equals(" ")) { for (File subcorpus : getSubcorpora(basePath, prefix) ) { getLinks(subcorpus, outPath, matcher, patterns, urnDictPath); }}
			if (!terms.equals(" ")) { for (File subcorpus : getSubcorpora(basePath, prefix)) { getLinks_termSearch(subcorpus.getName(), outPath, matcher, terms, urnDictPath); }}
		}
	}
	
	/**
	 * Maps all references found in the specified context files to records in the dara repository.
	 * If indexPath is null, outputs snippets only for found references; else outputs snippets for all 
	 * occurrences of found dataset names
	 * 
	 * @param path				path to the document corpus
	 * @param patterns			InfoLink context file generated using pattern-based search
	 * @param terms				InfoLink context file generated using term-based search
	 * @param outPath			output path
	 * @param indexPath			path of the lucene index
	 * @param urnDictPath		path to the map containing document filenames and corresponding IDs
	 * @param language			language of the documents / patterns
	 * @param searchInterface	data search interface URL
	 * @param queryCache		path of the query cache file
	 * @param externalURLs		path of the URL list for external datasets
	 * @param refSnippetsOnly	if set, outputs snippets only for found references; else outputs snippets for all occurrences of found dataset names
	 */
	public static void mineContexts(File path, String patterns, String terms, String outPath, String indexPath, String idMapPath, String searchInterface, String queryCache, String externalURLs) throws IOException
	{
		StudyMatcher matcher = new StudyMatcher(searchInterface, queryCache, externalURLs);
		if (indexPath!=null)
		{
			if (!patterns.equals(" ")) { getLinks(path, outPath, indexPath, matcher, patterns, idMapPath); } 
			if (!terms.equals(" ")) { getLinks_termSearch(path.getName(), outPath, matcher, terms, idMapPath); }
		}
		else
		{
			if (!patterns.equals(" ")) { getLinks(path, outPath, matcher, patterns, idMapPath); } 
			if (!terms.equals(" ")) { getLinks_termSearch(path.getName(), outPath, matcher, terms, idMapPath); }
		}
	}
	
	//here only one version needed - terms are included anyways due to term search, right??
	public static void getLinks_termSearch(String corpusName, String outPath, StudyMatcher matcher, String foundMentionsFilename, String urnDictFilename)
	{
		ContextMiner minerKnown = new ContextMiner(corpusName);
		HashMap<String,String> ssoarURNmap = minerKnown.readIDmap(urnDictFilename);
		HashMap<String,HashSet<String[]>> resKnownStudies = minerKnown.getKnownStudyRefs(foundMentionsFilename, minerKnown.corpusName, ssoarURNmap);
		// sort by reference type: doi vs. url vs. string
		HashMap<String, Collection<StudyLink>> links_doi = new HashMap<String, Collection<StudyLink>>();
		HashMap<String, Collection<StudyLink>> links_url = new HashMap<String, Collection<StudyLink>>();
		HashMap<String, Collection<StudyLink>> links_string = new HashMap<String, Collection<StudyLink>>();				
		ArrayList<HashMap<String, Collection<StudyLink>>> links_doiUrlString = new ArrayList<HashMap<String, Collection<StudyLink>>>();
		links_doiUrlString.add(links_doi);
		links_doiUrlString.add(links_url);
		links_doiUrlString.add(links_string);
						
		links_doiUrlString = minerKnown.mineStudyRefs(resKnownStudies, ssoarURNmap, matcher, links_doiUrlString, ExtractionMethod.TERM);
		System.out.println("Done mining term search results.");
		
		links_doi = links_doiUrlString.get(0);
		links_url = links_doiUrlString.get(1);
		links_string = links_doiUrlString.get(2);
		// include snippets of found references
		minerKnown.outputLinkMap(links_doi, outPath + File.separator + "links_doi_terms_unfiltered.csv");
		minerKnown.outputLinkMap(links_url, outPath + File.separator + "links_url_terms_unfiltered.csv");
		minerKnown.outputLinkMap(links_string, outPath + File.separator + "links_string_terms_unfiltered.csv");
	}
	
	/**
	 * Mines the contexts of Learner, matches them with records in da|ra and outputs the link files. refsnuppets...
	 */
	public static void getLinks(File corpus, String outputPath, String indexDir, StudyMatcher matcher, String foundContextsFilename, String idMapPath) throws IOException
	{
		String corpusName = corpus.getName();		
		ContextMiner miner = new ContextMiner(foundContextsFilename, corpusName);
		HashMap<String,String> ssoarURNmap = miner.readIDmap(idMapPath);
		ArrayList<HashMap<String,HashSet<String[]>>> docMaps = miner.getTitleAndVersion(ssoarURNmap);
		// key: filename, value: set of study references (each reference consists of a name and a version)
		HashMap<String,HashSet<String[]>> docStudyMap = docMaps.get(0);
		// key: filename, value: set of contexts (each context consists of left context, study name, right context)
		HashMap<String,HashSet<String[]>> docNoVersionMap = docMaps.get(1);
				
		// sort by reference type: doi vs. url vs. string
		HashMap<String, Collection<StudyLink>> links_doi = new HashMap<String, Collection<StudyLink>>();
		HashMap<String, Collection<StudyLink>> links_url = new HashMap<String, Collection<StudyLink>>();
		HashMap<String, Collection<StudyLink>> links_string = new HashMap<String, Collection<StudyLink>>();
				
		ArrayList<HashMap<String, Collection<StudyLink>>> links_doiUrlString = new ArrayList<HashMap<String, Collection<StudyLink>>>();
		links_doiUrlString.add(links_doi);
		links_doiUrlString.add(links_url);
		links_doiUrlString.add(links_string);
		
		links_doiUrlString = miner.mineStudyRefs(docStudyMap, ssoarURNmap, matcher, links_doiUrlString, ExtractionMethod.PATTERN);
		System.out.println("Done mining references with version specification.");
		// for studies without numerical information (on years, numbers or versions)
		links_doiUrlString = miner.mineStudyRefs(docNoVersionMap, ssoarURNmap, matcher, links_doiUrlString, ExtractionMethod.PATTERN);
		System.out.println("Done mining references without any numerical information.");
			
		links_doi = links_doiUrlString.get(0);
		links_url = links_doiUrlString.get(1);
		links_string = links_doiUrlString.get(2);

		String logFilename = outputPath + File.separator + "snippetErr.log";
		String snippetFilename = outputPath + File.separator + "allSnippets.xml";
		miner.outputLinkMap(links_doi, outputPath + File.separator + "links_doi_patterns_unfiltered.csv", indexDir, snippetFilename, ssoarURNmap, logFilename, false);
		miner.outputLinkMap(links_url, outputPath + File.separator + "links_url_patterns_unfiltered.csv", indexDir, snippetFilename, ssoarURNmap, logFilename, false);
		miner.outputLinkMap(links_string, outputPath + File.separator + "links_string_patterns_unfiltered.csv", indexDir, snippetFilename, ssoarURNmap, logFilename, false);
	}
	
	public static void getLinks(File corpus, String outputPath, StudyMatcher matcher, String foundContextsFilename, String idMapPath)
	{
		String corpusName = corpus.getName();		
		ContextMiner miner = new ContextMiner(foundContextsFilename, corpusName);
		HashMap<String,String> ssoarURNmap = miner.readIDmap(idMapPath);
		ArrayList<HashMap<String,HashSet<String[]>>> docMaps = miner.getTitleAndVersion(ssoarURNmap);
		// key: filename, value: set of study references (each reference consists of a name and a version)
		HashMap<String,HashSet<String[]>> docStudyMap = docMaps.get(0);
		// key: filename, value: set of contexts (each context consists of left context, study name, right context)
		HashMap<String,HashSet<String[]>> docNoVersionMap = docMaps.get(1);
				
		// sort by reference type: doi vs. url vs. string
		HashMap<String, Collection<StudyLink>> links_doi = new HashMap<String, Collection<StudyLink>>();
		HashMap<String, Collection<StudyLink>> links_url = new HashMap<String, Collection<StudyLink>>();
		HashMap<String, Collection<StudyLink>> links_string = new HashMap<String, Collection<StudyLink>>();
				
		ArrayList<HashMap<String, Collection<StudyLink>>> links_doiUrlString = new ArrayList<HashMap<String, Collection<StudyLink>>>();
		links_doiUrlString.add(links_doi);
		links_doiUrlString.add(links_url);
		links_doiUrlString.add(links_string);
		
		links_doiUrlString = miner.mineStudyRefs(docStudyMap, ssoarURNmap, matcher, links_doiUrlString, ExtractionMethod.PATTERN);
		System.out.println("Done mining references with version specification.");
		// for studies without numerical information (on years, numbers or versions)
		links_doiUrlString = miner.mineStudyRefs(docNoVersionMap, ssoarURNmap, matcher, links_doiUrlString, ExtractionMethod.PATTERN);
		System.out.println("Done mining references without any numerical information.");
			
		links_doi = links_doiUrlString.get(0);
		links_url = links_doiUrlString.get(1);
		links_string = links_doiUrlString.get(2);
		
		miner.outputLinkMap(links_doi, outputPath + File.separator + "links_doi_patterns_unfiltered.csv");
		miner.outputLinkMap(links_url, outputPath + File.separator + "links_url_patterns_unfiltered.csv");
		miner.outputLinkMap(links_string, outputPath + File.separator + "links_string_patterns_unfiltered.csv");
	}
	
	//...
	
	/**
	 * Calls the <emph>mineContexts</emph> method with the specified parameters and the current data 
	 * search interface.
	 * 
	 * @param args	
	 * <ul>
	 * <li>args[0]: basePath (root path of text corpus)</li>
	 * <li>args[1]: prefix of all subcorpora (all subdirectories inside of basePath containing text documents to process). Set to " " to not process any subcorpora</li>
	 * <li>args[2]: contexts_patterns_filename (input path for all contexts found by pattern-based search)</li>
	 * <li>args[3]: contexts_terms_filename (input path for all contexts found by term-based search)</li>
	 * <li>args[4]: output path</li>
	 * <li>args[5]: path of the lucene index</li>
	 * <li>args[6]: path of the document filename ID map</li>
	 * <li>args[7]: path of the query cache used for mapping dataset references to records in dara. Set to " " if no query cache shall be used</li>
	 * <li>args[8]: path of the file listing external URLs for external datasets. Set to " " if no external URL list shall be used.</li>
	 * </ul>
	 */
	public static void main(String[] args) throws IOException //throws IOException//when reading link map...
	{ 
        if (args.length < 9) {
            System.out.println("Not enough arguments to ContextMiner");
            System.out.println("args[0]: basePath (root path of text corpus)");
            System.out.println("args[1]: prefix of all subcorpora (all subdirectories inside of basePath containing text documents to process). Set to \" \" to not process any subcorpora");
            System.out.println("args[2]: contexts_patterns_filename (input path for all contexts found by pattern-based search)");
            System.out.println("args[3]: contexts_terms_filename (input path for all contexts found by term-based search)");
            System.out.println("args[4]: output path");
            System.out.println("args[5]: path of the lucene index");
            System.out.println("args[6]: path of the document filename ID map");
            System.out.println("args[7]: path of the query cache used for mapping dataset references to records in dara. Set to \" \" if no query cache shall be used");
            System.out.println("args[8]: path of the file listing external URLs for external datasets. Set to \" \" if no external URL list shall be used.");
            throw new IllegalArgumentException("Not enough arguments to ContextMiner.main");
        }
		String basePath = args[0];
		String prefix = args[1];
		
		String contexts_patterns_filename =  args[2];
		String contexts_terms_filename =  args[3];
		
		String outPath = args[4];
		String indexPath = args[5]; if (indexPath.equals(" ")) { indexPath = null; }
		String urnDictPath  = args[6];
		
		String queryCache = args[7]; if (queryCache.equals(" ")) { queryCache = null; }
		String externalURLs = args[8]; if (externalURLs.equals(" ")) { externalURLs = null; }
		String daraSearchInterface = "http://www.da-ra.de/dara/study/web_search_show";
		
		if (!prefix.equals(" ")) { mineContexts(basePath, prefix, contexts_patterns_filename, contexts_terms_filename, outPath, indexPath, urnDictPath, daraSearchInterface, queryCache, externalURLs); }
		else { mineContexts(new File(basePath), contexts_patterns_filename, contexts_terms_filename, outPath, indexPath, urnDictPath, daraSearchInterface, queryCache, externalURLs); }
	}
}
