package io.github.infolis.resolve;

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

public class DaraSolrMatcher {

    Logger log = LoggerFactory.getLogger(DaraSolrMatcher.class);
    String solrBase = "http://www.da-ra.de/solr/dara/";
    String title;

    public DaraSolrMatcher(String title) throws UnsupportedEncodingException {
        this.title = URLParamEncoder.encode(title);
    }

    public JsonArray query() throws MalformedURLException, IOException {
        return executeQuery(constructQuery());
    }

    private URL constructQuery() throws MalformedURLException {
        String beginning = "select/?q=title:";
        String remainder = "&start=0&rows=10000&fl=doi,title&wt=json";
        log.debug(this.solrBase + beginning + this.title + remainder);
        return new URL(this.solrBase + beginning + this.title + remainder);
    }

    private JsonArray executeQuery(URL query) throws IOException {
		try (InputStream is = query.openStream()) {
			try (JsonReader reader = Json.createReader(is)) {
				JsonObject obj = reader.readObject();
				JsonObject response = obj.getJsonObject("response");
				JsonArray result = response.getJsonArray("docs");
				return result;
			}
		}
    }
}
