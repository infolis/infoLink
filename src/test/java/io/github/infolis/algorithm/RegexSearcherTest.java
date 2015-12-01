package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.TextualReference;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 * @author domi
 */
public class RegexSearcherTest extends InfolisBaseTest {

	private static final int NUMBER_OF_FILES = 7;
	private static final Logger log = LoggerFactory.getLogger(RegexSearcherTest.class);
	private List<String> textUris;
	private List<String> pdfUris;
	// left context: <word> <word> to find the
	// study title: <word>* <word> <word> <word> <word>*
	// right context: in <word> <word> <word> <word>
	// where word is an arbitrary string consisting of at least one character
	private final static InfolisPattern testPattern = new InfolisPattern(""
			+ "\\S++\\s\\S++"
			+ "\\s\\Qto\\E"
			+ "\\s\\Qfind\\E"
			+ "\\s\\Qthe\\E"
			+ "\\s\\s?"
			+ "(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?"
			+ "\\s\\Qin\\E"
			+ "\\s\\S+?\\s\\S+?\\s\\S+?\\s\\S+");

	String[] testStrings = {
			"Hallo, please try to find the FOOBAR in this short text snippet. Thank you.",
			"Hallo, please try to find the R2 in this short text snippet. Thank you.",
			"Hallo, please try to find the D2 in this short text snippet. Thank you.",
			"Hallo, please try to find the term in this short text snippet. Thank you.",
			"Hallo, please try to find the _ in this short text snippet. Thank you.",
			"Hallo, please try to find .the term. in this short text snippet. Thank you.",
			"Hallo, please try to find the FOOBAR in this short text snippet. Thank you."
	};

	@Before
	public void setUp() throws Exception {
		dataStoreClient.clear();
		textUris = new ArrayList<>();
		for (InfolisFile file : createTestTextFiles(NUMBER_OF_FILES, testStrings)) {
			textUris.add(file.getUri());
		}
		pdfUris = new ArrayList<>();
		for (InfolisFile file : createTestPdfFiles(NUMBER_OF_FILES, testStrings)) {
			pdfUris.add(file.getUri());
		}
		dataStoreClient.post(InfolisPattern.class, testPattern);
	}

	@Test
	public void testRegexSearcherWithPdf() throws Exception {

		Execution execution = new Execution();
		execution.getPatterns().add(testPattern.getUri());
		execution.setAlgorithm(RegexSearcher.class);
		execution.getInputFiles().addAll(pdfUris);
		Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
		algo.run();
		// find the contexts of "FOOBAR" and "term" (see also
		// FrequencyBasedBootstrappingTest)
		log.debug("LOG: {}", execution.getLog());
		for (TextualReference sc: dataStoreClient.get(TextualReference.class, execution.getTextualReferences())) {
			log.debug("Study Context: {}", sc);
		}
		assertEquals(3, execution.getTextualReferences().size());
	}

	@Test
	public void testRegexSearcher() throws Exception {

		Execution execution = new Execution();
		execution.getPatterns().add(testPattern.getUri());
		execution.setAlgorithm(RegexSearcher.class);
		execution.getInputFiles().addAll(textUris);
		Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
		algo.run();

		// find the contexts of "FOOBAR" and "term" (see also
		// FrequencyBasedBootstrappingTest)
		log.debug("LOG: {}", execution.getLog());
		assertEquals(3, execution.getTextualReferences().size());
	}

}
