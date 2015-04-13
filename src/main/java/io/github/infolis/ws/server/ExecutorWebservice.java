package io.github.infolis.ws.server;

import io.github.infolis.model.Execution;
import io.github.infolis.model.ParameterValues;
import io.github.infolis.ws.algorithm.BaseAlgorithm;
import io.github.infolis.ws.algorithm.PDF2TextAlgorithm;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Web service for executing algorithms.
 *
 * @author kba
 */
@Path("/executor")
public class ExecutorWebservice {
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("debug")
    public String debugMessage() {
        return "Yea Backend! ";
    }
//
////    @POST
////    @Consumes(MediaType.APPLICATION_JSON)
//    public static void startExecution(Execution e) throws IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
//        Class<? extends BaseAlgorithm> algo = null;
//        for (String s : e.getInputValues().keySet()) {
//            //get the algorithm that should be used
//            if (s.equals("algorithm")) {
//                String algorithmName = e.getInputValues().getFirst(s);
//                if (BaseAlgorithm.algorithms.containsKey(algorithmName)) {
//                    algo = BaseAlgorithm.algorithms.get(algorithmName);
//                }
//            }
//        }
//        //set the input parameters (they are currently static!)
//        Field[] f = algo.getDeclaredFields();
//        for (String s : e.getInputValues().getValues().keySet()) {
//            Object value = e.getInputValues().getValues().get(s);
//            for (Field fi : f) {
//                if (fi.getName().equals(s)) {
//                    fi.set(null, value);
//                }
//            }
//        }
//        //run method of the algorithm is started
//        Method m = algo.getMethod("run", null);
//        Object algoInstance = algo.newInstance();
//        m.invoke(algoInstance, null);
//        System.out.println(algo.getMethod("run", null));
//
//        Field[] output = algo.getDeclaredFields();
//        ParameterValues o = new ParameterValues();
//        Map<String, List<String>> outputMap = new HashMap();
//        for (Field fi : f) {
////            for (Annotation a : fi.getDeclaredAnnotations()) {
////                if (a instanceof ParameterTypeAnnotation) {
////                    if (((ParameterTypeAnnotation) a).type().equals("output")) {
////                        Object t = fi.get(null);
////                        outputMap.put(fi.getName(), t);
////                    }
////                }
////            }
//        }
//        o.setValues(outputMap);
//        e.setOutputValues(o);
//    }
//    
//    @POST
//    @Consumes(MediaType.APPLICATION_JSON)
//    public void startExecution2(Execution e) {
//        algorithms.put("PDF2Text", new PDF2TextAlgorithm());
//        BaseAlgorithm a = null;
//        for (String s : e.getInputValues().getValues().keySet()) {            
//            if (s.equals("infolis:algorithm")) {
//                a = algorithms.get(e.getInputValues().getValues().get(s).toString());
//                break;
//            }
//        }
//        a.setParams(e.getInputValues().getValues());
//        a.run();
//    }
    
    @POST
    public Response postExecution(Execution execution) {
    	String algoStr = execution.getAlgorithm();
    	Class<? extends BaseAlgorithm> algoClass = BaseAlgorithm.algorithms.get(algoStr);
    	if (null == algoClass) {
    		return Response.status(404).entity("No such algorithm: " + algoStr).build();
    	}
    	BaseAlgorithm algo;
        try {
			algo = algoClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			return Response.status(500).entity("Error instantiating algorithm " + algoClass.getName()).build();
		}
        algo.setExecution(execution);
        algo.run();
        return Response.accepted(execution).build();
    }
    
    public static void main(String[] args) {
        Execution e = new Execution();
        ParameterValues entry = new ParameterValues();
        entry.put("infolis:algorithm", "PDF2Text");
//        InfolisFile f = new InfolisFile();
 //       f.setFile(new File("in.pdf"));
//        entry.put("pdfInput", f);
        e.setInputValues(entry);
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
