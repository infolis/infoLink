package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.lucene.ContextHighlighter;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.store.FSDirectory;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for searching terms and complex phrase queries using a Lucene index.
 *
 * Difference between search term and search query: a search term represents the
 * entity to retrieve, e.g. the name of a dataset, while the complete query may
 * include additional information, e.g. required words surrounding the term
 * to retrieve. Search term must be included in search query. 
 *
 * Example: when searching for datasets of the study "Eurobarometer", entities
 * like "Eurobarometer-Datensatz" shall be found as well but only
 * "Eurobarometer" shall be listed as search term in the output file.
 *
 * Parameters: null {@link Execution#getMaxClauseCount()}
 * {@link Execution#getPhraseSlop()}
 * {@link Execution#getSearchQuery()}
 * {@link Execution#getSearchTerm())}
 *
 * @author kata
 * @author kba
 *
 */
public class LuceneSearcher extends BaseAlgorithm {

    public LuceneSearcher(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(LuceneSearcher.class);
    private static final String DEFAULT_FIELD_NAME = "contents";

    Execution createIndex() throws IOException {
		Execution execution = getExecution().createSubExecution(Indexer.class);
		execution.setInputFiles(getExecution().getInputFiles());
        getOutputDataStoreClient().post(Execution.class, execution);
        execution.instantiateAlgorithm(this).run();
		return execution;
	}
    
    public static TextualReference getContext(String term, String text, String fileUri, String patternUri, String entityUri) throws StringIndexOutOfBoundsException {
    	// do not treat term as regex when splitting
       	String[] contexts = text.split(Pattern.quote(term));
       	// if highlighter is configured to return the whole text 
       	// when it isn't able to find any passages, the context 
       	// size should be restricted here
       	String leftContext = contexts[0];
       	String rightContext = contexts[1];
       	if (leftContext.isEmpty()) leftContext = " ";
       	if (rightContext.isEmpty()) rightContext = " ";
       	TextualReference textRef = new TextualReference(leftContext, term, 
	       			rightContext, fileUri, patternUri, entityUri);
	       	return textRef;
    }
    	
    /**
     * Searches for this query in this index using a ComplexPhraseQueryParser.
     * Stores matching
     *
     * @param outputFile	path of the output file
     * @param append	if set, contexts will be appended to file, else file will
     * be overwritten
     * @throws IOException
     * @throws InvalidTokenOffsetsException 
     */
    @Override
    public void execute() throws IOException {
    	Execution tagExec = new Execution();
    	tagExec.setAlgorithm(TagSearcher.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.instantiateAlgorithm(this).run();
    	getExecution().getInputFiles().addAll(tagExec.getInputFiles());

    	if (null == getExecution().getIndexDirectory() || getExecution().getIndexDirectory().isEmpty()) {
    		debug(log, "No index directory specified, indexing on demand");
    		Execution indexerExecution = createIndex();
    		getExecution().setIndexDirectory(indexerExecution.getOutputDirectory());
    	}
        
    	IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(getExecution().getIndexDirectory())));
    	log.debug("Reading index at " + getExecution().getIndexDirectory() );
        IndexSearcher searcher = new IndexSearcher(indexReader);
        Analyzer analyzer = Indexer.createAnalyzer();
        
        ComplexPhraseQueryParser qp = new ComplexPhraseQueryParser(DEFAULT_FIELD_NAME, analyzer);
        // set phrase slop because dataset titles may consist of more than one word
        qp.setPhraseSlop(getExecution().getPhraseSlop()); // 0 requires exact match, 5 means that up to 5 edit operations may be carried out...
        qp.setAllowLeadingWildcard(getExecution().isAllowLeadingWildcards());
        BooleanQuery.setMaxClauseCount(getExecution().getMaxClauseCount());
        qp.setAutoGeneratePhraseQueries(false);
        qp.setLowercaseExpandedTerms(false);
        
        Query q;

        for (InfolisPattern pattern : getOutputDataStoreClient().get(
        		InfolisPattern.class, getExecution().getPatterns())) {
        	String query = pattern.getLuceneQuery();
        	// throws java.lang.IllegalArgumentException: Unknown query type 
            // "org.apache.lucene.search.WildcardQuery"
            // if quotes are present in absence of any whitespace inside of query
            // however, queries should be passed in correct form instead of being changed here
	        try {
	            q = qp.parse(query.trim());
	        } catch (ParseException e) {
	            error(log, "Could not parse searchquery '{}'", query);
	            getExecution().setStatus(ExecutionStatus.FAILED);
	            analyzer.close();
	            indexReader.close();
	            throw new RuntimeException();
	        }
	
	        debug(log, "Query: " + q.toString());
	        TopDocs td = searcher.search(q, 10000);
	        debug(log, "Number of hits (documents): " + td.totalHits);
	        ScoreDoc[] scoreDocs = td.scoreDocs;
	        TopDocs[] shardHits = { td };	
	        for (int i = 0; i < scoreDocs.length; i++) {
	            Document doc = searcher.doc(scoreDocs[i].doc);
	            InfolisFile file;
	            try {
	                file = getInputDataStoreClient().get(InfolisFile.class, doc.get("path"));
	            } catch (Exception e) {
	                error(log, "Could not retrieve file " + doc.get("path") + ": " + e.getMessage());
	                Indexer.createAnalyzer().close();
	                indexReader.close();
	                getExecution().setStatus(ExecutionStatus.FAILED);
	                return;
	            }
	        
	            String term = getExecution().getSearchTerm();
	            
	            // extract contexts
	            ContextHighlighter highlighter = new ContextHighlighter(Integer.MAX_VALUE - 1);
	            //PostingsHighlighter highlighter = new PostingsHighlighter(Integer.MAX_VALUE - 1);
	            
	            TopDocs currentDoc = TopDocs.merge(i, 1, shardHits);
	            // TODO use not only the one sentence containing the term, use also the preceding and following sentences?
	            // e.g. modify BreakIterator or use WholeBreakIterator and split text to sentences
	            //String highlights[] = highlighter.highlight(DEFAULT_FIELD_NAME, q, searcher, currentDoc, 100000);
	            String highlights[] = highlighter.highlight(DEFAULT_FIELD_NAME, q, searcher, currentDoc);
	            
	            for (String fragment: highlights) {
			           	log.trace("Fragment: " + fragment);
			            // remove tags inserted by the highlighter
			            fragment = fragment.replaceAll("</?b>", "").trim();
			            if (term != null) {
			            	try {
			            		TextualReference textRef = getContext(term, fragment, file.getUri(), 
			            				pattern.getUri(), file.getManifestsEntity());
			                   	// those textual references should be temporary - call with temp client
				               	getOutputDataStoreClient().post(TextualReference.class, textRef);
				                getExecution().getTextualReferences().add(textRef.getUri());
				            } catch (ArrayIndexOutOfBoundsException aioobe) {
				               	log.warn("Error: failed to split reference \"" + term + "\" in \"" + fragment + "\"");
				            }
			            }
			            else {
			            	TextualReference textRef = new TextualReference();
			            	textRef.setLeftText(fragment);
			            	textRef.setTextFile(file.getUri());
			            	textRef.setMentionsReference(file.getManifestsEntity());
			            	textRef.setPattern(pattern.getUri());
			            	// those textual references should be temporary if validation using regex is to be performed
			               	getOutputDataStoreClient().post(TextualReference.class, textRef);
			                getExecution().getTextualReferences().add(textRef.getUri());
			            }
		           	}
	            getExecution().getOutputFiles().add(file.getUri());
	            updateProgress(i, scoreDocs.length);
	        }
        }
        analyzer.close();
        indexReader.close();
        
        if (this.getExecution().getSearchTerm() != null) {
            log.debug("number of extracted contexts: " + getExecution().getTextualReferences().size());
        }
        log.debug("Finished LuceneSearcher#execute");
        getExecution().setStatus(ExecutionStatus.FINISHED);
    }



    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
//		if (null == this.getExecution().getInputFiles()
//				 || this.getExecution().getInputFiles().isEmpty())
        // throw new IllegalAlgorithmArgumentException(getClass(), "inputFiles",
        // "must be set and non-empty");
    }
}
