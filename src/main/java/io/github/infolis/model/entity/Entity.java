package io.github.infolis.model.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.infolis.infolink.patternLearner.Reliability;
import io.github.infolis.model.BaseModel;
import io.github.infolis.model.TextualReference;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.LoggerFactory;

/**
 * Class for all InFoLiS entities, e.g. patterns, datasets, publications.
 *
 * @author kata
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "Entity")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Entity extends BaseModel {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Entity.class);

    @XmlAttribute
    private String name;
    private String identifier;
    private String url;
    private Collection<TextualReference> textualReferences;

    @XmlAttribute
    private List<String> numericInfo = new ArrayList<>();
    private Map<String, Double> associations = new HashMap<>();
    private double reliability;
    private List<String> alternativeNames = new ArrayList<>();
    private String abstractText;
    private List<String> authors = new ArrayList<>();
    private List<String> subjects = new ArrayList<>();
    private String language;
    private Set<String> spatial = new HashSet<>();

    public Entity(String name) {
        this.name = name;
    }

    public Entity() {
    }
    
    public void setSpatial(Set<String> spatial) {
    	this.spatial = spatial;
    }
    
    public void addSpatial(String spatial) {
    	this.spatial.add(spatial);
    }
    
    public Set<String> getSpatial() {
    	return this.spatial;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setTextualReferences(Collection<TextualReference> textualReferences) {
        this.textualReferences = textualReferences;
    }

    public Collection<TextualReference> getTextualReferences() {
        return this.textualReferences;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    public String getURL() {
        return this.url;
    }

    public void setURL(String url) {
        this.url = url;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Set reliability to 1.0 for manually selected seed instances.
     */
    public void setIsSeed() {
        this.reliability = 1.0;
    }

    public void addNumericInfo(String numericInfo) {
        this.getNumericInfo().add(numericInfo);
    }

    public List<String> getNumericInfo() {
        return this.numericInfo;
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

    /**
     * @return the abstractText
     */
    public String getAbstractText() {
        return abstractText;
    }

    /**
     * @param abstractText the abstractText to set
     */
    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    /**
     * @return the authors
     */
    public List<String> getAuthors() {
        return authors;
    }

    /**
     * @param authors the authors to set
     */
    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public void addAuthor(String author) {
        this.authors.add(author);
    }

    /**
     * @return the subjects
     */
    public List<String> getSubjects() {
        return subjects;
    }

    /**
     * @param subjects the subjects to set
     */
    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public void addSubject(String subject) {
        this.subjects.add(subject);
    }

    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @param numericInfo the numericInfo to set
     */
    public void setNumericInfo(List<String> numericInfo) {
        this.numericInfo = numericInfo;
    }
}
