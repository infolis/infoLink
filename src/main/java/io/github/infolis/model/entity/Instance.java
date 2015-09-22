/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.model.entity;

import io.github.infolis.infolink.patternLearner.Reliability;

import java.io.IOException;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.lucene.queryParser.ParseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * Class for Instances (= terms recognized as candidates for dataset titles). 
 * 
 * @author kata
 * @author domi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "study")
@XmlAccessorType(XmlAccessType.FIELD)
public class Instance extends Entity {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Instance.class);
    @XmlAttribute
    private String number;
    private Map<String, Double> associations = new HashMap<>();
    private double reliability;
    private List<String> alternativeNames = new ArrayList<>();
    
    public Instance() {
    	super();
    }
    
    public Instance(String name) {
    	super(name);
    }
    
    /**
     * Set reliability to 1.0 for manually selected seed instances.
     */
    public void setIsSeed() {
    	this.reliability = 1.0;
    }
    
    /**
     * @return the number
     */
    public String getNumber() {
        return number;
    }

    /**
     * @param number the number (year, number, wave, ...) to set
     */
    public void setNumber(String number) {
        this.number = number;
    }
    
    public double getReliability() {
    	return this.reliability;
    }
    
    public boolean isReliable(Collection<InfolisPattern> reliablePatterns, int dataSize, Reliability r, double threshold) throws IOException, ParseException {
    	this.reliability = r.computeReliability(dataSize, reliablePatterns, this);
        if (this.getReliability() >= threshold) {
            return true;
        } else {
            return false;
        }
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
    
        public boolean addAssociation(String entityName, double score) {
        if (this.getAssociations().containsKey(entityName)) {
            log.debug("association between entity " + this.getName()
                    + " and entity " + entityName
                    + " already known, overwriting previously saved score.");
        }
        return (this.getAssociations().put(entityName, score) == null);
    }

    /**
     * @return the alternativeNames
     */
    public List<String> getAlternativeNames() {
        return alternativeNames;
    }

    /**
     * @param alternativeNames the alternativeNames to set
     */
    public void setAlternativeNames(List<String> alternativeNames) {
        this.alternativeNames = alternativeNames;
    }
    
    /**
     * @param alternativeNames the alternativeName to add
     */
    public void addAlternativeNames(String alternativeName) {
        this.alternativeNames.add(alternativeName);
    }
    
}
