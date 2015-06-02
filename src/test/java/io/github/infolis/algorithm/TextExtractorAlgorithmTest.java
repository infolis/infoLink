package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.TempFileResolver;
import io.github.infolis.infolink.luceneIndexing.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.util.SerializationUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextExtractorAlgorithmTest extends InfolisBaseTest {

	Logger log = LoggerFactory.getLogger(TextExtractorAlgorithmTest.class);

	@Test
	public void testLocalFile() throws IOException {

		InfolisFile inFile = new InfolisFile();
		Execution execution = new Execution();

		// Path tmpDir = Files.
		Path tempFile = Files.createTempFile("infolis-", ".pdf");
		String resPath = "/trivial.pdf";

		byte[] pdfBytes = IOUtils.toByteArray(getClass().getResourceAsStream(resPath));
		IOUtils.write(pdfBytes, Files.newOutputStream(tempFile));

		inFile.setFileName(tempFile.toString());
		inFile.setMd5(SerializationUtils.getHexMd5(pdfBytes));
		inFile.setMediaType("application/pdf");
		inFile.setFileStatus("AVAILABLE");

		try {
			OutputStream os = fileResolver.openOutputStream(inFile);
			IOUtils.write(pdfBytes, os);
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		dataStoreClient.post(InfolisFile.class, inFile);

		log.debug(inFile.getFileName());
		log.debug(inFile.getUri());

		assertNotNull(inFile.getUri());

		execution.getInputFiles().add(inFile.getUri());
		execution.setAlgorithm(TextExtractorAlgorithm.class);

		assertEquals(1, execution.getInputFiles().size());
		dataStoreClient.post(Execution.class, execution);
		assertEquals(inFile.getUri(), execution.getInputFiles().get(0));
		Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, fileResolver);
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
	}

}
