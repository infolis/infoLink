package io.github.infolis.infolink.luceneIndexing;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;


public class IndexerTest extends InfolisBaseTest {

	Logger log = LoggerFactory.getLogger(IndexerTest.class);

	@Test
	public void testIndexing() throws Exception {
		Path tempPath = Files.createTempDirectory("infolis-test-");
		FileUtils.forceDeleteOnExit(tempPath.toFile());
		
		List<InfolisFile> inputFiles = createTestFiles(100);

		List<String> uris = new ArrayList<>();
		for (InfolisFile file : inputFiles) {
            uris.add(file.getUri());
		}

		Execution execution = new Execution();
		execution.setAlgorithm(Indexer.class);
		execution.setInputFiles(uris);
		execution.setIndexDirectory(tempPath.toString());
		
		Algorithm algo = new Indexer();
		algo.setExecution(execution);
		algo.setFileResolver(tempFileResolver);
		algo.setDataStoreClient(localClient);
		algo.run();

		log.debug("File 0: {} " , inputFiles.get(0));
		assertEquals("Hallo, please try to find the FOOBAR in this short text snippet. Thank you.", IOUtils.toString(tempFileResolver.openInputStream(inputFiles.get(0))));
		log.debug("File 10: {} " , inputFiles.get(10));
		assertEquals("Hallo, please try to find the term in this short text snippet. Thank you.", IOUtils.toString(tempFileResolver.openInputStream(inputFiles.get(10))));
	}

}
