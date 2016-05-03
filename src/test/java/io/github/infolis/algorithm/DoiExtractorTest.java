package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisFile;

/**
 * 
 * @author kata
 *
 */
public class DoiExtractorTest extends InfolisBaseTest {
	
	List<String> testFileUris = new ArrayList<>();
	String[] testStrings = { "Das Beziehungs- und Familienpanel (pairfam) hat die DOI 10.4232/pairfam.5678.5.0.0.", 
			"please refer to da|ra (http://www.da-ra.de/) for further information."
	};
	
	public DoiExtractorTest() throws Exception {
		for (InfolisFile file : createTestTextFiles(2, testStrings)) testFileUris.add(file.getUri());
	}
	
	@Test
	public void testExecute() {
		Execution exec = new Execution(DoiExtractor.class);
		exec.setInputFiles(testFileUris);
		exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		for (TextualReference textRef : dataStoreClient.get(TextualReference.class, exec.getTextualReferences())) {
			assertEquals("10.4232/pairfam.5678.5.0.0", textRef.getReference());
		}
	}
}