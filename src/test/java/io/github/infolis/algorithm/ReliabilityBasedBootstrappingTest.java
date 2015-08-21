/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.algorithm;

import static org.junit.Assert.*;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.SerializationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 */
public class ReliabilityBasedBootstrappingTest extends InfolisBaseTest {

	Logger log = LoggerFactory.getLogger(ReliabilityBasedBootstrappingTest.class);
	private List<String> uris = new ArrayList<>();
	private final static String term = "FOOBAR";
	private final static List<String> terms = Arrays.asList(term);

	public ReliabilityBasedBootstrappingTest() throws Exception {
		String[] testStrings = {
				"Hallo, please try to find the FOOBAR in this short text snippet. Thank you.",
				"Hallo, please try to find the R2 in this short text snippet. Thank you.",
				"Hallo, please try to find the D2 in this short text snippet. Thank you.",
				"Hallo, please try to find the term in this short text snippet. Thank you.",
				"Hallo, please try to find the _ in this short text snippet. Thank you.",
				"Hallo, please try to find .the term. in this short text snippet. Thank you.",
				"Hallo, please try to find the FOOBAR in this short text snippet. Thank you."
		};
		for (InfolisFile file : createTestTextFiles(7, testStrings)) {
			uris.add(file.getUri());
		}
	}
	
	@Test
	public void testGetTopK() {
		int k = 5;
		Map<Double, Collection<String>> patternScoreMap = new HashMap<>();
		patternScoreMap.put(0.0, Arrays.asList("0"));
		patternScoreMap.put(1.0, Arrays.asList("1"));
		patternScoreMap.put(2.0, Arrays.asList("2"));
		patternScoreMap.put(3.0, Arrays.asList("3"));
		patternScoreMap.put(4.0, Arrays.asList("4"));
		patternScoreMap.put(5.0, Arrays.asList("5", "5b"));
		Set<String> expectedTopK = new HashSet<>(Arrays.asList("5", "5b", "4", "3", "2"));
		Set<String> topK = ReliabilityBasedBootstrapping.getTopK(patternScoreMap, k).keySet();
		assertEquals(expectedTopK, topK);
		
		Map<Double, Collection<String>> reducedMap = ReliabilityBasedBootstrapping.removeBelowThreshold(patternScoreMap, 4.0);
		Map<Double, Collection<String>> expectedReducedMap = new HashMap<>();
		expectedReducedMap.put(4.0, Arrays.asList("4"));
		expectedReducedMap.put(5.0, Arrays.asList("5", "5b"));
		assertEquals(expectedReducedMap, reducedMap);
		
		assertEquals(new HashSet<>(Arrays.asList("5", "5b", "4")), ReliabilityBasedBootstrapping.getTopK(reducedMap, 5).keySet());
	}

	/**
	 * Tests basic functionality using no threshold for pattern induction (=
	 * accept all). For a more detailed test refer to patternLearner.LearnerTest
	 * class.
	 * 
	 * @param strategy
	 * @throws Exception
	 */
	@Test
	public void testReliabilityBasedBootstrapping() throws Exception {
		Execution execution = new Execution();
		execution.setAlgorithm(ReliabilityBasedBootstrapping.class);
		execution.getTerms().addAll(terms);
		execution.setInputFiles(uris);
		execution.setThreshold(-0.0);
		execution.setBootstrapStrategy(Execution.Strategy.reliability);
		Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, fileResolver);
		algo.run();

		assertTrue("StudyContexts must not be empty!", execution.getStudyContexts().size() > 0);
		for (String s : execution.getStudyContexts()) {
			StudyContext studyContext = dataStoreClient.get(StudyContext.class, s);
			InfolisPattern pat = dataStoreClient.get(InfolisPattern.class, studyContext.getPattern());
			log.debug("Study Context:\n {}Pattern: {}", studyContext.toXML(), pat.getPatternRegex());
			assertNotNull("StudyContext must have pattern set!", studyContext.getPattern());
			assertNotNull("StudyContext must have term set!", studyContext.getTerm());
			assertNotNull("StudyContext must have file set!", studyContext.getFile());
		}
		log.debug(SerializationUtils.dumpExecutionLog(execution));
	}

}
