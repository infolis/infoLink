
package io.github.infolis.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 *
 * @author domi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityLink {
    
    private Entity referenceEntity;
    private Entity mentionsReference;
    private double confidence;
    private String linkReason;

    public EntityLink(Entity referenceEntity, Entity mentionsReference, double confidence, String linkReason) {
        this.referenceEntity = referenceEntity;
        this.mentionsReference = mentionsReference;
        this.confidence = confidence;
        this.linkReason = linkReason;
    }
    
    /**
     * @return the referenceEntity
     */
    public Entity getReferenceEntity() {
        return referenceEntity;
    }

    /**
     * @param referenceEntity the referenceEntity to set
     */
    public void setReferenceEntity(Entity referenceEntity) {
        this.referenceEntity = referenceEntity;
    }

    /**
     * @return the mentionsReferece
     */
    public Entity getMentionsReference() {
        return mentionsReference;
    }

    /**
     * @param mentionsReferece the mentionsReferece to set
     */
    public void setMentionsReference(Entity mentionsReferece) {
        this.mentionsReference = mentionsReferece;
    }

    /**
     * @return the confidence
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * @param confidence the confidence to set
     */
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    /**
     * @return the linkReason
     */
    public String getLinkReason() {
        return linkReason;
    }

    /**
     * @param linkReason the linkReason to set
     */
    public void setLinkReason(String linkReason) {
        this.linkReason = linkReason;
    }
    
    
}
