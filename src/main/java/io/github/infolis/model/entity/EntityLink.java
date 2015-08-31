
package io.github.infolis.model.entity;

/**
 *
 * @author domi
 */
public class EntityLink {
    
    private Entity referenceEntity;
    private Entity mentionsReferece;
    private double confidence;
    private String linkReason;

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
    public Entity getMentionsReferece() {
        return mentionsReferece;
    }

    /**
     * @param mentionsReferece the mentionsReferece to set
     */
    public void setMentionsReferece(Entity mentionsReferece) {
        this.mentionsReferece = mentionsReferece;
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
