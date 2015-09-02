package io.github.infolis.model;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sun.research.ws.wadl.Link;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.SearchResult;

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

    /**
     * @return the textualReference
     */
    public String getTextualReference() {
        return textualReference;
    }

    /**
     * @param textualReference the textualReference to set
     */
    public void setTextualReference(String textualReference) {
        this.textualReference = textualReference;
    }

    /**
     * @return the queryForQueryService
     */
    public String getQueryForQueryService() {
        return queryForQueryService;
    }

    /**
     * @param queryForQueryService the queryForQueryService to set
     */
    public void setQueryForQueryService(String queryForQueryService) {
        this.queryForQueryService = queryForQueryService;
    }

    /**
     * @return the queriesForQueryService
     */
    public List<String> getQueriesForQueryService() {
        return queriesForQueryService;
    }

    /**
     * @param queriesForQueryService the queriesForQueryService to set
     */
    public void setQueriesForQueryService(List<String> queriesForQueryService) {
        this.queriesForQueryService = queriesForQueryService;
    }

    /**
     * @return the searchResults
     */
    public Set<String> getSearchResults() {
        return searchResults;
    }

    /**
     * @param searchResults the searchResults to set
     */
    public void setSearchResults(Set<String> searchResults) {
        this.searchResults = searchResults;
    }

    /**
     * @return the queryServices
     */
    public List<String> getQueryServices() {
        return queryServices;
    }

    /**
     * @param queryServices the queryServices to set
     */
    public void setQueryServices(List<String> queryServices) {
        this.queryServices = queryServices;
    }

	public enum Strategy {
		mergeCurrent, mergeNew, mergeAll, separate, reliability;
	}

	private Class<? extends Algorithm> algorithm;
	private ExecutionStatus status = ExecutionStatus.PENDING;
	private List<String> log = new ArrayList<>();
	private Date startTime;
	private Date endTime;

	//
	// Parameters
	//
	private List<String> inputFiles = new ArrayList<>();
	private List<String> outputFiles = new ArrayList<>();
	// TextExtractor
	private boolean removeBib = false;
	private String outputDirectory = "";

	// SearchTermPosition
	private int phraseSlop = 10;
	private boolean allowLeadingWildcards = true;
	private int maxClauseCount = Integer.MAX_VALUE;
	private String searchTerm;
	private String searchQuery;
	private List<String> studyContexts = new ArrayList<>();
	private List<String> matchingFilenames = new ArrayList<>();
	private List<String> studies = new ArrayList<>();
	private boolean overwrite = false;
	private List<String> pattern = new ArrayList<>();
	private boolean upperCaseConstraint = false;
	private boolean requiresContainedInNP = false;
	private List<String> terms = new ArrayList<>();
	private int maxIterations = 10;
	private double threshold = 0.8;
	// private Strategy bootstrapStrategy = Strategy.separate;
	private Strategy bootstrapStrategy = Strategy.mergeAll;
	private String inputMediaType = null;
	private String outputMediaType = null;

	// Resolver
	private Set<EntityLink> links;
        
        //MetaDataExtractor
        private String textualReference;
        private String queryForQueryService;
                
        //FederatedSearcher
        private List<String> queriesForQueryService;
        private List<String> queryServices;
        private Set<String> searchResults;
        
        
        
	//
	// CONSTRUCTORS
	// /

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
	// GETTERS / SETTERS
	//

	public ExecutionStatus getStatus() {
		return status;
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status;
	}

	/**
	 * @return a {@link List} of {@link String} log entries
	 */
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

	public List<String> getStudyContexts() {
		return studyContexts;
	}

	public void setStudyContexts(List<String> studyContexts) {
		this.studyContexts = studyContexts;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public List<String> getMatchingFilenames() {
		return matchingFilenames;
	}

	public void setMatchingFilenames(List<String> matchingFilenames) {
		this.matchingFilenames = matchingFilenames;
	}

	/**
	 * @return the pattern
	 */
	public List<String> getPattern() {
		return pattern;
	}

	/**
	 * @param pattern
	 *            the pattern to set
	 */
	public void setPattern(List<String> pattern) {
		this.pattern = pattern;
	}

	/**
	 * @return the upperCaseConstraint
	 */
	public boolean isUpperCaseConstraint() {
		return upperCaseConstraint;
	}

	/**
	 * @param upperCaseConstraint
	 *            the upperCaseConstraint to set
	 */
	public void setUpperCaseConstraint(boolean upperCaseConstraint) {
		this.upperCaseConstraint = upperCaseConstraint;
	}

	/**
	 * @return the requiresContainedInNP
	 */
	public boolean isRequiresContainedInNP() {
		return requiresContainedInNP;
	}

	/**
	 * @param requiresContainedInNP
	 *            the requiresContainedInNP to set
	 */
	public void setRequiresContainedInNP(boolean requiresContainedInNP) {
		this.requiresContainedInNP = requiresContainedInNP;
	}

	/**
	 * @return the studies
	 */
	public List<String> getStudies() {
		return studies;
	}

	/**
	 * @param studies
	 *            the studies to set
	 */
	public void setStudies(List<String> studies) {
		this.studies = studies;
	}

	/**
	 * @return the terms
	 */
	public List<String> getTerms() {
		return terms;
	}

	/**
	 * @param terms
	 *            the terms to set
	 */
	public void setTerms(List<String> terms) {
		this.terms = terms;
	}

	/**
	 * @return the maxIterations
	 */
	public int getMaxIterations() {
		return maxIterations;
	}

	/**
	 * @param maxIterations
	 *            the maxIterations to set
	 */
	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	/**
	 * @return the threshold
	 */
	public double getThreshold() {
		return threshold;
	}

	/**
	 * @param threshold
	 *            the threshold to set
	 */
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	/**
	 * @return the bootstrapStrategy
	 */
	public Strategy getBootstrapStrategy() {
		return bootstrapStrategy;
	}

	/**
	 * @param bootstrapStrategy
	 *            the bootstrapStrategy to set
	 */
	public void setBootstrapStrategy(Strategy bootstrapStrategy) {
		this.bootstrapStrategy = bootstrapStrategy;
	}

	public Set<EntityLink> getLinks() {
		return this.links;
	}

	public void setLinks(Set<EntityLink> links) {
		this.links = links;
	}
}
