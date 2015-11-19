package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.infolink.datasetMatcher.HTMLQueryService;
import io.github.infolis.infolink.datasetMatcher.QueryService;
import io.github.infolis.infolink.datasetMatcher.SolrQueryService;
import io.github.infolis.model.Execution;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.util.NumericInformationExtractor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author domi
 */
public class ResolverTest extends InfolisBaseTest {

    /**
     * Check whether the most suitable search results from one source
     * is chosen. 
     * 
     * @throws IOException 
     */
    @Test
    public void evaluateSearchResultsFromOneSource() throws IOException {
        
        Entity p = new Entity();
        p.setIdentifier("xyz");
        p.setName("abc");
        p.setInfolisFile(p.getUri());
        dataStoreClient.post(Entity.class, p);
        //instantiate the textual reference which is later used to
        //compare against the search results
        TextualReference r = new TextualReference("the reference to the", "Studierendensurvey", "2000 is to be extracted as", "document", "pattern",p.getUri());
        dataStoreClient.post(TextualReference.class, r);
        
        System.out.println("ref: " +r.getMentionsReference());
        
        Execution execution = new Execution();
        execution.setAlgorithm(Resolver.class);
        //load the search results
        execution.setSearchResults(loadResults());
        List<String> references = Arrays.asList(r.getUri());
        execution.setTextualReferences(references);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();        
        List<EntityLink> ents = dataStoreClient.get(EntityLink.class, execution.getLinks());     
        
        //should be the study with the highest confidence
        //titel: Studiensituation und studentische Orientierungen 2000/01 (Studierenden-Survey)
        //which is the only study where a numerical overlap can be found
        Entity toEntity = dataStoreClient.get(Entity.class, ents.get(0).getToEntity());
		assertEquals(toEntity.getIdentifier(), "10.4232/1.4208");    
    }
    
    /**
     * Tests the combination of several query service results.
     * In this case, especially the reliability of the source is important.
     * 
     * @throws IOException 
     */
    @Test
    public void evaluateSearchResultCombination() throws IOException {
        Entity p = new Entity();
        p.setIdentifier("xyz");
        p.setName("abc");
        dataStoreClient.post(Entity.class, p);
        TextualReference r = new TextualReference("the reference to the", "Studierendensurvey", "2000 is to be extracted as", "document", "pattern",p.getUri());
        dataStoreClient.post(TextualReference.class, r);
        //get the search results from both query services
        List<String> combinedResults = new ArrayList<>();
        combinedResults.addAll(loadResults());
        combinedResults.addAll(loadOtherResults());
        
        Execution execution = new Execution();
        execution.setAlgorithm(Resolver.class);
        execution.setSearchResults(combinedResults);
        
        List<String> references = Arrays.asList(r.getUri());
        execution.setTextualReferences(references);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();        
        List<EntityLink> ents = dataStoreClient.get(EntityLink.class, execution.getLinks());     
        
        Entity toEntity = dataStoreClient.get(Entity.class, ents.get(0).getToEntity());
        assertEquals(toEntity.getIdentifier(), "10.4232/1.4208");    
        
    }
    
    /**
     * Load results from an HTML query service.
     * 
     * @return 
     */
    public List<String> loadOtherResults() {
        QueryService qs = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show",0.5);              
        dataStoreClient.post(QueryService.class, qs);
        List<String> qsList = new ArrayList<>();
        qsList.add(qs.getUri());
        
        SearchQuery sq = new SearchQuery();
        sq.setQuery("?q=title:Studierendensurvey");
        dataStoreClient.post(SearchQuery.class, sq);
        
        Execution execution = new Execution();
        execution.setAlgorithm(FederatedSearcher.class);
        execution.setSearchQuery(sq.getUri());
        execution.setQueryServices(qsList);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        
        return execution.getSearchResults();        
    }
    
    /**
     * Load results from a SOLR service. Since the service is only
     * available on the server, use a downloaded query response to
     * simulate the search.
     * 
     * @return
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public List<String> loadResults() throws FileNotFoundException, IOException {
        InputStream is = new FileInputStream(new File(getClass().getResource("/solr/solrTitleResponse.json").getFile()));
        JsonReader reader = null;
        List<String> searchResults = new ArrayList<>();
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
                SearchResult r = new SearchResult();
                r.setIdentifier(doi);
                r.setListIndex(i);
                List allTitles = Arrays.asList(title1,title2);
                r.setTitles(allTitles);
                r.setDate(Long.toString(System.currentTimeMillis()));
                List allTags = Arrays.asList("http://www.da-ra.de/solr/dara/");
                QueryService solr = new SolrQueryService("http://www.da-ra.de/solr/dara/", 1.0);             
                dataStoreClient.post(QueryService.class, solr);
                r.setQueryService(solr.getUri());
                r.setNumericInformation(NumericInformationExtractor.extractNumbersFromString(title1));
                dataStoreClient.post(SearchResult.class, r);
                searchResults.add(r.getUri());
            }
        }
            finally {
            reader.close();
            is.close();        
        }
        return searchResults;
    }
    
}   
