/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.ws.algorithm;

import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.util.FileResolver;
import io.github.infolis.ws.client.FrontendClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class PDF2TextAlgorithm extends BaseAlgorithm{
	
	private static final Logger log  = LoggerFactory.getLogger(PDF2TextAlgorithm.class);
   
    @Override
    public void execute() {
    	for (String inputFileURIString : getExecution().getInputValues().get("pdfInput")) {
            URI inputFileURI = URI.create(inputFileURIString);
            InfolisFile inputFile = FrontendClient.get(InfolisFile.class, inputFileURI);
            
            // TODO something with inputFile
            String asText = "This is nonsense for testing. InputFile: " + inputFile.getUri();
            
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
            getExecution().getOutputValues().get("pdfOutput").add(outputFile.getUri());
    	}
    }

	@Override
	public void validate() {
		if (! getExecution().getInputValues().containsKey("pdfInput")) {
			throw new IllegalArgumentException("Required parameter 'pdfInput' is missing!");
		}
		if (! getExecution().getOutputValues().containsKey("pdfOutput")) {
            getExecution().getOutputValues().putEmpty("pdfOutput");
		}
	}


}
