package searching;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import patternLearner.Util;
import luceneIndexing.Indexer;

/**
 * Class for searching terms and complex phrase queries using a Lucene index.
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
public class Search_Term_Position 
{ 
	
	public String indexPath; // location of the lucene index
	public String filename; // name of the output file
	public String term; // search term
	public String query; // search query
	
	/**
	 * Class constructor specifying the path of the Lucene index to use for searching, the name of the 
	 * output file for saving the found contexts, the search term and the search query.
	 * 
	 * Difference between search term and search query: a search term represents the entity to retrieve, 
	 * e.g. the name of a dataset, while the complete query may include additional information, e.g. 
	 * allowed characters surrounding the term to retrieve.
	 * 
	 * Example: when searching for datasets of the study "Eurobarometer", entities like 
	 * "Eurobarometer-Datensatz" shall be found as well but only "Eurobarometer" shall be listed as 
	 * search term in the output file.
	 * 
	 * @param indexPath	location of the Lucene index
	 * @param filename	path to the output file
	 * @param term		the term to retrieve
	 * @param query		the lucene query to search for term
	 */
	public Search_Term_Position(String indexPath, String filename, String term, String query)
	{
		this.indexPath = indexPath;
		this.filename = filename;
		this.term = term;
		this.query = query;
	}
	
	/**
	 * Main method calling complexSearch method for given arguments
	 * 
	 * @param args	args[0]: path to lucene index; args[1]: path to output file; args[2]: search term; args[3]: lucene query
	 * @throws IOException
	 * @throws ParseException
	 * @throws org.apache.lucene.queryParser.ParseException
	 */
	public static void main(String[] args) throws IOException, ParseException, org.apache.lucene.queryParser.ParseException 
	{
		if (args.length < 4) {
			System.out.println("Usage: Search_Term_Position <indexPath> <filename> <term> <query>");
			System.out.println("<indexPath>	location of the Lucene index");
			System.out.println("<filename>	path to the output file");
			System.out.println("<term>	the term to retrieve");
			System.out.println("<query>	the lucene query to search for term");
			System.exit(1);
		}
		Search_Term_Position termSearcher = new Search_Term_Position(args[0], args[1], args[2], args[3]); 
		try { termSearcher.complexSearch(new File(termSearcher.filename), true); } catch (Exception e) { e.printStackTrace();}
	} 
	
	/**
	 * Normalizes a query by applying a Lucene analyzer. Make sure the analyzer used here is the 
	 * same as the analyzer used for indexing the text files!
	 * 
	 * @param 	query	the Lucene query to be normalized
	 * @return	a normalized version of the query
	 */
	public static String normalizeQuery(String query) 
	{
		Analyzer analyzer = Indexer.getAnalyzer();
		String field = "contents";
		String result = new String();
        TokenStream stream  = analyzer.tokenStream(field, new StringReader(query));
        try 
        {
            while(stream.incrementToken()) { result += " " + (stream.getAttribute(TermAttribute.class).term()); }
        }
        catch(IOException e) {
            // not thrown due to using a string reader...
        }
        analyzer.close();
        return result.trim();
    }  
	
	/**
	 * Searches for this query in this index using a ComplexPhraseQueryParser and writes the extracted  
	 * contexts to outputFile.
	 * 
	 * @param outputFile	path of the output file
	 * @param append		if set, contexts will be appended to file, else file will be overwritten
	 * @throws Exception
	 */
	public void complexSearch(File outputFile, boolean append) throws Exception
	{
		Directory d = FSDirectory.open(new File(this.indexPath));
		IndexReader r = IndexReader.open(d);
		IndexSearcher searcher = new IndexSearcher(r);
		String defaultFieldName="contents";
		Analyzer analyzer = Indexer.getAnalyzer();
		QueryParser qp=new ComplexPhraseQueryParser(Version.LUCENE_35,defaultFieldName,analyzer);
		//qp.setFuzzyPrefixLength(1); 
		qp.setPhraseSlop(7); // 0 requires exact match, 5 means that up to 5 edit operations may be carried out...
		qp.setAllowLeadingWildcard(true);
		BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
		//throws java.lang.IllegalArgumentException: Unknown query type "org.apache.lucene.search.WildcardQuery"
		//if quotes are present in absence of any whitespace inside of query
		Query q;
		try { q = qp.parse(this.query.trim()); }
		catch (IllegalArgumentException iae) { q = qp.parse(this.query.trim().replace("\"","")); }
		System.out.println("Query: " + q.toString());
		TopDocs td = searcher.search(q,10000);
		ScoreDoc[] sd = td.scoreDocs;
		
		// add xml header if not appending to existing xml file
		if (!append) { Util.prepareOutputFile(this.filename); }
		
		// highlighter does not work well with wildcard queries :-/
		//  also, the highlighted term/s may be at the beginning or end of the fragment - context 
		// of 5 preceding and following words not always included... therefore not usable here :(
		/*Formatter formatter = new SimpleHTMLFormatter(highlight_startTag, highlight_endTag);
		QueryScorer qs = new QueryScorer(q.rewrite(r), r, defaultFieldName, defaultFieldName);
		Highlighter high = new Highlighter(formatter, qs);
		high.setTextFragmenter(new SimpleSpanFragmenter(qs, 300));
		high.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);*/
		for (int i = 0; i < sd.length; i++)
		{
			Document doc = searcher.doc(sd[i].doc);
			System.out.println(doc.get("path"));
			/*String[] bestFrags = high.getBestFragments(analyzer, defaultFieldName, doc.get(defaultFieldName), 100);
			exportContexts(bestFrags, doc.get("path"), outputFile, append);*/
			new GetContext(doc.get("path"), term, 0, 10, new File(this.filename));
		}
		searcher.close();
		analyzer.close();
		r.close();
		d.close();
		if (!append) { Util.completeOutputFile(this.filename); }
	}
	
	/**
	 * Searches for this query in this index using a ComplexPhraseQueryParser and returns the paths to 
	 * the documents where a hit to at least one of the terms in the query was found.
	 * 
	 * @return	an array of all filenames where a hit was found
	 * @throws Exception
	 */
	public String[] complexSearch() throws Exception
	{
		Directory d = FSDirectory.open(new File(this.indexPath));
		IndexReader r = IndexReader.open(d);
		IndexSearcher searcher = new IndexSearcher(r);
		String defaultFieldName="contents";
		Analyzer analyzer = Indexer.getAnalyzer();
		QueryParser qp=new ComplexPhraseQueryParser(Version.LUCENE_35,defaultFieldName,analyzer);
		qp.setPhraseSlop(7); // 0 requires exact match, 5 means that up to 5 edit operations may be carried out...
		qp.setAllowLeadingWildcard(true);
		BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
		//throws java.lang.IllegalArgumentException: Unknown query type "org.apache.lucene.search.WildcardQuery"
		//if quotes are present in absence of any whitespace inside of query
		Query q;
		try { q = qp.parse(this.query.trim()); }
		catch (IllegalArgumentException iae) { q = qp.parse(this.query.trim().replace("\"","")); }
		System.out.println("Query: " + q.toString());
		TopDocs td = searcher.search(q,10000);
		ScoreDoc[] sd = td.scoreDocs;
		String[] scoreDocPaths = new String[sd.length];
		for (int i = 0; i < sd.length; i++)
		{
			Document doc = searcher.doc(sd[i].doc);
			System.out.println(doc.get("path"));
			scoreDocPaths[i] = doc.get("path");
		}
		searcher.close();
		analyzer.close();
		r.close();
		d.close();
		return scoreDocPaths;
	}
}
