package io.github.infolis.resolve;

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

/**
 *
 * @author kata
 * @author domi
 *
 * Constructs a query to find an entity using an HTML search interface, 
 * then extracts and returns the search results from the HTML response.
 */
public class HTMLQueryService extends QueryService {

    private static final Logger log = LoggerFactory.getLogger(HTMLQueryService.class);

    private int maxNumber = 1000;

    public HTMLQueryService() {
        super();
    }

    public HTMLQueryService(String target) {
        super(target);
    }

    public HTMLQueryService(String target, double reliability) {
        super(target, reliability);
    }
    
    /**
     * Constructs a search URL from the base URL and the query.
     *
     * @param title	the query term
     * @param maxNumber	the maximum number of hits to be displayed
     * @return	the query URL
     * @throws MalformedURLException
     */
    public URL constructQueryURL(String title, String pubDate, String doi, int maxNumber) throws MalformedURLException {
        try {
        	String query = String.format("%s?title=%s&publicationDate=%s&doi=%s&max=%s&lang=de",
                    this.target,
                    // URLEncoder transforms plain text into the application/x-www-form-urlencoded MIME format
                    // as described in the HTML specification (GET-style URLs or POST forms)
                    // does not work with the new dara search function
                    //URLEncoder.encode(searchTerm, "UTF-8"),
                    URLParamEncoder.encode(title),
                    pubDate,
                    doi,
                    String.valueOf(maxNumber));
        	// overlapping matches possible for all optional parameters
        	for (int i = 0; i<2; i++) query = query.replaceAll("&.*?=&", "&");
        	if (title.isEmpty()) query = query.replaceAll("title=&", "");
        	return new URL(query);
        } catch (UnsupportedEncodingException e) {
            log.debug(e.getMessage());
            String query = String.format("%s?title=%s&publicationDate=%s&doi=%s&max=%s&lang=de", 
            		this.target, title, pubDate, doi, String.valueOf(maxNumber));
            query = query.replaceAll("&.*?=&", "&");
            return new URL(query);
        }
    }

    public URL createQuery(Entity entity) throws MalformedURLException {
    	String title = "";
    	String pubDate = "";
    	String doi = "";
    	if (this.getQueryStrategy().contains(QueryService.QueryField.title)) {
    		title = entity.getName();
    	}
    	
    	if (this.getQueryStrategy().contains(QueryService.QueryField.publicationDate)) {
    		pubDate = entity.getNumber();
    	}
    	
    	if (this.getQueryStrategy().contains(QueryService.QueryField.numericInfoInTitle)) {
    		if (!title.isEmpty()) log.debug("Warning: both title and numericInfoInTitle strategies set. Using numericInfoInTitle");
    		title = entity.getName() + " " + entity.getNumber();
    		/*
    		title = entity.getName();
    		for (String numInfo : entity.getNumericInfo()) {
    			title += " " + numInfo;
    		}*/
    	}
    	
    	if (this.getQueryStrategy().contains(QueryService.QueryField.doi)) {
    		doi = entity.getIdentifier();
    	}
    	return constructQueryURL(title, pubDate, doi, this.maxNumber);
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
            Elements names = hit.getElementsByAttributeValueMatching("href", "/dara/study/web_show?.*");
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
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            sr.setDate(dateFormat.format(date));
            i++;
            results.add(sr);
        }
        return results;
    }

    /**
     * @return the maxNumber
     */
    public int getMaxNumber() {
        return maxNumber;
    }

    /**
     * @param maxNumber the maxNumber to set
     */
    public void setMaxNumber(int maxNumber) {
        this.maxNumber = maxNumber;
    }
}
