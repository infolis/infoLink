package io.github.infolis.resolve;

import static org.junit.Assert.assertEquals;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.algorithm.FederatedSearcher;
import io.github.infolis.model.Execution;
import io.github.infolis.model.SearchQuery;
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
		SearchQuery searchQuery;
		Map<String, String> doiTitleMap;
		
		ExpectedOutput(QueryService queryService, SearchQuery searchQuery, Map<String, String> doiTitleMap) {
			this.queryService = queryService;
			this.searchQuery = searchQuery;
			this.doiTitleMap = doiTitleMap;
		}
		
		QueryService getQueryService() {
			return this.queryService;
		}
		
		SearchQuery getSearchQuery() {
			return this.searchQuery;
		}
		
		Map<String, String> getDoiTitleMap() {
			return this.doiTitleMap;
		}
	}
	
	private static Set<ExpectedOutput> getExpectedOutput() {
		Set<ExpectedOutput> expectedOutput = HTMLQueryServiceTest.getExpectedOutput();
		if (null != System.getProperty("gesisRemoteTest")) {
			expectedOutput.addAll(SolrQueryServiceTest.getExpectedOutput());
		}
		return expectedOutput;
	};
	
	@Test
    public void testQueryService() throws IOException {
    	Map<String, String> doiTitleMap = new HashMap<>();
        for (ExpectedOutput expectedOutputItem : expectedOutput) {
        	SearchQuery searchQuery = expectedOutputItem.getSearchQuery();
        	dataStoreClient.post(SearchQuery.class, searchQuery);
        	QueryService queryService = expectedOutputItem.getQueryService();
        	dataStoreClient.post(QueryService.class, queryService);

        	Execution execution = new Execution();
        	execution.setAlgorithm(FederatedSearcher.class);
        	execution.setSearchQuery(searchQuery.getUri());
        	execution.setQueryServices(Arrays.asList(queryService.getUri()));
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