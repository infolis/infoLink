
package io.github.infolis.infolink.querying;

import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.util.InformationExtractor;
import io.github.infolis.util.URLParamEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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

import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author kata
 * @author domi
 *
 */
public class DaraSolrQueryService extends QueryService {

    public DaraSolrQueryService() {
        super("http://www.da-ra.de/solr/dara/", 0.8);
    }
    
    private static final Logger log = LoggerFactory.getLogger(DaraSolrQueryService.class);
    
    /**
     * Constructs a query url for given title, pubDate and doi.
     * 
     * @param title	the query title
     * @param pubDate	the publication date
     * @param doi	the doi
     * @return	a url representing the query
     * @throws MalformedURLException
     */
    public URL constructQueryURL(String title, String pubDate, String doi, int maxNumber, String resourceType) throws MalformedURLException {
    	String beginning = "select/";
        String remainder = "&start=0&rows=" + maxNumber + "&fl=doi,title&wt=json";
        String query = "?q=";
        if (!title.isEmpty()) query += "title:" + title;
        if (!pubDate.isEmpty()) query += "+publicationDate:" + pubDate;
        if (!doi.isEmpty()) query += "+doi:" + doi;
        if (!resourceType.isEmpty()) query += "+resourceType:" + resourceType;
        query = query.replaceAll("=\\+", "");
        return new URL(target + beginning + query + remainder);
    }
    
    public URL createQuery(Entity entity) throws MalformedURLException {
    	String title = "";
    	String pubDate = "";
    	String doi = "";
    	if (this.getQueryStrategy().contains(QueryService.QueryField.title)) {
    		try {
				title = URLParamEncoder.encode("\"" + ClientUtils.escapeQueryChars(entity.getName()) + "\"");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Cannot encode \"" + title + "\"");
			}
    	}
    	
    	if (this.getQueryStrategy().contains(QueryService.QueryField.publicationDate)) {
            if(entity.getNumericInfo()!= null && entity.getNumericInfo().size()>0) {
            	pubDate = ClientUtils.escapeQueryChars(entity.getNumericInfo().get(0));
            }
    	}
    	
    	if (this.getQueryStrategy().contains(QueryService.QueryField.numericInfoInTitle)) {
    		if (!title.isEmpty()) log.debug("Warning: both title and numericInfoInTitle strategies set. Using numericInfoInTitle"); 
            if(entity.getNumericInfo()!= null && entity.getNumericInfo().size()>0) {
            	try {
					title = URLParamEncoder.encode("\"" + ClientUtils.escapeQueryChars(entity.getName()) + " " + ClientUtils.escapeQueryChars(entity.getNumericInfo().get(0)) + "\"");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					throw new IllegalArgumentException("Cannot encode \"" + title + "\"");
				}
            } else
				try {
					title = URLParamEncoder.encode("\"" + ClientUtils.escapeQueryChars(entity.getName()) + "\"");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					throw new IllegalArgumentException("Cannot encode \"" + title + "\"");
				}
            
    		/*for (String numInfo : entity.getNumericInfo()) {
    			title += " " + numInfo;
    		}*/
    	}
    	
    	if (this.getQueryStrategy().contains(QueryService.QueryField.doi)) {
    		doi = entity.getIdentifiers().get(0);
    	}
    	// resourceType field in da|ra: "2" denotes dataset
    	return constructQueryURL(title, pubDate, doi, this.getMaxNumber(), "2");
    }
    
    public List<SearchResult> find(Entity entity) {
        //TODO: use solr results and do not parse JSON
        List<SearchResult> results = new ArrayList<>();

        URL url = null;
        JsonArray result = null;
		try {
			url = new URL(createQuery(entity).toString());
			log.debug("Opening stream: " + url);
			InputStream is = url.openStream();
			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            JsonReader reader = Json.createReader(isr);

            JsonObject obj = reader.readObject();
            JsonObject response = obj.getJsonObject("response");
            result = response.getJsonArray("docs");
            reader.close();
            is.close();
            isr.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read response for \"" + url.toString() + "\"");
		} catch (IOException e) {
			e.printStackTrace();
			return results;
		}
		
        int listIndex = 0;
        for (JsonObject item : result.getValuesAs(JsonObject.class)) {
        	SearchResult sr = new SearchResult();
        	JsonArray identifier = null;
        	sr.setQueryService(this.getUri());
        	sr.setListIndex(listIndex);
        	sr.setTags(getTags());
        	try {
        		identifier = item.getJsonArray("doi");
	            sr.setIdentifier(identifier.getString(0));
        	} catch (NullPointerException npe) {
        		log.warn("search result does not have a doi. Ignoring");
        		//sr.setIdentifier("");
        		break;
            }
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
	        log.debug("Creating search result: titles: " + titles + "; identifier: " + identifier);
	        results.add(sr);
	        listIndex++;
        }
        return results;
    }

}
