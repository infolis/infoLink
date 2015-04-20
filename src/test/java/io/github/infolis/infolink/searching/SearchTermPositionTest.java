package io.github.infolis.infolink.searching;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import io.github.infolis.infolink.luceneIndexing.Indexer;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.InfolisFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.queryParser.ParseException;
import org.junit.Ignore;
import org.junit.Test;



public class SearchTermPositionTest
{
	String indexPath = "indexPath";
	String testCorpus = "testCorpus";
	String filename = "filename";
	String testDocument1 = testCorpus + File.separator + "testDocument1";
	String testDocument2 = testCorpus + File.separator + "testDocument2";
	
	String testString1 = "Please try to find the term in this short text snippet.";
	String testString2 = "Please try to find the _ in this short text snippet.";
	String testString3 = "Please try to find the .term. in this short text snippet.";
	
	String testString4 = "Hallo, please try to find the term in this short text snippet. Thank you.";
	String testString5 = "Hallo, please try to find the _ in this short text snippet. Thank you.";
	String testString6 = "Hallo, please try to find .the term. in this short text snippet. Thank you.";

	@Test
	public void getContextTest() {
		// minimum context size is 5
		
		SearchTermPosition stp = new SearchTermPosition(indexPath, filename, "term", "query");
		try { 
			List<StudyContext> contextList1 = stp.getContext("document", "term", testString1); 
			List<StudyContext> contextList2 = stp.getContext("document", "term", testString2); 
			List<StudyContext> contextList3 = stp.getContext("document", "term", testString3);
			assertEquals(1,contextList1.size());
			assertEquals(0,contextList2.size());
			assertEquals(1,contextList3.size());
			assertEquals(testString1, contextList1.get(0).toString());
			assertEquals("try to find the . term . in this short text", contextList3.get(0).toString());
			assertEquals("document", contextList1.get(0).getDocument());
			assertEquals("document", contextList3.get(0).getDocument());
			assertEquals("term", contextList1.get(0).getTerm());
			assertEquals("term", contextList3.get(0).getTerm());
			assertNull(contextList1.get(0).getPattern());
			assertNull(contextList3.get(0).getPattern());
		}
		catch(IOException ioe) { ioe.printStackTrace(); }
	}
	
	//TODO: delete files after testing
	public void createInputFiles() {
		try { 
			InfolisFileUtils.writeToFile(new File(testDocument1), "UTF-8", testString4, false);
			InfolisFileUtils.writeToFile(new File(testDocument2), "UTF-8", testString5, false);
			InfolisFileUtils.writeToFile(new File(testDocument2), "UTF-8", testString6, true);
			
			Indexer.main(new String[]{ testCorpus, indexPath });
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
	public void complexSearch_getContextTest() {
		createInputFiles();
		// terms shall be found even if enclosed by characters removed by the analyzer, e.g. punctuation
		// e.g., when "ALLBUS." is found as term, all occurrences of "ALLBUS." or "ALLBUS" or "ALLBUS," etc. are to be found
		SearchTermPosition stp = new SearchTermPosition(indexPath, filename, "term", "term");//"\"term\""
		SearchTermPosition stp2 = new SearchTermPosition(indexPath, filename, "term,", "term,");
		SearchTermPosition stp3 = new SearchTermPosition(indexPath, filename, ".term.", ".term.");
		SearchTermPosition stp4 = new SearchTermPosition(indexPath, filename, "terma", "terma");
		
		SearchTermPosition stpA = new SearchTermPosition(indexPath, filename, "the term", "\"the term\"");
		SearchTermPosition stpB = new SearchTermPosition(indexPath, filename, "the term,", "\"the term,\"");
		SearchTermPosition stpC = new SearchTermPosition(indexPath, filename, ".the term.", "\".the term.\"");
		SearchTermPosition stpD = new SearchTermPosition(indexPath, filename, "the terma", "\"the term\"");
		SearchTermPosition stpE = new SearchTermPosition(indexPath, filename, "the. term.", "\"the. term.\"");
		try { 
			List<StudyContext> contextList = stp.complexSearch_getContexts(); 
			assertEquals(2, contextList.size());
			List<StudyContext> contextList2 = stp2.complexSearch_getContexts(); 
			assertEquals(2, contextList2.size());
			List<StudyContext> contextList3 = stp3.complexSearch_getContexts(); 
			assertEquals(2, contextList3.size());
			List<StudyContext> contextList4 = stp4.complexSearch_getContexts(); 
			assertEquals(0, contextList4.size());
			
			List<StudyContext> contextListA = stpA.complexSearch_getContexts(); 
			assertEquals(2, contextListA.size());
			List<StudyContext> contextListB = stpB.complexSearch_getContexts(); 
			assertEquals(2, contextListB.size());
			List<StudyContext> contextListC = stpC.complexSearch_getContexts(); 
			assertEquals(2, contextListC.size());
			List<StudyContext> contextListD = stpD.complexSearch_getContexts(); 
			assertEquals(0, contextListD.size());
			List<StudyContext> contextListE = stpE.complexSearch_getContexts(); 
			assertEquals(2, contextListE.size());
			
			assertEquals("please try to find the term in this short text snippet.", contextList.get(0).toString());
			assertEquals("Hallo, please try to find the term in this short text snippet.", contextListA.get(0).toString());
			assertEquals("please try to find . the term . in this short text", contextListA.get(1).toString());
		}
		catch (IOException ioe) { ioe.printStackTrace(); }
		catch (ParseException pe) { pe.printStackTrace(); }
		
	}

	@Ignore
	public void testNormalizeQuery() throws Exception {
		throw new RuntimeException("not yet implemented");
	}
}