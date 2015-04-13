/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.ws.server.algorithm;

import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.ParameterValues;
import io.github.infolis.model.util.FileResolver;
import io.github.infolis.ws.client.FrontendClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class PDF2TextWebservice extends AlgorithmWebservice{
	
	private static final Logger log  = LoggerFactory.getLogger(PDF2TextWebservice.class);
   
    static {
    	inputParameterNames.add("pdfInput");
		outputParameterNames.add("pdfOutput");
    }

    private ParameterValues ownParameter = new ParameterValues();
    
    private List<InfolisFile> pdfInputList;
    private List<InfolisFile> pdfOutputList; 

    @Override
    public void run() {
    	for (String inputFileURIString : ownParameter.get("pdfInput")) {
            URI inputFileURI = URI.create(inputFileURIString);
            InfolisFile inputFile = FrontendClient.get(InfolisFile.class, inputFileURI);
            
            // TODO something with inputFile
            String asText = "This is nonsense for testing.";
            
            //
            // Create the file POJO
            InfolisFile outputFile = new InfolisFile();
            outputFile.setFileName("some-meaningful-name");
            outputFile.setMediaType("text/plain");
            outputFile.setFileStatus("AVAILABLE");
            // md5
            MessageDigest digest = null;
			try {
				digest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				/**
				 * This really, really, really cannot happen
				 */
			}
            digest.update(asText.getBytes());
            String md5 = DatatypeConverter.printHexBinary(digest.digest());
            outputFile.setMd5(md5);
            
            log.debug("File to post: " + FrontendClient.toJSON(outputFile));
            
            //
            // Store the file data with md5 as ID
            OutputStream outputStream;
			try {
				outputStream = FileResolver.getOutputStream(md5);
				IOUtils.write(asText, outputStream);
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
            
            //
            // Post to LD API
            outputFile = FrontendClient.post(InfolisFile.class, outputFile);
            pdfOutputList.add(outputFile);
    	}
//        InfolisFile pdfInput = FrontendClient.get(InfolisFile.class, URI.create(ownParameter.getFirst("pdfInput")));
////        pdfOutput = FrontendClient.get(InfolisFile.class, URI.create(ownParameter.getFirst("pdfOutput")));
//        System.out.println("test");
//        System.out.println(pdfInput);
//        setPdfOutput(new InfolisFile());
//        pdfOutput.setFileId("out");
//        System.out.println(pdfInput);
    }


//    /**
//     * @param pdfInput the pdfInput to set
//     */
//    public void setPdfInput(InfolisFile pdfInput) {
//        this.pdfInput = pdfInput;
//    }
//
//    /**
//     * @param pdfOutput the pdfOutput to set
//     */
//    public void setPdfOutput(InfolisFile pdfOutput) {
//        this.pdfOutput = pdfOutput;
//    }

	@Override
	public ParameterValues getParams() {
		return ownParameter;
	}

}
