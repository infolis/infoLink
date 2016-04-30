/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.algorithm;

import static org.junit.Assert.assertNotNull;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.algorithm.BootstrappingTest.ExpectedOutput;
import io.github.infolis.model.Execution;
import io.github.infolis.model.BootstrapStrategy;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.TextualReference;
import io.github.infolis.util.SerializationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 * @author domi
 * @author kba
 */
public class FrequencyBasedBootstrappingTest extends InfolisBaseTest {

	Logger log = LoggerFactory.getLogger(FrequencyBasedBootstrappingTest.class);
	private List<String> uris = new ArrayList<>();
	private final static String term = "FOOBAR";
	private final static List<String> terms = Arrays.asList(term);
	private static String[] testStrings = {
			"Hallo , please try to find the FOOBAR in this short text snippet . Thank you .",
			"Hallo , please try to find the R2 in this short text snippet . Thank you .",
			"Hallo , please try to find the D2 in this short text snippet . Thank you .",
			"Hallo , please try to find the term in this short text snippet . Thank you .",
			"Hallo , please try to find the _ in this short text snippet . Thank you .",
			"Hallo , please try to find the term . in this short text snippet . Thank you .",
			"Hallo , please try to find the FOOBAR in this short text snippet . Thank you ."
	};

	public FrequencyBasedBootstrappingTest() throws Exception {
		for (InfolisFile file : createTestTextFiles(7, testStrings)) {
			uris.add(file.getUri());
		}
	}

	/**
	 * Tests basic functionality using no threshold for pattern induction (=
	 * accept all)
	 *
	 * @param strategy
	 * @throws Exception
	 */
	void testFrequencyBasedBootstrapping(BootstrapStrategy strategy) throws Exception {

		Execution execution = new Execution();
		execution.setAlgorithm(FrequencyBasedBootstrapping.class);
		execution.getSeeds().addAll(terms);
		execution.setInputFiles(uris);
		execution.setSearchTerm(terms.get(0));
		execution.setReliabilityThreshold(0.0);
		execution.setBootstrapStrategy(strategy);
		execution.setTokenize(false);

		Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
		algo.run();

		for (String s : execution.getTextualReferences()) {
			TextualReference studyContext = dataStoreClient.get(TextualReference.class, s);
			InfolisPattern pat = dataStoreClient.get(InfolisPattern.class, studyContext.getPattern());
			log.debug("Study Context:\n {}Pattern: {}", studyContext.toXML(), pat.getPatternRegex());
			assertNotNull("StudyContext must have pattern set!", studyContext.getPattern());
			assertNotNull("StudyContext must have term set!", studyContext.getReference());
			assertNotNull("StudyContext must have file set!", studyContext.getFile());
		}
		log.debug(SerializationUtils.dumpExecutionLog(execution));
	}

	@Test
	public void testBootstrapping_basic() throws Exception {

		testFrequencyBasedBootstrapping(BootstrapStrategy.separate);
		testFrequencyBasedBootstrapping(BootstrapStrategy.mergeCurrent);
		testFrequencyBasedBootstrapping(BootstrapStrategy.mergeNew);
		testFrequencyBasedBootstrapping(BootstrapStrategy.mergeAll);
	}
	
	// set expected output to test this bootstrapping algorithm with its current configuration 
	// in BoostrappingTest
	public static Set<BootstrappingTest.ExpectedOutput> getExpectedOutput() {
		String testSentence3 = testStrings[3];
		String testSentence0 = testStrings[0];
		String testSentence5 = testStrings[5];
    	// find all contexts for terms "FOOBAR" and "term"
    	// "R2", "D2" and "_" are to be rejected: study titles must consist of at least
    	// 3 letters (as currently defined in study regex. Change regex to alter this behaviour)
    	Set<String> expectedStudies_separate = new HashSet<String>(Arrays.asList("term", "FOOBAR", "term ."));
    	Set<String> expectedPatterns_separate = new HashSet<String>(Arrays.asList(
    			"\\Qfind\\E\\s\\Qthe\\E\\s(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s\\Qin\\E",
    			"\\Qfind\\E\\s\\Qthe\\E\\s(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s[.,;!?]"));
    	Set<String> expectedContexts_separate = new HashSet<String>(Arrays.asList(
    			testSentence3,
    			testSentence0,
    			testSentence5));
    	Set<String> expectedStudies_mergeCurrent = new HashSet<String>(Arrays.asList("term", "FOOBAR", "term ."));
    	Set<String> expectedPatterns_mergeCurrent = new HashSet<String>(Arrays.asList(
    			"\\Qto\\E\\s\\Qfind\\E\\s\\Qthe\\E\\s(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s[.,;!?]",
    			"\\Qfind\\E\\s\\Qthe\\E\\s(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s\\Qin\\E"));
    	Set<String> expectedContexts_mergeCurrent = new HashSet<String>(Arrays.asList(
    			testSentence3,
    			testSentence0,
    			testSentence5));
    	Set<String> expectedStudies_mergeNew = new HashSet<String>(Arrays.asList("term", "FOOBAR", "term ."));
    	Set<String> expectedPatterns_mergeNew = new HashSet<String>(Arrays.asList(
    			"\\Qfind\\E\\s\\Qthe\\E\\s(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s\\Qin\\E",
    			"\\Qfind\\E\\s\\Qthe\\E\\s(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s[.,;!?]"));
    	Set<String> expectedContexts_mergeNew = new HashSet<String>(Arrays.asList(
    			testSentence3,
    			testSentence0,
    			testSentence5));
    	Set<String> expectedStudies_mergeAll = new HashSet<String>(Arrays.asList("term", "FOOBAR", "term ."));
    	Set<String> expectedPatterns_mergeAll = new HashSet<String>(Arrays.asList(
    			"\\Qto\\E\\s\\Qfind\\E\\s\\Qthe\\E\\s(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s[.,;!?]",
    			"\\Qfind\\E\\s\\Qthe\\E\\s(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s\\Qin\\E"));
    	Set<String> expectedContexts_mergeAll = new HashSet<String>(Arrays.asList(
    			testSentence3,
    			testSentence0,
    			testSentence5));

    	Set<ExpectedOutput> expectedOutput = new HashSet<ExpectedOutput>();
    	expectedOutput.addAll(Arrays.asList(
    			new ExpectedOutput(FrequencyBasedBootstrapping.class, BootstrapStrategy.separate, 0.25, expectedStudies_separate, expectedPatterns_separate, expectedContexts_separate),
    			new ExpectedOutput(FrequencyBasedBootstrapping.class, BootstrapStrategy.mergeCurrent, 0.25, expectedStudies_mergeCurrent, expectedPatterns_mergeCurrent, expectedContexts_mergeCurrent),
    			new ExpectedOutput(FrequencyBasedBootstrapping.class, BootstrapStrategy.mergeNew, 0.25, expectedStudies_mergeNew, expectedPatterns_mergeNew, expectedContexts_mergeNew),
    			new ExpectedOutput(FrequencyBasedBootstrapping.class, BootstrapStrategy.mergeAll, 0.25, expectedStudies_mergeAll, expectedPatterns_mergeAll, expectedContexts_mergeAll)
    	));
    	return expectedOutput;
	}

}
