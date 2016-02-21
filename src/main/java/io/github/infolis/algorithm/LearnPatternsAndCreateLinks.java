package io.github.infolis.algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.BootstrapStrategy;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;

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
		try {
			debug(log, "Step1: Learning patterns and extracting textual references...");
			Execution learnExec = learn();
			updateProgress(1, 2);
			debug(log, "Step 2: Creating links...");
			Execution linkingExec = createLinks(learnExec);
			updateProgress(2, 2);
			getExecution().setTextualReferences(linkingExec.getTextualReferences());
			getExecution().setLinks(linkingExec.getLinks());
			//TODO: set all created entities
			debug(log, "Done. Returning textual references and entity links");
			getExecution().setStatus(ExecutionStatus.FINISHED);
		}
		catch (Exception e) {
			error(log, "Execution threw an Exception: %s", e);
			getExecution().setStatus(ExecutionStatus.FAILED);
		}
	}
	
	private Execution createLinks(Execution learnExec) {
		Execution linkExec = new Execution();
		linkExec.setSearchResultLinkerClass(getExecution().getSearchResultLinkerClass());
		linkExec.setAlgorithm(ReferenceLinker.class);
		linkExec.setInputFiles(getExecution().getInputFiles());
		
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
	
	private Execution learn() {
		Execution learnExec = new Execution();
		//TODO set all optional parameters
		learnExec.setInputFiles(getExecution().getInputFiles());
		learnExec.setBootstrapStrategy(getExecution().getBootstrapStrategy());
		if (getExecution().getBootstrapStrategy().equals(BootstrapStrategy.reliability)){
			learnExec.setAlgorithm(ReliabilityBasedBootstrapping.class);
		}
		else learnExec.setAlgorithm(FrequencyBasedBootstrapping.class);
		learnExec.setSeeds(getExecution().getSeeds());
		learnExec.setReliabilityThreshold(getExecution().getReliabilityThreshold());
		learnExec.instantiateAlgorithm(this).run();
		return learnExec;
	}
	
	@Override
	public void validate() {
		// TODO: Validator with validations for each parameter to choose from
		// all non-optional fields must be given...
	}
}