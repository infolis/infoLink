package io.github.infolis.infolink.datasetMatcher;

import io.github.infolis.model.entity.SearchResult;
import java.util.Set;

/**
 *
 * @author domi
 */
public interface QueryService {
    
    public abstract String adaptQuery(String solrQuery);
    
    public abstract Set<SearchResult> executeQuery(String adaptedQuery);      
}
