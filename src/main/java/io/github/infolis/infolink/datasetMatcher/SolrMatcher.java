/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.infolink.datasetMatcher;

import io.github.infolis.util.URLParamEncoder;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class SolrMatcher {

    Logger log = LoggerFactory.getLogger(SolrMatcher.class);
    String solrBase = "http://www.da-ra.de/solr/dara/";
    String title;

    public SolrMatcher(String solrBase) throws UnsupportedEncodingException {
        this.solrBase = solrBase;
    }

    public JsonArray query(String title) throws MalformedURLException, IOException {
        this.title = URLParamEncoder.encode(title);
        return executeQuery(constructQuery());
    }

    //TODO: Query abstrahieren
    private URL constructQuery() throws MalformedURLException {
        String beginning = "select/?q=title:";
        String remainder = "&start=0&rows=10000&fl=doi,title&wt=json";
        log.debug(this.solrBase + beginning + this.title + remainder);
        return new URL(this.solrBase + beginning + this.title + remainder);
    }

    private JsonArray executeQuery(URL query) throws IOException {
        InputStream is = null;
        JsonReader reader = null;
        try {
            is = query.openStream();
            reader = Json.createReader(is);
            JsonObject obj = reader.readObject();
            JsonObject response = obj.getJsonObject("response");
            JsonArray result = response.getJsonArray("docs");
            return result;
        } finally {
            reader.close();
            is.close();
        }
    }

}
