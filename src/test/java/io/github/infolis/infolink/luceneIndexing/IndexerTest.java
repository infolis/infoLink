package io.github.infolis.infolink.luceneIndexing;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexerTest extends InfolisBaseTest {

	Logger log = LoggerFactory.getLogger(IndexerTest.class);

	@Test
	public void testIndexing() throws Exception {
		String indexDir = Files.createTempDirectory("infolis-test-").toString();

		List<InfolisFile> inputFiles = createTestFiles();

		List<String> uris = new ArrayList<>();
		for (InfolisFile file : inputFiles) {
            uris.add(file.getUri());
		}

		Execution execution = new Execution();
		execution.setAlgorithm(Indexer.class);
		execution.setInputFiles(uris);
		execution.setIndexDirectory(indexDir);
		
		Algorithm algo = new Indexer();
		algo.setExecution(execution);
		algo.setFileResolver(tempFileResolver);
		algo.setDataStoreClient(localClient);
		algo.run();
		
//		log.debug("File 10: {} " , inputFiles.get(10));
//		log.debug("File 10 Contents: {}", IOUtils.toString(fileResolver.openInputStream(inputFiles.get(10))));
		FileUtils.deleteDirectory(new File(indexDir));
	}

}
