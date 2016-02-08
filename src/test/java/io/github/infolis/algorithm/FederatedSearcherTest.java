package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.resolve.DaraHTMLQueryService;
//import io.github.infolis.resolve.DaraSolrQueryService;
import io.github.infolis.resolve.HTMLQueryService;
import io.github.infolis.resolve.QueryService;
//import io.github.infolis.resolve.SolrQueryService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * 
 * @author kata
 *
 */
public class FederatedSearcherTest extends InfolisBaseTest {

    @Test
    public void testDaraQueryServices() throws IOException {
        Execution execution = new Execution();
        Entity entity = new Entity();
        entity.setName("Studierendensurvey");
        entity.setNumber("2012/13");
        dataStoreClient.post(Entity.class, entity);
        execution.setLinkedEntities(Arrays.asList(entity.getUri()));
        HTMLQueryService queryService = new DaraHTMLQueryService();
        //SolrQueryService queryService = new DaraSolrQueryService();
        queryService.setMaxNumber(10);
        dataStoreClient.post(QueryService.class, queryService);
        execution.setQueryServices(Arrays.asList(queryService.getUri()));
        execution.setAlgorithm(FederatedSearcher.class);
        execution.setSearchResultRankerClass(BestMatchRanker.class);
        Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
        algo.run();

        List<SearchResult> searchResults = dataStoreClient.get(SearchResult.class, execution.getSearchResults());
        assertEquals(1, searchResults.size());
        assertEquals("Studiensituation und studentische Orientierungen 2012/13 (Studierenden-Survey)", searchResults.get(0).getTitles().get(0));
        assertEquals("10.4232/1.5126", searchResults.get(0).getIdentifier());
    }

}
