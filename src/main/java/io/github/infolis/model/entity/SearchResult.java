
package io.github.infolis.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author domi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResult extends Entity {
    
    private int listIndex;
    private double relevanceScore;
    private List<String> titles = new ArrayList<>();
    private List<String> numericInformation = new ArrayList<>();
    private String date;

    /**
     * @return the listIndex
     */
    public int getListIndex() {
        return listIndex;
    }

    /**
     * @param listIndex the listIndex to set
     */
    public void setListIndex(int listIndex) {
        this.listIndex = listIndex;
    }

    /**
     * @return the relevanceScore
     */
    public double getRelevanceScore() {
        return relevanceScore;
    }

    /**
     * @param relevanceScore the relevanceScore to set
     */
    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    /**
     * @return the titles
     */
    public List<String> getTitles() {
        return titles;
    }

    /**
     * @param titles the titles to set
     */
    public void setTitles(List<String> titles) {
        this.titles = titles;
    }

    /**
     * @param title the titles to set
     */
    public void addTitle(String title) {
        this.titles.add(title);
    }
    
    /**
     * @param numericInfo the titles to set
     */
    public void addNumericInformation(String numericInfo) {
        this.getNumericInformation().add(numericInfo);
    }

    /**
     * @return the numericInformation
     */
    public List<String> getNumericInformation() {
        return numericInformation;
    }

    /**
     * @param numericInformation the numericInformation to set
     */
    public void setNumericInformation(List<String> numericInformation) {
        this.numericInformation = numericInformation;
    }

    /**
     * @return the date
     */
    public String getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(String date) {
        this.date = date;
    }
    
}
