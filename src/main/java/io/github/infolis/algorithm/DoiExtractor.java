package io.github.infolis.algorithm;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.util.RegexUtils;

/**
 * 
 * @author kata
 *
 */
public class DoiExtractor extends BaseAlgorithm {

	public DoiExtractor(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
			FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private void extractDois(List<String> inputFileUris) {
		String doiRegex = RegexUtils.doiRegex;
		InfolisPattern doiInfolisPat = new InfolisPattern();
		doiInfolisPat.setPatternRegex(doiRegex);
		getInputDataStoreClient().post(InfolisPattern.class, doiInfolisPat);
		Execution exec = getExecution().createSubExecution(RegexSearcher.class);
		exec.setInputFiles(inputFileUris);
		exec.setPatterns(Arrays.asList(doiInfolisPat.getUri()));
		exec.setTags(getExecution().getTags());
		exec.instantiateAlgorithm(this).run();
		getExecution().setTextualReferences(exec.getTextualReferences());
	}

	@Override
	public void execute() throws IOException {
		Execution tagExec = getExecution().createSubExecution(TagSearcher.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.getInfolisPatternTags().addAll(getExecution().getInfolisPatternTags());
    	tagExec.instantiateAlgorithm(this).run();
    	
    	getExecution().getPatterns().addAll(tagExec.getPatterns());
    	getExecution().getInputFiles().addAll(tagExec.getInputFiles());
    	
        extractDois(getExecution().getInputFiles());
        getExecution().setStatus(ExecutionStatus.FINISHED);
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub
		
	}
	
}