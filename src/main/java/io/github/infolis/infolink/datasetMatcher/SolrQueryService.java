/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.infolink.datasetMatcher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import static io.github.infolis.algorithm.MetaDataExtractor.complexNumericInfoRegex;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.util.LimitedTimeMatcher;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 *
 * @author domi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolrQueryService extends QueryService {

    private String target;

    public SolrQueryService() {
        super();
    }

    public SolrQueryService(String target) {
        super(target);
    }

    public String adaptQuery(String solrQuery) {
        String beginning = "select/";
        String remainder = "&start=0&rows=10000&fl=doi,title&wt=json";
        return "" + target + beginning + solrQuery + remainder;
    }

    @Override
    public List<SearchResult> executeQuery(String solrQuery) {
        List<SearchResult> results = new ArrayList<>();
        try {
            URL url = new URL(adaptQuery(solrQuery));
            InputStream is = url.openStream();
            JsonReader reader = Json.createReader(is);
            JsonObject obj = reader.readObject();
            JsonObject response = obj.getJsonObject("response");
            JsonArray result = response.getJsonArray("docs");
            reader.close();
            is.close();
            
            int listIndex =0;
            for (JsonObject item : result.getValuesAs(JsonObject.class)) {
                SearchResult sr = new SearchResult();
                sr.setListIndex(listIndex);
                JsonArray titles = item.getJsonArray("title");
                for (int i = 0; i < titles.size(); i++) {
                    String title = titles.get(i).toString();
                    String num = getNumericInfo(title);
                    sr.addTitle(title);
                    sr.addNumericInformation(num);
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
