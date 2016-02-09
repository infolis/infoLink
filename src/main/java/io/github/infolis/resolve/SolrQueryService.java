/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.resolve;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.apache.solr.client.solrj.util.ClientUtils;

import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.util.InformationExtractor;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 * @author domi
 *
 * Query service to perform a Solr query.
 * Currently only used for da|ra, if other portals follow,
 * we need to think about which fields etc. are queried.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolrQueryService extends QueryService {

	private static final Logger log = LoggerFactory.getLogger(SolrQueryService.class);
	
    public SolrQueryService() {
        super();
    }

    public SolrQueryService(String target) {
        super(target);
    }

    public SolrQueryService(String target,double reliability) {
        super(target,reliability);
    }
    
    /**
     * Constructs a query url for given title, pubDate and doi.
     * 
     * @param title	the query title
     * @param pubDate	the publication date
     * @param doi	the doi
     * @return	a url representing the query
     * @throws MalformedURLException
     */
    //TODO use field resourceType...also in htmlQueryService and all calling methods
    //TODO maxNum param for rows
    public URL constructQueryURL(String title, String pubDate, String doi, int maxNumber, String resourceType) throws MalformedURLException {
    	String beginning = "select/";
        String remainder = "&start=0&rows=" + maxNumber + "&fl=doi,title&wt=json";
        String query = "?q=";
        if (!title.isEmpty()) query += "title:" + title;
        if (!pubDate.isEmpty()) query += " +publicationDate:" + pubDate;
        if (!doi.isEmpty()) query += " +doi:" + doi;
        if (!pubDate.isEmpty()) query += " +resourceType:" + resourceType;
        query = query.replaceAll("= \\+", "");
        return new URL(target + beginning + query + remainder);
    }
    
    //TODO these methods in subclass? depend on schema of repository...
    public URL createQuery(Entity entity) throws MalformedURLException {
    	String title = "";
    	String pubDate = "";
    	String doi = "";
    	if (this.getQueryStrategy().contains(QueryService.QueryField.title)) {
    		title = ClientUtils.escapeQueryChars(entity.getName());
    	}
    	
    	if (this.getQueryStrategy().contains(QueryService.QueryField.publicationDate)) {
    		pubDate = ClientUtils.escapeQueryChars(entity.getNumber());
    	}
    	
    	if (this.getQueryStrategy().contains(QueryService.QueryField.numericInfoInTitle)) {
    		if (!title.isEmpty()) log.debug("Warning: both title and numericInfoInTitle strategies set. Using numericInfoInTitle"); 
    		title = ClientUtils.escapeQueryChars(entity.getName()) + "%20" + ClientUtils.escapeQueryChars(entity.getNumber());
    		/*for (String numInfo : entity.getNumericInfo()) {
    			title += "%20" + numInfo;
    		}*/
    	}
    	
    	if (this.getQueryStrategy().contains(QueryService.QueryField.doi)) {
    		doi = entity.getIdentifier();
    	}
    	//TODO use resourceType field
    	//TODO use maxNumber field
    	return constructQueryURL(title, pubDate, doi, 1000, "");
    }
    
    @Override
    public List<SearchResult> find(Entity entity) {
        //TODO: use solr results and do not parse JSON
        List<SearchResult> results = new ArrayList<>();
        try {

            URL url = createQuery(entity);
            log.debug("Opening stream: " + url);
            InputStream is = url.openStream();
            JsonReader reader = Json.createReader(is);
            JsonObject obj = reader.readObject();
            JsonObject response = obj.getJsonObject("response");
            JsonArray result = response.getJsonArray("docs");
            reader.close();
            is.close();

            int listIndex = 0;
            for (JsonObject item : result.getValuesAs(JsonObject.class)) {
                SearchResult sr = new SearchResult();
                sr.setListIndex(listIndex);
                JsonArray identifier = item.getJsonArray("doi");
                sr.setIdentifier(identifier.getString(0));
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                sr.setDate(dateFormat.format(date));
                JsonArray titles = item.getJsonArray("title");
                for (int i = 0; i < titles.size(); i++) {
                	// remove " at beginning and end of title
                    String title = titles.get(i).toString().substring(1, titles.get(i).toString().length()-1);
                    List<String> numericInfo = InformationExtractor.getNumericInfo(title);
                    sr.addTitle(title);
                    for (String num : numericInfo) sr.addNumericInformation(num);                    
                }              
                
                results.add(sr);
                listIndex++;
            }
        } catch (Exception ex) {
            //TODO: catch exception
        }
        return results;
    }
}
