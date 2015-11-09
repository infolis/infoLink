package io.github.infolis.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.regex.Pattern;

import org.junit.Test;


public class RegexUtilsTest {

	@Test
	public void testGetContextMinerYearPatterns() throws Exception {
		Pattern pat = RegexUtils.getContextMinerYearPatterns()[0];
		assertThat(pat, is(not(nullValue())));
		assertThat(pat.matcher("1995").matches(), is(true));
		assertThat(pat.matcher("1995-1998").matches(), is(true));
		assertThat(pat.matcher("1995 bis 1998").matches(), is(true));
		assertThat(pat.matcher("1995 to 1998").matches(), is(true));
		assertThat(pat.matcher("1995       till '98").matches(), is(true));
		
		assertThat(pat.matcher("NaN").matches(), is(false));
		assertThat(pat.matcher("(1998)").matches(), is(false));
	}

	@Test
	public void normalizeQueryTest() {
		assertEquals("term", RegexUtils.normalizeQuery("term", true));
		assertEquals("term", RegexUtils.normalizeQuery("term,", true));
		assertEquals("term", RegexUtils.normalizeQuery(".term.", true));
		assertEquals("terma", RegexUtils.normalizeQuery("terma", true));
		
		assertEquals("\"the term\"", RegexUtils.normalizeQuery("the term", true));
		assertEquals("\"the term\"", RegexUtils.normalizeQuery("the term,", true));
		assertEquals("\"the term\"", RegexUtils.normalizeQuery(".the term.", true));
		assertEquals("\"the term\"", RegexUtils.normalizeQuery("the. term.", true));
	}
	
	@Test
	public void testIsStopword() {
		assertTrue(RegexUtils.isStopword("the"));
		assertTrue(RegexUtils.isStopword("thethe"));
		assertTrue(RegexUtils.isStopword("tothe"));
		assertTrue(RegexUtils.isStopword("e"));
		assertTrue(RegexUtils.isStopword("."));
		assertTrue(RegexUtils.isStopword(".the"));
		assertTrue(RegexUtils.isStopword("142"));
		assertTrue(RegexUtils.isStopword("142."));
		assertFalse(RegexUtils.isStopword("term"));
		assertFalse(RegexUtils.isStopword("theterm"));
		assertFalse(RegexUtils.isStopword("B142"));
		assertFalse(RegexUtils.isStopword("Daten"));
		assertTrue(RegexUtils.isStopword("für"));
	}
	
	@Test
	public void testNormalizeAndEscapeRegex() {
		assertEquals("\\Q\\E" + RegexUtils.percentRegex + "\\Q\\E", RegexUtils.normalizeAndEscapeRegex("2%"));
		assertEquals("\\Q\\E" + RegexUtils.numberRegex + "\\Q\\E", RegexUtils.normalizeAndEscapeRegex("2"));
		assertEquals("\\Q\\E" + RegexUtils.yearRegex + "\\Q\\E", RegexUtils.normalizeAndEscapeRegex("2000"));
	}
	

}
