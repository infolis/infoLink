package io.github.infolis.infolink.datasetMatcher;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import io.github.infolis.InfolisBaseTest;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaraWebMatcherTest  extends InfolisBaseTest {
	
	Logger log = LoggerFactory.getLogger(DaraWebMatcherTest.class);
	String datasetName = "Studierendensurvey";
	// use cache for test - database in da|ra may change
	String queryCachePath = getClass().getResource("/queryCache.csv").getPath();
	String urlListFile = null;
	String searchInterface = "http://www.da-ra.de/dara/study/web_search_show";
	Map<String, String> expectedOutput = new HashMap<>();
	
	public DaraWebMatcherTest() {
		expectedOutput.put("10.4232/1.4263", "Studiensituation und studentische Orientierungen 2006/07 (Studierenden-Survey)");
		expectedOutput.put("10.4232/1.4344", "Studiensituation und studentische Orientierungen 2003/04 (Studierenden-Survey)");
		expectedOutput.put("10.4232/1.11059", "Studiensituation und studentische Orientierungen 2009/10 (Studierenden-Survey)");
		expectedOutput.put("10.4232/1.1884", "Studiensituation und studentische Orientierungen 1982/83 (Studierenden-Survey)");
		expectedOutput.put("10.4232/1.1885", "Studiensituation und studentische Orientierungen 1984/85 (Studierenden-Survey)");
		expectedOutput.put("10.4232/1.3130", "Studiensituation und studentische Orientierungen 1992/93 (Studierenden-Survey)");
		expectedOutput.put("10.4232/1.3131", "Studiensituation und studentische Orientierungen 1994/95 (Studierenden-Survey)");
		expectedOutput.put("10.4232/1.2416", "Studiensituation und studentische Orientierungen 1986/87 (Studierenden-Survey)");
		expectedOutput.put("10.4232/1.2417", "Studiensituation und studentische Orientierungen 1989/90 (Studierenden-Survey)");
		expectedOutput.put("10.4232/1.5126", "Studiensituation und studentische Orientierungen 2012/13 (Studierenden-Survey)");
		expectedOutput.put("10.4232/1.4208", "Studiensituation und studentische Orientierungen 2000/01 (Studierenden-Survey)");
		expectedOutput.put("10.4232/1.3511", "Studiensituation und studentische Orientierungen 1997/98 (Studierenden-Survey)");
	}
	
	@Test
	public void testMatch() {
		DaraWebMatcher matcher = new DaraWebMatcher(searchInterface, this.queryCachePath, this.urlListFile);
		assertEquals(expectedOutput, matcher.match(this.datasetName));
	}
}