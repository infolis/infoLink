package io.github.infolis.resolve;

import static org.junit.Assert.assertEquals;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.algorithm.FederatedSearcher;
import io.github.infolis.model.Execution;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.resolve.HTMLQueryService;
import io.github.infolis.resolve.QueryService;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 * 
 */
public class HTMLQueryServiceTest extends InfolisBaseTest {

	Logger log = LoggerFactory.getLogger(HTMLQueryServiceTest.class);
	Set<ExpectedOutput> expectedOutput = new HashSet<>();
	
	public HTMLQueryServiceTest() {
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
	
	public Set<ExpectedOutput> getExpectedOutput() {
		Set<ExpectedOutput> expectedOutput = DaraHTMLQueryServiceTest.getExpectedOutput();
		// add other expected output here to test other sub-classes of HTMLQueryService
		return expectedOutput;
	}
	
    @Test
    public void testHTMLQueryService() throws IOException {
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


    @Test
    public void testAdaptQuery() throws IOException {
        HTMLQueryService queryService = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show");
        queryService.setMaxNumber(600);
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setQuery("?q=title:Studierendensurvey");
        String query = queryService.adaptQuery(searchQuery);
        Assert.assertEquals("http://www.da-ra.de/dara/study/web_search_show?title=Studierendensurvey&max=600&lang=de", query);
    }

}
