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
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for searching terms and complex phrase queries using a Lucene index.
 *
 * Difference between search term and search query: a search term represents the
 * entity to retrieve, e.g. the name of a dataset, while the complete query may
 * include additional information, e.g. allowed characters surrounding the term
 * to retrieve.
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

    /**
     * Searches for this query in this index using a ComplexPhraseQueryParser.
     * Stores matching
     *
     * @param outputFile	path of the output file
     * @param append	if set, contexts will be appended to file, else file will
     * be overwritten
     * @throws IOException
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
        QueryParser qp = new ComplexPhraseQueryParser(DEFAULT_FIELD_NAME, Indexer.createAnalyzer());
        // set phrase slop because dataset titles may consist of more than one word
        qp.setPhraseSlop(getExecution().getPhraseSlop()); // 0 requires exact match, 5 means that up to 5 edit operations may be carried out...
        qp.setAllowLeadingWildcard(getExecution().isAllowLeadingWildcards());
        BooleanQuery.setMaxClauseCount(getExecution().getMaxClauseCount());
        qp.setAutoGeneratePhraseQueries(false);
        // throws java.lang.IllegalArgumentException: Unknown query type "org.apache.lucene.search.WildcardQuery"
        // if quotes are present in absence of any whitespace inside of query
        // however, queries should be passed in correct form instead of being changed here
        Query q;
        try {
            q = qp.parse(getExecution().getSearchQuery().trim());
        } catch (ParseException e) {
            error(log, "Could not parse searchquery '%s'", getExecution().getSearchQuery());
            getExecution().setStatus(ExecutionStatus.FAILED);
            Indexer.createAnalyzer().close();
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

            if (this.getExecution().getSearchTerm() != null) {
                InputStream openInputStream = this.getInputFileResolver().openInputStream(file);
                String text = IOUtils.toString(openInputStream);
                openInputStream.close();
                for (TextualReference sC : getContexts(getInputDataStoreClient(), file.getUri(), getExecution().getSearchTerm(), text)) {
                    // note that the URI changes if inputDataStoreClient != outputDataStoreClient!
                    getOutputDataStoreClient().post(TextualReference.class, sC);
                    getExecution().getTextualReferences().add(sC.getUri());
                }
            }
            getExecution().getMatchingFiles().add(file.getUri());
            updateProgress(i, scoreDocs.length);

        }
        Indexer.createAnalyzer().close();
        indexReader.close();
        //TODO close searcher, indexReader, FSDirectory
        if (this.getExecution().getSearchTerm() != null) {
            log.debug("number of extracted contexts: " + getExecution().getTextualReferences().size());
        }
        log.debug("Finished LuceneSearcher#execute");
        getExecution().setStatus(ExecutionStatus.FINISHED);
    }

    public static List<TextualReference> getContexts(DataStoreClient outputDataStoreClient, String fileName, String term, String text) throws IOException, ArrayIndexOutOfBoundsException  {
        InfolisPattern infolisPat = null;
        // extract the sentence in which term occurs + adjacent sentences, if existing
        // note: with this regex, term may be a substring of a word
        //Pattern pat = Pattern.compile("(.*?" + System.getProperty("line.separator") + "+)?.*?" + Pattern.quote(term) + ".*(" + System.getProperty("line.separator") + "+.*)?");
        // note: with this regex, term may not be a substring of a word
        Pattern pat = Pattern.compile("(.*?" + System.getProperty("line.separator") + "+)?.*?[-—\\s/]" + Pattern.quote(term) + "[-—\\s/].*(" + System.getProperty("line.separator") + "+.*)?");
        //Pattern pat = Pattern.compile("(.*?" + System.getProperty("line.separator") + "+)?.*?\\s" + Pattern.quote(term) + "\\s.*(" + System.getProperty("line.separator") + "+.*)?");
        String threadName = String.format("For '%s' in '%s...'", pat, text.substring(0, Math.min(100, text.length())));
        LimitedTimeMatcher ltm = new LimitedTimeMatcher(pat, text, RegexUtils.maxTimeMillis, threadName);
        List<TextualReference> contextList = new ArrayList<>();
        ltm.run();

        // these patterns are used for extracting contexts of known study titles,
        // do not confuse with patterns to detect study references -> do not post
        if (!ltm.finished()) {
            throw new IOException("Matcher timed out!");
        }
        if (ltm.finished() && ltm.matched()) {
            infolisPat = new InfolisPattern(pat.toString());
        }
        while (ltm.matched()) {
        	log.debug("Found context: " + ltm.group());
            Entity p = new Entity();
            p.setFile(fileName);
            outputDataStoreClient.post(Entity.class, p);
            try {
            	TextualReference sC = new TextualReference(ltm.group().split(term)[0], term, ltm.group().split(term)[1], fileName, infolisPat.getUri(), p.getUri());
            	contextList.add(sC);
            } catch (ArrayIndexOutOfBoundsException e) {
            	log.warn("Error: failed to split reference: \"" + term + "\"");
            	//for (String word : ltm.group().split(term))	log.warn(word);
            	// TODO may happen when term is at the beginning or end of input
            	throw new ArrayIndexOutOfBoundsException();
            } 
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
        // throw new IllegalAlgorithmArgumentException(getClass(), "inputFiles",
        // "must be set and non-empty");
        if (null == this.getExecution().getSearchQuery()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "searchQuery", "must be set and non-empty");
        }
    }
}
