/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.ws.execution;

import io.github.infolis.ws.algorithm.Algorithm;

import java.util.List;

/**
 *
 * @author domi
 */
public class Configuration {
    
    private List<Parameter> parameter;
    private Algorithm algorithm;

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
    public Algorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * @param algorithm the algorithm to set
     */
    public void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
    }
}
