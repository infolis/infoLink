package io.github.infolis.resolve;

import static org.junit.Assert.assertEquals;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.algorithm.FederatedSearcher;
import io.github.infolis.algorithm.SearchResultLinker;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.SearchResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author kata
 *
 */
public class QueryServiceTest extends InfolisBaseTest {
	
	Logger log = LoggerFactory.getLogger(QueryServiceTest.class);
	Set<ExpectedOutput> expectedOutput = new HashSet<>();
	
	public QueryServiceTest() {
        expectedOutput = getExpectedOutput();
	}
	
	static class ExpectedOutput {
		QueryService queryService;
		Entity entity;
		Map<String, String> doiTitleMap;
		Class<? extends SearchResultLinker> searchResulterLinkerClass;
		
		ExpectedOutput(QueryService queryService, Entity entity, Class<? extends SearchResultLinker> searchResultLinkerClass, Map<String, String> doiTitleMap) {
			this.queryService = queryService;
			this.entity = entity;
			this.searchResulterLinkerClass = searchResultLinkerClass;
			this.doiTitleMap = doiTitleMap;
		}
		
		QueryService getQueryService() {
			return this.queryService;
		}
		
		Entity getEntity() {
			return this.entity;
		}
		
		Class<? extends SearchResultLinker> getSearchResultLinkerClass() {
			return this.searchResulterLinkerClass;
		}
		
		Map<String, String> getDoiTitleMap() {
			return this.doiTitleMap;
		}
	}
	
	private static Set<ExpectedOutput> getExpectedOutput() {
		Set<ExpectedOutput> expectedOutput = DaraHTMLQueryServiceTest.getExpectedOutput();
		if (null != System.getProperty("gesisRemoteTest")) {
			expectedOutput.addAll(DaraSolrQueryServiceTest.getExpectedOutput());
		}
		return expectedOutput;
	};
	
	@Test
    public void testQueryService() throws IOException {
    	Map<String, String> doiTitleMap = new HashMap<>();
        for (ExpectedOutput expectedOutputItem : expectedOutput) {
        	Entity entity = expectedOutputItem.getEntity();
        	dataStoreClient.post(Entity.class, entity);
        	QueryService queryService = expectedOutputItem.getQueryService();
        	dataStoreClient.post(QueryService.class, queryService);

        	Execution execution = new Execution();
        	execution.setAlgorithm(FederatedSearcher.class);
        	execution.setLinkedEntities(Arrays.asList(entity.getUri()));
        	execution.setQueryServices(Arrays.asList(queryService.getUri()));
        	execution.setSearchResultLinkerClass(expectedOutputItem.getSearchResultLinkerClass());
        	execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();

        	List<String> searchResultURIs = execution.getSearchResults();
        	List<SearchResult> searchResults = dataStoreClient.get(SearchResult.class, searchResultURIs);
        
        	for (SearchResult sr : searchResults) {
        		doiTitleMap.put(sr.getIdentifier(), sr.getTitles().get(0));
        		log.debug(sr.getIdentifier());
        		log.debug(sr.getTitles().get(0));
        	}
        	assertEquals(expectedOutputItem.getDoiTitleMap(), doiTitleMap);

        }
    }
	
}