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

    private String toEntity;
    private String fromEntity;
    private double confidence;
    private String linkReason;
    private String relation;

    public EntityLink() {
    }

	public EntityLink(String fromEntity, String toEntity, double confidence, String linkReason)
	{
		this.fromEntity = fromEntity;
		this.toEntity = toEntity;
		this.confidence = confidence;
		this.linkReason = linkReason;
	}

	public String getToEntity()
	{
		return toEntity;
	}

	public void setToEntity(String toEntity)
	{
		this.toEntity = toEntity;
	}

	public String getFromEntity()
	{
		return fromEntity;
	}

	public void setFromEntity(String fromEntity)
	{
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

    /**
     * @return the relation
     */
    public String getRelation() {
        return relation;
    }

    /**
     * @param relation the relation to set
     */
    public void setRelation(String relation) {
        this.relation = relation;
    }
}
