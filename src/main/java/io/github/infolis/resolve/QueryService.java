package io.github.infolis.resolve;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.github.infolis.model.BaseModel;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.SearchResult;

import java.util.List;
import java.util.Set;

/**
 *
 * @author domi
 * @author kata
 */
@JsonTypeInfo(use = Id.CLASS,include = JsonTypeInfo.As.PROPERTY,property = "type")
@JsonSubTypes({
    @Type(value = HTMLQueryService.class),
    @Type(value = SolrQueryService.class),
    })
public abstract class QueryService extends BaseModel {

    public QueryService() {
    }

    protected String target = "";
    private double reliability =0.0;
    protected Set<QueryField> queryStrategy;

    public QueryService(String target) {
        this.target = target;
    }

    public QueryService(String target, double reliability) {
        this.target = target;
        this.reliability = reliability;
    }

    public abstract List<SearchResult> find(Entity entity);

    /**
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * @return the reliability
     */
    public double getReliability() {
        return reliability;
    }

    /**
     * @param reliability the reliability to set
     */
    public void setReliability(double reliability) {
        this.reliability = reliability;
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
