package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.resolve.DaraHTMLQueryService;
import io.github.infolis.resolve.HTMLQueryService;
import io.github.infolis.resolve.QueryService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author domi
 */
public class FederatedSearcherTest extends InfolisBaseTest {

    @Test
    public void testDaraQueryServices() throws IOException {
        Execution execution = new Execution();
        SearchQuery searchQuery = postDoiQuery("?q=title:ALLBUS");
        execution.setSearchQuery(searchQuery.getUri());
        HTMLQueryService queryService = new DaraHTMLQueryService();
        queryService.setMaxNumber(10);
        dataStoreClient.post(QueryService.class, queryService);
        execution.setQueryServices(Arrays.asList(queryService.getUri()));
        execution.setAlgorithm(FederatedSearcher.class);
        Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
        algo.run();

        List<SearchResult> sr = dataStoreClient.get(SearchResult.class, execution.getSearchResults());

        //be careful, could change from time to time
        Assert.assertEquals("Anomie (ALLBUS)", sr.get(0).getTitles().get(0));
        Assert.assertEquals("10.6102/zis58", sr.get(0).getIdentifier());
        Assert.assertEquals(0, sr.get(0).getListIndex());

    }

    public SearchQuery postDoiQuery(String q) throws IOException {
        SearchQuery sq = new SearchQuery();
        sq.setQuery(q);
        dataStoreClient.post(SearchQuery.class, sq);
        return sq;
    }

}
