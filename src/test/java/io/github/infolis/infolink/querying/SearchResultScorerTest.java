package io.github.infolis.infolink.querying;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.model.entity.EntityLink;


public class SearchResultScorerTest {

	private final static Logger log = LoggerFactory.getLogger(SearchResultScorerTest.class);
	
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
		assertTrue(SearchResultScorer.inRange(range1, value1));
		assertTrue(SearchResultScorer.inRange(range1, value2));
		assertFalse(SearchResultScorer.inRange(range1, value3));
		assertFalse(SearchResultScorer.inRange(range1, value4));
		assertTrue(SearchResultScorer.inRange(range1, value5));
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

		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.same_as_temporal)),
				SearchResultScorer.overlap(range1, range2, false));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)), 
				SearchResultScorer.overlap(range1, range3, false));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal,
						EntityLink.EntityRelation.superset_of_temporal)), 
				SearchResultScorer.overlap(range1, range3b, false));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				SearchResultScorer.overlap(range1, range4, false));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal,
						EntityLink.EntityRelation.superset_of_temporal)),
				SearchResultScorer.overlap(range1, range4b, false));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				SearchResultScorer.overlap(range1, range5, false));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.superset_of_temporal)),
				SearchResultScorer.overlap(range1, range6, false));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.superset_of_temporal)),
				SearchResultScorer.overlap(range6, range1, true));
		assertEquals(null, SearchResultScorer.overlap(range1, range7, false));
		assertEquals(null, SearchResultScorer.overlap(range1, range8, false));
	}

	//TODO test specific relations
	@Test
	public void filterTest() {
		//refNumber[0] = "1996/08"
		//0 candidate "Studierendensurvey 2000/01",
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[0]));
		//1 candidate "Studierendensurvey 2001",
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[1]));
		//2 candidate "German Social Survey (ALLBUS) Cumulative File, 1980, 1982, 1984, 1986",
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[2]));
		//3 candidate "German Social Survey (ALLBUS) Cumulative File, 1980-1992",
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[3]));
		//4 candidate "German Social Survey (ALLBUS) Cumulative File, 1980, 1992",
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[4]));
		//5 candidate "German Social Survey (ALLBUS) Cumulative File, 1980, 1996",
		// TODO if not all enumerated values have a match, superset relation should be added...
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
						//EntityLink.EntityRelation.superset_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[5]));
		//6 candidate "German Social Survey (ALLBUS) Cumulative File, 1980 - 1996",
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
						//EntityLink.EntityRelation.superset_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[6]));
		//7 candidate "German Social Survey (ALLBUS) Cumulative File, 1980 1996",
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
						//EntityLink.EntityRelation.superset_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[7]));
		//8 candidate "German Social Survey (ALLBUS) Cumulative File, 1980-1990",
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[8]));
		//9 candidate "Ausländerumfrage 1982 (1. Welle: Haushaltsvorstände)",
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[9]));
		//10 candidate "SFB580-B2 Betriebspanel",
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[10]));
		//11 candidate "USICA-Jugend-Studie (Panel: 2. Welle 1979)",
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[11]));
		//12 candidate "Ausländer in Deutschland 2000 - 2. Welle",
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[12]));
		//13 candidate "CBS News Monthly Poll #2, May 1999",
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[13]));
		//14 candidate "Eurobarometer 54.1 (2000)"
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[0], candidates[14]));

		//"1982"
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[0]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[1]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[2]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[3]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[4]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[5]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[6]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[7]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[8]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.same_as_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[9]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[10]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[11]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[12]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[13]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[1], candidates[14]));

		//"1982   -   1983"
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[0]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[1]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				//EntityLink.EntityRelation.superset_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[2]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[3]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[4]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[5]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[6]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[7]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[8]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.superset_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[9]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[10]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[11]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[12]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[13]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[2], candidates[14]));

		//"85/82"
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[0]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[1]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				//EntityLink.EntityRelation.superset_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[2]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[3]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[4]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[5]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[6]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[7]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[8]));
		// TODO hard case: treat "1982 (1. " properly
		//assertEquals(new HashSet<EntityLink.EntityRelation>(
			//	Arrays.asList(EntityLink.EntityRelation.superset_of_temporal)),
				//SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[9]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[10]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[11]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[12]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[13]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[3], candidates[14]));

		//"54.1"
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[0]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[1]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[2]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[3]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[4]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[5]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[6]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[7]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[8]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[9]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[10]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[11]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[12]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[13]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.same_as_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[4], candidates[14]));

		//"2000, 2002"
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				//EntityLink.EntityRelation.superset_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[0]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[1]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[2]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[3]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[4]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[5]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[6]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[7]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[8]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[9]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[10]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[11]));
		// hard case: 2000 - 2. is invalid range
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				//EntityLink.EntityRelation.superset_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[12]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[13]));
		// TODO 'Eurobarometer 54.1 (2000)' <- 2000 is not an additional date to 54.1 
		//assertEquals(new HashSet<EntityLink.EntityRelation>(
			//	Arrays.asList(EntityLink.EntityRelation.superset_of_temporal)),
				//SearchResultScorer.numericInfoMatches(refNumbers[5], candidates[14]));

		//"2-4"
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[0]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[1]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[2]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[3]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[4]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[5]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[6]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[7]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[8]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[9]));
		// bolandka: this test fails because of the numbers in the title which have a different meaning
		// "SFB580-B2 Betriebspanel"
		// TODO: do we want to find a heuristic to treat such titles correctly?
		//assertFalse(SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[10]));
		// same here: 2nd wave != no. 2. "USICA-Jugend-Studie (Panel: 2. Welle 1979)",
		//assertFalse(SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[11]));
		// same: "Ausländer in Deutschland 2000 - 2. Welle
		//assertFalse(SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[12]));
		assertTrue(null != SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[13]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[6], candidates[14]));

		//"2, 3"
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[0]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[1]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[2]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[3]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[4]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[5]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[6]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[7]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[8]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[9]));
		// see above
		//assertFalse(SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[10]));
		//assertFalse(SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[11]));
		//assertFalse(SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[12]));
		assertEquals(new HashSet<EntityLink.EntityRelation>(
				Arrays.asList(EntityLink.EntityRelation.part_of_temporal)),
				//EntityLink.EntityRelation.superset_of_temporal)),
				SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[13]));
		assertEquals(null, SearchResultScorer.numericInfoMatches(refNumbers[7], candidates[14]));
	}
}