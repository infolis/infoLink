/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.infolink.datasetMatcher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.infolis.algorithm.NumericInformationExtractor;
import io.github.infolis.model.entity.SearchResult;
import java.io.InputStream;
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

/**
 *
 * @author domi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolrQueryService extends QueryService {

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

            int listIndex = 0;
            for (JsonObject item : result.getValuesAs(JsonObject.class)) {
                SearchResult sr = new SearchResult();
                sr.setListIndex(listIndex);
                JsonArray titles = item.getJsonArray("title");
                for (int i = 0; i < titles.size(); i++) {
                    String title = titles.get(i).toString();
                    String num = NumericInformationExtractor.getNumericInfo(title);
                    sr.addTitle(title);
                    sr.addNumericInformation(num);
                    //TODO: better have an unique name like da|ra etc.
                    sr.addTag(target);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = new Date();
                    sr.setDate(dateFormat.format(date));
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
