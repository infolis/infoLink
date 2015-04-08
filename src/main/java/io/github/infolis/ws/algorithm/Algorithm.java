/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.ws.algorithm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.json.JSONObject;
import org.reflections.Reflections;

/**
 *
 * @author domi
 */
public abstract class Algorithm implements Runnable {

    public static Map<String, Class<? extends Algorithm>> algorithms;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public abstract JSONObject getDescription();

    @Override
    public abstract void run();

    public static void initialize() {
        algorithms = new HashMap<>();
        Reflections reflections = new Reflections("io.github.infolis.algorithm");
        Set<Class<? extends Algorithm>> subTypes = reflections.getSubTypesOf(Algorithm.class);         
        for(Class<? extends Algorithm> myClass : subTypes) {
            
            algorithms.put(myClass.getSimpleName(), myClass);
        }
    }
    
    public abstract Map<String, Object> getParams();
    
    public abstract void setParams(Map<String, Object> params);
    
}



