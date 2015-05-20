package io.github.infolis.infolink.patternLearner;

import java.io.File;
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
import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.Study;
import io.github.infolis.model.StudyContext;

import org.apache.commons.io.FileUtils;
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
		for (InfolisFile file : createTestFiles(10)) {
            uris.add(file.getUri());
            String str = FileUtils.readFileToString(new File(file.getFileName()));
            log.debug(str);
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
        execution.instantiateAlgorithm(localClient, tempFileResolver).run();
        //TODO: use this when URIs are posted in FrequencyBasedBootstrapping instead of the term string
        //assertEquals(expectedStudies, getTerms(execution.getStudies()));
        assertEquals(expectedStudies, new HashSet<>(execution.getStudies()));
        assertEquals(expectedPatterns, getRegex(execution.getPattern()));
        assertEquals(expectedContexts, getContextStrings(execution.getStudyContexts()));
    }
    
    Set<String> getRegex(List<String> patternURIs) {
    	Set<String> regexSet = new HashSet<String>();
    	for (String uri : patternURIs) {
    		InfolisPattern pattern = localClient.get(InfolisPattern.class, uri);
    		regexSet.add(pattern.getPatternRegex());
    	}
    	return regexSet;
    }
    
    Set<String> getContextStrings(List<String> contextURIs) {
    	Set<String> contextSet = new HashSet<String>();
    	for (String uri : contextURIs) {
    		StudyContext infolisContext = localClient.get(StudyContext.class, uri);
    		contextSet.add(infolisContext.getLeftText() + " " + infolisContext.getTerm() + " " + infolisContext.getRightText());
    	}
    	return contextSet;
    }
    //TODO: use this when URIs are posted in FrequencyBasedBootstrapping instead of the term string
    /*
    Set<String> getTerms(List<String> studyURIs) {
    	Set<String> termSet = new HashSet<String>();
    	for (String uri : studyURIs) {
    		Study study = localClient.get(Study.class, uri);
    		termSet.add(study.getName());
    	}
    	return termSet;
    }*/
    
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
    
    //TODO: add values for reliability-based bootstrapping
    //TODO: insert correct expected patterns and corresponding contexts
    Set<ExpectedOutput> getExpectedOutput() {
    	// find all contexts for terms "FOOBAR" and "term"
    	// "R2", "D2" and "_" are to be rejected: study titles must consist of at least 
    	// 3 letters (as currently defined in study regex. Change regex to alter this behaviour)
    	Set<String> expectedStudies_separate = new HashSet<String>(Arrays.asList("term", "FOOBAR"));
    	Set<String> expectedPatterns_separate = new HashSet<String>(Arrays.asList(
    			"\\Qfind\\E\\s\\Qthe\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E",
    			"\\Qfind\\E\\s\\Q.the\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Q.\\E"));
    	Set<String> expectedContexts_separate = new HashSet<String>(Arrays.asList(
    			"please try to find the term in this short text snippet.",
    			"please try to find the FOOBAR in this short text snippet.",
    			"please try to find .the term . in this short text"));
    	Set<String> expectedStudies_mergeCurrent = new HashSet<String>(Arrays.asList("term", "FOOBAR"));
    	Set<String> expectedPatterns_mergeCurrent = new HashSet<String>(Arrays.asList(
    			"\\Qfind\\E\\s\\Qthe\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E"));
    	Set<String> expectedContexts_mergeCurrent = new HashSet<String>(Arrays.asList(
    			"please try to find the term in this short text snippet.",
    			"please try to find the FOOBAR in this short text snippet.",
    			"please try to find .the term . in this short text"));
    	Set<String> expectedStudies_mergeNew = new HashSet<String>(Arrays.asList("term", "FOOBAR"));
    	Set<String> expectedPatterns_mergeNew = new HashSet<String>(Arrays.asList(
    			"\\Qfind\\E\\s\\Qthe\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E", 
    			"\\Qfind\\E\\s\\Q.the\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Q.\\E"));
    	Set<String> expectedContexts_mergeNew = new HashSet<String>(Arrays.asList(
    			"please try to find the term in this short text snippet.",
    			"please try to find the FOOBAR in this short text snippet.",
    			"please try to find .the term . in this short text",
    			"please try to find .the term . in this short text"));
    	Set<String> expectedStudies_mergeAll = new HashSet<String>(Arrays.asList("term", "FOOBAR"));
    	Set<String> expectedPatterns_mergeAll = new HashSet<String>(Arrays.asList(
    			"\\Qfind\\E\\s\\Qthe\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E"));
    	Set<String> expectedContexts_mergeAll = new HashSet<String>(Arrays.asList(
    			"please try to find the term in this short text snippet.",
    			"please try to find the FOOBAR in this short text snippet.",
    			"please try to find .the term . in this short text"));

    	
    	Set<String> expectedStudies_reliability = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedPatterns_reliability = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedContexts_reliability = new HashSet<String>(Arrays.asList("dummy"));
    	
    	Set<ExpectedOutput> expectedOutput = new HashSet<ExpectedOutput>();
    	expectedOutput.addAll(Arrays.asList(
    			new ExpectedOutput(FrequencyBasedBootstrapping.class, Execution.Strategy.separate, 0.2, expectedStudies_separate, expectedPatterns_separate, expectedContexts_separate)//,
    			//new ExpectedOutput(FrequencyBasedBootstrapping.class, Execution.Strategy.mergeCurrent, 0.2, expectedStudies_mergeCurrent, expectedPatterns_mergeCurrent, expectedContexts_mergeCurrent)//,
    			//new ExpectedOutput(FrequencyBasedBootstrapping.class, Execution.Strategy.mergeNew, 0.2, expectedStudies_mergeNew, expectedPatterns_mergeNew, expectedContexts_mergeNew)//,
    			//new ExpectedOutput(FrequencyBasedBootstrapping.class, Execution.Strategy.mergeAll, 0.2, expectedStudies_mergeAll, expectedPatterns_mergeAll, expectedContexts_mergeAll)//,
    			//new ExpectedOutput(ReliabilityBasedBootstrapping.class, Execution.Strategy.reliability, 0.4, expectedStudies_reliability, expectedPatterns_reliability, expectedContexts_reliability)
    	));
    	return expectedOutput;
    }
    
    @Test
    public void testBootstrapping() throws Exception {
    	Set<ExpectedOutput> expectedOutputs = getExpectedOutput();
    	for(ExpectedOutput expected : expectedOutputs) {
    		testBootstrapping(expected.algorithm, expected.strategy, expected.threshold, expected.studies, expected.patterns, expected.contexts);
    	}
    }

}
