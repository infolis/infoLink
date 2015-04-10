package io.github.infolis.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author domi
 * @author kba
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class Execution {

    public enum Status {
        PENDING, STARTED, FINISHED, FAILED
    }
    
    private String algorithm;
    private Status currentStatus;
    private ParameterValues inputValues;
    private ParameterValues outputValues;
    
}
