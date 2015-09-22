package io.github.infolis.model.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.infolis.model.BaseModel;
import io.github.infolis.model.TextualReference;

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

    //TODO: list of names instead one? 
    @XmlAttribute
    private String name;
    private String identifier;
    private List<String> tags;  
  //TODO use uris instead of TextualReference objects
    //private Collection<String> textualReferences;
    private Collection<TextualReference> textualReferences;
    
    //TODO: use entity identifier types
    public enum EntityIdentifierType { DOI, URL, STRING; }

    public Entity(String name) {
        this.name = name;
    }

    public Entity() {}

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
    //TODO use uris instead of TextualReference objects
    /*
    public void setTextualReferences(Collection<String> uris) {
    	this.textualReferences = uris;
    }
    
    public Collection<String> getTextualReferences() {
    	return this.textualReferences;
    }*/
    
    public void setTextualReferences(Collection<TextualReference> textualReferences) {
    	this.textualReferences = textualReferences;
    }
    
    public Collection<TextualReference> getTextualReferences() {
    	return this.textualReferences;
    }

    /**
     * @return the tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * @param tags the tags to set
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * @param tag the tag to add
     */
    public void addTag(String tag) {
        if (tags == null) {
            tags = new ArrayList<>();
        }
        tags.add(tag);
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    
}
