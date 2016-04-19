package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.util.LimitedTimeMatcher;
import io.github.infolis.util.RegexUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
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
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.QueryTermScorer;
import org.apache.lucene.search.highlight.Scorer;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.search.postingshighlight.PostingsHighlighter;
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
    
    public static TextualReference getContext(String term, String text, String fileUri, String patternUri, String entityUri) {
    	// do not treat term as regex when splitting
       	String[] contexts = text.split(Pattern.quote(term));
       	TextualReference textRef = new TextualReference(contexts[0], term, 
       			contexts[1], fileUri, patternUri, entityUri);
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
        // throws java.lang.IllegalArgumentException: Unknown query type 
        // "org.apache.lucene.search.WildcardQuery"
        // if quotes are present in absence of any whitespace inside of query
        // however, queries should be passed in correct form instead of being changed here
        Query q;
        try {
            q = qp.parse(getExecution().getSearchQuery().trim());
        } catch (ParseException e) {
            error(log, "Could not parse searchquery '%s'", getExecution().getSearchQuery());
            getExecution().setStatus(ExecutionStatus.FAILED);
            analyzer.close();
            indexReader.close();
            throw new RuntimeException();
        }

        debug(log, "Query: " + q.toString());
        TopDocs td = searcher.search(q, 10000);
        debug(log, "Number of hits (documents): " + td.totalHits);
        ScoreDoc[] scoreDocs = td.scoreDocs;

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
                persistExecution();
                return;
            }

            String term = getExecution().getSearchTerm();
            
            // extract contexts
            QueryScorer scorer = new QueryScorer(q, DEFAULT_FIELD_NAME);
            //PostingsHighlighter highlighter = new PostingsHighlighter();
            Highlighter highlighter = new Highlighter(scorer);
            TokenStream stream = TokenSources.getAnyTokenStream(searcher
            	          .getIndexReader(), td.scoreDocs[i].doc, DEFAULT_FIELD_NAME, 
            	          doc, analyzer);
            // TODO make sure fragments contain whole words only
            Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 1000);
            highlighter.setTextFragmenter(fragmenter);
            highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
            try {
            	String[] fragments = highlighter.getBestFragments(stream, doc.get(DEFAULT_FIELD_NAME), 
            			1000, "--@@--").split("--@@--");
	           	for (String fragment: fragments) {
		           	log.trace("Fragment: " + fragment);
		            	
		            Entity e = new Entity();
		            e.setFile(file.getUri());
		            getOutputDataStoreClient().post(Entity.class, e);
		            // remove tags inserted by the highlighter
		            fragment = fragment.replaceAll("</?B>", "").trim();
		            if (term != null) {
		            	try {
		            		// term search, thus no pattern URI in textRef
		            		TextualReference textRef = getContext(term, fragment, file.getUri(), 
		            				"", e.getUri());
		            		// note that the URI changes if inputDataStoreClient != outputDataStoreClient!
		                   	// TODO those textual references should be temporary - check
			               	getOutputDataStoreClient().post(TextualReference.class, textRef);
			                getExecution().getTextualReferences().add(textRef.getUri());
			            } catch (ArrayIndexOutOfBoundsException aioobe) {
			               	log.warn("Error: failed to split reference: \"" + term + "\"");
			               	// TODO may happen when term is at the beginning or end of input
			               	throw new ArrayIndexOutOfBoundsException();
			            } 
		            }
		            else {
		            	TextualReference textRef = new TextualReference();
		            	textRef.setLeftText(fragment);
		            	textRef.setFile(file.getUri());
		            	textRef.setMentionsReference(e.getUri());
		            	// TODO those textual references should be temporary if validation 
		            	// by regex is to be performed - check
		               	getOutputDataStoreClient().post(TextualReference.class, textRef);
		                getExecution().getTextualReferences().add(textRef.getUri());
		            }
	           	}
            } catch (InvalidTokenOffsetsException e) {
            	log.warn(e.getMessage());
            	analyzer.close();
                indexReader.close();
            	throw new IOException();
            } finally {
                stream.close();
            }
            // TODO matchingFiles are not needed anymore
            // TODO moreover, outputFiles could be used instead
            getExecution().getMatchingFiles().add(file.getUri());
            updateProgress(i, scoreDocs.length);
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
//		if (null != this.getExecution().getOutputFiles()
//				 && !this.getExecution().getOutputFiles().isEmpty())
//			throw new IllegalAlgorithmArgumentException(getClass(), "outputFiles", "must NOT be set");
//		if (null == this.getExecution().getInputFiles()
//				 || this.getExecution().getInputFiles().isEmpty())
        // throw new IllegalAlgorithmArgumentException(getClass(), "inputFiles",
        // "must be set and non-empty");
        if (null == this.getExecution().getSearchQuery()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "searchQuery", "must be set and non-empty");
        }
    }
}
