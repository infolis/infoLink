package io.github.infolis.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.infolis.model.BaseModel;
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

    private Entity toEntity;
    private Entity fromEntity;
    private double confidence;
    private String linkReason;

    public EntityLink(Entity toEntity, Entity fromEntity, double confidence, String linkReason) {
        this.toEntity = toEntity;
        this.fromEntity = fromEntity;
        this.confidence = confidence;
        this.linkReason = linkReason;
    }

    public EntityLink() {
    }

    ;
    
    /**
     * @return the referenceEntity
     */
    public Entity getToEntity() {
        return toEntity;
    }

    /**
     * @param toEntity the referenceEntity to set
     */
    public void setToEntity(Entity toEntity) {
        this.toEntity = toEntity;
    }

    /**
     * @return the mentionsReferece
     */
    public Entity getFromEntity() {
        return fromEntity;
    }

    /**
     * @param fromEntity the mentionsReferece to set
     */
    public void setFromEntity(Entity fromEntity) {
        this.fromEntity = fromEntity;
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
