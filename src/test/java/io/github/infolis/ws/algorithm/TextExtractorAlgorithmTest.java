package io.github.infolis.ws.algorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.file.FileResolveStrategy;
import io.github.infolis.model.file.FileResolver;
import io.github.infolis.model.file.FileResolverFactory;
import io.github.infolis.model.util.SerializationUtils;
import io.github.infolis.ws.client.FrontendClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TextExtractorAlgorithmTest {
	
	Logger log = LoggerFactory.getLogger(TextExtractorAlgorithmTest.class);
		
	@Test
	public void testSimple() throws IOException {
        TextExtractorAlgorithm extractorAlgo = new TextExtractorAlgorithm();
        Execution execution = new Execution();

        extractorAlgo.setExecution(execution);
		FileResolver resolver = FileResolverFactory.create(FileResolveStrategy.LOCAL);
		extractorAlgo.setFileResolver(resolver);
		
        String resPath = "/trivial.pdf";
        Path tempFile = Files.createTempFile("infolis-", ".pdf");
		IOUtils.copy(
        		getClass().getResourceAsStream(resPath),
        		Files.newOutputStream(tempFile));
        byte[] pdfBytes = IOUtils.toByteArray(Files.newInputStream(tempFile));

        InfolisFile inFile = new InfolisFile();
        inFile.setFileName(tempFile.toString());
        inFile.setMd5(SerializationUtils.getHexMd5(pdfBytes)); 
        inFile.setMediaType("application/pdf");
        inFile.setFileStatus("AVAILABLE");
        IOUtils.write(pdfBytes, resolver.openOutputStream(inFile));
        inFile = FrontendClient.post(InfolisFile.class, inFile);

        assertNotNull(inFile.getUri());

        execution.getInputValues().put(TextExtractorAlgorithm.PARAM_PDF_INPUT, inFile.getUri());
        assertEquals(execution.getInputValues().get(TextExtractorAlgorithm.PARAM_PDF_INPUT).size(), 1);
        assertEquals(execution.getInputValues().get(TextExtractorAlgorithm.PARAM_PDF_INPUT).get(0), inFile.getUri());
        execution = FrontendClient.post(Execution.class, execution);
        log.debug("{}", execution.getInputValues().keySet().size());
        
        log.debug("{}", SerializationUtils.toJSON(execution));
        
//        extractorAlgo.validate();
//        extractorAlgo.execute();
        extractorAlgo.run();
	}

}
