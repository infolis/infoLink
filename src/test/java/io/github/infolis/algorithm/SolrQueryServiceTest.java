package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.infolink.datasetMatcher.HTMLQueryService;
import io.github.infolis.infolink.datasetMatcher.QueryService;
import io.github.infolis.infolink.datasetMatcher.SolrQueryService;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.SearchResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 *
 * @author domi
 */
public class SolrQueryServiceTest extends InfolisBaseTest {
    
    @Test
    public void searchWeb() {        
        
        QueryService qs = new SolrQueryService("http://www.da-ra.de/solr/dara/");              
        dataStoreClient.post(QueryService.class, qs);
        List<String> qsList = new ArrayList<>();
        qsList.add(qs.getUri());
        
        Assume.assumeNotNull(System.getProperty("gesisRemoteTest")); 
        Execution execution = new Execution();
        execution.setAlgorithm(FederatedSearcher.class);
        execution.setQueryForQueryService("?q=title:Studierendensurvey");
        execution.setQueryServices(qsList);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        
        List<String> sr= execution.getSearchResults();
        List<SearchResult> result = dataStoreClient.get(SearchResult.class, sr);
        
        //TODO: find test examples
    }
    
    @Test
    public void testQueryAdaption() {
        SolrQueryService qs = new SolrQueryService("http://www.da-ra.de/solr/dara/");
        String query = qs.adaptQuery("?q=title:ALLBUS");        
        Assert.assertEquals("http://www.da-ra.de/solr/dara/select/?q=title:ALLBUS&start=0&rows=10000&fl=doi,title&wt=json", query);
    }
}
