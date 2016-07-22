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
 * This algorithm searches for a set of given patterns, then executes the 
 * ReferenceLinker algorithm to create EntityLinks from the resulting textualReferences.
 *
 * Used algorithms: InfolisPatternSearcher - ReferenceLinker
 *
 * @author domi
 * @author kata
 *
 */
public class SearchPatternsAndCreateLinks extends BaseAlgorithm {

    public SearchPatternsAndCreateLinks(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(SearchPatternsAndCreateLinks.class);

    @Override
    public void execute() throws IOException {

    	Execution tagExec = getExecution().createSubExecution(TagSearcher.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.getInfolisPatternTags().addAll(getExecution().getInfolisPatternTags());
    	tagExec.instantiateAlgorithm(this).run();
    	getExecution().getPatterns().addAll(tagExec.getPatterns());
    	getExecution().getInputFiles().addAll(tagExec.getInputFiles());
        List<String> textualRefs = searchPatterns(getExecution().getPatterns(), getExecution().getInputFiles());

        List<String> createdLinks = createLinks(textualRefs);
        
        debug(log, "Created links: " + createdLinks);
        getExecution().setLinks(createdLinks);
        getExecution().setStatus(ExecutionStatus.FINISHED);
    }
    
    private List<String> searchPatterns(List<String> patterns, List<String> input) {
    	debug(log, "Executing InfolisPatternSearcher with patterns " + patterns);
    	Execution search = getExecution().createSubExecution(InfolisPatternSearcher.class);
        search.setPatterns(patterns);
        search.setInputFiles(input);
        search.setPhraseSlop(getExecution().getPhraseSlop());
        search.setUpperCaseConstraint(getExecution().isUpperCaseConstraint());
        search.setIndexDirectory(getExecution().getIndexDirectory());
        getOutputDataStoreClient().post(Execution.class, search);
        search.instantiateAlgorithm(this).run();
        updateProgress(1, 2);
    	debug(log, "Done executing InfolisPatternSearcher, found textualReferences: " + search.getTextualReferences());
        return search.getTextualReferences();
    }
    
    protected List<String> createLinks(List<String> textualRefs) {
    	Execution exec = new Execution();
    	if (null != getExecution().getQueryServices()) {
    		exec.setQueryServices(getExecution().getQueryServices());
    	}
    	if (null != getExecution().getQueryServiceClasses()) {
    		exec.setQueryServiceClasses(getExecution().getQueryServiceClasses());
    	}
    	exec.setTextualReferences(textualRefs);
    	exec.setSearchResultLinkerClass(getExecution().getSearchResultLinkerClass());
    	exec.setAlgorithm(ReferenceLinker.class);
    	exec.instantiateAlgorithm(this).run();
    	updateProgress(2, 2);
    	debug(log, "Done executing ReferenceLinker, created entityLinks: " + exec.getLinks());
    	return exec.getLinks();
    }
    
    
    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        Execution exec = this.getExecution();
		if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) &&
        		(null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())){
            throw new IllegalArgumentException("Must set at least one inputFile!");
        }
        if ((null == exec.getPatterns() || exec.getPatterns().isEmpty()) &&
        		(null == exec.getInfolisPatternTags() || exec.getInfolisPatternTags().isEmpty()))
        {
            throw new IllegalArgumentException("No patterns given.");
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
