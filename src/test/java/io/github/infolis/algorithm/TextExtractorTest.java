package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.SerializationUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextExtractorTest extends InfolisBaseTest {

	Logger log = LoggerFactory.getLogger(TextExtractorTest.class);
	private byte[] pdfBytes;
	Path tempFile;

	@Before
	public void setUp() throws IOException {
		dataStoreClient.clear();
		pdfBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/trivial.pdf"));
		tempFile = Files.createTempFile("infolis-", ".pdf");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUnknownMediaType() throws Exception {
		InfolisFile inFile = new InfolisFile();
		inFile.setFileName(tempFile.toString());
		inFile.setMd5(SerializationUtils.getHexMd5(pdfBytes));
		inFile.setMediaType("invalid/mediaType");
		inFile.setFileStatus("AVAILABLE");
		writeFile(inFile);

		Execution execution = new Execution();
		execution.getInputFiles().add(inFile.getUri());
		execution.setAlgorithm(TextExtractor.class);
		dataStoreClient.post(Execution.class, execution);
		Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
		algo.run();
		assertTrue(StringUtils.join(execution.getLog()).contains("not a PDF"));
	}



	@Test
	public void testLocalFile() throws IOException {

		InfolisFile inFile = new InfolisFile();
		Execution execution = new Execution();

		inFile.setFileName(tempFile.toString());
		inFile.setMd5(SerializationUtils.getHexMd5(pdfBytes));
		inFile.setMediaType("application/pdf");
		inFile.setFileStatus("AVAILABLE");

		writeFile(inFile);

		log.debug(inFile.getFileName());
		log.debug(inFile.getUri());

		assertNotNull(inFile.getUri());

		execution.getInputFiles().add(inFile.getUri());
		execution.setAlgorithm(TextExtractor.class);

		assertEquals(1, execution.getInputFiles().size());
		dataStoreClient.post(Execution.class, execution);
		assertEquals(inFile.getUri(), execution.getInputFiles().get(0));
		Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
		algo.run();

		log.debug("{}", execution.getOutputFiles());
		assertEquals(ExecutionStatus.FINISHED, algo.getExecution().getStatus());
		assertEquals(1, execution.getOutputFiles().size());

		String fileId = algo.getExecution().getOutputFiles().get(0);
		InfolisFile outFile = dataStoreClient.get(InfolisFile.class, fileId);
		InputStream in = fileResolver.openInputStream(outFile);
		String x = IOUtils.toString(in);
		in.close();
		// for (char c : x.toCharArray()) {
		// log.debug("{}", (int)c);
		// }
		assertEquals("Foo. Bar!", x.trim());
		log.debug(SerializationUtils.dumpExecutionLog(execution));
	}

	private void writeFile(InfolisFile inFile) {
		dataStoreClient.post(InfolisFile.class, inFile);
		try {
			OutputStream os = fileResolver.openOutputStream(inFile);
			IOUtils.write(pdfBytes, os);
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
