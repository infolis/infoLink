/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author domi
 */
@XmlRootElement
public class InFoLiSFile {
    
    InFoLiSFile(String f ) {
        this.fileId = f;
    }

    public InFoLiSFile() {
    }
    
    
    private String fileId;

    /**
     * @return the file
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * @param fileId the file to set
     */
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    
}
