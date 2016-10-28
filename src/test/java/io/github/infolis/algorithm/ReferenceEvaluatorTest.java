package io.github.infolis.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.infolink.annotations.Annotation;
import io.github.infolis.infolink.annotations.AnnotationHandler;
import io.github.infolis.infolink.annotations.AnnotationHandlerTest;
import io.github.infolis.infolink.annotations.WebAnno3TsvHandler;
import io.github.infolis.infolink.annotations.WebAnnoTsvHandler;
import io.github.infolis.infolink.evaluation.Agreement;
import io.github.infolis.infolink.annotations.Annotation.Metadata;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisFile;

/**
 * 
 * @author kata
 *
 */
public class ReferenceEvaluatorTest extends InfolisBaseTest {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ReferenceEvaluatorTest.class);
	
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
		textRef.setReference("PIAA");
		textRef.setTextFile(file1.getUri());
		textualReferences.add(textRef);
		TextualReference textRef2 = new TextualReference();
		textRef2.setTextFile(file2.getUri());
		textRef2.setReference("Programme for the International Assessment of Adult Competencies which was");
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
	
	@Test
	public void testCompareWebAnno2() {
		AnnotationHandler h = new WebAnnoTsvHandler();
		List<Annotation> annotations = h.parse(AnnotationHandlerTest.getTestAnnotationStringOldFormat());
		List<TextualReference> textualReferences = new ArrayList<>();
		TextualReference textRef = new TextualReference();
		textRef.setReference("Gesundheitssurvey");
		textualReferences.add(textRef);
		TextualReference textRef2 = new TextualReference();
		textRef2.setReference("Koch Institutes");
		textualReferences.add(textRef2);
		Set<Metadata> relevantFields = new HashSet<>();
		relevantFields.addAll(Arrays.asList(
				Metadata.title_b, 
				Metadata.creator, Metadata.creator_b, Metadata.creator_i));
		Agreement agreement = ReferenceEvaluator.compare(textualReferences, AnnotationHandler.mergeNgrams(annotations), relevantFields);
		agreement.logStats();
	}
	
	@Test
	public void testCompareWebAnno3() {
		AnnotationHandler h = new WebAnno3TsvHandler();
		List<Annotation> annotations = h.parse(AnnotationHandlerTest.getTestAnnotationString());
		//for (Annotation anno : annotations) log.debug(anno.toString());
		List<TextualReference> textualReferences = new ArrayList<>();
		TextualReference textRef = new TextualReference();
		textRef.setReference("PIAAC");
		textualReferences.add(textRef);
		TextualReference textRef2 = new TextualReference();
		textRef2.setReference("Programme for the International Assessment of Adult Competencies");
		textualReferences.add(textRef2);
		Set<Metadata> relevantFields = new HashSet<>();
		relevantFields.addAll(Arrays.asList(
				Metadata.title_b));
		Agreement agreement = ReferenceEvaluator.compare(textualReferences, AnnotationHandler.mergeNgrams(annotations), relevantFields);
		agreement.logStats();
	}
	
	
}