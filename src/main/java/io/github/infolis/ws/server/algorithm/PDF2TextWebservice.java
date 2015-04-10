/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.ws.server.algorithm;

import io.github.infolis.model.InFoLiSFile;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

/**
 *
 * @author domi
 */
public class PDF2TextWebservice extends AlgorithmWebservice{
   
    private Map<String, Object> ownParameter = new HashMap();
    
    public PDF2TextWebservice() {
        ownParameter.put("pdfInput", null);
        ownParameter.put("pdfOutput", null);
    }
    
    private String version;
    
    private InFoLiSFile pdfInput;  
    //@ParameterTypeAnnotation(type="output")
    private InFoLiSFile pdfOutput; 

    @Override
    public JSONObject getDescription() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run() {
        pdfInput = (InFoLiSFile)ownParameter.get("pdfInput");
        pdfOutput =(InFoLiSFile)ownParameter.get("pdfOutput");
        System.out.println("test");
        setPdfOutput(new InFoLiSFile());
        pdfOutput.setFileId("out");
        System.out.println(pdfInput);
    }

    /**
     * @return the params
     */
    public Map<String, Object> getParams() {
        return ownParameter;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @param pdfInput the pdfInput to set
     */
    public void setPdfInput(InFoLiSFile pdfInput) {
        this.pdfInput = pdfInput;
    }

    /**
     * @param pdfOutput the pdfOutput to set
     */
    public void setPdfOutput(InFoLiSFile pdfOutput) {
        this.pdfOutput = pdfOutput;
    }

    @Override
    public void setParams(Map<String, Object> params) {        
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
