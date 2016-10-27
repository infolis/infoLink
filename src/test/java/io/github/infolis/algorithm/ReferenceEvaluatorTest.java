package io.github.infolis.algorithm;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.infolink.annotations.AnnotationHandlerTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisFile;

/**
 * 
 * @author kata
 *
 */
public class ReferenceEvaluatorTest extends InfolisBaseTest {
	
	@Test
	public void test() throws Exception {
		// create annotation test file
		String[] annotations = {AnnotationHandlerTest. getTestAnnotationString()};
		List<InfolisFile> files = createTestTextFiles(2, annotations);
		// post for usage as goldstandard
		List<String> fileUris = dataStoreClient.post(InfolisFile.class, files);
		
		// create textual references for test
		List<TextualReference> textualReferences = new ArrayList<>();
		TextualReference textRef = new TextualReference();
		textRef.setReference("PIAAC");
		textualReferences.add(textRef);
		TextualReference textRef2 = new TextualReference();
		textRef2.setReference("Programme for the International Assessment of Adult Competencies");
		textualReferences.add(textRef2);
		List<String> textRefUris = dataStoreClient.post(TextualReference.class, textualReferences);
		
		// execute
		Execution exec = new Execution();
		exec.setAlgorithm(ReferenceEvaluator.class);
		exec.setInputFiles(fileUris);
		exec.setTextualReferences(textRefUris);
		exec.setTokenize(true);
		exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
	}
	
}