package io.github.infolis.infolink.patternLearner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.algorithm.FrequencyBasedBootstrapping;
import io.github.infolis.algorithm.ReliabilityBasedBootstrapping;
import io.github.infolis.algorithm.SearchTermPositionTest;
import io.github.infolis.infolink.luceneIndexing.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
*
* @author kata
*/
public class LearnerTest extends InfolisBaseTest {
	
	Logger log = LoggerFactory.getLogger(SearchTermPositionTest.class);
	private List<String> uris = new ArrayList<>();
	private final static String term = "FOOBAR";
    private final static List<String> terms = Arrays.asList(term);

    public LearnerTest() throws Exception {
    	
		for (InfolisFile file : createTestFiles()) {
            uris.add(file.getUri());
		}
	}
	
void testBootstrapping(Class<? extends Algorithm> algorithm, Execution.Strategy strategy, double threshold, Set<String> expectedStudies, Set<String> expectedPatterns, Set<String> expectedContexts) throws Exception {
    	
    	Execution execution = new Execution();
        execution.setAlgorithm(algorithm);
        execution.getTerms().addAll(terms);
        execution.setInputFiles(uris);
        execution.setSearchTerm(terms.get(0));
        execution.setThreshold(threshold);
        execution.setBootstrapStrategy(strategy);
        Algorithm algo = algorithm.newInstance();
        algo.setDataStoreClient(localClient);
        algo.setFileResolver(tempFileResolver);
        algo.setExecution(execution);
        algo.run();
        assertEquals(expectedStudies, new HashSet<String>(execution.getStudies()));
        assertEquals(expectedPatterns, new HashSet<String>(execution.getPattern()));
        assertEquals(expectedContexts, new HashSet<String>(execution.getStudyContexts()));
    }
    
    private class ExpectedOutput {
    	
    	Class<? extends Algorithm> algorithm;
    	Execution.Strategy strategy;
    	double threshold;
    	Set<String> studies;
    	Set<String> patterns;
    	Set<String> contexts;
    	
    	ExpectedOutput(Class<? extends Algorithm> algorithm, Execution.Strategy strategy, double threshold, Set<String> studies, Set<String> patterns, Set<String> contexts) {
    		
    		this.algorithm = algorithm;
    		this.strategy = strategy;
    		this.threshold = threshold;
    		this.studies = studies;
    		this.patterns = patterns;
    		this.contexts = contexts;
    	}
    }
    
    //TODO: add actual values
    Set<ExpectedOutput> getExpectedOutput() {
    	
    	Set<String> expectedStudies_separate = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedPatterns_separate = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedContexts_separate = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedStudies_mergeCurrent = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedPatterns_mergeCurrent = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedContexts_mergeCurrent = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedStudies_mergeNew = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedPatterns_mergeNew = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedContexts_mergeNew = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedStudies_mergeAll = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedPatterns_mergeAll = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedContexts_mergeAll = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedStudies_reliability = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedPatterns_reliability = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedContexts_reliability = new HashSet<String>(Arrays.asList("dummy"));
    	
    	Set<ExpectedOutput> expectedOutput = new HashSet<ExpectedOutput>();
    	expectedOutput.addAll(Arrays.asList(
    			new ExpectedOutput(FrequencyBasedBootstrapping.class, Execution.Strategy.separate, 0.2, expectedStudies_separate, expectedPatterns_separate, expectedContexts_separate),
    			new ExpectedOutput(FrequencyBasedBootstrapping.class, Execution.Strategy.mergeCurrent, 0.2, expectedStudies_mergeCurrent, expectedPatterns_mergeCurrent, expectedContexts_mergeCurrent),
    			new ExpectedOutput(FrequencyBasedBootstrapping.class, Execution.Strategy.mergeNew, 0.2, expectedStudies_mergeNew, expectedPatterns_mergeNew, expectedContexts_mergeNew),
    			new ExpectedOutput(FrequencyBasedBootstrapping.class, Execution.Strategy.mergeAll, 0.2, expectedStudies_mergeAll, expectedPatterns_mergeAll, expectedContexts_mergeAll),
    			new ExpectedOutput(ReliabilityBasedBootstrapping.class, Execution.Strategy.reliability, 0.4, expectedStudies_reliability, expectedPatterns_reliability, expectedContexts_reliability)
    	));
    	return expectedOutput;
    }
    
    @Test
    public void testBootstrapping() throws Exception {

    	throw new RuntimeException ("Values for comparison missing");
    	/*
    	Set<ExpectedOutput> expectedOutputs = getExpectedOutput();
    	for(ExpectedOutput expected : expectedOutputs) {
    		testBootstrapping(expected.algorithm, expected.strategy, expected.threshold, expected.studies, expected.patterns, expected.contexts);
    	}
    	*/
    }

}
