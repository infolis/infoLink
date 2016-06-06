package io.github.infolis.infolink.querying;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.github.infolis.model.BaseModel;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.SearchResult;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 *
 * @author domi
 * @author kata
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = Id.CLASS,include = JsonTypeInfo.As.PROPERTY,property = "queryServiceType")
@JsonSubTypes({
    @Type(value = DaraHTMLQueryService.class),
    @Type(value = DaraSolrQueryService.class),
    })
public abstract class QueryService extends BaseModel {

    public QueryService() {
    }

    protected String target = "";
    private double serviceReliability =0.0;
    protected Set<QueryField> queryStrategy;
    protected int maxNumber = 1000;
    
    public QueryService(String target) {
        this.target = target;
    }

    public QueryService(String target, double reliability) {
        this.target = target;
        this.serviceReliability = reliability;
    }
    
    public abstract URL createQuery(Entity entity) throws MalformedURLException;

    public abstract List<SearchResult> find(Entity entity);

    /**
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * @return the serviceReliability
     */
    public double getServiceReliability() {
        return serviceReliability;
    }

    /**
     * @param reliability the serviceReliability to set
     */
    public void setReliability(double serviceReliability) {
        this.serviceReliability = serviceReliability;
    }
    
    /**
     * @return the maxNumber
     */
    public int getMaxNumber() {
        return maxNumber;
    }

    /**
     * @param maxNumber the maxNumber to set
     */
    public void setMaxNumber(int maxNumber) {
        this.maxNumber = maxNumber;
    }
    
    public void setQueryStrategy(Set<QueryField> queryStrategy) {
    	this.queryStrategy = queryStrategy;
    }
    
    public Set<QueryField> getQueryStrategy() {
    	return this.queryStrategy;
    }
    
    /**
     * Fields that can be used in a query. 
     * 
     * title: query title of entity using the title field
     * publicationDate: query numericInfo of entity using the publicationDate field
     * numericInfoInTitle: query title and numericInfo of entity using the title field
     * doi: query doi of entity using the doi field
     */
    public enum QueryField {
    	title, publicationDate, numericInfoInTitle, doi
    }
}
