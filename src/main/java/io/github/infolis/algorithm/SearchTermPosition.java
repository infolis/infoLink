package io.github.infolis.algorithm;

import io.github.infolis.infolink.luceneIndexing.Indexer;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.LimitedTimeMatcher;
import io.github.infolis.util.RegexUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
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
 * @author kata
 * @author kba
 *
 */

public class SearchTermPosition extends BaseAlgorithm
{ 
	
	private Execution execution;
	private static final Logger log = LoggerFactory.getLogger(SearchTermPosition.class);
	private static final String DEFAULT_FIELD_NAME = "contents";
	private static final String ALLOWED_CHARS = "[\\s\\-–\\\\/:.,;()&_?!]";

        @Override
	public Execution getExecution() {
		return execution;
	}

        @Override
	public void setExecution(Execution execution) {
		this.execution = execution;
	}
	
//	/**
//	 * Main method calling complexSearch method for given arguments
//	 * 
//	 * @param args	args[0]: path to lucene index; args[1]: path to output file; args[2]: search term; args[3]: lucene query
//	 * @throws IOException
//	 * @throws ParseException
//	 * @throws org.apache.lucene.queryParser.ParseException
//	 */
//	public static void main(String[] args) throws IOException, ParseException
//	{
//		if (args.length < 4) {
//			System.out.println("Usage: SearchTermPosition <indexPath> <filename> <term> <query>");
//			System.out.println("<indexPath>	location of the Lucene index");
//			System.out.println("<filename>	path to the output file");
//			System.out.println("<term>	the term to retrieve");
//			System.out.println("<query>	the lucene query to search for term");
//			System.exit(1);
//		}
//		Execution execution = new Execution();
//		execution.setAlgorithm(SearchTermPosition.class);
//
//		execution.getInputFiles().add(args[0]);
////		execution.getOutputFiles().add(args[1]);
//		execution.setSearchTerm(args[2]);
//		execution.setSearchQuery(args[3]);
//		
//		SearchTermPosition algo = new SearchTermPosition();
//		algo.setFileResolver(FileResolverFactory.create(DataStoreStrategy.LOCAL));
//		algo.setDataStoreClient(DataStoreClientFactory.create(DataStoreStrategy.LOCAL));
//		algo.setExecution(execution);
//
//		algo.run();
//
//	} 
	
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
		Path tempPath = Files.createTempDirectory("infolis-index-");
		FileUtils.forceDeleteOnExit(tempPath.toFile());
		
		createIndex(tempPath);
		
		Directory d = FSDirectory.open(tempPath.toFile());
		
		IndexReader r = IndexReader.open(d);
		IndexSearcher searcher = new IndexSearcher(r);
		Analyzer analyzer = Indexer.createAnalyzer();
		QueryParser qp = new ComplexPhraseQueryParser(Version.LUCENE_35, DEFAULT_FIELD_NAME, analyzer);
		// set phrase slop because dataset titles may consist of more than one word
		qp.setPhraseSlop(this.execution.getPhraseSlop()); // 0 requires exact match, 5 means that up to 5 edit operations may be carried out...
		qp.setAllowLeadingWildcard(this.execution.isAllowLeadingWildcards());
		BooleanQuery.setMaxClauseCount(this.execution.getMaxClauseCount());
		// throws java.lang.IllegalArgumentException: Unknown query type "org.apache.lucene.search.WildcardQuery"
		// if quotes are present in absence of any whitespace inside of query
		// however, queries should be passed in correct form instead of being changed here
		Query q;
		try {
			q = qp.parse(this.execution.getSearchQuery().trim());
		}
		catch (ParseException e) {
			this.execution.fatal("Could not parse searchquery '" + this.execution.getSearchQuery() + "'.");
			this.execution.setStatus(ExecutionStatus.FAILED);
				
			searcher.close();
			throw new RuntimeException();
		}

		log.debug("Query: " + q.toString());
		TopDocs td = searcher.search(q,10000);
		log.debug("Number of hits: " + td.totalHits);
		ScoreDoc[] sd = td.scoreDocs;
		
		for (int i = 0; i < sd.length; i++)
		{
			Document doc = searcher.doc(sd[i].doc);
//			log.debug(doc.get("path"));
			InfolisFile file = getDataStoreClient().get(InfolisFile.class, (doc.get("path")));
//			log.debug("{}", file);
			
			InputStream openInputStream = this.getFileResolver().openInputStream(file);
			String text = IOUtils.toString(openInputStream);
			openInputStream.close();
			
			// note: this class is meant for searching for terms, not for patterns
			// used to generate contexts for inducing new patterns, not for creating output
			// output contexts are those created by pattern-based search
			// Add contexts
			if (this.getExecution().getSearchTerm() != null) {
				for (StudyContext sC : getContexts(file.getUri(), this.getExecution().getSearchTerm(), text)) {
					getDataStoreClient().post(StudyContext.class, sC);
					this.execution.getStudyContexts().add(sC.getUri());
				}
			}
			getExecution().getMatchingFilenames().add(file.getUri());
		}
		searcher.close();
		analyzer.close();
		r.close();
		d.close();
	}
	
	private void createIndex(Path tempPath) throws IOException {
		Execution execution = new Execution();
		execution.setAlgorithm(Indexer.class);
		execution.setInputFiles(this.getExecution().getInputFiles());
		execution.setIndexDirectory(tempPath.toString());
		
		Algorithm algo = execution.instantiateAlgorithm(getDataStoreClient(), getFileResolver());
		algo.execute();
	}

	List<StudyContext> getContexts(String fileName, String term, String text) throws IOException
	{
	    // search for phrase using regex
	    // first group: left context (consisting of 5 words)
	    // second group: right context (consisting of 5 words)
	    // contexts may or may not be separated from the query by whitespace!
	    // e.g. "Eurobarometer-Daten" with "Eurobarometer" as query term
	    // pattern should be case-sensitive! Else, e.g. the study "ESS" would be found in "vergessen"...
	    // Pattern pat = Pattern.compile( leftContextPat + query + rightContextPat, Pattern.CASE_INSENSITIVE );
	    InfolisPattern infolisPat = null;
	    Pattern pat = Pattern.compile(RegexUtils.leftContextPat_ + Pattern.quote(term) + RegexUtils.rightContextPat_);
	    String threadName = String.format("For '%s' in '%s...'", pat, text.substring(0, Math.min(100, text.length())));
		LimitedTimeMatcher ltm = new LimitedTimeMatcher(pat, text, 10_000, threadName);
//	    log.debug(text);
	    List<StudyContext> contextList = new LinkedList<StudyContext>();
	    ltm.run();
//	    log.debug("Pattern: " + pat + " found " + ltm.matched());
	    if (ltm.finished() && !ltm.matched()) {	//TODO: this checks for more characters than actually replaced by currently used analyzer - not neccessary and not a nice way to do it
	    	// refer to normalizeQuery for a better way to do this
	    	String[] termParts = term.split("\\s+");
	    	String term_normalized = "";
	    	for (String part : termParts) {
	    		term_normalized += Pattern.quote(
	    				part.replace("-", " ")
	    					.replace("–", " ")
	    					.replace(".", " ")
	    					.replace("<", " ")
	    					.replace(">", " ")
	    					.replace("(", " ")
	    					.replace(")", " ")
	    					.replace(":", " ")
	    					.replace(",", " ")
	    					.replace(";", " ")
	    					.replace("/", " ")
	    					.replace("\\", " ")
	    					.replace("&", " ")
                            .replace("_", "")
                            .replace("?", "")
                            .replace("!", "")
                            .trim()
                        ) + ALLOWED_CHARS;
	    	}
	    	pat = Pattern.compile(RegexUtils.leftContextPat_ + ALLOWED_CHARS + term_normalized + RegexUtils.rightContextPat_);
	    	threadName = String.format("2222221212 Term '%s%s' in '%s[...]'", ALLOWED_CHARS, term_normalized, text.substring(0, Math.min(100, text.length())));
	    	ltm = new LimitedTimeMatcher(pat, text, 10_000, threadName);
	    	ltm.run();
	    }
	    // these patterns are used for extracting contexts of known study titles, do not confuse with patterns to detect study references
	    if (ltm.finished() && ltm.matched()) {
	    	infolisPat = new InfolisPattern(pat.toString());
	    	this.getDataStoreClient().post(InfolisPattern.class, infolisPat);
//	    	log.debug("Posted Pattern: {}", infolisPat.getUri());
	    }
	    while (ltm.matched()) {
//	    	log.debug("Pattern: " + pat + " found " + ltm.matched());
	    	StudyContext sC = new StudyContext();
	    	sC.setLeftText(ltm.group(1).trim());
	    	//sC.setLeftWords(Arrays.asList(m.group(1).trim().split("\\s+")));
	    	sC.setLeftWords(Arrays.asList(ltm.group(2).trim(), ltm.group(3).trim(), ltm.group(4).trim(), ltm.group(5).trim(), ltm.group(6).trim()));
	    	sC.setTerm(term);
	    	sC.setRightText(ltm.group(7).trim());
	    	//sC.setRightWords(Arrays.asList(m.group(2).trim().split("\\s+")));
	    	sC.setRightWords(Arrays.asList(ltm.group(8).trim(), ltm.group(9).trim(), ltm.group(10).trim(), ltm.group(11).trim(), ltm.group(12).trim()));
	    	sC.setFile(fileName);
	    	sC.setPattern(infolisPat.getUri());

	    	contextList.add(sC);
	    	ltm.run();
	    }
	    return contextList;
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
//		if (null != this.getExecution().getOutputFiles()
//				 && !this.getExecution().getOutputFiles().isEmpty())
//			throw new IllegalAlgorithmArgumentException(getClass(), "outputFiles", "must NOT be set");
//		if (null == this.getExecution().getInputFiles()
//				 || this.getExecution().getInputFiles().isEmpty())
//			throw new IllegalAlgorithmArgumentException(getClass(), "inputFiles", "must be set and non-empty");
		if (null == this.getExecution().getSearchQuery())
			throw new IllegalAlgorithmArgumentException(getClass(), "searchQuery", "must be set and non-empty");
	}
}
