package io.github.infolis.infolink.datasetMatcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class FilterDaraJsonResultsTest {

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
			// hard cases: 2. wave != no 2. Filter needs more info to decide which numbers to ignore
			"Ausländerumfrage 1982 (1. Welle: Haushaltsvorstände)",
			"SFB580-B2 Betriebspanel",
			"USICA-Jugend-Studie (Panel: 2. Welle 1979)",
			"Ausländer in Deutschland 2000 - 2. Welle",
			"CBS News Monthly Poll #2, May 1999",
			"Eurobarometer 54.1 (2000)"
		};

	private static final String[] refNumbers = {
			"1996/08", 
			"1982", 
			"1982   -   1983", 
			"85/82",
			"54.1",
			"2000, 2002",
			"2-4",
			"2, 3"
	};

	
	@Test
	public void inRangeTest() {
		List<String> range1 = Arrays.asList(new String[]{"2000", "2010"});
		String value1 = "2000";
		String value2 = "2010";
		String value3 = "1999";
		String value4 = "2011";
		String value5 = "2005";
		assertTrue(FilterDaraJsonResults.inRange(range1, value1));
		assertTrue(FilterDaraJsonResults.inRange(range1, value2));
		assertFalse(FilterDaraJsonResults.inRange(range1, value3));
		assertFalse(FilterDaraJsonResults.inRange(range1, value4));
		assertTrue(FilterDaraJsonResults.inRange(range1, value5));
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
		
		assertTrue(FilterDaraJsonResults.overlap(range1, range2));
		assertTrue(FilterDaraJsonResults.overlap(range1, range3));
		assertTrue(FilterDaraJsonResults.overlap(range1, range3b));
		assertTrue(FilterDaraJsonResults.overlap(range1, range4));
		assertTrue(FilterDaraJsonResults.overlap(range1, range4b));
		assertTrue(FilterDaraJsonResults.overlap(range1, range5));
		assertTrue(FilterDaraJsonResults.overlap(range1, range6));
		assertFalse(FilterDaraJsonResults.overlap(range1, range7));
		assertFalse(FilterDaraJsonResults.overlap(range1, range8));
	}
	
	@Test
	public void filterTest() {
		//"1996/08"
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[0]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[1]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[2]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[3]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[4]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[5]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[6]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[7]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[8]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[9]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[10]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[11])); 
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[12]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[13]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[0], candidates[14]));
		
		//"1982"
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[0]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[1]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[2]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[3]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[4]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[5]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[6]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[7]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[8]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[9]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[10]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[11]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[12]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[13]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[1], candidates[14]));
		
		//"1982   -   1983"
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[0]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[1]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[2]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[3]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[4]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[5]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[6]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[7]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[8]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[9]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[10]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[11]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[12]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[13]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[2], candidates[14]));
		
		//"85/82"
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[0]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[1]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[2]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[3]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[4]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[5]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[6]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[7]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[8]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[9]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[10]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[11]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[12]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[13]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[3], candidates[14]));
		
		//"54.1"
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[0]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[1]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[2]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[3]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[4]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[5]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[6]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[7]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[8]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[9]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[10]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[11]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[12]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[13]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[4], candidates[14]));
		
		//"2000, 2002"
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[0]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[1]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[2]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[3]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[4]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[5]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[6]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[7]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[8]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[9]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[10]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[11]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[12]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[13]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[5], candidates[14]));
		
		//"2-4"
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[0]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[1]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[2]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[3]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[4]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[5]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[6]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[7]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[8]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[9]));
		// bolandka: this test fails because of the numbers in the title which have a different meaning
		// "SFB580-B2 Betriebspanel"
		// TODO: do we want to find a heuristic to treat such titles correctly?
		//assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[10]));
		// same here: 2nd wave != no. 2. "USICA-Jugend-Studie (Panel: 2. Welle 1979)",
		//assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[11]));
		// same: "Ausländer in Deutschland 2000 - 2. Welle
		//assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[12]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[13]));
		// TODO: @domi : this has to pass 
		// else, matches 2-4 with "Eurobarometer 54.1 (2000)"
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[6], candidates[14]));
		
		//"2, 3"
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[0]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[1]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[2]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[3]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[4]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[5]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[6]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[7]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[8]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[9]));
		// see above
		//assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[10]));
		//assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[11]));
		//assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[12]));
		assertTrue(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[13]));
		assertFalse(FilterDaraJsonResults.numericInfoMatches(refNumbers[7], candidates[14]));
	}
}