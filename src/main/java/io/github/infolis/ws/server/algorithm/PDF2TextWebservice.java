/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.ws.server.algorithm;

import io.github.infolis.model.InfolisFile;
import io.github.infolis.ws.client.FrontendClient;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author domi
 */
public class PDF2TextWebservice extends AlgorithmWebservice{
   
    private Map<String, String> ownParameter = new HashMap<>();
    
    public PDF2TextWebservice() {
        ownParameter.put("pdfInput", null);
        ownParameter.put("pdfOutput", null);
    }
    
    private InfolisFile pdfInput;  
    //@ParameterTypeAnnotation(type="output")
    private InfolisFile pdfOutput; 

    @Override
    public void run() {
        pdfInput = FrontendClient.get(InfolisFile.class, URI.create(ownParameter.get("pdfInput")));
        pdfOutput = FrontendClient.get(InfolisFile.class, URI.create(ownParameter.get("pdfOutput")));
        System.out.println("test");
        setPdfOutput(new InfolisFile());
//        pdfOutput.setFileId("out");
//        System.out.println(pdfInput);
    }

//    /**
//     * @return the params
//     */
//    public Map<String, Object> getParams() {
//        return ownParameter;
//    }


    /**
     * @param pdfInput the pdfInput to set
     */
    public void setPdfInput(InfolisFile pdfInput) {
        this.pdfInput = pdfInput;
    }

    /**
     * @param pdfOutput the pdfOutput to set
     */
    public void setPdfOutput(InfolisFile pdfOutput) {
        this.pdfOutput = pdfOutput;
    }

    @Override
    public void setParams(Map<String, Array<String>> params) {        
        for(String s : params.keySet()) {
            System.out.println("s: " +s + " value: " + params.get(s));
            if(s.equals("infolis:algorithm")) {
                continue;
            }
            if(ownParameter.containsKey(s)) {
                ownParameter.put(s, params.get(s));
            }
        }
    }
}
