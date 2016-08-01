package io.github.infolis.algorithm;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.BootstrapStrategy;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.util.SerializationUtils;


/**
 * 
 * @author kata
 *
 */
public class LearnPatternsAndCreateLinks extends BaseAlgorithm {
	
	public LearnPatternsAndCreateLinks(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
            		FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private static final Logger log = LoggerFactory.getLogger(LearnPatternsAndCreateLinks.class);
	
	@Override
	public void execute() {
		Execution tagExec = getExecution().createSubExecution(TagSearcher.class);
		tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
		tagExec.instantiateAlgorithm(this).run();
		getExecution().getInputFiles().addAll(tagExec.getInputFiles());
		try {
			debug(log, "Step1: Learning patterns and extracting textual references...");
			Execution learnExec = learn();
			updateProgress(1, 2);
			debug(log, "Step 2: Creating links...");
			Execution linkingExec = createLinks(learnExec);
			updateProgress(2, 2);
			getExecution().setTextualReferences(linkingExec.getTextualReferences());
			getExecution().setLinks(linkingExec.getLinks());
			debug(log, "Done. Returning {} textual references and {} entity links", 
					getExecution().getTextualReferences().size(),
					getExecution().getLinks().size());
			log.debug(SerializationUtils.toCsv(getExecution().getLinks(), getOutputDataStoreClient()));
			getExecution().setStatus(ExecutionStatus.FINISHED);        
		} catch (IllegalArgumentException | IllegalAlgorithmArgumentException | IOException e) {
			error(log, "Execution threw an Exception: {}", e);
			getExecution().setStatus(ExecutionStatus.FAILED);
		}
	}
	
	private Execution createLinks(Execution learnExec) 
			throws IllegalAlgorithmArgumentException, IOException {
		Execution linkExec = new Execution();
		linkExec.setSearchResultLinkerClass(getExecution().getSearchResultLinkerClass());
		linkExec.setAlgorithm(ReferenceLinker.class);
		linkExec.setInputFiles(getExecution().getInputFiles());
		linkExec.setTags(getExecution().getTags());
		
		linkExec.setTextualReferences(learnExec.getTextualReferences());
		if (null != getExecution().getQueryServiceClasses()) {
			linkExec.setQueryServiceClasses(getExecution().getQueryServiceClasses());
		}
		if (null != getExecution().getQueryServices()) {
			linkExec.setQueryServices(getExecution().getQueryServices());
		}
		linkExec.instantiateAlgorithm(this).run();
		return linkExec;
	}
	
	private Execution learn() throws IllegalArgumentException, IOException {
		Execution learnExec = new Execution();
		learnExec.setTags(getExecution().getTags());
		learnExec.setInputFiles(getExecution().getInputFiles());
		learnExec.setBootstrapStrategy(getExecution().getBootstrapStrategy());
		if (getExecution().getBootstrapStrategy().equals(BootstrapStrategy.reliability)){
			learnExec.setAlgorithm(ReliabilityBasedBootstrapping.class);
		}
		else learnExec.setAlgorithm(FrequencyBasedBootstrapping.class);
		learnExec.setStartPage(getExecution().getStartPage());
		learnExec.setRemoveBib(getExecution().isRemoveBib());
		learnExec.setTokenize(getExecution().isTokenize());
		learnExec.setTokenizeNLs(getExecution().getTokenizeNLs());
		learnExec.setPtb3Escaping(getExecution().getPtb3Escaping());
		learnExec.setPhraseSlop(getExecution().getPhraseSlop());
		learnExec.setSeeds(getExecution().getSeeds());
		learnExec.setUpperCaseConstraint(getExecution().isUpperCaseConstraint());
		learnExec.setReliabilityThreshold(getExecution().getReliabilityThreshold());
		learnExec.setMaxIterations(getExecution().getMaxIterations());
		
		learnExec.instantiateAlgorithm(this).run();
		return learnExec;
	}
	
	@Override
    public void validate() throws IllegalAlgorithmArgumentException {
        Execution exec = this.getExecution();
		if (null == exec.getSeeds() || exec.getSeeds().isEmpty()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "seeds", "Required parameter 'seeds' is missing!");
        }
        if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) &&
    		(null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())) {
            throw new IllegalAlgorithmArgumentException(getClass(), "inputFiles", "Required parameter 'inputFiles' is missing!");
        }
        if (null == exec.getBootstrapStrategy()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "bootstrapStrategy", "Required parameter 'bootstrapStrategy' is missing!");
        }
        if (null == exec.isTokenize()) {
        	log.warn("Warning: tokenize parameter not set. Defaulting to true.");
        	this.getExecution().setTokenize(true);
        }
        boolean queryServiceSet = false;
        if (null != exec.getQueryServiceClasses() && !exec.getQueryServiceClasses().isEmpty()) {
            queryServiceSet = true;
        }
		if (null != exec.getQueryServices() && !exec.getQueryServices().isEmpty()) {
            queryServiceSet = true;
		}
		if (!queryServiceSet) {
            throw new IllegalAlgorithmArgumentException(getClass(), "queryService", "Required parameter 'query services' is missing!");
        }
		if (null == exec.getSearchResultLinkerClass()) {
			throw new IllegalAlgorithmArgumentException(getClass(), "searchResultLinkerClass", "Required parameter 'SearchResultLinkerClass' is missing!");
		}
    }
}
