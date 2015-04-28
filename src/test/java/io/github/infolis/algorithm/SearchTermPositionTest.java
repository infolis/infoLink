package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.infolink.luceneIndexing.Indexer;
import io.github.infolis.model.Execution;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.InfolisFileUtils;
import io.github.infolis.util.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class SearchTermPositionTest {
	
	private final static DataStoreClient client = DataStoreClientFactory.local();
	Logger log = LoggerFactory.getLogger(SearchTermPositionTest.class);

	String indexDir; 
	String testCorpus = "testCorpus";
	String filename = "filename";
	String testDocument1 = testCorpus + File.separator + "testDocument1";
	String testDocument2 = testCorpus + File.separator + "testDocument2";
	
	String testString1 = "Please try to find the term in this short text snippet.";
	String testString2 = "Please try to find the _ in this short text snippet.";
	String testString3 = "Please try to find the . term . in this short text snippet.";
	
	String testString4 = "Hallo, please try to find the term in this short text snippet. Thank you.";
	String testString5 = "Hallo, please try to find the _ in this short text snippet. Thank you.";
	String testString6 = "Hallo, please try to find .the term. in this short text snippet. Thank you.";

	@Before
	public void setUp() throws IOException {
		indexDir = Files.createTempDirectory("infolis-test-").toString();
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.deleteDirectory(new File(indexDir));
	}

	@Test
	public void getContextTest() {
		// minimum context size is 5
		
		try { 
			List<StudyContext> contextList1 = SearchTermPosition.getContexts("document", "term", testString1); 
//			List<StudyContext> contextList2 = SearchTermPosition.getContexts("document", "term", testString2); 
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
		catch(IOException ioe) { ioe.printStackTrace(); }
	}
	
	//TODO: delete files after testing
	public void createInputFiles() {
		try { 
			InfolisFileUtils.writeToFile(new File(testDocument1), "UTF-8", testString4, false);
			InfolisFileUtils.writeToFile(new File(testDocument2), "UTF-8", testString5, false);
			InfolisFileUtils.writeToFile(new File(testDocument2), "UTF-8", testString6, true);
			
			Indexer.main(new String[]{ testCorpus, indexDir });
		}
		catch(IOException ioe) { ioe.printStackTrace(); System.exit(1); }
	}
	
	@Test
	public void normalizeQueryTest() {
		assertEquals("term", SearchTermPosition.normalizeQuery("term", true));
		assertEquals("term", SearchTermPosition.normalizeQuery("term,", true));
		assertEquals("term", SearchTermPosition.normalizeQuery(".term.", true));
		assertEquals("terma", SearchTermPosition.normalizeQuery("terma", true));
		
		assertEquals("\"the term\"", SearchTermPosition.normalizeQuery("the term", true));
		assertEquals("\"the term\"", SearchTermPosition.normalizeQuery("the term,", true));
		assertEquals("\"the term\"", SearchTermPosition.normalizeQuery(".the term.", true));
		assertEquals("\"the term\"", SearchTermPosition.normalizeQuery("the. term.", true));
	}
	
	@Test
	public void complexSearch_getContextTest() throws Exception {
		createInputFiles();

		// terms shall be found even if enclosed by characters removed by the analyzer, e.g. punctuation
		// e.g., when "ALLBUS." is found as term, all occurrences of "ALLBUS." or "ALLBUS" or "ALLBUS," etc. are to be found
		// assertEquals("please try to find the term in this short text snippet.", testContexts("term", "term", 2).get(0).toString());
//		testContexts("term,", "term,", 2);
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
//		exec.setIndexDirectory(indexDir);
		exec.setFirstOutputFile(Files.createTempFile("infolis-", ".txt").toString());
		exec.instantiateAlgorithm(DataStoreStrategy.LOCAL).run();
		log.debug(SerializationUtils.toJSON(exec));
		assertEquals(expectedSize, exec.getStudyContexts().size());
		ArrayList<StudyContext> contextList = new ArrayList<StudyContext>();
		for (String uri : exec.getStudyContexts()) {
			contextList.add(client.get(StudyContext.class, uri));
		}
		return contextList;
	}

	@Ignore
	public void testNormalizeQuery() throws Exception {
		throw new RuntimeException("not yet implemented");
	}
}