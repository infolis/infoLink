
package io.github.infolis.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 *
 * @author domi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Publication extends Entity {
    
    private String infolisFile;

    /**
     * @return the infolisFile
     */
    public String getInfolisFile() {
        return infolisFile;
    }

    /**
     * @param infolisFile the infolisFile to set
     */
    public void setInfolisFile(String infolisFile) {
        this.infolisFile = infolisFile;
    }
    
}
