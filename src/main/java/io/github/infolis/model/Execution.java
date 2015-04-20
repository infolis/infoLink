package io.github.infolis.model;

import io.github.infolis.algorithm.Algorithm;

import java.util.ArrayList;
import java.util.Date;
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

	private Class<? extends Algorithm> algorithm;
	private ExecutionStatus status = ExecutionStatus.PENDING;
	private List<String> log = new ArrayList<String>();
	private Date startTime;
	private Date endTime;
	
	// Parameters
	private List<String> inputFiles = new ArrayList<String>();
	private List<String> outputFiles  = new ArrayList<String>();
    private boolean removeBib = false;
    private String outputDirectory = "";
	

	public ExecutionStatus getStatus() {
		return status;
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status;
	}

	public List<String> getLog() {
		return log;
	}

	public void setLog(List<String> log) {
		this.log = log;
	}

	public List<String> getInputFiles() {
		return inputFiles;
	}

	public void setInputFiles(List<String> paramPdfInput) {
		this.inputFiles = paramPdfInput;
	}

	public List<String> getOutputFiles() {
		return outputFiles;
	}

	public void setOutputFiles(List<String> paramPdfOutput) {
		this.outputFiles = paramPdfOutput;
	}

	public boolean isRemoveBib() {
		return removeBib;
	}

	public void setRemoveBib(boolean removeBib) {
		this.removeBib = removeBib;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public Class<? extends Algorithm> getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(Class<? extends Algorithm> algorithm) {
		this.algorithm = algorithm;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

}
