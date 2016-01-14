package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.resolve.HTMLQueryService;
import io.github.infolis.resolve.QueryService;
import io.github.infolis.resolve.SolrQueryService;
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
        p.setFile(p.getUri());
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
        HTMLQueryService qs = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show",0.5);
        qs.setMaxNumber(15);
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
                List<String> allTitles = Arrays.asList(title1,title2);
                r.setTitles(allTitles);
                r.setDate(Long.toString(System.currentTimeMillis()));
                List<String> allTags = Arrays.asList("http://www.da-ra.de/solr/dara/");
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

    @Test
    public void reproduceError() {
        //http://infolis.gesis.org/infolink/api/execution/297d0e30-8fcc-11e5-87d1-9febea864153
        Entity p = new Entity();
        p.setIdentifier("xyz");
        p.setName("abc");
        dataStoreClient.post(Entity.class, p);
        TextualReference r = new TextualReference("1243–1245. Terwey, M. (2000), »", "ALLBUS", ": A German General Social", "document", "pattern",p.getUri());
        dataStoreClient.post(TextualReference.class, r);
        //get the search results from both query services
        List<String> combinedResults = new ArrayList<>();

        SearchResult r1 = new SearchResult();
        r1.setIdentifier("10.6102/zis58");
        r1.setListIndex(0);
        List<String> allTitles = Arrays.asList("Anomie (ALLBUS)");
        r1.setTitles(allTitles);
        //r1.setNumericInformation(NumericInformationExtractor.extractNumbersFromString("Anomie (ALLBUS)"));
        dataStoreClient.post(SearchResult.class, r1);

        SearchResult r2 = new SearchResult();
        r2.setIdentifier("10.6102/zis13");
        r2.setListIndex(1);
        List<String> allTitles2 = Arrays.asList("Arbeitsorientierung (ALLBUS)");
        r2.setTitles(allTitles2);
        //r2.setNumericInformation(NumericInformationExtractor.extractNumbersFromString("Arbeitsorientierung (ALLBUS)"));
        dataStoreClient.post(SearchResult.class, r2);

        combinedResults.add(r1.getUri());
        combinedResults.add(r2.getUri());

        Execution execution = new Execution();
        execution.setAlgorithm(Resolver.class);
        execution.setSearchResults(combinedResults);

        List<String> references = Arrays.asList(r.getUri());
        execution.setTextualReferences(references);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        List<EntityLink> ents = dataStoreClient.get(EntityLink.class, execution.getLinks());

        Entity toEntity = dataStoreClient.get(Entity.class, ents.get(0).getToEntity());
    }

    @Test
    public void testNPE() {
    	SearchResult resultWithoutYear = new SearchResult();
    	resultWithoutYear.addTitle("ALLBUS");
    	resultWithoutYear.setUri("uri");
    	resultWithoutYear.setListIndex(0);
    	resultWithoutYear.setIdentifier("id");
    	resultWithoutYear.setNumericInformation(new ArrayList<String>());

    	SearchResult res2 = new SearchResult();
    	res2.setNumericInformation(new ArrayList<String>());
    	res2.addTitle("ALLBUS CAPI");
    	res2.setUri("uri2");
    	res2.setListIndex(1);
    	res2.setIdentifier("id2");

    	dataStoreClient.post(SearchResult.class, resultWithoutYear);
    	dataStoreClient.post(SearchResult.class, res2);

    	Entity entity = new Entity();
    	dataStoreClient.post(Entity.class, entity);

    	//TextualReference r = new TextualReference("this is a reference to", "ALLBUS", "of some unspecified year. Match?", "document", "pattern", "entity.getUri()");
    	TextualReference r = new TextualReference("this is a reference to", "ALLBUS", "2000 some unspecified year. Match?", "document", "pattern", entity.getUri());
    	dataStoreClient.post(TextualReference.class, r);

    	Execution execution = new Execution();
        execution.setAlgorithm(Resolver.class);
        execution.setTextualReferences(Arrays.asList(r.getUri()));
        //execution.setSearchResults(Arrays.asList(resultWithoutYear.getUri(), res2.getUri()));
        execution.setSearchResults(Arrays.asList(resultWithoutYear.getUri()));
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
    }

}
