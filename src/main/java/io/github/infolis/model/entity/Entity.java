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
import io.github.infolis.model.EntityType;
import io.github.infolis.model.TextualReference;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.LoggerFactory;

/**
 * Class for InFoLiS entities, e.g. datasets, cited data and publications.
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
    private List<String> identifiers = new ArrayList<>();
    private String url;
    private EntityType entityType;
    // TODO do not persist
    private Collection<TextualReference> textualReferences;
    @XmlAttribute
    private List<String> numericInfo = new ArrayList<>();
    private Map<String, Double> associations = new HashMap<>();
    private double entityReliability = 1;
    private List<String> alternativeNames = new ArrayList<>();
    private String abstractText;
    private List<String> authors = new ArrayList<>();
    private List<String> subjects = new ArrayList<>();
    private String language;
    private String versionInfo;
    private Set<String> spatial = new HashSet<>();
    
    private String entityView;
    
    // additional bibliographic metadata
    private String journal = null;
    private String series = null;
    private String collection = null;
    private String number = null;
    private String volume = null;
    private String pages = null;
    private List<String> editors = new ArrayList<>();
    private String corporateEditor = null;
    private String publisher = null;
    private String isbn = null;
    private String issn = null;
    private String publicationType = null;//collection article //journal article //working paper //expert report //monograph //review
    private List<String> classification = new ArrayList<>();
    private List<String> methodKeywords = new ArrayList<>();
    private List<String> freeKeywords = new ArrayList<>();
    private String location = null;
    private String licence = null;
    private String dataProvider = null;
    private String publicationStatus = null; //published version //published version; reviewed //

    public Entity(String name) {
        this.name = name;
    }
    
    // copy all attributes except the uri
    public Entity(Entity copyFrom) {
    	this.name = copyFrom.getName();
    	this.identifiers = copyFrom.getIdentifiers();
    	this.url = copyFrom.getURL();
    	this.numericInfo = copyFrom.getNumericInfo();
    	this.entityReliability = copyFrom.getEntityReliability();
    	this.alternativeNames = copyFrom.getAlternativeNames();
    	this.abstractText = copyFrom.getAbstractText();
    	this.authors = copyFrom.getAuthors();
    	this.subjects = copyFrom.getSubjects();
    	this.language = copyFrom.getLanguage();
    	this.versionInfo = copyFrom.getVersionInfo();
    	this.spatial = copyFrom.getSpatial();
    	this.entityType = copyFrom.getEntityType();
    	
    	this.setTags(copyFrom.getTags());
    	
    	this.textualReferences = copyFrom.getTextualReferences();
    	this.associations = copyFrom.getAssociations();
    	
    	this.entityView = copyFrom.getEntityView();
        
    	this.journal = copyFrom.getJournal();
        this.series = copyFrom.getSeries();
        this.collection = copyFrom.getCollection();
        this.number = copyFrom.getNumber();
        this.volume = copyFrom.getVolume();
        this.pages = copyFrom.getPages();
        this.editors = copyFrom.getEditors();
        this.corporateEditor = copyFrom.getCorporateEditor();
        this.publisher = copyFrom.getPublisher();
        this.isbn = copyFrom.getIsbn();
        this.issn = copyFrom.getIssn();
        this.publicationType = copyFrom.getPublicationType();
        this.classification = copyFrom.getClassification();
        this.methodKeywords = copyFrom.getMethodKeywords();
        this.freeKeywords = copyFrom.getFreeKeywords();
        this.location = copyFrom.getLocation();
        this.licence = copyFrom.getLicence();
        this.dataProvider = copyFrom.getDataProvider();
       	this.publicationStatus = copyFrom.getPublicationStatus();
    }

    public Entity() {
    }
      
    public void setEntityType(EntityType entityType) {
    	this.entityType = entityType;
    }
    
    public EntityType getEntityType() {
    	return this.entityType;
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
    public List<String> getIdentifiers() {
        return this.identifiers;
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
    public void setIdentifiers(List<String> identifiers) {
        this.identifiers = identifiers;
    }
    
    public void addIdentifier(String identifier) {
    	if (null == this.identifiers) {
    		this.identifiers = new ArrayList<>();
    	}
        this.identifiers.add(identifier);
    }

    /**
     * Set reliability to 1.0 for manually selected seed instances.
     */
    public void setIsSeed() {
        this.entityReliability = 1.0;
    }

    public void addNumericInfo(String numericInfo) {
        this.getNumericInfo().add(numericInfo);
    }

    public List<String> getNumericInfo() {
        return this.numericInfo;
    }

    public double getEntityReliability() {
        return this.entityReliability;
    }
    
    public void setEntityReliability(double reliability) {
    	this.entityReliability = reliability;
    }

    public boolean isReliable(Collection<InfolisPattern> reliablePatterns, int dataSize, Reliability r, double threshold) throws IOException, ParseException {
        this.entityReliability = r.computeReliability(dataSize, reliablePatterns, this);
        if (this.getEntityReliability() >= threshold) {
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
    
    public void setVersionInfo(String versionInfo) {
    	this.versionInfo = versionInfo;
    }
    
    public String getVersionInfo() {
    	return this.versionInfo;
    }
    
    public void setEntityView(String view) {
    	this.entityView = view;
    }
    
    public String getEntityView() {
    	return this.entityView;
    }

    /**
     * @param numericInfo the numericInfo to set
     */
    public void setNumericInfo(List<String> numericInfo) {
        this.numericInfo = numericInfo;
    }
    
    public void setPublicationType(String publicationType) {
    	this.publicationType = publicationType;
    }
    
    public String getPublicationType() {
    	return this.publicationType;
    }
    
    public void setEditors(List<String> editors) {
    	this.editors = editors;
    }
    
    public List<String> getEditors() {
    	return this.editors;
    }
    
    public void setCorporateEditor(String corporateEditor) {
    	this.corporateEditor = corporateEditor;
    }
    
    public String getCorporateEditor() {
    	return this.corporateEditor;
    }
    
    public void setCollection(String collection) {
    	this.collection = collection;
    }
    
    public String getCollection() {
    	return this.collection;
    }
    
    public void setJournal(String journal) {
    	this.journal = journal;
    }
    
    public String getJournal() {
    	return this.journal;
    }
    
    public void setSeries(String series) {
    	this.series = series;
    }
    
    public String getSeries() {
    	return this.series;
    }
    
    public void setNumber(String number) {
    	this.number = number;
    }
    
    public String getNumber() {
    	return this.number;
    }
    
    public void setVolume(String volume) {
    	this.volume = volume;
    }
    
    public String getVolume() {
    	return this.volume;
    }
    
    public void setPublisher(String publisher) {
    	this.publisher = publisher;
    }
    
    public String getPublisher() {
    	return this.publisher;
    }
    
    public void setLocation(String location) {
    	this.location = location;
    }
    
    public String getLocation() {
    	return this.location;
    }
    
    public void setPages(String pages) {
    	this.pages = pages;
    }
    
    public String getPages() {
    	return this.pages;
    }
    
    public void setIsbn(String isbn) {
    	this.isbn = isbn;
    }
    
    public String getIsbn() {
    	return this.isbn;
    }
    
    public void setIssn(String issn) {
    	this.issn = issn;
    }
    
    public String getIssn() {
    	return this.issn;
    }
    
    public void setClassification(List<String> classification) {
    	this.classification = classification;
    }
    
    public List<String> getClassification() {
    	return this.classification;
    }
    
    public void setMethodKeywords(List<String> methodKeywords) {
    	this.methodKeywords = methodKeywords;
    }
    
    public List<String> getMethodKeywords() {
    	return this.methodKeywords;
    }
    
    public void setFreeKeywords(List<String> freeKeywords) {
    	this.freeKeywords = freeKeywords;
    }
    
    public List<String> getFreeKeywords() {
    	return this.freeKeywords;
    }
    
    public void setLicence(String licence) {
    	this.licence = licence;
    }
    
    public String getLicence() {
    	return this.licence;
    }
    
    public void setDataProvider(String dataProvider) {
    	this.dataProvider = dataProvider;
    }
    
    public String getDataProvider() {
    	return this.dataProvider;
    }
    
    public void setPublicationStatus(String publicationStatus) {
    	this.publicationStatus = publicationStatus;
    }
    
    public String getPublicationStatus() {
    	return this.publicationStatus;
    }
}
