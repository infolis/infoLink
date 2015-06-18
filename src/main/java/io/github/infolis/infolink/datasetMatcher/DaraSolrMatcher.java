package io.github.infolis.infolink.datasetMatcher;

import java.io.IOException;
import java.io.InputStream;
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
	
	DaraSolrMatcher(String title) {
		this.title = title;
	}
	
	public JsonArray query() throws MalformedURLException, IOException {
		return executeQuery(constructQuery());
	}
	
	public URL constructQuery() throws MalformedURLException {
		String beginning = "select/?q=title:";
		String remainder = "&start=0&rows=10000&fl=doi,title&wt=json";
		return new URL(this.solrBase + beginning + this.title + remainder);
	}
	
	public JsonArray executeQuery(URL query) throws IOException {
		InputStream is = null;
		JsonReader reader = null;
		try {
			is = query.openStream();
			reader = Json.createReader(is);
			JsonObject obj = reader.readObject();
			JsonObject response = obj.getJsonObject("response");
			JsonArray result = response.getJsonArray("docs");
			return result;		
		}
		catch (IOException ioe) { throw new IOException(); }
		finally { reader.close(); is.close(); }
	}
}