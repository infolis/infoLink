package io.github.infolis.infolink.searching;

import static org.junit.Assert.*;

import org.junit.Test;

import io.github.infolis.infolink.searching.Context;
import io.github.infolis.infolink.searching.Search_Term_Position;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.queryParser.ParseException;



public class Search_Term_PositionTest
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
		
		Search_Term_Position stp = new Search_Term_Position(indexPath, filename, "term", "query");
		try { 
			List<Context> contextList1 = stp.getContext("document", "term", testString1); 
			List<Context> contextList2 = stp.getContext("document", "term", testString2); 
			List<Context> contextList3 = stp.getContext("document", "term", testString3);
			assertEquals(1,contextList1.size());
			assertEquals(0,contextList2.size());
			assertEquals(1,contextList3.size());
			assertEquals(testString1, contextList1.get(0).toString());
			assertEquals("try to find the . term . in this short text", contextList3.get(0).toString());
			assertEquals("document", contextList1.get(0).document);
			assertEquals("document", contextList3.get(0).document);
			assertEquals("term", contextList1.get(0).term);
			assertEquals("term", contextList3.get(0).term);
			assertNull(contextList1.get(0).pattern);
			assertNull(contextList3.get(0).pattern);
		}
		catch(IOException ioe) { ioe.printStackTrace(); }
	}
	
	//TODO: delete files after testing
	public void createInputFiles() {
		try { 
			io.github.infolis.infolink.patternLearner.Util.writeToFile(new File(testDocument1), "UTF-8", testString4, false);
			io.github.infolis.infolink.patternLearner.Util.writeToFile(new File(testDocument2), "UTF-8", testString5, false);
			io.github.infolis.infolink.patternLearner.Util.writeToFile(new File(testDocument2), "UTF-8", testString6, true);
			
			io.github.infolis.infolink.luceneIndexing.Indexer.main(new String[]{ testCorpus, indexPath });
		}
		catch(IOException ioe) { ioe.printStackTrace(); System.exit(1); }
	}
	
	@Test
	public void normalizeQueryTest() {
		assertEquals("term", Search_Term_Position.normalizeQuery("term"));
		assertEquals("term", Search_Term_Position.normalizeQuery("term,"));
		assertEquals("term", Search_Term_Position.normalizeQuery(".term."));
		assertEquals("terma", Search_Term_Position.normalizeQuery("terma"));
		
		assertEquals("\"the term\"", Search_Term_Position.normalizeQuery("the term"));
		assertEquals("\"the term\"", Search_Term_Position.normalizeQuery("the term,"));
		assertEquals("\"the term\"", Search_Term_Position.normalizeQuery(".the term."));
		assertEquals("\"the term\"", Search_Term_Position.normalizeQuery("the. term."));
	}
	
	@Test
	public void complexSearch_getContextTest() {
		createInputFiles();
		// terms shall be found even if enclosed by characters removed by the analyzer, e.g. punctuation
		// e.g., when "ALLBUS." is found as term, all occurrences of "ALLBUS." or "ALLBUS" or "ALLBUS," etc. are to be found
		Search_Term_Position stp = new Search_Term_Position(indexPath, filename, "term", "term");//"\"term\""
		Search_Term_Position stp2 = new Search_Term_Position(indexPath, filename, "term,", "term,");
		Search_Term_Position stp3 = new Search_Term_Position(indexPath, filename, ".term.", ".term.");
		Search_Term_Position stp4 = new Search_Term_Position(indexPath, filename, "terma", "terma");
		
		Search_Term_Position stpA = new Search_Term_Position(indexPath, filename, "the term", "\"the term\"");
		Search_Term_Position stpB = new Search_Term_Position(indexPath, filename, "the term,", "\"the term,\"");
		Search_Term_Position stpC = new Search_Term_Position(indexPath, filename, ".the term.", "\".the term.\"");
		Search_Term_Position stpD = new Search_Term_Position(indexPath, filename, "the terma", "\"the term\"");
		Search_Term_Position stpE = new Search_Term_Position(indexPath, filename, "the. term.", "\"the. term.\"");
		try { 
			List<Context> contextList = stp.complexSearch_getContexts(); 
			assertEquals(2, contextList.size());
			List<Context> contextList2 = stp2.complexSearch_getContexts(); 
			assertEquals(2, contextList2.size());
			List<Context> contextList3 = stp3.complexSearch_getContexts(); 
			assertEquals(2, contextList3.size());
			List<Context> contextList4 = stp4.complexSearch_getContexts(); 
			assertEquals(0, contextList4.size());
			
			List<Context> contextListA = stpA.complexSearch_getContexts(); 
			assertEquals(2, contextListA.size());
			List<Context> contextListB = stpB.complexSearch_getContexts(); 
			assertEquals(2, contextListB.size());
			List<Context> contextListC = stpC.complexSearch_getContexts(); 
			assertEquals(2, contextListC.size());
			List<Context> contextListD = stpD.complexSearch_getContexts(); 
			assertEquals(0, contextListD.size());
			List<Context> contextListE = stpE.complexSearch_getContexts(); 
			assertEquals(2, contextListE.size());
			
			assertEquals("please try to find the term in this short text snippet.", contextList.get(0).toString());
			assertEquals("Hallo, please try to find the term in this short text snippet.", contextListA.get(0).toString());
			assertEquals("please try to find . the term . in this short text", contextListA.get(1).toString());
		}
		catch (IOException ioe) { ioe.printStackTrace(); }
		catch (ParseException pe) { pe.printStackTrace(); }
		
	}
}