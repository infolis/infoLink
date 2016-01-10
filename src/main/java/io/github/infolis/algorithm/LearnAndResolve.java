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
public class LearnAndResolve extends BaseAlgorithm {
	
	public LearnAndResolve(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
            		FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private static final Logger log = LoggerFactory.getLogger(LearnAndResolve.class);
	
	@Override
	public void execute() {
		try {
			debug(log, "Step1: Learning patterns...");
			Execution learnExec = learn();
			updateProgress(1, 2);
			//TODO step2: resolving references (using textual references generated while learning)
			debug(log, "Step 2: Applying patterns and resolving references...");
			Execution applyAndResolveExec = applyAndResolve(learnExec);
			updateProgress(2, 2);
			getExecution().setTextualReferences(applyAndResolveExec.getTextualReferences());
			getExecution().setLinks(applyAndResolveExec.getLinks());
			//getExecution().setLinkedEntities(applyAndResolveExec.getLinkedEntities());
			debug(log, "Done. Returning textual references and entity links");
			getExecution().setStatus(ExecutionStatus.FINISHED);
		}
		catch (Exception e) {
			error(log, "Execution threw an Exception: %s", e);
			getExecution().setStatus(ExecutionStatus.FAILED);
		}
	}
	
	private Execution applyAndResolve(Execution learnExec) {
		Execution applyAndResolveExec = new Execution();
		applyAndResolveExec.setAlgorithm(ApplyPatternAndResolve.class);
		applyAndResolveExec.setInputFiles(getExecution().getInputFiles());
		
		// TODO applyPatterns not needed, Textual refs are already found while learning. Resolve only
		applyAndResolveExec.setPatterns(learnExec.getPatterns());
		applyAndResolveExec.setQueryServiceClasses(getExecution().getQueryServiceClasses());
		applyAndResolveExec.instantiateAlgorithm(this).run();
		return applyAndResolveExec;
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