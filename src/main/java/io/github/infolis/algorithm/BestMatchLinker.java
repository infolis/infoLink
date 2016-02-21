package io.github.infolis.algorithm;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.infolink.querying.QueryService.QueryField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author kata
 *
 */
public class BestMatchLinker extends SearchResultLinker {

	private static final Logger log = LoggerFactory.getLogger(BestMatchLinker.class);
	
	public BestMatchLinker(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
        setWeightForNumberBasedScore(0);
        setWeightForQSReliability(1);
        setWeightForListIndex(1);
        Set<QueryField> queryStrategy = new HashSet<>();
        queryStrategy.add(QueryField.numericInfoInTitle);
        setQueryStrategy(queryStrategy); 
    }
	
	@Override
	public void execute() {
		log.debug("Creating link to best match...");
		String textRefURI = getExecution().getTextualReferences().get(0);
        TextualReference textRef = getInputDataStoreClient().get(TextualReference.class, textRefURI);
		Map<SearchResult, Double> scoreMap = rankResults(textRef);
		Map<SearchResult, Double> bestMatch = getBestSearchResult(scoreMap); 
        List<String> entityLinks = createLinks(textRef, bestMatch);
        getExecution().setLinks(entityLinks);
        getExecution().setStatus(ExecutionStatus.FINISHED);
	}

}