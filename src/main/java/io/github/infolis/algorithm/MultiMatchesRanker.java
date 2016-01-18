package io.github.infolis.algorithm;

import java.util.List;
import java.util.Map;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author kata
 *
 */
public class MultiMatchesRanker extends SearchResultRanker {
	
private static final Logger log = LoggerFactory.getLogger(MultiMatchesRanker.class);
	
	public MultiMatchesRanker(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
        setWeightForNumberBasedScore(1);
        setWeightForQSReliability(1);
        setWeightForListIndex(0);
    }
	
	@Override
	public void execute() {
		log.debug("Creating links to all matches...");
		String textRefURI = getExecution().getTextualReferences().get(0);
        TextualReference textRef = getInputDataStoreClient().get(TextualReference.class, textRefURI);
		Map<SearchResult, Double> scoreMap = rankResults(textRef);
        List<String> entityLinks = createLinks(textRef, scoreMap);
        getExecution().setLinks(entityLinks);
        getExecution().setStatus(ExecutionStatus.FINISHED);
	}
}