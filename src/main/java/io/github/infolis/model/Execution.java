package io.github.infolis.model;

import java.util.ArrayList;
import java.util.List;

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
public class Execution extends BaseModel {

    public enum Status {
        PENDING, STARTED, FINISHED, FAILED
    }
    
    private String algorithm;
    private Status status = Status.PENDING;
    private ParameterValues inputValues = new ParameterValues();
    private ParameterValues outputValues = new ParameterValues();
    private List<String> log = new ArrayList<String>();
    
	public String getAlgorithm() {
		return algorithm;
	}
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public ParameterValues getInputValues() {
		return inputValues;
	}
	public void setInputValues(ParameterValues inputValues) {
		this.inputValues = inputValues;
	}
	public ParameterValues getOutputValues() {
		return outputValues;
	}
	public void setOutputValues(ParameterValues outputValues) {
		this.outputValues = outputValues;
	}
	public List<String> getLog() {
		return log;
	}
	public void setLog(List<String> log) {
		this.log = log;
	}
    
}
