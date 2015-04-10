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
public class Execution {

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the input
     */
    public InputValues getInput() {
        return input;
    }

    /**
     * @param input the input to set
     */
    public void setInput(InputValues input) {
        this.input = input;
    }

    /**
     * @return the currentStatus
     */
    public Status getCurrentStatus() {
        return currentStatus;
    }

    /**
     * @param currentStatus the currentStatus to set
     */
    public void setCurrentStatus(Status currentStatus) {
        this.currentStatus = currentStatus;
    }

    /**
     * @return the output
     */
    public OutputValues getOutput() {
        return output;
    }

    /**
     * @param output the output to set
     */
    public void setOutput(OutputValues output) {
        this.output = output;
    }
    
    public enum Status {
        STARTED, FINISHED, PENDING, FAILED
    }
    
    private String name; 
    private InputValues input;
    private Status currentStatus;
    private OutputValues output;
    
}
