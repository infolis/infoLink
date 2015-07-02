package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IndexerTest extends InfolisBaseTest {

	Logger log = LoggerFactory.getLogger(IndexerTest.class);

	@Test
	public void testIndexing() throws Exception {
		Path tempPath = Files.createTempDirectory("infolis-test-");
		FileUtils.forceDeleteOnExit(tempPath.toFile());
		String[] testStrings = {
				"Hallo, please try to find the FOOBAR in this short text snippet. Thank you.",
				"Hallo, please try to find the R2 in this short text snippet. Thank you.",
				"Hallo, please try to find the D2 in this short text snippet. Thank you.",
				"Hallo, please try to find the term in this short text snippet. Thank you.",
				"Hallo, please try to find the _ in this short text snippet. Thank you.",
				"Hallo, please try to find .the term. in this short text snippet. Thank you.",
				"Hallo, please try to find the FOOBAR in this short text snippet. Thank you."
		};
		List<InfolisFile> inputFiles = createTestFiles(100, testStrings);

		List<String> uris = new ArrayList<>();
		for (InfolisFile file : inputFiles) {
            uris.add(file.getUri());
		}

		Execution execution = new Execution();
		execution.setAlgorithm(Indexer.class);
		execution.setInputFiles(uris);
		execution.setIndexDirectory(tempPath.toString());
		Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, fileResolver);

		algo.run();

		log.debug("File 0: {} " , inputFiles.get(0));
		InputStream in0 = fileResolver.openInputStream(inputFiles.get(0));
		assertEquals("Hallo, please try to find the FOOBAR in this short text snippet. Thank you.", IOUtils.toString(in0));
		in0.close();
		log.debug("File 10: {} " , inputFiles.get(10));
		InputStream in10 = fileResolver.openInputStream(inputFiles.get(10));
		assertEquals("Hallo, please try to find the term in this short text snippet. Thank you.", IOUtils.toString(in10));
		in10.close();
	}

}
