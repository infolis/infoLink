/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.ws.server.algorithm;

import io.github.infolis.model.ParameterValues;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

/**
 *
 * @author domi
 */
public abstract class AlgorithmWebservice implements Runnable {
	
    protected static Set<String> inputParameterNames = new HashSet<>();
    protected static Set<String> outputParameterNames = new HashSet<>();

    public static Map<String, Class<? extends AlgorithmWebservice>> algorithms;
    
//    @GET
//    @Produces(MediaType.APPLICATION_JSON)
//    public abstract JSONObject getDescription();

    @Override
    public abstract void run();

    public static void initialize() {
        algorithms = new HashMap<>();
//        Reflections reflections = new Reflections("io.github.infolis.ws.server.algorithm");
        Reflections reflections = new Reflections(AlgorithmWebservice.class.getPackage().toString());
        Set<Class<? extends AlgorithmWebservice>> subTypes = reflections.getSubTypesOf(AlgorithmWebservice.class);         
        for(Class<? extends AlgorithmWebservice> myClass : subTypes) {
            algorithms.put(myClass.getSimpleName(), myClass);
        }
    }
    
    public abstract ParameterValues getParams();
    
    public void setParams(Map<String, List<String>> params) {        
        for(String s : params.keySet()) {
            System.out.println("s: " +s + " value: " + params.get(s));
            if(inputParameterNames.contains(s)) {
                getParams().put(s, params.get(s));
            }
        }
    }

    
}



