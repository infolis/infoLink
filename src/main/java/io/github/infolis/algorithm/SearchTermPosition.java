package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.infolink.luceneIndexing.Indexer;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.InfolisFileUtils;
import io.github.infolis.util.RegexUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for searching terms and complex phrase queries using a Lucene index.
 * 
 * Difference between search term and search query: a search term represents the entity to retrieve, 
 * e.g. the name of a dataset, while the complete query may include additional information, e.g. 
 * allowed characters surrounding the term to retrieve.
 * 
 * Example: when searching for datasets of the study "Eurobarometer", entities like 
 * "Eurobarometer-Datensatz" shall be found as well but only "Eurobarometer" shall be listed as 
 * search term in the output file.
 * 
 * Parameters:
 * {@link Execution#getMaxClauseCount()}
 * {@link Execution#getPhraseSlop()}
 * {@link Execution#getSearchQuery()}
 * {@link Execution#getSearchTerm())}
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
//TODO: REMOVE FILENAME...
public class SearchTermPosition extends BaseAlgorithm
{ 
	
	private Execution execution;
	private static final Logger log = LoggerFactory.getLogger(SearchTermPosition.class);

	public Execution getExecution() {
		return execution;
	}

	public void setExecution(Execution execution) {
		this.execution = execution;
	}
	
	/**
	 * Main method calling complexSearch method for given arguments
	 * 
	 * @param args	args[0]: path to lucene index; args[1]: path to output file; args[2]: search term; args[3]: lucene query
	 * @throws IOException
	 * @throws ParseException
	 * @throws org.apache.lucene.queryParser.ParseException
	 */
	public static void main(String[] args) throws IOException, ParseException
	{
		if (args.length < 4) {
			System.out.println("Usage: SearchTermPosition <indexPath> <filename> <term> <query>");
			System.out.println("<indexPath>	location of the Lucene index");
			System.out.println("<filename>	path to the output file");
			System.out.println("<term>	the term to retrieve");
			System.out.println("<query>	the lucene query to search for term");
			System.exit(1);
		}
		Execution execution = new Execution();
		execution.setAlgorithm(SearchTermPosition.class);

		execution.getInputFiles().add(args[0]);
		execution.getOutputFiles().add(args[1]);
		execution.setSearchTerm(args[2]);
		execution.setSearchQuery(args[3]);
		
		SearchTermPosition algo = new SearchTermPosition();
		algo.setFileResolver(FileResolverFactory.create(DataStoreStrategy.LOCAL));
		algo.setDataStoreClient(DataStoreClientFactory.create(DataStoreStrategy.LOCAL));
		algo.setExecution(execution);

		algo.run();

	} 
	
	/**
	 * Normalizes a query by applying a Lucene analyzer. Make sure the analyzer used here is the 
	 * same as the analyzer used for indexing the text files!
	 * 
	 * @param 	query	the Lucene query to be normalized
	 * @return	a normalized version of the query
	 */
	@SuppressWarnings("deprecation")
	public static String normalizeQuery(String query, boolean quoteIfSpace)
	{
		Analyzer analyzer = Indexer.createAnalyzer();
		String field = "contents";
		String result = new String();
		TokenStream stream = analyzer.tokenStream(field, new StringReader(query));
		try
		{
			while (stream.incrementToken()) {
				result += " " + (stream.getAttribute(TermAttribute.class).term());
			}
		} catch (IOException e) {
			// not thrown due to using a string reader...
		}
		analyzer.close();
		if (quoteIfSpace && result.trim().matches(".*\\s.*")) {
				return "\"" + result.trim() + "\"";
		}
		return result.trim();
	}
	
	/**
	 * Searches for this query in this index using a ComplexPhraseQueryParser and writes the extracted  
	 * contexts to outputFile.
	 * 
	 * @param outputFile	path of the output file
	 * @param append		if set, contexts will be appended to file, else file will be overwritten
	 * @throws IOException 
	 */
	@Override
	public void execute() throws IOException
	{
		Directory d = FSDirectory.open(new File(execution.getIndexDirectory()));
		IndexReader r = IndexReader.open(d);
		IndexSearcher searcher = new IndexSearcher(r);
		String defaultFieldName="contents";
		Analyzer analyzer = Indexer.createAnalyzer();
		QueryParser qp = new ComplexPhraseQueryParser(Version.LUCENE_35, defaultFieldName, analyzer);
		// set phrase slop because dataset titles may consist of more than one word
		qp.setPhraseSlop(this.execution.getPhraseSlop()); // 0 requires exact match, 5 means that up to 5 edit operations may be carried out...
		qp.setAllowLeadingWildcard(this.execution.isAllowLeadingWildcards());
		BooleanQuery.setMaxClauseCount(this.execution.getMaxClauseCount());
		//throws java.lang.IllegalArgumentException: Unknown query type "org.apache.lucene.search.WildcardQuery"
		//if quotes are present in absence of any whitespace inside of query
		Query q;
		try {
			q = qp.parse(this.execution.getSearchQuery().trim());
		} catch (ParseException iae) {
			try {
				q = qp.parse(this.execution.getSearchQuery().trim().replace("\"", ""));
			} catch (ParseException e) {
				this.execution.logFatal("Could not parse searchquery '" + this.execution.getSearchQuery() + "'.");
				this.execution.setStatus(ExecutionStatus.FAILED);
				
				searcher.close();
				throw new RuntimeException();
			}
		}
		System.out.println("Query: " + q.toString());
		TopDocs td = searcher.search(q,10000);
		ScoreDoc[] sd = td.scoreDocs;
		
		// add xml header if not appending to existing xml file
		if (this.execution.isOverwrite()) {
			InfolisFileUtils.prepareOutputFile(this.execution.getFirstOutputFile());
		}
		
		for (int i = 0; i < sd.length; i++)
		{
			Document doc = searcher.doc(sd[i].doc);
			System.out.println(doc.get("path"));

			String text = InfolisFileUtils.readFile(new File(doc.get("path")), "UTF-8");
			
			// Add contexts
			for (StudyContext sC : getContexts(doc.get("path"), this.getExecution().getSearchTerm(), text)) {
				getDataStoreClient().post(StudyContext.class, sC);
				this.execution.getStudyContexts().add(sC.getUri());
			}
			log.debug(doc.get("path"));

			getExecution().getMatchingFilenames().add(doc.get("path"));
		}
		searcher.close();
		analyzer.close();
		r.close();
		d.close();
		if (!this.execution.isOverwrite()) {
			InfolisFileUtils.completeOutputFile(this.execution.getFirstOutputFile());
		}
	}
	
	protected static List<StudyContext> getContexts(String filename, String term, String text) throws IOException
	{
	    // search for phrase using regex
	    // first group: left context (consisting of 5 words)
	    // second group: right context (consisting of 5 words)
	    // contexts may or may not be separated from the query by whitespace!
	    // e.g. "Eurobarometer-Daten" with "Eurobarometer" as query term
	    // pattern should be case-sensitive! Else, e.g. the study "ESS" would be found in "vergessen"...
	    // Pattern pat = Pattern.compile( leftContextPat + query + rightContextPat, Pattern.CASE_INSENSITIVE );
	    Pattern pat = Pattern.compile(RegexUtils.leftContextPat + Pattern.quote(term) + RegexUtils.rightContextPat);
	    Matcher m = pat.matcher(text);
            log.debug("Pattern: " + pat + " found " + m.find());
	    List<StudyContext> contextList = new ArrayList<StudyContext>();
	    //TODO: USE SAFEMATCHING
	    //TODO: PUT SAFEMATCHING IN OTHER CLASS..
	    boolean matchFound = m.find();
	    if (matchFound == false) 
	    
	    {	//TODO: this checks for more characters than actually replaced by currently used analyzer - not neccessary and not a nice way to do it
	    	// refer to normalizeQuery for a better way to do this
	    	String[] termParts = term.split("\\s+");
	    	String term_normalized = "";
	    	for (String part : termParts) {
	    		term_normalized += Pattern.quote(
	    				part.replace("-", " ")
	    					.replace("–", " ")
	    					.replace(".", " ")
	    					.replace("(", " ")
	    					.replace(")", " ")
	    					.replace(":", " ")
	    					.replace(",", " ")
	    					.replace(";", " ")
	    					.replace("/", " ")
	    					.replace("\\", " ")
	    					.replace("&", " ")
                            .replace("_", "")
                            .trim()
                        ) + "[\\s\\-–\\\\/:.,;()&_]";
	    	}
	    	pat = Pattern.compile(RegexUtils.leftContextPat + "[\\s\\-–\\\\/:.,;()&_]" + term_normalized + RegexUtils.rightContextPat);
	    	m = pat.matcher(text);
	    	matchFound = m.find();
	    }
            log.debug("Pattern: " + pat + " found " + matchFound);
	    while (matchFound)
	    {
	    	StudyContext newContext = new StudyContext(m.group(1).trim(), term, m.group(2).trim(), filename, null);
	    	contextList.add(newContext);
	    	matchFound = m.find();
	    }
	    return contextList;
	}

	@Override
	public void validate() {
		if (null == this.getExecution().getOutputFiles()
				|| this.execution.getOutputFiles().size() != 1) {
			throw new IllegalArgumentException("Must set exactly one outputFile!");
		}
		if (null == this.execution.getIndexDirectory()) {
			throw new IllegalArgumentException("Index directory not set.");
		}
		if (!Files.exists(Paths.get(this.execution.getIndexDirectory()))) {
			throw new IllegalArgumentException("Index directory doesn't exist.");
		}
	}
	
}
