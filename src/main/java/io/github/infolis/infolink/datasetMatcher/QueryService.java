package io.github.infolis.infolink.datasetMatcher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.github.infolis.model.BaseModel;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.entity.SearchResult;
import java.util.List;

/**
 *
 * @author domi
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

    public QueryService(String target) {
        this.target = target;
    }
    
    public QueryService(String target, double reliability) {
        this.target = target;
        this.reliability = reliability;
    }
    
    public abstract List<SearchResult> executeQuery(SearchQuery solrQuery);

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
}
