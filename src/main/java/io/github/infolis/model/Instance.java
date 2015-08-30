/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.model;

import io.github.infolis.infolink.patternLearner.Reliability;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.lucene.queryParser.ParseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
    
    @XmlAttribute
    private String number;

    public Instance(String name) {
    	super(name);
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
    
    public boolean isReliable(Set<StudyContext> contexts_patterns, int dataSize, Set<String> reliablePatterns, Map<String, Set<StudyContext>> contexts_seeds, Reliability r, double threshold) throws IOException, ParseException {
    	double instanceReliability = r.computeReliability(contexts_patterns, dataSize, reliablePatterns, contexts_seeds, this);
        if (instanceReliability >= threshold) {
            return true;
        } else {
            return false;
        }
    }
    
}
