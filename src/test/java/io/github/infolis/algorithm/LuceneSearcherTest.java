package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.TextualReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

    String testString1 = "Please try to find the term in this short text snippet";
    String testString2 = "Please try to find the _ in this short text snippet .";
    String testString3 = "Please try to find the . term . in this short text snippet .";
    String testString4 = "Hallo , please try to find the term in this short text snippet . Thank you .";
    String testString5 = "Hallo , please try to find the _ in this short text snippet. Thank you .";
    String testString6 = "Hallo , please try to find . the term . in this short text snippet . Thank you .";
    List<String> uris = new ArrayList<>();
    Execution indexerExecution;

    public LuceneSearcherTest() throws Exception {
        String[] testStrings = {
            "Hallo , please try to find the FOOBAR in this short text snippet . Thank you .",
            "Hallo , please try to find the R2 in this short text snippet . Thank you .",
            "Hallo , please try to find the D2 in this short text snippet . Thank you .",
            "Hallo , please try to find the term in this short text snippet . Thank you .",
            "Hallo , please try to find the _ in this short text snippet . Thank you .",
            "Hallo , please try to find . the term . in this short text snippet . Thank you .",
            "Hallo , please try to find the FOOBAR in this short text snippet . Thank you ."
        };
        for (InfolisFile file : createTestTextFiles(100, testStrings)) {
            uris.add(file.getUri());
        }
    }

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
        assertEquals(Arrays.asList("try", "to", "find", "the", "."), contextList3.get(0).getLeftWords());
        assertEquals("term", contextList3.get(0).getReference());
        assertEquals(Arrays.asList(".", "in", "this", "short", "text"), contextList3.get(0).getRightWords());
        assertEquals("document", contextList1.get(0).getFile());
        assertEquals("document", contextList3.get(0).getFile());
        assertEquals("term", contextList1.get(0).getReference());
        assertEquals("term", contextList3.get(0).getReference());
        assertEquals("term", contextList3.get(0).getReference());
    }

    @Test
    public void complexSearch_getContextTest() throws Exception {
    	indexerExecution = createIndex();
        assertEquals(29, testContexts("FOOBAR", "FOOBAR").size());
        assertEquals(28, testContexts("term", "term").size());
        //assertEquals(28, testContexts(". term .", "term").size());
        assertEquals(0, testContexts("terma", "terma").size());
        // same behaviour is expected for phrases
        assertEquals(29, testContexts("the FOOBAR", "\"the FOOBAR\"").size());
        //assertEquals(28, testContexts("the term,", "\"the term\"").size());
        assertEquals(14, testContexts(". the term .", "\"the term\"").size());
        assertEquals(0, testContexts("the terma", "\"the term\"").size());
       // assertEquals(28, testContexts("the. term?!", "\"the term\"").size());
        //assertEquals(0, testContexts("the...term?!", "\"the term\"").size());
        List<TextualReference> contextListA = testContexts("the term", "\"the term\"");
        assertEquals(", please try to find the term in this short text snippet", contextListA.get(0).toString());
        assertEquals("please try to find . the term . in this short text", contextListA.get(1).toString());
		// ...and for wildcard phrase queries
        // this query should find all test sentences except for those having a "." before "the" and having two words covered by the wildcard
        assertEquals(100 - 14, testContexts("", "\"to find the * in\"", 0).size());
        // this query should find all test sentences with ". the term ."
        assertEquals(14, testContexts(". the term .", "\"to find the * in\"", 2).size());
     // this query should find all test sentences with ". the term ." and "the term"
        assertEquals(28, testContexts("the term", "\"to find the * in\"", 2).size());
    }

    private Execution createIndex() throws IOException {
		Execution execution = new Execution();
		execution.setAlgorithm(Indexer.class);
		execution.setInputFiles(uris);
        //getOutputDataStoreClient().post(Execution.class, execution);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		return execution;
	}

    private List<TextualReference> testContexts(String searchTerm, String searchQuery) throws Exception {
        Execution exec = new Execution();
        exec.setAlgorithm(LuceneSearcher.class);
        exec.setSearchTerm(searchTerm);
        exec.setSearchQuery(searchQuery);
        exec.setPhraseSlop(0);
        exec.setInputFiles(uris);
        exec.setIndexDirectory(indexerExecution.getOutputDirectory());
        exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();


        ArrayList<TextualReference> contextList = new ArrayList<TextualReference>();
        for (String uri : exec.getTextualReferences()) {
            contextList.add(dataStoreClient.get(TextualReference.class, uri));
        }
        return contextList;

    }
    
    private List<TextualReference> testContexts(String searchTerm, String searchQuery, int phraseSlop) throws Exception {
        Execution exec = new Execution();
        exec.setAlgorithm(LuceneSearcher.class);
        exec.setSearchTerm(searchTerm);
        exec.setSearchQuery(searchQuery);
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

}
