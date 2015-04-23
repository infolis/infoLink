/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 *
 * @author domi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InfolisPattern extends BaseModel {

    private String patternRegex;
    private String luceneQuery;
    private String minimal;
    private Map<String, Double> associations;

    /**
     * @return the patternRegex
     */
    public String getPatternRegex() {
        return patternRegex;
    }

    /**
     * @param patternRegex the patternRegex to set
     */
    public void setPatternRegex(String patternRegex) {
        this.patternRegex = patternRegex;
    }

    /**
     * @return the luceneQuery
     */
    public String getLuceneQuery() {
        return luceneQuery;
    }

    /**
     * @param luceneQuery the luceneQuery to set
     */
    public void setLuceneQuery(String luceneQuery) {
        this.luceneQuery = luceneQuery;
    }

    /**
     * Adds an association between this pattern and a specified instance.
     *
     * @param instance	the instance whose association to store
     * @param score	pmi score for this pattern and instance
     * @return	true, if association is new; false if association was already
     * known
     */
    public boolean addAssociation(String instanceName, double score) {
        if (this.getAssociations().containsKey(instanceName)) {
            System.err.print("Warning: association between pattern " + this.patternRegex + " and instance " + instanceName + " already known! ");
        }
        return (this.getAssociations().put(instanceName, score) == null);
    }

    /**
     * @return the associations
     */
    public Map<String, Double> getAssociations() {
        return associations;
    }

    /**
     * @param associations the associations to set
     */
    public void setAssociations(Map<String, Double> associations) {
        this.associations = associations;
    }

    /**
     * @return the minimal
     */
    public String getMinimal() {
        return minimal;
    }

    /**
     * @param minimal the minimal to set
     */
    public void setMinimal(String minimal) {
        this.minimal = minimal;
    }
}
