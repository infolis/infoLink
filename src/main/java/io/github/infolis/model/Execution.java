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
    private List<String> log = new ArrayList<String>();

    // Parameters
    private List<String> paramInputFiles = new ArrayList<String>();
    private List<String> paramPdfOutput = new ArrayList<String>();
    private boolean removeBib = false;

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

    public List<String> getLog() {
        return log;
    }

    public void setLog(List<String> log) {
        this.log = log;
    }

    public List<String> getInputFiles() {
        return paramInputFiles;
    }

    public void setInputFiles(List<String> paramPdfInput) {
        this.paramInputFiles = paramPdfInput;
    }

    public List<String> getParamPdfOutput() {
        return paramPdfOutput;
    }

    public void setParamPdfOutput(List<String> paramPdfOutput) {
        this.paramPdfOutput = paramPdfOutput;
    }

    /**
     * @return the removeBib
     */
    public boolean isRemoveBib() {
        return removeBib;
    }

    /**
     * @param removeBib the removeBib to set
     */
    public void setRemoveBib(boolean removeBib) {
        this.removeBib = removeBib;
    }
}
