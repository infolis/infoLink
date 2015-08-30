package io.github.infolis.model;


import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class for all InFoLiS entities, e.g. patterns, datasets, publications.
 * 
 * @author kata
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "Entity")
@XmlAccessorType(XmlAccessType.FIELD)
public class Entity extends BaseModel {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Entity.class);
	@XmlAttribute
	String name; 
	
	public Entity(String name) {
		this.name = name;
	}
	
	public Entity() {
		
	}
	
	Map<String, Double> associations = new HashMap<>();
	double reliability;
    double threshold;
    
    public void setName(String name) {
    	this.name = name;
    }
    
    public String getName() {
    	return this.name;
    }
    
    /**
     * @param associations the associations to set
     */
    public void setAssociations(Map<String, Double> associations) {
        this.associations = associations;
    }
    
    /**
     * @return the associations
     */
    public Map<String, Double> getAssociations() {
        return this.associations;
    }
	
	public boolean addAssociation(String entityName, double score) {
		if (this.getAssociations().containsKey(entityName)) {
            log.debug("association between entity " + this.name + 
            		" and entity " + entityName + 
            		" already known, overwriting previously saved score.");
        }
        return (this.getAssociations().put(entityName, score) == null);
	}
	
	/**
     * 
     * @param threshold threshold for accepting this pattern
     */
    public void setThreshold(double threshold) {
    	this.threshold = threshold;
    }
    
    /**
     * 
     * @param threshold threshold for accepting this pattern
     */
    public double getThreshold() {
    	return this.threshold;
    }
    
    public double getReliability() {
    	return this.reliability;
    }
    
    public void setReliability(double reliability) {
    	this.reliability = reliability;
    }
    
    
}