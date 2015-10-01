
package io.github.infolis.model;

import io.github.infolis.model.TextualReference.ReferenceType;

/**
 *
 * @author domi
 */
public class SearchQuery extends BaseModel{
    
    private String query;
    private ReferenceType referenceType;

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

    /**
     * @return the referenceType
     */
    public ReferenceType getReferenceType() {
        return referenceType;
    }

    /**
     * @param referenceType the referenceType to set
     */
    public void setReferenceType(ReferenceType referenceType) {
        this.referenceType = referenceType;
    }
    
}
