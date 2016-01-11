package io.github.infolis.algorithm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

	public BootstrappingTest() throws Exception {
		String[] testStrings = {
				"Hallo, please try to find the FOOBAR in this short text snippet. Thank you.",
				"Hallo, please try to find the R2 in this short text snippet. Thank you.",
				"Hallo, please try to find the D2 in this short text snippet. Thank you.",
				"Hallo, please try to find the term in this short text snippet. Thank you.",
				"Hallo, please try to find the _ in this short text snippet. Thank you.",
				"Hallo, please try to find .the term. in this short text snippet. Thank you.",
				"Hallo, please try to find the FOOBAR in this short text snippet. Thank you."
		};
		for (InfolisFile file : createTestTextFiles(7, testStrings))
			uris7.add(file.getUri());
		pat.setPatternRegex("\\S++\\s\\S++\\s\\S++\\s\\S++\\s\\Q.the\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E\\s\\S+?\\s\\S+?\\s\\S+?\\s\\S+");
		pat.setLuceneQuery("the * in");
		pat2.setPatternRegex("\\S++\\s\\S++\\s\\S++\\s\\S++\\s\\Qthe\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E\\s\\S+?\\s\\S+?\\s\\S+?\\s\\S+");
		pat2.setLuceneQuery("the * in");
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

    public List<String> getContextsForPattern(InfolisPattern pattern) {
        Execution execution = new Execution();
        execution.getPatterns().add(pattern.getUri());
        execution.setAlgorithm(RegexSearcher.class);
        execution.getInputFiles().addAll(uris7);
        Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, fileResolver);
        algo.run();
        return execution.getTextualReferences();
    }

    private List<String> getReferenceStrings(Collection<String> URIs) {
        List<String> contexts = new ArrayList<>();
        for (String uri : URIs) {
            contexts.add(dataStoreClient.get(TextualReference.class, uri).toPrettyString());
        }
        return contexts;
    }

    @Test
    public void testGetContextsForSeed() throws IOException {
    	Execution e = new Execution();
    	e.setInputFiles(uris7);
    	Bootstrapping b = new FrequencyBasedBootstrapping(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
    	b.indexerExecution = indexerExecution;
    	b.setExecution(e);
    	List<TextualReference> refs = b.getContextsForSeed("term");
    	assertEquals(new HashSet<String>(Arrays.asList("please try to find the term in this short text snippet.",
    			"please try to find .the term . in this short text")),
    			new HashSet<String>(TextualReference.getContextStrings(refs)));
    }

    @Test
    /**
     * Tests whether optimized search using lucene yields the same result as
     * searching the regular expressions directly without prior filtering.
     *
     * @throws IOException
     */
    public void testGetContextsForPatterns() throws IOException {
    	List<String> references1 = getReferenceStrings(getContextsForPattern(pat));
    	references1.addAll(getReferenceStrings(getContextsForPattern(pat2)));
    	Set<String> references1set= new HashSet<>(references1);
    	Execution e = new Execution();
    	e.setInputFiles(uris7);
    	Bootstrapping b = new FrequencyBasedBootstrapping(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
    	b.indexerExecution = indexerExecution;
    	b.setExecution(e);
    	List<String> references2 = getReferenceStrings(b.getContextsForPatterns(Arrays.asList(pat, pat2)));
    	Set<String> references2set = new HashSet<>(references2);
    	assertEquals(references1set, references2set);
    	assertEquals(references1.size(), references2.size());
    }
    
    
    // test all bootstrapping algorithms 
    
    void testBootstrapping(Class<? extends Algorithm> algorithm, BootstrapStrategy strategy, double threshold, Set<String> expectedStudies, Set<String> expectedPatterns, Set<String> expectedContexts) throws Exception {
    	Execution execution = new Execution();
        execution.setAlgorithm(algorithm);
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
    		contextSet.add(infolisContext.getLeftText() + " " + infolisContext.getReference() + " " + infolisContext.getRightText());
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

    //TODO: add values for reliability-based bootstrapping
    Set<ExpectedOutput> getExpectedOutput() {
    	// find all contexts for terms "FOOBAR" and "term"
    	// "R2", "D2" and "_" are to be rejected: study titles must consist of at least
    	// 3 letters (as currently defined in study regex. Change regex to alter this behaviour)
    	Set<String> expectedStudies_separate = new HashSet<String>(Arrays.asList("term", "FOOBAR"));
    	Set<String> expectedPatterns_separate = new HashSet<String>(Arrays.asList(
    			"\\Qfind\\E\\s\\Qthe\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E",
    			"\\Qfind\\E\\s\\Q.the\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\Q.\\E"));
    	Set<String> expectedContexts_separate = new HashSet<String>(Arrays.asList(
    			"please try to find the term in this short text snippet.",
    			"please try to find the FOOBAR in this short text snippet.",
    			"please try to find .the term . in this short text"));
    	Set<String> expectedStudies_mergeCurrent = new HashSet<String>(Arrays.asList("term", "FOOBAR"));
    	Set<String> expectedPatterns_mergeCurrent = new HashSet<String>(Arrays.asList(
    			"\\Qfind\\E\\s\\Qthe\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E"));
    	Set<String> expectedContexts_mergeCurrent = new HashSet<String>(Arrays.asList(
    			"please try to find the term in this short text snippet.",
    			"please try to find the FOOBAR in this short text snippet."));
    	Set<String> expectedStudies_mergeNew = new HashSet<String>(Arrays.asList("term", "FOOBAR"));
    	Set<String> expectedPatterns_mergeNew = new HashSet<String>(Arrays.asList(
    			"\\Qfind\\E\\s\\Qthe\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E",
    			"\\Qfind\\E\\s\\Q.the\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\Q.\\E"));
    	Set<String> expectedContexts_mergeNew = new HashSet<String>(Arrays.asList(
    			"please try to find the term in this short text snippet.",
    			"please try to find the FOOBAR in this short text snippet.",
    			"please try to find .the term . in this short text"));
    	Set<String> expectedStudies_mergeAll = new HashSet<String>(Arrays.asList("term", "FOOBAR"));
    	Set<String> expectedPatterns_mergeAll = new HashSet<String>(Arrays.asList(
    			"\\Qfind\\E\\s\\Qthe\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E"));
    	Set<String> expectedContexts_mergeAll = new HashSet<String>(Arrays.asList(
    			"please try to find the term in this short text snippet.",
    			"please try to find the FOOBAR in this short text snippet."));


    	Set<String> expectedStudies_reliability = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedPatterns_reliability = new HashSet<String>(Arrays.asList("dummy"));
    	Set<String> expectedContexts_reliability = new HashSet<String>(Arrays.asList("dummy"));

    	Set<ExpectedOutput> expectedOutput = new HashSet<ExpectedOutput>();
    	expectedOutput.addAll(Arrays.asList(
    			new ExpectedOutput(FrequencyBasedBootstrapping.class, BootstrapStrategy.separate, 0.45, expectedStudies_separate, expectedPatterns_separate, expectedContexts_separate),
    			new ExpectedOutput(FrequencyBasedBootstrapping.class, BootstrapStrategy.mergeCurrent, 0.45, expectedStudies_mergeCurrent, expectedPatterns_mergeCurrent, expectedContexts_mergeCurrent),
    			new ExpectedOutput(FrequencyBasedBootstrapping.class, BootstrapStrategy.mergeNew, 0.45, expectedStudies_mergeNew, expectedPatterns_mergeNew, expectedContexts_mergeNew),
    			new ExpectedOutput(FrequencyBasedBootstrapping.class, BootstrapStrategy.mergeAll, 0.45, expectedStudies_mergeAll, expectedPatterns_mergeAll, expectedContexts_mergeAll)//,
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