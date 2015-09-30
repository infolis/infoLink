package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.infolink.datasetMatcher.HTMLQueryService;
import io.github.infolis.infolink.datasetMatcher.QueryService;
import io.github.infolis.infolink.datasetMatcher.SolrQueryService;
import io.github.infolis.model.Execution;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.util.NumericInformationExtractor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
    public void searchWeb() throws IOException {        
        
        QueryService qs = new SolrQueryService("http://www.da-ra.de/solr/dara/");              
        dataStoreClient.post(QueryService.class, qs);
        List<String> qsList = new ArrayList<>();
        qsList.add(qs.getUri());
        
        Assume.assumeNotNull(System.getProperty("gesisRemoteTest")); 
        Execution execution = new Execution();
        execution.setAlgorithm(FederatedSearcher.class);
        String searchQuery = postTitleQuery();
        execution.setSearchQuery(searchQuery);
        execution.setQueryServices(qsList);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        
        List<String> sr= execution.getSearchResults();
        List<SearchResult> result = dataStoreClient.get(SearchResult.class, sr);
        
        //TODO: find test examples
    }
    
    public String postTitleQuery() throws IOException {
        SearchQuery sq = new SearchQuery();
        sq.setQuery("?q=title:Studierendensurvey");
        sq.setReferenceType(TextualReference.ReferenceType.TITEL);
        dataStoreClient.post(SearchQuery.class, sq);
        return sq.getUri();
    }
    
    @Test
    public void testQueryAdaption() {
        SolrQueryService qs = new SolrQueryService("http://www.da-ra.de/solr/dara/",1.0);
        String query = qs.adaptQuery("?q=title:ALLBUS");        
        Assert.assertEquals("http://www.da-ra.de/solr/dara/select/?q=title:ALLBUS&start=0&rows=10000&fl=doi,title&wt=json", query);
    }
    
    @Test
    public void testTitleResponse() throws FileNotFoundException, IOException {
        InputStream is = new FileInputStream(new File(getClass().getResource("/solr/solrTitleResponse.json").getFile()));
        JsonReader reader = null;
        try {
            reader = Json.createReader(is);
            JsonObject obj = reader.readObject();
            JsonObject response = obj.getJsonObject("response");
            JsonArray result = response.getJsonArray("docs");
            int i=-1;
            for (JsonObject single : result.getValuesAs(JsonObject.class)) {
                i++;
                String title1 = single.getJsonArray("title").getString(0);
                String title2 = single.getJsonArray("title").getString(1);
                String doi = single.getJsonArray("doi").getString(0);
                switch (i) {
                case 0:
                    Assert.assertEquals("Studiensituation und studentische Orientierungen 2006/07 (Studierenden-Survey)", title1);
                    break;
                case 2:
                    Assert.assertEquals("College Situation and Student Orientations 1992/93", title2);
                    break;
                case 6:
                    Assert.assertEquals("10.4232/1.4344", doi);
                    break;
                }                
            }
            Assert.assertEquals(11,i);
        } finally {
            reader.close();
            is.close();
        }
    }
    
    @Test
    public void testDoiResponse() throws FileNotFoundException, IOException {
        InputStream is = new FileInputStream(new File(getClass().getResource("/solr/solrDOIResponse.json").getFile()));
        JsonReader reader = null;
        try {
            reader = Json.createReader(is);
            JsonObject obj = reader.readObject();
            JsonObject response = obj.getJsonObject("response");
            JsonArray result = response.getJsonArray("docs");
            int i=-1;
            for (JsonObject single : result.getValuesAs(JsonObject.class)) {
                i++;
                String title1 = single.getJsonArray("title").getString(0);
                String title2 = single.getJsonArray("title").getString(1);
                String doi = single.getJsonArray("doi").getString(0);
                switch (i) {
                case 0:
                    Assert.assertEquals("Flash Eurobarometer 35", title1);
                    Assert.assertEquals(title1, title2);
                    Assert.assertEquals("10.4232/1.2525", doi);
                    break;
                }                
            }
            Assert.assertEquals(0,i);
        } finally {
            reader.close();
            is.close();
        }
    }
    
}
