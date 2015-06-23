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
	}
	

}
