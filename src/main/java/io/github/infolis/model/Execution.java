package io.github.infolis.model;

import java.util.List;

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
    private Status status;
    private ParameterValues inputValues;
    private ParameterValues outputValues;
    private List<String> log;
    
}
