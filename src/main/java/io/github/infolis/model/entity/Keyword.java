/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.infolis.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.infolis.model.BaseModel;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "Keyword")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Keyword extends BaseModel {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Keyword.class);
    
    private String thesaurusURI;
    private String thesaurusLabel;
    private String referredEntity;
    private double confidenceScore;

    /**
     * @return the thesaurusURI
     */
    public String getThesaurusURI() {
        return thesaurusURI;
    }

    /**
     * @param thesaurusURI the thesaurusURI to set
     */
    public void setThesaurusURI(String thesaurusURI) {
        this.thesaurusURI = thesaurusURI;
    }

    /**
     * @return the thesaurusLabel
     */
    public String getThesaurusLabel() {
        return thesaurusLabel;
    }

    /**
     * @param thesaurusLabel the thesaurusLabel to set
     */
    public void setThesaurusLabel(String thesaurusLabel) {
        this.thesaurusLabel = thesaurusLabel;
    }

    /**
     * @return the referredEntity
     */
    public String getReferredEntity() {
        return referredEntity;
    }

    /**
     * @param referredEntity the referredEntity to set
     */
    public void setReferredEntity(String referredEntity) {
        this.referredEntity = referredEntity;
    }

    /**
     * @return the confidenceScore
     */
    public double getConfidenceScore() {
        return confidenceScore;
    }

    /**
     * @param confidenceScore the confidenceScore to set
     */
    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
}
