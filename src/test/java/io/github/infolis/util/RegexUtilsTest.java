package io.github.infolis.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.algorithm.LuceneSearcher;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;

/**
 * 
 * @author kata
 *
 */
public class RegexUtilsTest extends InfolisBaseTest {

	@Test
	public void testComplexNumericInfoRegex() throws Exception {
		Pattern pat = Pattern.compile(RegexUtils.complexNumericInfoRegex);
		assertThat(pat, is(not(nullValue())));
		assertThat(pat.matcher("1995").matches(), is(true));
		assertThat(pat.matcher("1995 bis 1998").matches(), is(true));
		assertThat(pat.matcher("1995-1998").matches(), is(true));		
		assertThat(pat.matcher("1995 to 1998").matches(), is(true));
		assertThat(pat.matcher("1995       till '98").matches(), is(true));
		
		Matcher m = pat.matcher("30850");
		assertThat(m.find(), is(true));
		assertEquals("30850", m.group());
		
		assertThat(pat.matcher("NaN").matches(), is(false));
		assertThat(pat.matcher("(1998)").matches(), is(false));	
	}

	@Test
	public void normalizeQueryTest() throws Exception {
		assertEquals("term", RegexUtils.normalizeQuery("term", true));
		assertEquals("terma", RegexUtils.normalizeQuery("terma", true));
		assertEquals("\"the term\"", RegexUtils.normalizeQuery("the term", true));
		assertEquals("\\\\\\:term", RegexUtils.normalizeQuery(":term", true));
		assertEquals("\"the\\\\\\: term\"", RegexUtils.normalizeQuery("the: term", true));
		
		String[] testStrings = {
				"Hallo , please try to find the (Datenbasis: 1990 , this short snippet .",
				"Hallo , please try to find the Datenbasis: 1990 , this short snippet .",
				"Hallo , please try to find the ( Datenbasis: 1990 , this short snippet .",
		};
		List<String> uris = new ArrayList<>();
		for (InfolisFile file : createTestTextFiles(3, testStrings)) uris.add(file.getUri());
		
		String pat = "(Datenbasis: 2000 ,";
		String lucenePat = "\"\\\\\\(Datenbasis\\\\\\: * *\"";
		assertEquals(lucenePat, "\"" + RegexUtils.normalizeAndEscapeRegex_lucene(pat) + "\"");
		InfolisPattern p = new InfolisPattern(lucenePat);
		dataStoreClient.post(InfolisPattern.class, p);
		
		Execution exec = new Execution();
		exec.setAlgorithm(LuceneSearcher.class);
		exec.setSearchTerm(null);
		exec.setPatterns(Arrays.asList(p.getUri()));
		exec.setPhraseSlop(0);
		exec.setInputFiles(uris);
		exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		assertEquals(Arrays.asList(uris.get(0)), exec.getOutputFiles());
		
		exec = new Execution();
		exec.setAlgorithm(LuceneSearcher.class);
		exec.setSearchTerm(null);
		lucenePat = "\"Datenbasis\\\\\\: * ,\"";
		p = new InfolisPattern(lucenePat);
		dataStoreClient.post(InfolisPattern.class, p);
		exec.setPatterns(Arrays.asList(p.getUri()));
		exec.setPhraseSlop(0);
		exec.setInputFiles(uris);
		exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		assertEquals(Arrays.asList(uris.get(1), uris.get(2)), exec.getOutputFiles());
	}

	@Test
	public void testIsStopword() {
		assertTrue(RegexUtils.isStopword("the"));
		assertTrue(RegexUtils.isStopword("thethe"));
		assertTrue(RegexUtils.isStopword("tothe"));
		assertTrue(RegexUtils.isStopword("e"));
		assertTrue(RegexUtils.isStopword("."));
		assertTrue(RegexUtils.isStopword("--"));
		assertTrue(RegexUtils.isStopword("-LRB-"));
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
		assertEquals(RegexUtils.percentRegex, RegexUtils.normalizeAndEscapeRegex("2%"));
		assertEquals(RegexUtils.complexNumericInfoRegex, RegexUtils.normalizeAndEscapeRegex("2"));
		assertEquals(RegexUtils.complexNumericInfoRegex, RegexUtils.normalizeAndEscapeRegex("2000"));
	}
	
	//TODO may change if different values for ignoreStudy are set in the config
	@Test
	public void ignoreStudyTest() {
		assertTrue(RegexUtils.ignoreStudy("eigene Berechnung"));
		assertTrue(RegexUtils.ignoreStudy("eigene Berechnungen"));
		assertTrue(RegexUtils.ignoreStudy("eigene Darstellung"));
		assertTrue(RegexUtils.ignoreStudy("eigene Darstellungen"));
		assertFalse(RegexUtils.ignoreStudy("ALLBUS"));
		assertFalse(RegexUtils.ignoreStudy("eigene Berechnung; ALLBUS"));
		assertFalse(RegexUtils.ignoreStudy("ALLBUS; eigene Berechnung"));
	}
	
	@Test
	public void testNormalizeAndEscapeRegex_lucene() {
		assertEquals("*", RegexUtils.normalizeAndEscapeRegex_lucene("30850"));
		assertEquals("*", RegexUtils.normalizeAndEscapeRegex_lucene("1836"));
		assertEquals("*", RegexUtils.normalizeAndEscapeRegex_lucene("1990 until 1992"));
		assertEquals("*", RegexUtils.normalizeAndEscapeRegex_lucene("1990 & 1992"));
		assertEquals("*", RegexUtils.normalizeAndEscapeRegex_lucene("1990 - 1992"));
		assertEquals("*", RegexUtils.normalizeAndEscapeRegex_lucene("1990-1992"));
	}


}
