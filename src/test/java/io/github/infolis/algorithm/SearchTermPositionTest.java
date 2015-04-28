package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;
import io.github.infolis.infolink.luceneIndexing.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.StudyContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class SearchTermPositionTest extends InfolisBaseTest{
	
	Logger log = LoggerFactory.getLogger(SearchTermPositionTest.class);

	String testString1 = "Please try to find the term in this short text snippet.";
	String testString2 = "Please try to find the _ in this short text snippet.";
	String testString3 = "Please try to find the . term . in this short text snippet.";
	
	String testString4 = "Hallo, please try to find the term in this short text snippet. Thank you.";
	String testString5 = "Hallo, please try to find the _ in this short text snippet. Thank you.";
	String testString6 = "Hallo, please try to find .the term. in this short text snippet. Thank you.";
	List<String> uris = new ArrayList<>();
	
	public SearchTermPositionTest() throws Exception {
		for (InfolisFile file : createTestFiles()) {
            uris.add(file.getUri());
		}
		log.debug("FOOO {} ", uris);
	}

	@Test
	public void getContextTest() throws IOException {
		// minimum context size is 5
		
			List<StudyContext> contextList1 = SearchTermPosition.getContexts("document", "term", testString1); 
			List<StudyContext> contextList2 = SearchTermPosition.getContexts("document", "term", testString2); 
			List<StudyContext> contextList3 = SearchTermPosition.getContexts("document", "term", testString3);
//			assertEquals(1,contextList1.size());
//			assertEquals(0,contextList2.size());
//			assertEquals(1,contextList3.size());
//			assertEquals(testString1, contextList1.get(0).toString());
			assertEquals(1, contextList3.size());
			assertEquals(Arrays.asList("try", "to", "find", "the", "."), contextList3.get(0).getLeftWords());
			assertEquals("term", contextList3.get(0).getTerm());
			assertEquals(Arrays.asList(".", "in", "this", "short", "text"), contextList3.get(0).getRightWords());
			assertEquals("document", contextList1.get(0).getFile());
			assertEquals("document", contextList3.get(0).getFile());
//			assertEquals("term", contextList1.get(0).getTerm());
//			assertEquals("term", contextList3.get(0).getTerm());
//			assertEquals("term", contextList3.get(0).getTerm());
//			assertNull(contextList1.get(0).getPattern());
//			assertNull(contextList3.get(0).getPattern());
	}
	
	@Test
	public void complexSearch_getContextTest() throws Exception {

		// terms shall be found even if enclosed by characters removed by the analyzer, e.g. punctuation
		// e.g., when "ALLBUS." is found as term, all occurrences of "ALLBUS." or "ALLBUS" or "ALLBUS," etc. are to be found
		// assertEquals("please try to find the term in this short text snippet.", testContexts("term", "term", 2).get(0).toString());
		testContexts("term,", "term,", 2);
//		testContexts(".term.", ".term.", 2);
//		testContexts("terma", "terma", 0);

		List<StudyContext> contextListA = testContexts("the term", "\"the term\"", 2);
		assertEquals("Hallo, please try to find the term in this short text snippet.", contextListA.get(0).toString());
//		assertEquals("please try to find . the term . in this short text", contextListA.get(1).toString());
//		testContexts("the term,", "\"the term,\"", 2);   
//		testContexts(".the term.", "\".the term.\"", 2); 
//		testContexts("the terma", "\"the term\"", 0);    
//		testContexts("the. term.", "\"the. term.\"", 2); 
	}

	private List<StudyContext> testContexts(String searchTerm, String searchQuery, int expectedSize) throws Exception {
		Execution exec = new Execution();
		exec.setAlgorithm(SearchTermPosition.class);
		exec.setSearchTerm(searchTerm);
		exec.setSearchQuery(searchQuery);
		for (String uri : uris) {
			exec.getInputFiles().add(uri);
		}
		System.out.println(uris.size());
		
		Algorithm algo = exec.instantiateAlgorithm(localClient, tempFileResolver);
		algo.run();

//		assertEquals(expectedSize, exec.getStudyContexts().size());
		ArrayList<StudyContext> contextList = new ArrayList<StudyContext>();
		for (String uri : exec.getStudyContexts()) {
			contextList.add(localClient.get(StudyContext.class, uri));
		}
		return contextList;
	}

	@Ignore
	public void testNormalizeQuery() throws Exception {
		throw new RuntimeException("not yet implemented");
	}
}