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
 * This algorithm searches for a set of given names, then executes the 
 * ReferenceLinker algorithm to create EntityLinks from the resulting textualReferences.
 *
 * Used algorithms: NameExtractor - ReferenceLinker
 *
 * @author kata
 *
 */
public class SearchNamesAndCreateLinks extends SearchPatternsAndCreateLinks {

    public SearchNamesAndCreateLinks(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(SearchNamesAndCreateLinks.class);

    @Override
    public void execute() throws IOException {
	
    	preprocessInputFiles();
    	
	List<String> textualRefs = searchNames();

        List<String> createdLinks = createLinks(textualRefs);
        
        debug(log, "Created links: " + createdLinks);
        getExecution().setLinks(createdLinks);
        getExecution().setStatus(ExecutionStatus.FINISHED);
	getOutputDataStoreClient().put(Execution.class, getExecution(), getExecution().getUri());
    }
    
    private List<String> searchNames() {
    	debug(log, "Executing NameExtractor");
    	Execution search = getExecution().createSubExecution(NameExtractor.class);
        search.setPatterns(getExecution().getPatterns());
        search.setInputFiles(getExecution().getInputFiles());
	search.setInfolisFileTags(getExecution().getInfolisFileTags());
        search.setIndexDirectory(getExecution().getIndexDirectory());
	search.setTokenize(getExecution().isTokenize());
	search.setRemoveBib(getExecution().isRemoveBib());
        getOutputDataStoreClient().post(Execution.class, search);
        search.instantiateAlgorithm(this).run();
        updateProgress(1, 2);
    	debug(log, "Done executing NameExtractor, found textualReferences: " + search.getTextualReferences());
        return search.getTextualReferences();
    }
}
