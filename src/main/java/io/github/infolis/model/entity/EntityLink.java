package io.github.infolis.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.infolis.model.BaseModel;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author domi
 */
@XmlRootElement(name = "link")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityLink extends BaseModel {

    private Entity referenceEntity;
    private Publication mentionsReference;
    private double confidence;
    private String linkReason;

    public EntityLink(Entity referenceEntity, Publication mentionsReference, double confidence, String linkReason) {
        this.referenceEntity = referenceEntity;
        this.mentionsReference = mentionsReference;
        this.confidence = confidence;
        this.linkReason = linkReason;
    }

    public EntityLink() {
    }

    ;
    
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
    public Publication getMentionsReference() {
        return mentionsReference;
    }

    /**
     * @param mentionsReferece the mentionsReferece to set
     */
    public void setMentionsReference(Publication mentionsReferece) {
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

    @Override
    public boolean equals(Object object) {
        EntityLink other = (EntityLink) object;
        if (this.getMentionsReference().getInfolisFile().equals(other.getMentionsReference().getInfolisFile())
                && this.getReferenceEntity().getIdentifier().equals(other.getReferenceEntity().getIdentifier())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(this.referenceEntity);
        hash = 31 * hash + Objects.hashCode(this.mentionsReference);
        return hash;
    }

}
