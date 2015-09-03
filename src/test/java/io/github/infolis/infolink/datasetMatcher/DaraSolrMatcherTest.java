package io.github.infolis.infolink.datasetMatcher;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaraSolrMatcherTest {

    Logger log = LoggerFactory.getLogger(DaraSolrMatcherTest.class);
    String datasetName = "Studierendensurvey";
    Map<String, String> expectedOutput = new HashMap<>();

    // note: da|ra's database may change! If test fails, check whether these values are still correct
    public DaraSolrMatcherTest() {
        expectedOutput.put("10.4232/1.4263", "Studiensituation und studentische Orientierungen 2006/07 (Studierenden-Survey)");
        expectedOutput.put("10.4232/1.4344", "Studiensituation und studentische Orientierungen 2003/04 (Studierenden-Survey)");
        expectedOutput.put("10.4232/1.11059", "Studiensituation und studentische Orientierungen 2009/10 (Studierenden-Survey)");
        expectedOutput.put("10.4232/1.1884", "Studiensituation und studentische Orientierungen 1982/83 (Studierenden-Survey)");
        expectedOutput.put("10.4232/1.1885", "Studiensituation und studentische Orientierungen 1984/85 (Studierenden-Survey)");
        expectedOutput.put("10.4232/1.3130", "Studiensituation und studentische Orientierungen 1992/93 (Studierenden-Survey)");
        expectedOutput.put("10.4232/1.3131", "Studiensituation und studentische Orientierungen 1994/95 (Studierenden-Survey)");
        expectedOutput.put("10.4232/1.2416", "Studiensituation und studentische Orientierungen 1986/87 (Studierenden-Survey)");
        expectedOutput.put("10.4232/1.2417", "Studiensituation und studentische Orientierungen 1989/90 (Studierenden-Survey)");
        expectedOutput.put("10.4232/1.5126", "Studiensituation und studentische Orientierungen 2012/13 (Studierenden-Survey)");
        expectedOutput.put("10.4232/1.4208", "Studiensituation und studentische Orientierungen 2000/01 (Studierenden-Survey)");
        expectedOutput.put("10.4232/1.3511", "Studiensituation und studentische Orientierungen 1997/98 (Studierenden-Survey)");
    }

    @Test
    public void testQuery() throws MalformedURLException, IOException {
        Assume.assumeNotNull(System.getProperty("gesisRemoteTest"));        
        Map<String, String> output = new HashMap<>();        
        DaraSolrMatcher matcher = new DaraSolrMatcher(this.datasetName);
        JsonArray json = matcher.query();
        for (JsonObject result : json.getValuesAs(JsonObject.class)) {
            String title1 = result.getJsonArray("title").getString(0);
            String doi = result.getJsonArray("doi").getString(0);
            output.put(doi, title1);
        }
        assertEquals(this.expectedOutput, output);
    }
}
