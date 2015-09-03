package io.github.infolis.infolink.datasetMatcher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.github.infolis.model.BaseModel;
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

    public QueryService(String target) {
        this.target = target;
    }
    private static final long maxTimeMillis = 75000;
    
    public abstract List<SearchResult> executeQuery(String solrQuery);

    /**
     * @return the target
     */
    public String getTarget() {
        return target;
    }
}
