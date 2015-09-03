package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.infolink.datasetMatcher.HTMLQueryService;
import io.github.infolis.infolink.datasetMatcher.QueryService;
import io.github.infolis.infolink.datasetMatcher.SolrQueryService;
import io.github.infolis.model.Execution;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assume;
import org.junit.Test;

/**
 *
 * @author domi
 */
public class FederatedSearcherTest extends InfolisBaseTest {
    
//    @Test
//    public void search() {
//        
//        QueryService qs = new SolrQueryService("http://www.da-ra.de/solr/dara/");              
//        dataStoreClient.post(QueryService.class, qs);
//        List<String> qsList = new ArrayList<>();
//        qsList.add(qs.getUri());
//        
//        Assume.assumeNotNull(System.getProperty("gesisRemoteTest")); 
//        Execution execution = new Execution();
//        execution.setAlgorithm(FederatedSearcher.class);
//        execution.setQueryForQueryService("?q=title:ALLBUS");
//        execution.setQueryServices(qsList);
//        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
//    }
    
    @Test
    public void searchWeb() {
        
        QueryService qs = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show");              
        dataStoreClient.post(QueryService.class, qs);
        List<String> qsList = new ArrayList<>();
        qsList.add(qs.getUri());
        
        Execution execution = new Execution();
        execution.setAlgorithm(FederatedSearcher.class);
        execution.setQueryForQueryService("?q=title:ALLBUS");
        execution.setQueryServices(qsList);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
    }
} 
    