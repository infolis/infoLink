package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.infolink.querying.DaraHTMLQueryService;
import io.github.infolis.infolink.querying.QueryService;

import java.io.IOException;
import java.util.ArrayList;
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
        List<String> numInfo = new ArrayList<>();
        numInfo.add("2012/13");
        entity.setNumericInfo(numInfo);
        dataStoreClient.post(Entity.class, entity);
        execution.setLinkedEntities(Arrays.asList(entity.getUri()));
        QueryService queryService = new DaraHTMLQueryService();
        dataStoreClient.post(QueryService.class, queryService);
        execution.setQueryServices(Arrays.asList(queryService.getUri(), queryService.getUri()));
        execution.setAlgorithm(FederatedSearcher.class);
        execution.setSearchResultLinkerClass(BestMatchLinker.class);
        Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
        algo.run();

        List<SearchResult> searchResults = dataStoreClient.get(SearchResult.class, execution.getSearchResults());
        // since the query service is given twice, FederatedSearcher should find the same result twice
        assertEquals(2, searchResults.size());
        assertEquals("10.4232/1.5126", searchResults.get(0).getIdentifier());
        assertEquals("10.4232/1.5126", searchResults.get(1).getIdentifier());
    }

}
