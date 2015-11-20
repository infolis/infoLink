
package io.github.infolis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author domi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchQuery extends BaseModel{
    
    private String query;

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query the query to set
     */
    public void setQuery(String query) {
        this.query = query;
    }
    
}
