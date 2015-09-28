package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.infolink.datasetMatcher.HTMLQueryService;
import io.github.infolis.infolink.datasetMatcher.QueryService;
import io.github.infolis.infolink.datasetMatcher.SolrQueryService;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.SearchResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
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
        execution.setSearchQuery("?q=title:Studierendensurvey");
        execution.setQueryServices(qsList);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        
        List<String> sr= execution.getSearchResults();
        List<SearchResult> result = dataStoreClient.get(SearchResult.class, sr);
        
        //TODO: find test examples
    }
    
    @Test
    public void testQueryAdaption() {
        SolrQueryService qs = new SolrQueryService("http://www.da-ra.de/solr/dara/",1.0);
        String query = qs.adaptQuery("?q=title:ALLBUS");        
        Assert.assertEquals("http://www.da-ra.de/solr/dara/select/?q=title:ALLBUS&start=0&rows=10000&fl=doi,title&wt=json", query);
    }
    
    @Test
    public void testResponse() throws FileNotFoundException, IOException {
        InputStream is = new FileInputStream(new File(getClass().getResource("/solr/solrResponse.json").getFile()));
        JsonReader reader = null;
        try {
            reader = Json.createReader(is);
            JsonObject obj = reader.readObject();
            JsonObject response = obj.getJsonObject("response");
            JsonArray result = response.getJsonArray("docs");
            Iterator i = result.iterator();
            while(i.hasNext()) {
                System.out.println(i.next());
            }
        } finally {
            reader.close();
            is.close();
        }
    }
    
}
