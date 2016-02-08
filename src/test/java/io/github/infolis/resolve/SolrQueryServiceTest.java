package io.github.infolis.resolve;

import io.github.infolis.model.entity.Entity;
import io.github.infolis.resolve.QueryServiceTest.ExpectedOutput;
import io.github.infolis.resolve.SolrQueryService;
import io.github.infolis.resolve.QueryService.QueryField;

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
 * @author domi
 * @author kata
 * 
 */
public class SolrQueryServiceTest {

	public static Set<ExpectedOutput> getExpectedOutput() {
		Set<ExpectedOutput> expectedOutput = DaraSolrQueryServiceTest.getExpectedOutput();
		// add other expected output here to test other sub-classes of SolrQueryService
		return expectedOutput;
	}
	
   
    @Test
    public void testCreateQuery() throws MalformedURLException {
        SolrQueryService queryService = new SolrQueryService("http://www.da-ra.de/solr/dara/", 0.5);
        Entity entity = new Entity();
        entity.setName("Studierendensurvey");
        Set<QueryField> queryStrategy = new HashSet<>();
        queryStrategy.add(QueryField.title);
        queryService.setQueryStrategy(queryStrategy);
        Assert.assertEquals(new URL("http://www.da-ra.de/solr/dara/select/?q=title:Studierendensurvey&start=0&rows=1000&fl=doi,title&wt=json"), queryService.createQuery(entity));
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
