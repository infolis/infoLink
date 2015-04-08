/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.ws.backend;

import io.github.infolis.ws.algorithm.Algorithm;
import io.github.infolis.ws.algorithm.PDF2Text;
import io.github.infolis.ws.algorithm.ParameterTypeAnnotation;
import io.github.infolis.ws.execution.Execution;
import io.github.infolis.ws.execution.InFoLiSFile;
import io.github.infolis.ws.execution.InputValue;
import io.github.infolis.ws.execution.OutputValue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author domi
 */
@Path("/backend")
public class Backend {
    
    private static Map<String, Algorithm> algorithms = new HashMap<>();
   // private Map<String, Object> parameter = new HashMap<>();
    
    public Backend() {
        algorithms.put("PDF2Text", new PDF2Text());
    }

//    @Context
//    private UriInfo context;

    /**
     * Creates a new instance of Backend
     */
//    public Backend() {
//    }

    /**
     * Retrieves representation of an instance of
     * de.mannheim.uni.Backend.Backend
     *
     * @return an instance of java.lang.String
     */
//    @GET
//    @Produces("application/json")
//    public String getJson() {
//        //TODO return proper representation object
//        throw new UnsupportedOperationException();
//    }

    /**
     * PUT method for updating or creating an instance of Backend
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
//    @PUT
//    @Consumes("application/json")
//    public void putJson(String content) {
//    }
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String message() {
        return "Yea Backend! ";
    }

//    @POST
//    @Consumes(MediaType.APPLICATION_JSON)
    public static void startExecution(Execution e) throws IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        Class<? extends Algorithm> algo = null;
        for (String s : e.getInput().getValues().keySet()) {
            //get the algorithm that should be used
            if (s.equals("infolis:algorithm")) {
                String algorithmName = e.getInput().getValues().get(s).toString();
                if (Algorithm.algorithms.containsKey(algorithmName)) {
                    algo = Algorithm.algorithms.get(algorithmName);
                }
            }
        }
        //set the input parameters (they are currently static!)
        Field[] f = algo.getDeclaredFields();
        for (String s : e.getInput().getValues().keySet()) {
            Object value = e.getInput().getValues().get(s);
            for (Field fi : f) {
                if (fi.getName().equals(s)) {
                    fi.set(null, value);
                }
            }
        }
        //run method of the algorithm is started
        Method m = algo.getMethod("run", null);
        Object algoInstance = algo.newInstance();
        m.invoke(algoInstance, null);
        System.out.println(algo.getMethod("run", null));

        Field[] output = algo.getDeclaredFields();
        OutputValue o = new OutputValue();
        Map<String, Object> outputMap = new HashMap();
        for (Field fi : f) {
            for (Annotation a : fi.getDeclaredAnnotations()) {
                if (a instanceof ParameterTypeAnnotation) {
                    if (((ParameterTypeAnnotation) a).type().equals("output")) {
                        Object t = fi.get(null);
                        outputMap.put(fi.getName(), t);
                    }
                }
            }
        }
        o.setValues(outputMap);
        e.setOutput(o);
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void startExecution2(Execution e) {
        algorithms.put("PDF2Text", new PDF2Text());
        Algorithm a = null;
        for (String s : e.getInput().getValues().keySet()) {            
            if (s.equals("infolis:algorithm")) {
                a = algorithms.get(e.getInput().getValues().get(s).toString());
                break;
            }
        }
        a.setParams(e.getInput().getValues());
        a.run();
    }
    
    public static void main(String[] args) {
        algorithms.put("PDF2Text", new PDF2Text());
        Execution e = new Execution();
        InputValue i = new InputValue();
        Map<String, Object> entry = new HashMap();
        entry.put("infolis:algorithm", "PDF2Text");
        InFoLiSFile f = new InFoLiSFile();
 //       f.setFile(new File("in.pdf"));
        entry.put("pdfInput", f);
        i.setValues(entry);
        e.setInput(i);
 //       startExecution2(e);        
    }
    

//    public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
//        Execution e = new Execution();
//        InputValue i = new InputValue();
//        Map<String, Object> entry = new HashMap();
//        entry.put("infolis:algorithm", "PDF2Text");
//        InFoLiSFile f = new InFoLiSFile();
//        f.setFile(new File("in.pdf"));
//        entry.put("pdfInput", f);
//        i.setValues(entry);
//        e.setInput(i);
//        Algorithm.initialize();
//        startExecution(e);
//        for (String s : e.getOutput().getValues().keySet()) {
//            System.out.println(s + " --- " + e.getOutput().getValues().get(s));
//        }
//    }

}
