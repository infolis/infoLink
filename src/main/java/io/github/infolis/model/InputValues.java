/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.model;

import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author domi
 */
@XmlRootElement
public class InputValues {
    
    private Map<String, Object> values;

    /**
     * @return the values
     */
    public Map<String, Object> getValues() {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(Map<String, Object> values) {
        this.values = values;
    }
    
}
