package io.github.infolis.infolink.datasetMatcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class FilterTest {

	private static final String[] candidates = {
			"Studierendensurvey 2000/01", 
			"Studierendensurvey 2001",
			"German Social Survey (ALLBUS) Cumulative File, 1980, 1982, 1984, 1986",
			"German Social Survey (ALLBUS) Cumulative File, 1980-1992",
			"German Social Survey (ALLBUS) Cumulative File, 1980, 1992",
			"German Social Survey (ALLBUS) Cumulative File, 1980, 1996",
			"German Social Survey (ALLBUS) Cumulative File, 1980 - 1996",
			"German Social Survey (ALLBUS) Cumulative File, 1980 1996",
			"German Social Survey (ALLBUS) Cumulative File, 1980-1990",
			"Ausländerumfrage 1982 (1. Welle: Haushaltsvorstände)",
			"SFB580-B2 Betriebspanel",
			"USICA-Jugend-Studie (Panel: 2. Welle 1979)",
			"Ausländer in Deutschland 2000 - 2. Welle",
			"CBS News Monthly Poll #2, May 1999"};

	private static final String[] refNumbers = {
			"1996/08", 
			"1982", 
			"1982   -   1983", 
			"85/01",
			"53.1",
			"2-4",
			"2, 3",
			"2000, 2002"};
	/*		List<String> range1b = Arrays.asList(new String[]{"00", "10"});
	List<String> range1c = Arrays.asList(new String[]{"2000", "10"});
	List<String> range1d = Arrays.asList(new String[]{"00", "2010"});*/
/*		String value6 = "05";
	String value7 = "00";
	String value8 = "11";*/
	
	@Test
	public void inRangeTest() {
		List<String> range1 = Arrays.asList(new String[]{"2000", "2010"});
		String value1 = "2000";
		String value2 = "2010";
		String value3 = "1999";
		String value4 = "2011";
		String value5 = "2005";
		assertTrue(Filter.inRange(range1, value1));
		assertTrue(Filter.inRange(range1, value2));
		assertFalse(Filter.inRange(range1, value3));
		assertFalse(Filter.inRange(range1, value4));
		assertTrue(Filter.inRange(range1, value5));
	}
	
	@Test
	public void OverlapTest() {
		List<String> range1 = Arrays.asList(new String[]{"2000", "2010"});
		
		List<String> range2 = Arrays.asList(new String[]{"2000", "2010"});
		List<String> range3 = Arrays.asList(new String[]{"1900", "2010"});
		List<String> range3b = Arrays.asList(new String[]{"1900", "2009"});
		List<String> range4 = Arrays.asList(new String[]{"2000", "2020"});
		List<String> range4b = Arrays.asList(new String[]{"2001", "2020"});
		List<String> range5 = Arrays.asList(new String[]{"1900", "2020"});
		List<String> range6 = Arrays.asList(new String[]{"2005", "2006"});
		List<String> range7 = Arrays.asList(new String[]{"1900", "1990"});
		List<String> range8 = Arrays.asList(new String[]{"2011", "2020"});
		
		assertTrue(Filter.overlap(range1, range2));
		assertTrue(Filter.overlap(range1, range3));
		assertTrue(Filter.overlap(range1, range3b));
		assertTrue(Filter.overlap(range1, range4));
		assertTrue(Filter.overlap(range1, range4b));
		assertTrue(Filter.overlap(range1, range5));
		assertTrue(Filter.overlap(range1, range6));
		assertFalse(Filter.overlap(range1, range7));
		assertFalse(Filter.overlap(range1, range8));
	}
	
	//TODO: add some more assertions
	@Test
	public void filterTest() {
		assertFalse(Filter.numericInfoMatches(refNumbers[0], candidates[0]));
		assertFalse(Filter.numericInfoMatches(refNumbers[0], candidates[1]));
		assertFalse(Filter.numericInfoMatches(refNumbers[0], candidates[2]));
		assertFalse(Filter.numericInfoMatches(refNumbers[0], candidates[3]));
		assertFalse(Filter.numericInfoMatches(refNumbers[0], candidates[4]));
		assertTrue(Filter.numericInfoMatches(refNumbers[0], candidates[5]));
		assertTrue(Filter.numericInfoMatches(refNumbers[0], candidates[6]));
		assertTrue(Filter.numericInfoMatches(refNumbers[0], candidates[7]));
		assertFalse(Filter.numericInfoMatches(refNumbers[0], candidates[8]));
		assertFalse(Filter.numericInfoMatches(refNumbers[0], candidates[9]));
		assertFalse(Filter.numericInfoMatches(refNumbers[0], candidates[10]));
		assertFalse(Filter.numericInfoMatches(refNumbers[0], candidates[11])); 
		assertFalse(Filter.numericInfoMatches(refNumbers[0], candidates[12]));
		assertFalse(Filter.numericInfoMatches(refNumbers[0], candidates[13]));
		
		assertFalse(Filter.numericInfoMatches(refNumbers[1], candidates[0]));
		assertFalse(Filter.numericInfoMatches(refNumbers[1], candidates[1]));
		assertTrue(Filter.numericInfoMatches(refNumbers[1], candidates[2]));
		assertTrue(Filter.numericInfoMatches(refNumbers[1], candidates[3]));
		assertFalse(Filter.numericInfoMatches(refNumbers[1], candidates[4]));
		assertFalse(Filter.numericInfoMatches(refNumbers[1], candidates[5]));
		assertTrue(Filter.numericInfoMatches(refNumbers[1], candidates[6]));
		assertFalse(Filter.numericInfoMatches(refNumbers[1], candidates[7]));
		assertTrue(Filter.numericInfoMatches(refNumbers[1], candidates[8]));
		assertTrue(Filter.numericInfoMatches(refNumbers[1], candidates[9]));
		assertFalse(Filter.numericInfoMatches(refNumbers[1], candidates[10]));
		assertFalse(Filter.numericInfoMatches(refNumbers[1], candidates[11]));
		assertFalse(Filter.numericInfoMatches(refNumbers[1], candidates[12]));
		assertFalse(Filter.numericInfoMatches(refNumbers[1], candidates[13]));
		
		assertFalse(Filter.numericInfoMatches(refNumbers[2], candidates[0]));
		assertFalse(Filter.numericInfoMatches(refNumbers[2], candidates[1]));
		assertTrue(Filter.numericInfoMatches(refNumbers[2], candidates[2]));
		assertTrue(Filter.numericInfoMatches(refNumbers[2], candidates[3]));
		assertFalse(Filter.numericInfoMatches(refNumbers[2], candidates[4]));
		assertFalse(Filter.numericInfoMatches(refNumbers[2], candidates[5]));
		assertTrue(Filter.numericInfoMatches(refNumbers[2], candidates[6]));
		assertFalse(Filter.numericInfoMatches(refNumbers[2], candidates[7]));
		assertTrue(Filter.numericInfoMatches(refNumbers[2], candidates[8]));
		assertTrue(Filter.numericInfoMatches(refNumbers[2], candidates[9]));
		assertFalse(Filter.numericInfoMatches(refNumbers[2], candidates[10]));
		assertFalse(Filter.numericInfoMatches(refNumbers[2], candidates[11]));
		assertFalse(Filter.numericInfoMatches(refNumbers[2], candidates[12]));
		assertFalse(Filter.numericInfoMatches(refNumbers[2], candidates[13]));
	}
}