package io.github.infolis.model;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.algorithm.FederatedSearcher;
import io.github.infolis.algorithm.Learner;
import io.github.infolis.algorithm.SearchTermPosition;
import io.github.infolis.algorithm.TextExtractorAlgorithm;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
	 * {@link Algorithm to execute}
	 */
	private Class<? extends Algorithm> algorithm;

	/**
	 * {@link ExecutionStatus} of the execution.
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

	//
	//
	//
	// Parameters
	//
	//
	//

	/**
	 * Input files {@link Learner} ...
	 */
	private List<String> inputFiles = new ArrayList<>();

	/**
	 * Output files {@link SearchTermPosition}
	 */
	private List<String> outputFiles = new ArrayList<>();

	/**
	 * Whether to remove bibliographies from text/plain articles.
	 * {@link TextExtractor}
	 */
	private boolean removeBib = false;

	/**
	 * Output directory of Indexer and TextExtractor. {@link Indexer}
	 * {@link TextExtractorAlgorithm}
	 */
	private String outputDirectory = "";

	/**
	 * Input directory of SearchTermPosition = output directory of indexer. 
	 * {@link SearchTermPosition}
	 */
	private String indexDirectory = "";
	
	/**
	 * {@link SearchTermPosition}
	 */
	private int phraseSlop = 10;

	/**
	 * {@link SearchTermPosition}
	 */
	private boolean allowLeadingWildcards = true;

	/**
	 * {@link SearchTermPosition}
	 */
	private int maxClauseCount = Integer.MAX_VALUE;

	/**
	 * {@link SearchTermPosition}
	 */
	private String searchTerm;

	/**
	 * {@link SearchTermPosition} {@link FederatedSearcher}
	 */
	private String searchQuery;

	/**
	 * {@link Learner} {@link FederatedSearcher} {@link MetaDataResolver}
	 */
	private List<String> textualReferences = new ArrayList<>();

	/**
	 * {@link SearchTermPosition}
	 */
	private List<String> matchingFiles = new ArrayList<>();

	/**
	 * {@link PatternApplier}
	 */
	private List<String> patterns = new ArrayList<>();

	// TODO @bolandka not used now, is it worth the computation?
	// @kba it is used in PatternApplier <- used in both bootstrapping methods, is very important
	private boolean upperCaseConstraint = false;

	/**
	 * Seeds used for bootstrapping.
	 */
	private List<String> seeds = new ArrayList<>();

	/**
	 * Maximum number of iterations.
	 */
	private int maxIterations = 10;

	/**
	 * reliablityThreshold (perIteration)?
	 */
	private double reliabilityThreshold = 0.8;

	/**
	 * Strategy to use for bootstrapping.
	 */
	private BootstrapStrategy bootstrapStrategy = BootstrapStrategy.mergeAll;

	/**
	 * {@link MetaDataResolver}
	 */
	private MetaDataExtractingStrategy metaDataExtractingStrategy = MetaDataExtractingStrategy.title;
        
	/*
	 * {@link Resolver}
	 */
	private List<String> links;

	/**
	 * {@link FederatedSearcher}
	 */
	private List<String> queryServices;

	/**
	 * {@link FederatedSearcher}
	 */
	private List<String> searchResults;
        
	/**
	 * 
	 * {@link LocalResolver}
	 */
	private List<String> linkedEntities;
        
	/**
	 * 
	 * TODO
	 */
	private List<String> tags;

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

	public List<String> getTextualReferences() {
		return textualReferences;
	}

	public void setTextualReferences(List<String> textualReferences) {
		this.textualReferences = textualReferences;
	}

	public List<String> getMatchingFiles() {
		return matchingFiles;
	}

	public void setMatchingFiles(List<String> matchingFilenUris) {
		this.matchingFiles = matchingFilenUris;
	}

	public List<String> getPatterns() {
		return patterns;
	}

	public void setPatternUris(List<String> patternUris) {
		this.patterns = patternUris;
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

    public MetaDataExtractingStrategy getMetaDataExtractingStrategy() {
        return metaDataExtractingStrategy;
    }

    public void setMetaDataExtractingStrategy(MetaDataExtractingStrategy metaDataExtractingStrategy) {
        this.metaDataExtractingStrategy = metaDataExtractingStrategy;
    }

    public List<String> getLinkedEntities() {
        return linkedEntities;
    }

    public void setLinkedEntities(List<String> linkedEntities) {
        this.linkedEntities = linkedEntities;
    }
    
    public List<String> getTags() {
		return tags;
	}
    
    public void setTags(List<String> tags) {
		this.tags = tags;
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

}
