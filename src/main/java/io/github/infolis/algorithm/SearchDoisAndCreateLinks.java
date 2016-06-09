package io.github.infolis.algorithm;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;

/**
 *
 * This algorithm searches for DOIs and then executes the 
 * ReferenceLinker algorithm to create EntityLinks from the 
 * resulting textualReferences.
 *
 * Used algorithms: DoiExtractor - ReferenceLinker
 *
 * @author kata
 *
 */

public class SearchDoisAndCreateLinks extends SearchPatternsAndCreateLinks {
	
	private static final Logger log = LoggerFactory.getLogger(SearchDoisAndCreateLinks.class);
	 
	public SearchDoisAndCreateLinks(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }
	
	@Override
    public void execute() throws IOException {
    	Execution tagExec = getExecution().createSubExecution(TagSearcher.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.instantiateAlgorithm(this).run();
    	getExecution().getInputFiles().addAll(tagExec.getInputFiles());
    	
    	getExecution().setSearchResultLinkerClass(DoiLinker.class);
        List<String> textualRefs = extractDois(getExecution().getInputFiles());
        List<String> createdLinks = createLinks(textualRefs);
        
        debug(log, "Created links: " + createdLinks);
        getExecution().setTextualReferences(textualRefs);
        getExecution().setLinks(createdLinks);
        getExecution().setStatus(ExecutionStatus.FINISHED);
    }

	private List<String> extractDois(List<String> input) {
    	debug(log, "Extracting Dois");
    	Execution search = getExecution().createSubExecution(DoiExtractor.class);
        search.setInputFiles(input);
        getOutputDataStoreClient().post(Execution.class, search);
        search.instantiateAlgorithm(this).run();
        updateProgress(1, 2);
    	debug(log, "Done executing DoiExtractor, found textualReferences: " 
    			+ search.getTextualReferences());
        return search.getTextualReferences();
    }
	
	@Override
    public void validate() throws IllegalAlgorithmArgumentException {
        Execution exec = this.getExecution();
		if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) &&
        		(null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())){
            throw new IllegalArgumentException("Must set at least one inputFile!");
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
    }

}