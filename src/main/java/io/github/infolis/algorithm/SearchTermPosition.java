package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Publication;
import io.github.infolis.util.LimitedTimeMatcher;
import io.github.infolis.util.RegexUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
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
	
	public SearchTermPosition(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}

	private static final Logger log = LoggerFactory.getLogger(SearchTermPosition.class);
	private static final String DEFAULT_FIELD_NAME = "contents";
	private static final String ALLOWED_CHARS = "[\\s\\-–\\\\/:.,;()&_?!]";

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
		Execution indexExecution = createIndex();
		
		IndexSearcher searcher = new IndexSearcher(IndexReader.open(FSDirectory.open(new File(indexExecution.getOutputDirectory()))));
		QueryParser qp = new ComplexPhraseQueryParser(Version.LUCENE_35, DEFAULT_FIELD_NAME, Indexer.createAnalyzer());
		// set phrase slop because dataset titles may consist of more than one word
		qp.setPhraseSlop(indexExecution.getPhraseSlop()); // 0 requires exact match, 5 means that up to 5 edit operations may be carried out...
		qp.setAllowLeadingWildcard(indexExecution.isAllowLeadingWildcards());
		BooleanQuery.setMaxClauseCount(indexExecution.getMaxClauseCount());
		// throws java.lang.IllegalArgumentException: Unknown query type "org.apache.lucene.search.WildcardQuery"
		// if quotes are present in absence of any whitespace inside of query
		// however, queries should be passed in correct form instead of being changed here
		Query q;
		try {
			q = qp.parse(getExecution().getSearchQuery().trim());
		}
		catch (ParseException e) {
			fatal(log, "Could not parse searchquery '%s'", indexExecution.getSearchQuery());
			getExecution().setStatus(ExecutionStatus.FAILED);
			searcher.close();
			throw new RuntimeException();
		}

		debug(log, "Query: " + q.toString());
		TopDocs td = searcher.search(q, 10000);
		debug(log, "Number of hits: " + td.totalHits);
		ScoreDoc[] scoreDocs = td.scoreDocs;
		
		for (int i = 0; i < scoreDocs.length; i++)
		{
			Document doc = searcher.doc(scoreDocs[i].doc);
//			log.debug(doc.get("path"));
			InfolisFile file = getInputDataStoreClient().get(InfolisFile.class, doc.get("path"));
//			log.debug("{}", file);
			
			InputStream openInputStream = this.getInputFileResolver().openInputStream(file);
			String text = IOUtils.toString(openInputStream);
			openInputStream.close();
			
			// note: this class is meant for searching for terms, not for patterns
			// used to generate contexts for inducing new patterns, not for creating output
			// output contexts are those created by pattern-based search
			// Add contexts
			if (this.getExecution().getSearchTerm() != null) {
				for (TextualReference sC : getContexts(getInputDataStoreClient(), file.getUri(), getExecution().getSearchTerm(), text)) {
					
					// note that the URI changes if inputDataStoreClient != outputDataStoreClient!
					getOutputDataStoreClient().post(TextualReference.class, sC);
					getExecution().getStudyContexts().add(sC.getUri());
				}
			}
			getExecution().getMatchingFilenames().add(file.getUri());
		}
		searcher.close();
		Indexer.createAnalyzer().close();
		IndexReader.open(FSDirectory.open(new File(indexExecution.getOutputDirectory()))).close();
		FSDirectory.open(new File(indexExecution.getOutputDirectory())).close();
		log.debug("Finished SearchTermPosition#execute");
	}
	
	private Execution createIndex() throws IOException {
		Execution execution = new Execution();
		execution.setAlgorithm(Indexer.class);
		execution.setInputFiles(this.getExecution().getInputFiles());
		execution.setAllowLeadingWildcards(this.getExecution().isAllowLeadingWildcards());
//		0 requires exact match, 5 means that up to 5 edit operations may be carried out...
		execution.setPhraseSlop(this.getExecution().getPhraseSlop());
		BooleanQuery.setMaxClauseCount(this.getExecution().getMaxClauseCount());

		Algorithm algo = execution.instantiateAlgorithm(this);
		algo.execute();

		return execution;
	}

	public static List<TextualReference> getContexts(DataStoreClient outputDataStoreClient, String fileName, String term, String text) throws IOException
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
	    List<TextualReference> contextList = new ArrayList<>();
	    ltm.run();
//	    log.debug("Pattern: " + pat + " found " + ltm.matched());
	    if (ltm.finished() && !ltm.matched()) {
	    	pat = Pattern.compile(RegexUtils.leftContextPat_ + ALLOWED_CHARS + removeSpecialCharsFromTerm(term) + RegexUtils.rightContextPat_);
	    	ltm = new LimitedTimeMatcher(pat, text, 10_000, threadName);
	    	ltm.run();
	    }

	    // these patterns are used for extracting contexts of known study titles, do not confuse with patterns to detect study references
	    if (! ltm.finished()) {
	    	throw new IOException("Matcher timed out!");
	    } 
	    if (ltm.finished() && ltm.matched()) {
	    	infolisPat = new InfolisPattern(pat.toString());
	    	outputDataStoreClient.post(InfolisPattern.class, infolisPat);
//	    	log.debug("Posted Pattern: {}", infolisPat.getUri());
	    }
	    while (ltm.matched()) {
                
//	    	log.debug("Pattern: " + pat + " found " + ltm.matched());
                Publication p = new Publication();
                outputDataStoreClient.post(Publication.class, p);
	    	TextualReference sC = new TextualReference(ltm.group(1).trim(), term, ltm.group(7).trim(), fileName, infolisPat.getUri(), p.getUri());
	    	contextList.add(sC);
	    	ltm.run();
	    }
	    return contextList;
	}

	//TODO: this checks for more characters than actually replaced by currently used analyzer - not neccessary and not a nice way to do it
	// refer to normalizeQuery for a better way to do this
	private static String removeSpecialCharsFromTerm(String term) {
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
		return term_normalized;
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
//		if (null != this.getExecution().getOutputFiles()
//				 && !this.getExecution().getOutputFiles().isEmpty())
//			throw new IllegalAlgorithmArgumentException(getClass(), "outputFiles", "must NOT be set");
//		if (null == this.getExecution().getInputFiles()
//				 || this.getExecution().getInputFiles().isEmpty())
		// throw new IllegalAlgorithmArgumentException(getClass(), "inputFiles",
		// "must be set and non-empty");
		if (null == this.getExecution().getSearchQuery())
			throw new IllegalAlgorithmArgumentException(getClass(), "searchQuery", "must be set and non-empty");
	}
}
