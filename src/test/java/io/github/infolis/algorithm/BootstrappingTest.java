package io.github.infolis.algorithm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.BootstrapStrategy;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.util.RegexUtils;

import static org.junit.Assert.*;

/**
 *
 * @author kata
 */
public class BootstrappingTest extends InfolisBaseTest {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(BootstrappingTest.class);
	Execution indexerExecution = new Execution();

	private List<String> uris7 = new ArrayList<>();
	private static InfolisPattern pat = new InfolisPattern();
	private static InfolisPattern pat2 = new InfolisPattern();
	
	private List<String> uris20 = new ArrayList<>();
	private final static String term = "FOOBAR";
    private final static List<String> terms = Arrays.asList(term);
    
    String[] testStrings = {
			"Hallo , please try to find the FOOBAR in this short text snippet . Thank you .",
			"Hallo , please try to find the R2 in this short text snippet . Thank you .",
			"Hallo , please try to find the D2 in this short text snippet . Thank you .",
			"Hallo , please try to find the term in this short text snippet . Thank you .",
			"Hallo , please try to find the _ in this short text snippet . Thank you .",
			"Hallo , please try to find the term . in this short text snippet . Thank you .",
			"Hallo , please try to find the FOOBAR in this short text snippet . Thank you ."
	};

	public BootstrappingTest() throws Exception {
		
		for (InfolisFile file : createTestTextFiles(7, testStrings)) uris7.add(file.getUri());
		pat.setPatternRegex("\\S++\\s\\S++\\s\\S++\\s\\S++\\s\\Q.the\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E\\s\\S+?\\s\\S+?\\s\\S+?\\s\\S+");
		pat.setMinimal("\\Q.the\\E" + RegexUtils.studyRegex_ngram + "\\Qin\\E");
		pat.setLuceneQuery("\".the * in\"");
		pat2.setPatternRegex("\\S++\\s\\S++\\s\\S++\\s\\S++\\s\\Qthe\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E\\s\\S+?\\s\\S+?\\s\\S+?\\s\\S+");
		pat2.setMinimal("\\Qthe\\E" + RegexUtils.studyRegex_ngram + "\\Qin\\E");
		pat2.setLuceneQuery("\"the * in\"");
		dataStoreClient.post(InfolisPattern.class, pat);
		dataStoreClient.post(InfolisPattern.class, pat2);
		indexerExecution = createIndex();
		
		for (InfolisFile file : createTestTextFiles(20, testStrings)) {
            uris20.add(file.getUri());
            String str = FileUtils.readFileToString(new File(file.getFileName()));
            log.debug(str);
		}
	}


	public Execution createIndex() throws IOException {
		Execution execution = new Execution();
		execution.setAlgorithm(Indexer.class);
		execution.setInputFiles(uris7);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		return execution;
	}

    @Test
    public void testGetContextsForSeed() throws IOException {
    	Execution e = new Execution();
    	e.setTokenize(false);
    	e.setInputFiles(uris7);
    	Bootstrapping b = new FrequencyBasedBootstrapping(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
    	b.indexerExecution = indexerExecution;
    	b.setExecution(e);
    	List<TextualReference> refs = b.getContextsForSeed("term");
    	assertEquals(new HashSet<String>(Arrays.asList(testStrings[3], testStrings[5])),
    			new HashSet<String>(TextualReference.getContextStrings(refs)));
    }

    // test all bootstrapping algorithms 
    void testBootstrapping(Class<? extends Algorithm> algorithm, BootstrapStrategy strategy, double threshold, Set<String> expectedStudies, Set<String> expectedPatterns, Set<String> expectedContexts) throws Exception {
    	Execution execution = new Execution();
        execution.setAlgorithm(algorithm);
        execution.setTokenize(false);
        execution.getSeeds().addAll(terms);
        execution.setInputFiles(uris20);
        execution.setSearchTerm(terms.get(0));
        execution.setReliabilityThreshold(threshold);
        execution.setBootstrapStrategy(strategy);
        execution.setUpperCaseConstraint(false);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        //TODO: use this when URIs are posted in FrequencyBasedBootstrapping instead of the term string
        //assertEquals(expectedStudies, getTerms(execution.getStudies()));
        // TODO replace, since exeution.getStudies() is gone
        assertEquals(expectedPatterns, getRegex(execution.getPatterns()));
        assertEquals(expectedContexts, getContextStrings(execution.getTextualReferences()));
    }

    Set<String> getRegex(List<String> patternURIs) {
    	Set<String> regexSet = new HashSet<String>();
    	for (String uri : patternURIs) {
    		InfolisPattern pattern = dataStoreClient.get(InfolisPattern.class, uri);
    		regexSet.add(pattern.getMinimal());
    	}
    	return regexSet;
    }

    Set<String> getContextStrings(List<String> contextURIs) {
    	Set<String> contextSet = new HashSet<String>();
    	for (String uri : contextURIs) {
    		TextualReference infolisContext = dataStoreClient.get(TextualReference.class, uri);
    		contextSet.add(infolisContext.getLeftText() + infolisContext.getReference() + infolisContext.getRightText());
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

    static class ExpectedOutput {
    	Class<? extends Algorithm> algorithm;
    	BootstrapStrategy strategy;
    	double threshold;
    	Set<String> studies;
    	Set<String> patterns;
    	Set<String> contexts;

    	ExpectedOutput(Class<? extends Algorithm> algorithm, BootstrapStrategy strategy, double threshold, Set<String> studies, Set<String> patterns, Set<String> contexts) {
    		this.algorithm = algorithm;
    		this.strategy = strategy;
    		this.threshold = threshold;
    		this.studies = studies;
    		this.patterns = patterns;
    		this.contexts = contexts;
    	}
    }

 
    Set<ExpectedOutput> getExpectedOutput() {
    	Set<ExpectedOutput> expectedOutput = FrequencyBasedBootstrappingTest.getExpectedOutput();
    	expectedOutput.addAll(ReliabilityBasedBootstrappingTest.getExpectedOutput());
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