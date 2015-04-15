package io.github.infolis.ws.algorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.file.FileResolver;
import io.github.infolis.model.file.FileResolverFactory;
import io.github.infolis.model.file.FileResolverStrategy;
import io.github.infolis.model.util.SerializationUtils;
import io.github.infolis.ws.client.FrontendClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TextExtractorAlgorithmTest {
	
	Logger log = LoggerFactory.getLogger(TextExtractorAlgorithmTest.class);
		
	@Test
	public void testLocalFile() throws IOException {
        TextExtractorAlgorithm algo = new TextExtractorAlgorithm();
		FileResolver resolver = FileResolverFactory.create(FileResolverStrategy.LOCAL);
        InfolisFile inFile = new InfolisFile();
        Execution execution = new Execution();
        Path tempFile = Files.createTempFile("infolis-", ".pdf");
        String resPath = "/trivial.pdf";
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getResourceAsStream(resPath));
        IOUtils.write(pdfBytes, Files.newOutputStream(tempFile));

        algo.setExecution(execution);
		algo.setFileResolver(resolver);
		
        inFile.setFileName(tempFile.toString());
        inFile.setMd5(SerializationUtils.getHexMd5(pdfBytes)); 
        inFile.setMediaType("application/pdf");
        inFile.setFileStatus("AVAILABLE");
        FrontendClient.post(InfolisFile.class, inFile);

        assertNotNull(inFile.getUri());

        execution.getInputFiles().add(inFile.getUri());

        assertEquals(1, execution.getInputFiles().size());
        assertEquals(inFile.getUri(), execution.getInputFiles().get(0));

        FrontendClient.post(Execution.class, execution);
       
        algo.run();

        log.debug("{}", execution.getOutputFiles());
        assertEquals(Execution.Status.FINISHED, algo.getExecution().getStatus());
        assertEquals(1, execution.getOutputFiles().size());
	}

}
