package io.github.infolis.algorithm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.infolink.querying.QueryService.QueryField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author kata
 *
 */
public class MultiMatchesLinker extends SearchResultLinker {
	
	private static final Logger log = LoggerFactory.getLogger(MultiMatchesLinker.class);
	
	public MultiMatchesLinker(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
        setWeightForNumberBasedScore(1);
        setWeightForQSReliability(1);
        setWeightForListIndex(0);
        Set<QueryField> queryStrategy = new HashSet<>();
        queryStrategy.add(QueryField.title);
        setQueryStrategy(queryStrategy); 
    }
	
	@Override
	public void execute() {
		log.debug("Creating links to all matches...");
		if (null != getExecution().getLinkedEntities() && !getExecution().getLinkedEntities().isEmpty()) {
			String entityUri = getExecution().getLinkedEntities().get(0);
			Entity entity = getInputDataStoreClient().get(Entity.class, entityUri);
			List<CandidateTargetEntity> candidates = rankResults(entity);
			candidates = getMatchingSearchResults(candidates, 0.55);
	        List<String> entityLinks = createLinks(entity, candidates);
	        getExecution().setLinks(entityLinks);
		}
		if (null != getExecution().getTextualReferences() && !getExecution().getTextualReferences().isEmpty()) {
			String textRefURI = getExecution().getTextualReferences().get(0);
			TextualReference textRef = getInputDataStoreClient().get(TextualReference.class, textRefURI);
			List<CandidateTargetEntity> candidates = rankResults(textRef);
			candidates = getMatchingSearchResults(candidates, 0.55);
	        List<String> entityLinks = createLinks(textRef, candidates);
	        getExecution().getLinks().addAll(entityLinks);
		}
        getExecution().setStatus(ExecutionStatus.FINISHED);
	}
}