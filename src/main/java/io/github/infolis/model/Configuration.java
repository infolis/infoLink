/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.model;

import io.github.infolis.ws.server.algorithm.AlgorithmWebservice;

import java.util.List;

/**
 *
 * @author domi
 */
public class Configuration {
    
    private List<Parameter> parameter;
    private AlgorithmWebservice algorithm;

    /**
     * @return the parameter
     */
    public List<Parameter> getParameter() {
        return parameter;
    }

    /**
     * @param parameter the parameter to set
     */
    public void setParameter(List<Parameter> parameter) {
        this.parameter = parameter;
    }
    
    public void aaParameter(Parameter parameter) {
        this.parameter.add(parameter);
    }

    /**
     * @return the algorithm
     */
    public AlgorithmWebservice getAlgorithm() {
        return algorithm;
    }

    /**
     * @param algorithm the algorithm to set
     */
    public void setAlgorithm(AlgorithmWebservice algorithm) {
        this.algorithm = algorithm;
    }
}
