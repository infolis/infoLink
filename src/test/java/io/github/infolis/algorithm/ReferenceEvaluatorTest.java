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
		// create annotation test files
		String[] annotations = {AnnotationHandlerTest. getTestAnnotationString()};
		List<InfolisFile> files = createTestTextFiles(2, annotations);
		// post for usage as goldstandard
		List<String> fileUris = dataStoreClient.post(InfolisFile.class, files);
		// add file names
		InfolisFile f1 = dataStoreClient.get(InfolisFile.class, fileUris.get(0));
		f1.setOriginalName("goldstandard/file1.tsv");
		dataStoreClient.put(InfolisFile.class, f1, f1.getUri());
		InfolisFile f2 = dataStoreClient.get(InfolisFile.class, fileUris.get(1));
		f2.setOriginalName("goldstandard/file2.tsv");
		dataStoreClient.put(InfolisFile.class, f2, f2.getUri());
		
		// create infolisFiles for test
		InfolisFile file1 = new InfolisFile();
		file1.setOriginalName("/foo/bar/file1.txt");
		InfolisFile file2 = new InfolisFile();
		file2.setOriginalName("file2.pdf");
		dataStoreClient.post(InfolisFile.class, file1);
		dataStoreClient.post(InfolisFile.class, file2);
		
		// create textualReferences for test
		List<TextualReference> textualReferences = new ArrayList<>();
		TextualReference textRef = new TextualReference();
		textRef.setReference("PIAAC");
		textRef.setTextFile(file1.getUri());
		textualReferences.add(textRef);
		TextualReference textRef2 = new TextualReference();
		textRef2.setTextFile(file2.getUri());
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