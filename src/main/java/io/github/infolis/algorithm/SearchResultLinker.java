package io.github.infolis.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.EntityType;
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
	private int maxNum = 1000;

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
    
    public void setMaxNum(int maxNum) {
    	this.maxNum = maxNum;
    }
    
    public int getMaxNum() {
    	return this.maxNum;
    }
    
    public static class CandidateTargetEntity {
    	SearchResult searchResult;
    	double score;
    	Set<EntityLink.EntityRelation> entityRelations = new HashSet<>();
    
	    public void setSearchResult(SearchResult searchResult) {
	    	this.searchResult = searchResult;
	    }
	    
	    public void setScore(double score) {
	    	this.score = score;
	    }
	    
	    public void setEntityRelations(Set<EntityLink.EntityRelation> entityRelations) {
	    	this.entityRelations = entityRelations;
	    }
	    
	    public void addEntityRelation(EntityLink.EntityRelation entityRelation) {
	    	this.entityRelations.add(entityRelation);
	    }
    }
    
    public List<CandidateTargetEntity> rankResults(Entity entity) {
    	List<String> searchResultURIs = getExecution().getSearchResults();
        List<SearchResult> searchResults = getInputDataStoreClient().get(
        		SearchResult.class, searchResultURIs);
        
        List<CandidateTargetEntity> candidates = new ArrayList<>();
        int counter = 0;
        for (SearchResult searchResult : searchResults) {
            counter++;
            double confidenceValue = 0.0;
            log.debug("Computing score based on numbers. Weight: " + weights[0]);
            CandidateTargetEntity searchResultCandidate = SearchResultScorer.computeScoreBasedOnNumbers(entity, searchResult);
            if (0 != weights[0]) confidenceValue = weights[0] * searchResultCandidate.score;
            log.debug("Adding score based on query service reliability. Weight: " + weights[1]);
            confidenceValue += weights[1] * getInputDataStoreClient().get(QueryService.class, searchResult.getQueryService()).getServiceReliability();
            log.debug("Adding score based on list index. Weight: " + weights[2]);
            // normalize: +1 to avoid NaN if only results contains only one search result
            confidenceValue += weights[2] * (1 - ((double) searchResult.getListIndex() / ((double) searchResults.get(searchResults.size() - 1).getListIndex() + 1)));
            log.debug("Confidence score: " + confidenceValue);
            CandidateTargetEntity candidate = new CandidateTargetEntity();
            candidate.searchResult = searchResult;
            candidate.score = confidenceValue;
            candidate.entityRelations = searchResultCandidate.entityRelations;
            candidates.add(candidate);
            updateProgress(counter, searchResults.size());
        }
        return candidates;
    }

    public List<CandidateTargetEntity> rankResults(TextualReference textRef) {
    	List<String> searchResultURIs = getExecution().getSearchResults();
        List<SearchResult> searchResults = getInputDataStoreClient().get(SearchResult.class, searchResultURIs);
        
        List<CandidateTargetEntity> candidates = new ArrayList<>();
        int counter = 0;
        for (SearchResult searchResult : searchResults) {
            counter++;
            double confidenceValue = 0.0;
            log.debug("Computing score based on numbers. Weight: " + weights[0]);
            CandidateTargetEntity searchResultCandidate = SearchResultScorer.computeScoreBasedOnNumbers(textRef, searchResult);
            if (0 != weights[0]) confidenceValue = weights[0] * searchResultCandidate.score;
            log.debug("Adding score based on query service reliability. Weight: " + weights[1]);
            confidenceValue += weights[1] * getInputDataStoreClient().get(QueryService.class, searchResult.getQueryService()).getServiceReliability();
            log.debug("Adding score based on list index. Weight: " + weights[2]);
            // normalize: +1 to avoid NaN if only results contains only one search result
            confidenceValue += weights[2] * (1 - ((double) searchResult.getListIndex() / ((double) searchResults.get(searchResults.size() - 1).getListIndex() + 1)));
            log.debug("Confidence score: " + confidenceValue);
            CandidateTargetEntity candidate = new CandidateTargetEntity();
            candidate.searchResult = searchResult;
            candidate.score = confidenceValue;
            candidate.entityRelations = searchResultCandidate.entityRelations;
            candidates.add(candidate);
            updateProgress(counter, searchResults.size());
        }
        return candidates;
    }
    
    // the confidence score of the best result equals the confidence score of the QueryService
    public  List<CandidateTargetEntity> getBestResultsAtFirstIndex() {
    	List<CandidateTargetEntity> candidates = new ArrayList<>();
    	List<SearchResult> searchResults = getInputDataStoreClient().get(
    			SearchResult.class, getExecution().getSearchResults());
        for (SearchResult searchResult : searchResults) {
        	if (searchResult.getListIndex() == 0) {
        		double confidence = 
        				weights[1] * getInputDataStoreClient().get(QueryService.class, searchResult.getQueryService())
        				.getServiceReliability();
        		CandidateTargetEntity candidate = new CandidateTargetEntity();
        		candidate.searchResult = searchResult;
        		candidate.score = confidence;
        		candidate.entityRelations = new HashSet<>(Arrays.asList(
        				EntityLink.EntityRelation.unknown));
                candidates.add(candidate);
        	}
        }
        return candidates;
    }
    
    public List<CandidateTargetEntity> getBestSearchResult(List<CandidateTargetEntity> candidates) {
    	CandidateTargetEntity bestCandidate = null;
        double bestScore = -1.0;
        log.debug("Selecting the best search results");
        for (CandidateTargetEntity candidate : candidates) {
            if (candidate.score > bestScore) {
            	bestScore = candidate.score;
                bestCandidate = candidate;
            }
        }
        log.debug("Best search result: " 
        		+ bestCandidate.searchResult.getIdentifier() + ": " 
        		+ bestCandidate.searchResult.getTitles());
        log.debug("Score: " + bestScore);
        List<CandidateTargetEntity> bestCandidates = new ArrayList<>();
        CandidateTargetEntity candidate = new CandidateTargetEntity();
        candidate.searchResult = bestCandidate.searchResult;
        candidate.score = bestScore;
        candidate.entityRelations = new HashSet<>(Arrays.asList(
        		EntityLink.EntityRelation.unknown));
        bestCandidates.add(candidate);
        return bestCandidates;
    }
    
    public List<CandidateTargetEntity> getMatchingSearchResults(
    		List<CandidateTargetEntity> candidates, double threshold) {
        log.debug("Selecting all search results with score above or equal to threshold");
        List<CandidateTargetEntity> matchingCandidates = new ArrayList<>();
        for (CandidateTargetEntity candidate : candidates) {
        	log.debug("Score for search result " + candidate.searchResult.getUri() + ": " 
        			+ candidate.score);
            if (candidate.score >= threshold) {
            	matchingCandidates.add(candidate);
            }
        }
        return matchingCandidates;
    }
    
    public List<String> createLinks(Entity fromEntity, 
    		List<CandidateTargetEntity> candidates) {
    	List<String> entityLinks = new ArrayList<>();
    	for (CandidateTargetEntity candidate : candidates) {
	    	Entity toEntity = new Entity();
	    	toEntity.setTags(candidate.searchResult.getTags());
	    	toEntity.addAllTags(getExecution().getTags());
	    	// TODO as of now, setting EntityType to dataset is always correct
	    	// if queryservices are added which incorporate databases also, 
	    	// distinguish the types here
	    	toEntity.setEntityType(EntityType.dataset);
	    	toEntity.addIdentifier(candidate.searchResult.getIdentifier());
	        if (candidate.searchResult.getTitles() != null 
	        		&& candidate.searchResult.getTitles().size()>0) {
	        	toEntity.setName(candidate.searchResult.getTitles().get(0));
	        }
	        if (candidate.searchResult.getNumericInformation() != null 
	        		&& candidate.searchResult.getNumericInformation().size()>0) {
                    List<String> numInfo = new ArrayList<>();
                    numInfo.add(candidate.searchResult.getNumericInformation().get(0));
                    toEntity.setNumericInfo(numInfo);
	        }
	        getOutputDataStoreClient().post(Entity.class, toEntity);
	        
	        log.debug("Creating link for entity: " + fromEntity.getUri());
	        EntityLink el = new EntityLink(fromEntity.getUri(), toEntity.getUri(), 
	        		candidate.score, 
	        		"");
	        el.setEntityRelations(candidate.entityRelations);
	        el.setTags(toEntity.getTags());
	
	        getOutputDataStoreClient().post(EntityLink.class, el);
	        entityLinks.add(el.getUri());
    	}
        return entityLinks;
    }
    

    
    public List<String> createLinks(TextualReference textRef, 
    		List<CandidateTargetEntity> candidates) {
    	List<String> entityLinks = new ArrayList<>();
    	for (CandidateTargetEntity candidate : candidates) {
	    	Entity referencedInstance = new Entity();
	        referencedInstance.setTags(candidate.searchResult.getTags());
	        referencedInstance.addAllTags(getExecution().getTags());
	        // TODO as of now, setting EntityType to dataset is always correct
	    	// if queryservices are added which incorporate databases also, 
	    	// distinguish the types here
	        referencedInstance.setEntityType(EntityType.dataset);
	        referencedInstance.addIdentifier(candidate.searchResult.getIdentifier());
	        if(candidate.searchResult.getTitles() != null && candidate.searchResult.getTitles().size()>0) {
	            referencedInstance.setName(candidate.searchResult.getTitles().get(0));
	        }
	        if(candidate.searchResult.getNumericInformation() != null 
	        		&& candidate.searchResult.getNumericInformation().size()>0) {
                    List<String> numInfo = new ArrayList<>();
                    numInfo.add(candidate.searchResult.getNumericInformation().get(0));
	            referencedInstance.setNumericInfo(numInfo);
	        }
	        getOutputDataStoreClient().post(Entity.class, referencedInstance);
	        String linkReason = textRef.getUri();
	        
	        log.debug("Creating link for TextualReference: " + textRef.getReference() + "; mentionsReference: " + textRef.getMentionsReference());
	        log.debug("File: " + textRef.getTextFile());
	        EntityLink el = new EntityLink(textRef.getMentionsReference(), 
	        		referencedInstance.getUri(), 
	        		candidate.score, 
	        		linkReason);
	        el.setEntityRelations(candidate.entityRelations);
	        el.setTags(referencedInstance.getTags());
	
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
        	if (null == getExecution().getLinkedEntities() || getExecution().getLinkedEntities().isEmpty()) {
        		throw new IllegalAlgorithmArgumentException(getClass(), "linkedEntities/textualReferences", "Required parameter 'linked entities' or 'textual references' is missing!");
        	}
        }
    }
}