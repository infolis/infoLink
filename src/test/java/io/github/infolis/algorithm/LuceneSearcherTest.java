package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.TextualReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 *
 */
public class LuceneSearcherTest extends InfolisBaseTest {

    Logger log = LoggerFactory.getLogger(LuceneSearcherTest.class);

    String testString1 = "Please try to find the term in this short text snippet .";
    String testString2 = "Please try to find the _ in this short text snippet .";
    String testString3 = "Please try to find the . term . in this short text snippet .";
    String testString4 = "Hallo , please try to find the term in this short text snippet . Thank you .";
    String testString5 = "Hallo , please try to find the _ in this short text snippet . Thank you .";
    String testString6 = "Hallo , please try to find . the term . in this short text snippet . Thank you .";
    List<String> uris = new ArrayList<>();
    Execution indexerExecution;
    String[] testStrings = {
            "Hallo , please try to find the FOOBAR in this short text snippet . Thank you .",
            "Hallo , please try to find the R2 in this short text snippet . Thank you .",
            "Hallo , please try to find the D2 in this short text snippet . Thank you .",
            "Hallo , please try to find the term in this short text snippet . Thank you .",
            "Hallo , please try to find the _ in this short text snippet . Thank you .",
            "Hallo , please try to find . the term . in this short text snippet . Thank you .",
            "Hallo , please try to find the FOOBAR in this short text snippet . Thank you ."
        };

    public LuceneSearcherTest() throws Exception {
        for (InfolisFile file : createTestTextFiles(100, testStrings)) {
            uris.add(file.getUri());
        }
    }
    /*
    @Test
    public void testGetContexts() throws ArrayIndexOutOfBoundsException, IOException {
    	String test = "line1\nline2\nline3 term line3\nline4\nline5\nline6";
    	List<TextualReference> refList = LuceneSearcher.getContexts(dataStoreClient, "filename", "term", test);
    	assertEquals(1, refList.size());
    	for (TextualReference ref : refList) {
    		assertEquals("line2\nline3 ", ref.getLeftText());
    		assertEquals(" line3\nline4", ref.getRightText());
    	}
    	test = "line1 term line1";
    	refList = LuceneSearcher.getContexts(dataStoreClient, "filename", "term", test);
    	assertEquals(1, refList.size());
    	for (TextualReference ref : refList) {
    		assertEquals("line1 ", ref.getLeftText());
    		assertEquals(" line1", ref.getRightText());
    	}
    	test = "line1-term line1";
    	refList = LuceneSearcher.getContexts(dataStoreClient, "filename", "term", test);
    	assertEquals(1, refList.size());
    	for (TextualReference ref : refList) {
    		assertEquals("line1-", ref.getLeftText());
    		assertEquals(" line1", ref.getRightText());
    		assertEquals(Arrays.asList("line1-", ""), ref.getLeftWords());
    		assertEquals(Arrays.asList(" ", "line1"), ref.getRightWords());
    	}
    	test = "line1-termline1";
    	refList = LuceneSearcher.getContexts(dataStoreClient, "filename", "term", test);
    	assertEquals(0, refList.size());
    }*/
    /*
    @Test
    public void getContextTest() throws IOException {
    	indexerExecution = createIndex();
        List<TextualReference> contextList1 = LuceneSearcher.getContexts(dataStoreClient, "document", "term", testString1);
        List<TextualReference> contextList2 = LuceneSearcher.getContexts(dataStoreClient, "document", "term", testString2);
        List<TextualReference> contextList3 = LuceneSearcher.getContexts(dataStoreClient, "document", "term", testString3);
        assertEquals(1, contextList1.size());
        assertEquals(0, contextList2.size());
        assertEquals(1, contextList3.size());
        assertEquals(testString1, contextList1.get(0).toString());
        assertEquals(1, contextList3.size());
        assertEquals("Please try to find the . ", contextList3.get(0).getLeftText());
        assertEquals(Arrays.asList("Please", "try", "to", "find", "the", ".", " "), contextList3.get(0).getLeftWords());
        assertEquals("term", contextList3.get(0).getReference());
        assertEquals(" . in this short text snippet .", contextList3.get(0).getRightText());
        assertEquals(Arrays.asList(" ", ".", "in", "this", "short", "text", "snippet", "."), contextList3.get(0).getRightWords());
        assertEquals("document", contextList1.get(0).getFile());
        assertEquals("document", contextList3.get(0).getFile());
        assertEquals("term", contextList1.get(0).getReference());
        assertEquals("term", contextList3.get(0).getReference());
        assertEquals("term", contextList3.get(0).getReference());
    }*/

    @Test
    public void complexSearch_getContextTest() throws Exception {
    	indexerExecution = createIndex();
        assertEquals(29, testContexts("FOOBAR", "FOOBAR", 0).size());
        assertEquals(28, testContexts("term", "term", 0).size());
        assertEquals(0, testContexts("terma", "terma", 0).size());
        // same behaviour is expected for phrases
        assertEquals(29, testContexts("the FOOBAR", "\"the FOOBAR\"", 0).size());
        assertEquals(28, testContexts("term", "\"the term\"", 0).size());
        List<TextualReference> contextListA = testContexts("the term", "\"the term\"", 0);
        assertEquals(testStrings[3], contextListA.get(0).toString().trim());
        // current context extraction method extracts the one sentence in which the term is found. 
     	String testSentence3 = testStrings[3];
     	String testSentence5 = testStrings[5];
        assertEquals(new HashSet<String>(Arrays.asList(testSentence3, testSentence5)), new HashSet<String>(Arrays.asList(contextListA.get(1).toString().trim(), contextListA.get(0).toString().trim())));
		// ...and for wildcard phrase queries
        // this query should find all test sentences except for those having a "." before "the" and having two words covered by the wildcard
        assertEquals(100 - 14, testExecute(null, "\"to find the * in\"", 0).size());
        // this query should find all test sentences with ". the term ."
        assertEquals(14, testContexts("", "\"to find . the * in\"", 2).size());
        // this query should find all test sentences with ". the term ." and "the term"
        assertEquals(28, testContexts("the term", "\"to find the term in\"", 2).size());
    }

    private Execution createIndex() throws IOException {
		Execution execution = new Execution();
		execution.setAlgorithm(Indexer.class);
		execution.setInputFiles(uris);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		return execution;
	}
    
    private List<TextualReference> testContexts(String searchTerm, String searchQuery, int phraseSlop) throws Exception {
        Execution exec = new Execution();
        exec.setAlgorithm(LuceneSearcher.class);
        exec.setSearchTerm(searchTerm);
        InfolisPattern pat = new InfolisPattern(searchQuery);
        dataStoreClient.post(InfolisPattern.class, pat);
        exec.setPatterns(Arrays.asList(pat.getUri()));
        exec.setPhraseSlop(phraseSlop);
        exec.setInputFiles(uris);
        exec.setIndexDirectory(indexerExecution.getOutputDirectory());
        exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();


        ArrayList<TextualReference> contextList = new ArrayList<TextualReference>();
        for (String uri : exec.getTextualReferences()) {
            contextList.add(dataStoreClient.get(TextualReference.class, uri));
        }
        return contextList;

    }
    
    private List<String> testExecute(String searchTerm, String searchQuery, int phraseSlop) throws Exception {
        Execution exec = new Execution();
        exec.setAlgorithm(LuceneSearcher.class);
        exec.setSearchTerm(searchTerm);
        InfolisPattern pat = new InfolisPattern(searchQuery);
        dataStoreClient.post(InfolisPattern.class, pat);
        exec.setPatterns(Arrays.asList(pat.getUri()));
        exec.setPhraseSlop(phraseSlop);
        exec.setInputFiles(uris);
        exec.setIndexDirectory(indexerExecution.getOutputDirectory());
        exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        return exec.getMatchingFiles();
    }

}
