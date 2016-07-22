package io.github.infolis.infolink.querying;

import io.github.infolis.model.entity.Entity;
import io.github.infolis.infolink.querying.QueryService.QueryField;
import io.github.infolis.infolink.querying.QueryServiceTest.ExpectedOutput;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author kata
 * @author domi
 *
 */
public class DaraSolrQueryServiceTest {
	
	public static Set<ExpectedOutput> getExpectedOutput() {
		QueryService queryService = new DaraSolrQueryService();
		// equal results must be retrieved when submitting queries via solr and submitting them via web interface
		Set<ExpectedOutput> expectedOutputHtml = DaraHTMLQueryServiceTest.getExpectedOutput();
		Set<ExpectedOutput> expectedOutput = new HashSet<ExpectedOutput>();
		for (ExpectedOutput outputHtml : expectedOutputHtml) {
			ExpectedOutput output = new ExpectedOutput(queryService, outputHtml.getEntity(), outputHtml.getSearchResultLinkerClass(), outputHtml.getDoiTitleMap());
			expectedOutput.add(output);
		}
		return expectedOutput;
	}
	
	@Test
    public void testCreateTitleQuery() throws MalformedURLException {
        QueryService queryService = new DaraSolrQueryService();
        Entity entity = new Entity();
        entity.setName("Studierendensurvey");
        Set<QueryField> queryStrategy = new HashSet<>();
        queryStrategy.add(QueryField.title);
        queryService.setQueryStrategy(queryStrategy);
        //Assert.assertEquals(new URL("http://www.da-ra.de/solr/dara/select/?q=title:Studierendensurvey+resourceType:2&start=0&rows=1000&fl=doi,title&wt=json"), queryService.createQuery(entity));
    }
	
	@Test
    public void testCreateNumInTitleQuery() throws MalformedURLException {
        QueryService queryService = new DaraSolrQueryService();
        Entity entity = new Entity();
        entity.setName("Studierendensurvey");
        Set<QueryField> queryStrategy = new HashSet<>();
        queryStrategy.add(QueryField.numericInfoInTitle);
        queryService.setQueryStrategy(queryStrategy);
        //Assert.assertEquals(new URL("http://www.da-ra.de/solr/dara/select/?q=title:Studierendensurvey+resourceType:2&start=0&rows=1000&fl=doi,title&wt=json"), queryService.createQuery(entity));
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