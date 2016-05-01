package io.github.infolis.model;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.algorithm.BaseAlgorithm;
import io.github.infolis.algorithm.FederatedSearcher;
import io.github.infolis.algorithm.SearchResultLinker;
import io.github.infolis.algorithm.LuceneSearcher;
import io.github.infolis.algorithm.TextExtractor;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.querying.QueryService;
import io.github.infolis.util.RegexUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.lang.reflect.Field;

/**
 *
 * @author domi
 * @author kba
 * @author kata
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class Execution extends BaseModel {

	private static final Logger logger = LoggerFactory.getLogger(Execution.class);

	//
	//
	//
	// CONSTRUCTORS AND METHODS
	//
	//
	//

	public Execution() {
	}

	public Execution(Class<? extends Algorithm> algo) {
		this.algorithm = algo;
	}

	public Algorithm instantiateAlgorithm(DataStoreClient dataStoreClient, FileResolver fileResolver) {
		return instantiateAlgorithm(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
	}

	public Algorithm instantiateAlgorithm(Algorithm copyFrom)
	{
		return instantiateAlgorithm(
				copyFrom.getInputDataStoreClient(),
				copyFrom.getOutputDataStoreClient(),
				copyFrom.getInputFileResolver(),
				copyFrom.getOutputFileResolver());
	}

	public Algorithm instantiateAlgorithm(
			DataStoreClient inputDataStoreClient,
			DataStoreClient outputDataStoreClient,
			FileResolver inputFileResolver,
			FileResolver outputFileResolver
			) {
		if (null == this.getAlgorithm()) {
			throw new IllegalArgumentException(
					"Must set 'algorithm' of execution before calling instantiateAlgorithm.");
		}
		Algorithm algo;
		try {
			Constructor<? extends Algorithm> constructor = this.algorithm.getDeclaredConstructor(DataStoreClient.class, DataStoreClient.class,
					FileResolver.class, FileResolver.class);
			algo = constructor.newInstance(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		algo.setExecution(this);
		logger.debug("Created instance for algorithm '{}'", this.getAlgorithm());
		return algo;
	}

	//
	//
	//
	// EXECUTION ATTRIBUTES
	//
	//
	//

	/**
         * The algorithm which is supposed to be executed within this
         * execution.
         * 
	 * {@link Algorithm}
	 */
	private Class<? extends Algorithm> algorithm;

	/**
	 * Status of the execution (PENDING, STARTED, FINISHED, FAILED).
         * Default (when starting): ExecutionStatus.PENDING
         * 
         * {@link ExecutionStatus} 
	 */
	private ExecutionStatus status = ExecutionStatus.PENDING;

	/**
	 * Log messages of this execution.
	 */
	private List<String> log = new ArrayList<>();

	/**
	 * Timestamp when execution started.
	 */
	private Date startTime;

	/**
	 * Timestamp when execution ended.
	 */
	private Date endTime;

	/**
	 * Progress of the execution in percent, a value between [0..100].
	 */
	private long progress =0;

	//
	//
	//
	// Parameters
	//
	//
	//

	/**
	 * Input files can either be pdfs or text files.
         * They are for example used to search patterns within the
         * Pattern Applier algorithm.
         * 
         * {@link TextExtractor} {@link Bootstrapping}
         * {@link InfolisPatternSearcher} {@link SearchPatternsAndCreateLinks} 
         * {@link Indexer} 
	 */ 
	private List<String> inputFiles = new ArrayList<>();

	/**
	 * Output files to save the output files (txt files) of algorithms. 
         * These files can serve as input for following algorimths.
         * For example, the TextExtraction algorithm extracts texts of pdfs
         * and stores these texts as output files.
         * 
         * {@link LuceneSearcher} {@link TextExtractor} 
	 */
	private List<String> outputFiles = new ArrayList<>();

	/**
	 * Whether to remove bibliographies from text/plain articles.   
         * Default: false
         * 
	 * {@link TextExtractor}
	 */
	private boolean removeBib = false;
	
	/**
	 * Whether to tokenize text input. Bootstrapping requires tokenized 
	 * input texts to perform well. It can either be called on tokenized 
	 * input texts or it can be called on untokenized text or pdf files and 
	 * perform tokenization itself. If unspecified, defaults to false for 
	 * TextExtractor. For Bootstrapping, this field has to be set explicitly 
	 * as this information is crucial for good performance.
         * Default: null
         * 
	 * {@link TextExtractor} {@link Bootstrapping}
	 */
	private Boolean tokenize = null;

	/**
	 * Output directory of Indexer and TextExtractor. 
         * 
	 * {@link TextExtractor} {@link Indexer} {@link Bootstrapping} 
	 */
	private String outputDirectory = "";

	/**
	 * Input directory of LuceneSearcher = output directory of indexer. 
         * 
	 * {@link Indexer} {@link LuceneSearcher}
	 */
	private String indexDirectory = "";
	
	/**
         * The slop for phrases used by the Lucene query parser. 
         * It determines how similar two phrases must be to be matched.
         * If zero, then only exact phrase matches, if 10 up to 10 edit
         * operations may be carried out.
         * Default: 10 
         * 
	 * {@link Bootstrapping} {@link LuceneSearcher}
	 */
	private int phraseSlop = 10;

	/**
         * Determines whether the Lucene query parser is allowed to
         * use leading wildcard characters.
         * Default: true       
         * 
	 * {@link Bootstrapping} {@link LuceneSearcher}
	 */
	private boolean allowLeadingWildcards = true;

	/**
         * The  maximum number of clauses permitted per BooleanQuery (Lucence search).
         * A boolean query represents a query that matches documents
         * matching boolean combinations of other queries.
         * Default: Integer max value
         * 
	 * {@link Bootstrapping} {@link LuceneSearcher}
	 */
	private int maxClauseCount = Integer.MAX_VALUE;

	/**
         * A search term that can be used in different algorithms 
         * whenever something a certain term needs to be searched in a text. 
         * For example, the bootstrapping algorithms need a seed in the 
         * beginning to start the whole process. The search term represents 
         * such a seed, e.g. the study name "ALLBUS".
         * 
	 * {@link LuceneSearcher}
	 */
	private String searchTerm;

	/**
         * Any kind of search query that can be used within the algorithms.
         * For example, it represtens the search query which is used
         * to perform a search in different repositories to find
         * fitting research data.
         * 
	 * {@link LuceneSearcher} {@link FederatedSearcher} {@link ApplyPatternAndResolve}
	 */
	private String searchQuery;
	
	/**
	 * Group numbers to use for RegexSearcher.
	 * 
	 * {@Link RegexSearcher}
	 */
	private int referenceGroup = RegexUtils.doiGroupNum;
	
	private int leftContextGroup = RegexUtils.doiLeftContextGroupNum;
	
	private int rightContextGroup = RegexUtils.doiRightContextGroupNum;

	/**
         * A textual reference represents any kind of reference that
         * can be find in a text, e.g. a term like a study name has been found in a publication.
         * Besides the text and the term that has been found in the text,
         * it also contains the context, i.e. where the term has been detected.
         * 
         * {@link FederatedSearcher} {@link MetaDataExtractor}
         * {@link Resolver} {@link LuceneSearcher} {@link SearchPatternsAndCreateLinks}
         * {@link PatternApplier} {@link Bootstrapping}
	 */
	private List<String> textualReferences = new ArrayList<>();

	/**
         * A list of patterns (internally expressed as regular expression) 
         * that can be applied on texts, e.g. to find links to research data. 
         * 
	 * {@link PatternApplier} {@link ApplyPatternAndResolve} {@link Bootstrapping}
	 */
	private List<String> patterns = new ArrayList<>();
 
        /**
         * Indicates whether we require a term to contain
         * at least one upper case character.
         * The idea behind is that especially a study name is supposed to be a 
         * named entity and thus should contain at least one upper-case character.
         * Default: false
         * 
         * {@link PatternApplier} {@link Bootstrapping}
         */
	private boolean upperCaseConstraint = false;

	/**
	 * Seeds used for bootstrapping, e.g. study names to start
         * with like "ALLBUS".
         * 
         * {@link Bootstrapping}
	 */
	private List<String> seeds = new ArrayList<>();

	/**
	 * Maximum number of iterations during the bootstrapping process.
         * A high number of iterations can lead to a increased run time.       
         * Default: 10
         * 
         * {@link Bootstrapping}
	 */
	private int maxIterations = 10;

        
        //TODO: also used for frequencyBasedBootstrapping, should we just name 
        //it bootstrapping threshold?
	/**
	 * Determines which patterns (and entities for reliability based bootstrapping)
         * are the relevant ones. For the frequency based bootstrapping
         * this means how often a pattern need to occur and for the
         * reliability based bootstrapping how reliable the pattern and the entities
         * used to generate this pattern are.         * 
         * Default: 0.8
         * 
         * {@link Bootstrapping}
	 */
	private double reliabilityThreshold = 0.8;

	/**
	 * Strategy to use for bootstrapping. Can either be: 
         * mergeCurrent, mergeNew, mergeAll, separate, reliability.
         * The first four strategies are different kinds of
         * strategies for the frequency based bootstrapping. They mainly differ 
         * in the way how to handle patterns that have been generated in previous
         * iterations. The strategy reliability referes to the reliability
         * based bootstrapping.       
         * Default: mergeAll
         * 
         * {@link BootstrapStrategy} {@link Bootstrapping} 
	 */
	private BootstrapStrategy bootstrapStrategy = BootstrapStrategy.mergeAll;

	/**
	 * The SearchResultLinkerClass determines the SearchResultLinker to 
	 * use. That class is responsible for deciding which SearchResults to 
	 * select for creating links.
	 */
	private Class<? extends SearchResultLinker> searchResultLinkerClass;
        
	/**
         * As a final step, links between the texts and the discovered
         * named entities (research data) are established and saved in this list.
         * 
	 * {@link Resolver} {@link ApplyPatternAndResolve}
	 */
	private List<String> links;

	/**
         * We can search different repositories for named entities.
         * One query service represents one specific type of search, e.g.
         * a SOLR-based search or a search within a portal returning HTML.
         * This list contains all query services that should be used.
         * 
	 * {@link FederatedSearcher} {@link ApplyPatternAndResolve}
	 */
	private List<String> queryServices;

        
        /**
         * We can search different repositories for named entities.
         * TODO
	 */
	private List<Class<? extends QueryService>> queryServiceClasses;
        
	/**
         * After a search in one or more repositories, a list 
         * of search results is returned. These results not only contain
         * the repository which was searched but also information like
         * the relevance score.
         * 
	 * {@link FederatedSearcher} {@link ApplyPatternAndResolve}
	 */
	private List<String> searchResults;
        
        //TODO: include local search
	/**
	 * Beside the search in external repositories, we can also
         * search in our own database. As use case, we get a URN for a publication
         * from a user and want to show all named entities that are linked to 
         * this publication. With an interal search using the generated links,
         * we can find this entities which are returned in this list.
         * 
         * 
	 * {@link LocalResolver}
	 */
	private List<String> linkedEntities;
        
	/**
	 * A tag indicates which corpus of documents and/or patterns should be used.
         * For example, a user likes to apply patterns learnt on
         * documents of a specific topic like social science.
         * This map contains names of classes extending the BaseModel as keys 
         * and a list of tags as values. This parameter is for using tags 
         * inside the application.
         * 
	 */

	private Set<String> infolisPatternTags = new HashSet<>();
	private Set<String> infolisFileTags = new HashSet<>();
	
	/**
	 * A tag indicates which corpus of documents and/or patterns should be used.
         * For example, a user likes to apply patterns learnt on
         * documents of a specific topic like social science.
         * These tags describe the execution to make it searchable via its tags.
         * 
	 */
	private Set<String> tags = new HashSet<>();
	
	/**
	 * Flag used by TextExtractor: if set to false, pdfs for which corresponding text 
	 * files already exist in the specified text directory will not be converted again, instead 
	 * the existing text files will be returned as InfolisFile instances. If set to true, all 
	 * pdfs will be converted regardless of any existing files in the text directory. 
	 * Default: true.
	 * {@link TextExtractor}
	 */
	private boolean overwriteTextfiles = true;
	
	/**
	 * Determines whether new line characters are to be tokenized.
	 * {@link Tokenizer}
	 */
	private boolean tokenizeNLs = true;
	
	/**
	 * Enable all traditional PTB3 token transforms (like parentheses becoming -LRB-, -RRB-).
	 * {@link Tokenizer}
	 */
	private boolean ptb3Escaping = true;
	
	/**
	 * Index (starting at 1 rather than 0) of the first page to extract. 
	 * Useful to ignore title pages if present.
	 * {@link TextExtractor}
	 */
	private int startPage = 1;

	//
	//
	//
	// GETTERS / SETTERS
	//
	//
	//

	public ExecutionStatus getStatus() {
		return status;
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status;
	}

	public List<String> getLog() {
		return log;
	}

	public void setLog(List<String> log) {
		this.log = log;
	}

	public List<String> getInputFiles() {
		return inputFiles;
	}

	public void setInputFiles(List<String> paramPdfInput) {
		this.inputFiles = paramPdfInput;
	}

	@JsonIgnore
	public String getFirstInputFile() {
		return inputFiles.get(0);
	}

	@JsonIgnore
	public void setFirstInputFile(String fileName) {
		if (null == inputFiles) {
			inputFiles = new ArrayList<>();
		}
		if (inputFiles.size() > 0) {
			inputFiles.set(0, fileName);
		} else {
			inputFiles.add(fileName);
		}
	}

	public List<String> getOutputFiles() {
		return outputFiles;
	}

	public void setOutputFiles(List<String> paramPdfOutput) {
		this.outputFiles = paramPdfOutput;
	}

	@JsonIgnore
	public String getFirstOutputFile() {
		return outputFiles.get(0);
	}

	@JsonIgnore
	public void setFirstOutputFile(String fileName) {
		if (null == outputFiles) {
			outputFiles = new ArrayList<>();
		}
		if (outputFiles.size() > 0) {
			outputFiles.set(0, fileName);
		} else {
			outputFiles.add(fileName);
		}
	}

	public boolean isRemoveBib() {
		return removeBib;
	}

	public void setRemoveBib(boolean removeBib) {
		this.removeBib = removeBib;
	}
	
	public Boolean isTokenize() {
		return this.tokenize;
	}
	
	public void setTokenize(boolean tokenize) {
		this.tokenize = tokenize;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}
	
	public String getIndexDirectory() {
		return indexDirectory;
	}

	public void setIndexDirectory(String indexDirectory) {
		this.indexDirectory = indexDirectory;
	}

	public Class<? extends Algorithm> getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(Class<? extends Algorithm> algorithm) {
		this.algorithm = algorithm;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public int getPhraseSlop() {
		return phraseSlop;
	}

	public void setPhraseSlop(int phraseSlop) {
		this.phraseSlop = phraseSlop;
	}

	public boolean isAllowLeadingWildcards() {
		return allowLeadingWildcards;
	}

	public void setAllowLeadingWildcards(boolean allowLeadingWildcards) {
		this.allowLeadingWildcards = allowLeadingWildcards;
	}

	public int getMaxClauseCount() {
		return maxClauseCount;
	}

	public void setMaxClauseCount(int maxClauseCount) {
		this.maxClauseCount = maxClauseCount;
	}

	public String getSearchTerm() {
		return searchTerm;
	}

	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
	}

	public String getSearchQuery() {
		return searchQuery;
	}

	public void setSearchQuery(String searchQuery) {
		this.searchQuery = searchQuery;
	}
	
	public void setLeftContextGroup(int groupNum) {
		this.leftContextGroup = groupNum;
	}
	
	public void setRightContextGroup(int groupNum) {
		this.rightContextGroup = groupNum;
	}
	
	public void setReferenceGroup(int groupNum) {
		this.referenceGroup = groupNum;
	}
	
	public int getLeftContextGroup() {
		return this.leftContextGroup;
	}
	
	public int getRightContextGroup() {
		return this.rightContextGroup;
	}
	
	public int getReferenceGroup() {
		return this.referenceGroup;
	}

	public List<String> getTextualReferences() {
		return textualReferences;
	}

	public void setTextualReferences(List<String> textualReferences) {
		this.textualReferences = textualReferences;
	}

	public List<String> getPatterns() {
		return patterns;
	}

	public void setPatterns(List<String> patterns) {
		this.patterns = patterns;
	}

	public boolean isUpperCaseConstraint() {
		return upperCaseConstraint;
	}

	public void setUpperCaseConstraint(boolean upperCaseConstraint) {
		this.upperCaseConstraint = upperCaseConstraint;
	}

	public List<String> getLinks() {
		return links;
	}

	public List<String> getSeeds() {
		return seeds;
	}

	public void setSeeds(List<String> terms) {
		this.seeds = terms;
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public double getReliabilityThreshold() {
		return reliabilityThreshold;
	}

	public void setReliabilityThreshold(double threshold) {
		this.reliabilityThreshold = threshold;
	}

	public BootstrapStrategy getBootstrapStrategy() {
		return bootstrapStrategy;
	}

	public void setBootstrapStrategy(BootstrapStrategy bootstrapStrategy) {
		this.bootstrapStrategy = bootstrapStrategy;
	}

	public List<String> getQueryServices() {
		return queryServices;
	}

	public void setQueryServices(List<String> queryServices) {
		this.queryServices = queryServices;
	}

	public List<String> getSearchResults() {
		return searchResults;
	}

	public void setSearchResults(List<String> searchResults) {
		this.searchResults = searchResults;
	}

	public void setLinks(List<String> links) {
		this.links = links;
	}
	
	public Class<? extends SearchResultLinker> getSearchResultLinkerClass() {
		return this.searchResultLinkerClass;
	}
	
	public void setSearchResultLinkerClass (Class<? extends SearchResultLinker> searchResultLinkerClass) {
		this.searchResultLinkerClass = searchResultLinkerClass;
	}

    public List<String> getLinkedEntities() {
        return linkedEntities;
    }

    public void setLinkedEntities(List<String> linkedEntities) {
        this.linkedEntities = linkedEntities;
    }
    
    public Set<String> getTags() {
		return this.tags;
	}
    
    public void setTags(Set<String> tags) {
		this.tags = tags;
	}
    
    public long getProgress() {
        return progress;
    }

	public Set<String> getInfolisPatternTags()
	{
		return infolisPatternTags;
	}

	public void setInfolisPatternTags(Set<String> infolisPatternTags)
	{
		this.infolisPatternTags = infolisPatternTags;
	}

	public Set<String> getInfolisFileTags()
	{
		return infolisFileTags;
	}

	public void setInfolisFileTags(Set<String> infolisFileTags)
	{
		this.infolisFileTags = infolisFileTags;
	}
    
    public void setProgress(long progress) {
        this.progress = progress;
    }
    
    public void setOverwriteTextfiles(boolean overwriteTextfiles) {
    	this.overwriteTextfiles = overwriteTextfiles;
    }
    
    public boolean getOverwriteTextfiles() {
    	return this.overwriteTextfiles;
    }
    
    public void setTokenizeNLs(boolean tokenizeNLs) {
    	this.tokenizeNLs = tokenizeNLs;
    }
    
    public boolean getTokenizeNLs() {
    	return this.tokenizeNLs;
    }
    
    public void setPtb3Escaping(boolean ptb3Escaping) {
    	this.ptb3Escaping = ptb3Escaping;
    }
	
	public boolean getPtb3Escaping() {
		return this.ptb3Escaping;
	}
	
	public void setStartPage(int startPage) {
		this.startPage = startPage;
	}

	public int getStartPage() {
		return this.startPage;
	}
    
    public List<Class<? extends QueryService>> getQueryServiceClasses() {
        return queryServiceClasses;
    }
    
    public void setQueryServiceClasses(List<Class<? extends QueryService>> queryServiceClasses) {
        for(Class<? extends QueryService> qs : queryServiceClasses) {
            instantiateQueryService(qs);
            if(this.queryServiceClasses==null) {
                this.queryServiceClasses = new ArrayList<>();
            }
            this.queryServiceClasses.add(qs);
        }        
    }
    
    public void addQueryServiceClasses(Class<? extends QueryService> queryServiceClasses) {
        instantiateQueryService(queryServiceClasses);
        if(this.queryServiceClasses==null) {
            this.queryServiceClasses = new ArrayList<>();
            }
        this.queryServiceClasses.add(queryServiceClasses);
    }
    
    private QueryService instantiateQueryService(Class<? extends QueryService> qs) {
		if (null == qs) {
			throw new IllegalArgumentException(
					"Must set 'queryServiceClass' of execution before calling.");
		}
		QueryService queryService;
		try {
			Constructor<? extends QueryService> constructor = qs.getDeclaredConstructor();
			queryService = constructor.newInstance();
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		logger.debug("Created instance for queryService '{}'", qs);
		return queryService;
	}
    
    
    public void setProperty(String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = this.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);        
        if (field.getType() == Character.TYPE) {field.set(this, value.toString().charAt(0)); return;}
        if (field.getType() == Short.TYPE) {field.set(this, Short.parseShort(value.toString())); return;}
        if (field.getType() == Integer.TYPE) {field.set(this, Integer.parseInt(value.toString())); return;}
        if (field.getType() == Long.TYPE) {field.set(this, Long.parseLong(value.toString())); return;}
        if (field.getType() == Float.TYPE) {field.set(this, Float.parseFloat(value.toString())); return;}
        if (field.getType() == Double.TYPE) {field.set(this, Double.parseDouble(value.toString())); return;}
        if (field.getType() == Byte.TYPE) {field.set(this, Byte.parseByte(value.toString())); return;}
        if (field.getType() == Boolean.TYPE) {field.set(this, Boolean.parseBoolean(value.toString())); return;}
       // if (field.getGenericType() == );
        field.set(this, value);
    }
    
    public Execution createSubExecution(Class<? extends BaseAlgorithm> algo)
    {
    	Execution subExec = new Execution(algo);
    	//subExec.setLog(getLog());
    	return subExec;
    }

}
