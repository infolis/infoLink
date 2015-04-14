package io.github.infolis.ws.algorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.util.FileResolver;
import io.github.infolis.ws.client.FrontendClient;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TextExtractorAlgorithmTest {
	
	Logger log = LoggerFactory.getLogger(TextExtractorAlgorithmTest.class);
		
	@Test
	public void testSimple() throws IOException {
        String resPath = "/trivial.pdf";
        InputStream inStream = getClass().getResourceAsStream(resPath);
        assertNotNull(inStream);
        byte[] pdfBytes = IOUtils.toByteArray(inStream);

        InfolisFile inFile = new InfolisFile();
        inFile.setFileName(resPath);
        inFile.setMd5(FileResolver.getHexMd5(pdfBytes)); 
        inFile.setMediaType("application/pdf");
        inFile.setFileStatus("AVAILABLE");
        IOUtils.write(pdfBytes, FileResolver.getOutputStream(inFile));
        inFile = FrontendClient.post(InfolisFile.class, inFile);

        assertNotNull(inFile.getUri());

        Execution execution = new Execution();
        execution.getInputValues().put(TextExtractorAlgorithm.PARAM_PDF_INPUT, inFile.getUri());
        assertEquals(execution.getInputValues().get(TextExtractorAlgorithm.PARAM_PDF_INPUT).size(), 1);
        assertEquals(execution.getInputValues().get(TextExtractorAlgorithm.PARAM_PDF_INPUT).get(0), inFile.getUri());
        execution = FrontendClient.post(Execution.class, execution);
        log.debug("{}", execution.getInputValues().keySet().size());
        
        TextExtractorAlgorithm extractorAlgo = new TextExtractorAlgorithm();
        extractorAlgo.setExecution(execution);

        log.debug("{}", FrontendClient.toJSON(execution));
        
//        extractorAlgo.validate();
//        extractorAlgo.execute();
        extractorAlgo.run();
	}

}
