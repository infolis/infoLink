package io.github.infolis.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.infolink.querying.SearchResultScorer;
import io.github.infolis.infolink.querying.QueryService;
import io.github.infolis.infolink.querying.QueryService.QueryField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author kata
 * @author domi
 *
 */
public abstract class SearchResultLinker extends BaseAlgorithm {

	private static final Logger log = LoggerFactory.getLogger(SearchResultLinker.class);
	// weight for number-based score, weight for reliability of QueryService, weight for list index
	private int[] weights = {1, 1, 1};
	Set<QueryField> queryStrategy;

    public SearchResultLinker(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }
    
    public void setWeightForNumberBasedScore(int weight) {
    	weights[0] = weight;
    }
    
    public void setWeightForQSReliability(int weight) {
    	weights[1] = weight;
    }
    
    public void setWeightForListIndex(int weight) {
    	weights[2] = weight;
    }
    
    public void setQueryStrategy(Set<QueryField> queryStrategy) {
    	this.queryStrategy = queryStrategy;
    }
    
    public Set<QueryField> getQueryStrategy() {
    	return this.queryStrategy;
    }

    public Map<SearchResult, Double> rankResults(TextualReference textRef) {
    	List<String> searchResultURIs = getExecution().getSearchResults();
        List<SearchResult> searchResults = getInputDataStoreClient().get(SearchResult.class, searchResultURIs);
        
        Map<SearchResult, Double> scoreMap = new HashMap<>();
        int counter = 0;
        for (SearchResult searchResult : searchResults) {
            counter++;
            double confidenceValue = 0.0;
            log.debug("Computing score based on numbers. Weight: " + weights[0]);
            if (0 != weights[0]) confidenceValue = weights[0] * SearchResultScorer.computeScoreBasedOnNumbers(textRef, searchResult);
            log.debug("Adding score based on query service reliability. Weight: " + weights[1]);
            confidenceValue += weights[1] * getInputDataStoreClient().get(QueryService.class, searchResult.getQueryService()).getReliability();
            log.debug("Adding score based on list index. Weight: " + weights[2]);
            // normalize: +1 to avoid NaN if only results contains only one search result
            confidenceValue += weights[2] * (1 - ((double) searchResult.getListIndex() / ((double) searchResults.get(searchResults.size() - 1).getListIndex() + 1)));
            log.debug("Confidence score: " + confidenceValue);
            scoreMap.put(searchResult, confidenceValue);
            updateProgress(counter, searchResults.size());
        }
        return scoreMap;
    }
    
    public Map<SearchResult, Double> getBestSearchResult(Map<SearchResult, Double> scoreMap) {
    	SearchResult bestSearchResult = null;
        double bestScore = -1.0;
        log.debug("Selecting the best search results");
        for (SearchResult sr : scoreMap.keySet()) {
            if (scoreMap.get(sr) > bestScore) {
            	bestScore = scoreMap.get(sr);
                bestSearchResult = sr;
            }
        }
        log.debug("Best search result: " + bestSearchResult);
        log.debug("Score: " + bestScore);
        Map<SearchResult, Double> resultMap = new HashMap<>();
        resultMap.put(bestSearchResult, bestScore);
        return resultMap;
    }
    
    public Map<SearchResult, Double> getMatchingSearchResults(Map<SearchResult, Double> scoreMap, double threshold) {
        log.debug("Selecting all search results with score above or equal to threshold");
        Map<SearchResult, Double> resultMap = new HashMap<>();
        for (SearchResult sr : scoreMap.keySet()) {
        	log.debug("Score for search result " + sr.getUri() + ": " + scoreMap.get(sr));
            if (scoreMap.get(sr) >= threshold) {
            	resultMap.put(sr, scoreMap.get(sr));
            }
        }
        return resultMap;
    }
    
    public List<String> createLinks(TextualReference textRef, Map<SearchResult, Double> scoreMap) {
    	List<String> entityLinks = new ArrayList<>();
    	for (SearchResult searchResult : scoreMap.keySet()) {
	    	Entity referencedInstance = new Entity();
	        referencedInstance.setTags(getExecution().getTags());
	        referencedInstance.setIdentifier(searchResult.getIdentifier());
	        if(searchResult.getTitles() != null && searchResult.getTitles().size()>0) {
	            referencedInstance.setName(searchResult.getTitles().get(0));
	        }
	        if(searchResult.getNumericInformation() != null && searchResult.getNumericInformation().size()>0) {
	            referencedInstance.setNumber(searchResult.getNumericInformation().get(0));
	        }
	        getOutputDataStoreClient().post(Entity.class, referencedInstance);
	        String linkReason = textRef.getUri();
	        
	        log.debug("Creating link for TextualReference: " + textRef.getReference() + "; mentionsReference: " + textRef.getMentionsReference());
	        Entity fromEntity = getInputDataStoreClient().get(Entity.class, textRef.getMentionsReference());
	        log.debug("File: " + fromEntity.getFile());
	        EntityLink el = new EntityLink(fromEntity.getUri(), referencedInstance.getUri(), scoreMap.get(searchResult), linkReason);
	
	        //TODO should EntityLink have tags?
	        getOutputDataStoreClient().post(EntityLink.class, el);
	        entityLinks.add(el.getUri());
    	}
        return entityLinks;
    }
	
	@Override
    public void validate() throws IllegalAlgorithmArgumentException {
        if (null == getExecution().getSearchResults() || getExecution().getSearchResults().isEmpty() ){
            throw new IllegalAlgorithmArgumentException(getClass(), "searchResults", "Required parameter 'search results' is missing!");
        }
        if (null == getExecution().getTextualReferences() || getExecution().getTextualReferences().isEmpty()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "textualReferences", "Required parameter 'textual references' is missing!");
        }
    }
}