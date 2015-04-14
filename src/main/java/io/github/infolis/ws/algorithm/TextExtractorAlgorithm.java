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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class TextExtractorAlgorithm extends BaseAlgorithm{
	
	public static final String PARAM_PDF_OUTPUT = "pdfOutput";
	public static final String PARAM_REMOVE_BIBLIOGRAPHY = "removeBibliography";
	public static final String PARAM_PDF_INPUT = "pdfInput";
	
	private static final Logger log  = LoggerFactory.getLogger(TextExtractorAlgorithm.class);
	
    @Override
    public void execute() {
    	for (String inputFileURIString : getExecution().getInputValues().get(PARAM_PDF_INPUT)) {
            URI inputFileURI = URI.create(inputFileURIString);
            InfolisFile inputFile = FrontendClient.get(InfolisFile.class, inputFileURI);
            
            //
            // Create the output file POJO
            //
            InfolisFile outputFile = new InfolisFile();
            outputFile.setFileName("some-meaningful-name");
            outputFile.setMediaType("text/plain");
            outputFile.setFileStatus("AVAILABLE");
            
            log.debug("File to post: " + FrontendClient.toJSON(outputFile));

            //
            // TODO something with inputFile
            //
            String asText = "This is nonsense for testing. InputFile: " + inputFile.getUri();
            // TODO
//            PDF2Text extractor = new PDF2Text(false);
            
            
            //
            // Store the file data with md5 as ID
            //
            OutputStream outputStream;
			try {
                    outputFile.setMd5(FileResolver.getHexMd5(asText));
				outputStream = FileResolver.getOutputStream(outputFile);
				IOUtils.write(asText, outputStream);
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
            
            //
            // Post to LD API
            //
            outputFile = FrontendClient.post(InfolisFile.class, outputFile);
            getExecution().getOutputValues().get(PARAM_PDF_OUTPUT).add(outputFile.getUri());
    	}
    }

	@Override
	public void validate() {
		if (! getExecution().getInputValues().containsKey(PARAM_PDF_INPUT)) {
			throw new IllegalArgumentException("Required parameter '" + PARAM_PDF_INPUT + "' is missing!");
		}
		if (! getExecution().getInputValues().containsKey(PARAM_REMOVE_BIBLIOGRAPHY)) {
            getExecution().getInputValues().put(PARAM_REMOVE_BIBLIOGRAPHY, "false");
		}
		if (! getExecution().getOutputValues().containsKey(PARAM_PDF_OUTPUT)) {
            getExecution().getOutputValues().putEmpty(PARAM_PDF_OUTPUT);
		}
	}


}
