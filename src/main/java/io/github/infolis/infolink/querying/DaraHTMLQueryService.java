
package io.github.infolis.infolink.querying;

import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.util.InformationExtractor;
import io.github.infolis.util.URLParamEncoder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 
 * @author kata
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("io.github.infolis.infolink.querying.DaraHTMLQueryService")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "queryServiceType")
public class DaraHTMLQueryService extends QueryService {

    public DaraHTMLQueryService() {
        super("http://www.da-ra.de/dara/search/search_result", 0.5);
    }
    
    private static final Logger log = LoggerFactory.getLogger(DaraHTMLQueryService.class);
    
    /**
     * Constructs a search URL from the base URL and the query.
     *
     * @param title	the query term
     * @param pubDate the publication date
     * @param doi the doi
     * @param maxNumber	the maximum number of hits to be displayed
     * @param resourceType type of the resource (e.g. dataset of text)
     * @return	the query URL
     * @throws MalformedURLException
     */
    public URL constructQueryURL(String title, String pubDate, String doi, int maxNumber, String resourceType) throws MalformedURLException {
    	String remainder = "&lang=en&mdlang=de&max=" + maxNumber;
        String query = "?q=";
        if (!title.isEmpty()) {
        	try {
        		query += "title:" + URLParamEncoder.encode(title);
        	} catch (UnsupportedEncodingException e) {
        		query += "title:" + title;
        	}
        }
        if (!pubDate.isEmpty()) query += "+publicationDate:" + pubDate;
        if (!doi.isEmpty()) query += "+doi:" + doi;
        if (!resourceType.isEmpty()) query += "+resourceType:" + resourceType;
        query = query.replaceAll("=\\+", "=");
        return new URL(target + query + remainder);
    }

    public URL createQuery(Entity entity) throws MalformedURLException {
    	String title = "";
    	String pubDate = "";
    	String doi = "";
    	if (this.getQueryStrategy().contains(QueryService.QueryField.title)) {
    		title = entity.getName();
    	}
    	
    	if (this.getQueryStrategy().contains(QueryService.QueryField.publicationDate)) {
            if(entity.getNumericInfo()!= null && entity.getNumericInfo().size()>0) {
    		pubDate = entity.getNumericInfo().get(0);
            }    
    	}
    	
    	if (this.getQueryStrategy().contains(QueryService.QueryField.numericInfoInTitle)) {
    		if (!title.isEmpty()) log.debug("Warning: both title and numericInfoInTitle strategies set. Using numericInfoInTitle"); 
            if(entity.getNumericInfo()!= null && entity.getNumericInfo().size()>0) {
                title = entity.getName() + " " + entity.getNumericInfo().get(0);
            }
            else title = entity.getName();
            
    		/*for (String numInfo : entity.getNumericInfo()) {
    			title += " " + numInfo;
    		}*/
    	}
    	
    	if (this.getQueryStrategy().contains(QueryService.QueryField.doi)) {
    		doi = entity.getIdentifier();
    	}
    	// resourceType field in da|ra: "2" denotes dataset
    	return constructQueryURL(title, pubDate, doi, this.getMaxNumber(), "2");
    }

    @Override
    public List<SearchResult> find(Entity entity) {
        try {
            URL url = createQuery(entity);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            //connection.setConnectTimeout(6000);
            System.out.println("Reading from url " + url);
            //make sure that all data is read
            byte[] resultBuff = new byte[0];
            byte[] buff = new byte[1024];
            int k = -1;
            while ((k = connection.getInputStream().read(buff, 0, buff.length)) > -1) {
                byte[] tbuff = new byte[resultBuff.length + k]; // temp buffer size = bytes already read + bytes last read
                System.arraycopy(resultBuff, 0, tbuff, 0, resultBuff.length); // copy previous bytes
                System.arraycopy(buff, 0, tbuff, resultBuff.length, k);  // copy current lot
                resultBuff = tbuff; // call the temp buffer as your result buff
            }
            String htmlPage = new String(resultBuff);
            log.debug("parsing html page content...");
            return parseHTML(htmlPage);

        } catch (MalformedURLException ex) {
            log.error("Execution threw an Exception: {}", ex);
        } catch (IOException ex) {
        	log.error("Execution threw an Exception: {}", ex);
        }
        return null;
    }

    /**
     * Parses the HTML output of the dara search function and returns a map with
     * dataset DOIs (keys) and names (values).
     *
     * @param html	dara HTML output
     * @return	a map containing dataset DOIs (keys) and dataset names (values)
     */
    public List<SearchResult> parseHTML(String html) {
        List<SearchResult> results = new ArrayList<>();
        Document doc = Jsoup.parseBodyFragment(html);
        Elements hitlist = doc.getElementsByTag("li");
        int i = 0;
        for (Element hit : hitlist) {
            String title = "";
            String identifier = "";
            //TODO: search for tag "a" first to limit elements to search by attribute value?
            Elements names = hit.getElementsByAttributeValueMatching("href", "/dara/search/search_show?.*");
            Elements dois = hit.getElementsByAttributeValueContaining("href", "http://dx.doi.org");
            // each entry has exactly one name and one doi element
            //TODO: except for some datasets that are not registered but only referenced in dara!
            // e.g. "OECD Employment Outlook" -> no doi listed here -> ignored
            for (Element name : names) {
                title = name.text().trim();
            }
            for (Element doi : dois) {
                identifier = doi.text().trim();
            }
            if (title.isEmpty() && identifier.isEmpty()) {
                continue;
            }
            //create the search result
            log.debug("Creating search result: title: " + title + "; identifier: " + identifier);
            List<String> numericInfo = InformationExtractor.getNumericInfo(title);
            SearchResult sr = new SearchResult();
            sr.setIdentifier(identifier);
            sr.setTitles(new ArrayList<>(Arrays.asList(title)));
            sr.setNumericInformation(numericInfo);
            sr.setListIndex(i);
            sr.setQueryService(this.getUri());
            sr.setTags(getTags());
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            sr.setDate(dateFormat.format(date));
            i++;
            results.add(sr);
        }
        return results;
    }

}
