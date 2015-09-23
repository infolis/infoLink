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
public class SearchTermPositionTest extends InfolisBaseTest {
	
	Logger log = LoggerFactory.getLogger(SearchTermPositionTest.class);

	String testString1 = "Please try to find the term in this short text snippet.";
	String testString2 = "Please try to find the _ in this short text snippet.";
	String testString3 = "Please try to find the . term . in this short text snippet.";
	String testString4 = "Hallo, please try to find the term in this short text snippet. Thank you.";
	String testString5 = "Hallo, please try to find the _ in this short text snippet. Thank you.";
	String testString6 = "Hallo, please try to find .the term. in this short text snippet. Thank you.";
	List<String> uris = new ArrayList<>();

//	private SearchTermPosition stp;
	public SearchTermPositionTest() throws Exception {
		String[] testStrings = {
				"Hallo, please try to find the FOOBAR in this short text snippet. Thank you.",
				"Hallo, please try to find the R2 in this short text snippet. Thank you.",
				"Hallo, please try to find the D2 in this short text snippet. Thank you.",
				"Hallo, please try to find the term in this short text snippet. Thank you.",
				"Hallo, please try to find the _ in this short text snippet. Thank you.",
				"Hallo, please try to find .the term. in this short text snippet. Thank you.",
				"Hallo, please try to find the FOOBAR in this short text snippet. Thank you."
		};
		for (InfolisFile file : createTestTextFiles(100, testStrings)) {
            uris.add(file.getUri());
		}
//		stp = new SearchTermPosition(dataStoreClient, dataStoreClient,fileResolver, fileResolver);
	}

	@Test
	public void getContextTest() throws IOException {

			List<TextualReference> contextList1 = SearchTermPosition.getContexts(dataStoreClient, "document", "term", testString1); 
			List<TextualReference> contextList2 = SearchTermPosition.getContexts(dataStoreClient, "document", "term", testString2); 
			List<TextualReference> contextList3 = SearchTermPosition.getContexts(dataStoreClient, "document", "term", testString3);
			assertEquals(1,contextList1.size());
			assertEquals(0,contextList2.size());
			assertEquals(1,contextList3.size());
			assertEquals(testString1, contextList1.get(0).toString());
			assertEquals(1, contextList3.size());
			assertEquals(Arrays.asList("try", "to", "find", "the", "."), contextList3.get(0).getLeftWords());
			assertEquals("term", contextList3.get(0).getTerm());
			assertEquals(Arrays.asList(".", "in", "this", "short", "text"), contextList3.get(0).getRightWords());
			assertEquals("document", contextList1.get(0).getFile());
			assertEquals("document", contextList3.get(0).getFile());
			assertEquals("term", contextList1.get(0).getTerm());
			assertEquals("term", contextList3.get(0).getTerm());
			assertEquals("term", contextList3.get(0).getTerm());
	}
	
	@Test
	public void complexSearch_getContextTest() throws Exception {

		// terms shall be found even if enclosed by characters removed by the analyzer, e.g. punctuation
		// e.g., when "ALLBUS." is found as term, all occurrences of "ALLBUS." or "ALLBUS" or "ALLBUS," etc. are to be found
		assertEquals(29, testContexts("FOOBAR", "FOOBAR").size());
		assertEquals(28, testContexts("term", "term").size());
		assertEquals(28, testContexts(".term.", "term").size());
		assertEquals(0, testContexts("terma", "terma").size());
		// same behaviour is expected for phrases
		assertEquals(29, testContexts("the FOOBAR", "\"the FOOBAR\"").size());
		assertEquals(28, testContexts("the term,", "\"the term\"").size());
		assertEquals(28, testContexts(".the term.", "\"the term\"").size());
		assertEquals(0, testContexts("the terma", "\"the term\"").size());
		assertEquals(28, testContexts("the. term?!", "\"the term\"").size());
		assertEquals(0, testContexts("the...term?!", "\"the term\"").size());
		List<TextualReference> contextListA = testContexts("the term", "\"the term\"");
		assertEquals("Hallo, please try to find the term in this short text snippet.", contextListA.get(0).toString());
		assertEquals("please try to find . the term . in this short text", contextListA.get(1).toString());
		// ...and for wildcard phrase queries
		// this query should find all test sentences except those having "_" as term
		// ("_" should not be indexed by analyzer, thus there should be no word to match the wildcard)
		assertEquals(100-14, testContexts("", "\"to find the * in\"").size());
	}

	private List<TextualReference> testContexts(String searchTerm, String searchQuery) throws Exception {

		Execution exec = new Execution();
        exec.setAlgorithm(SearchTermPosition.class);
        exec.setSearchTerm(searchTerm);
		exec.setSearchQuery(searchQuery);
        exec.setInputFiles(uris);
        Algorithm algo = exec.instantiateAlgorithm(dataStoreClient, fileResolver);
        algo.run();

		ArrayList<TextualReference> contextList = new ArrayList<TextualReference>();
		for (String uri : exec.getTextualReferences()) {
			contextList.add(dataStoreClient.get(TextualReference.class, uri));
		}
		return contextList;
		
	}

}