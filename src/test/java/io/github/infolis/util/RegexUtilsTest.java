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
import java.util.regex.Pattern;

import org.junit.Test;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.algorithm.LuceneSearcher;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;

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
		assertThat(pat.matcher("1995-1998").matches(), is(true));
		assertThat(pat.matcher("1995 bis 1998").matches(), is(true));
		assertThat(pat.matcher("1995 to 1998").matches(), is(true));
		assertThat(pat.matcher("1995       till '98").matches(), is(true));

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
		String lucenePat = "\"\\\\\\(Datenbasis\\\\\\: * ,\"";
		assertEquals(lucenePat, "\"" + RegexUtils.normalizeAndEscapeRegex_lucene(pat) + "\"");
		
		Execution exec = new Execution();
		exec.setAlgorithm(LuceneSearcher.class);
		exec.setSearchTerm(null);
		exec.setSearchQuery(lucenePat);
		exec.setPhraseSlop(0);
		exec.setInputFiles(uris);
		exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		assertEquals(Arrays.asList(uris.get(0)), exec.getMatchingFiles());
		
		exec = new Execution();
		exec.setAlgorithm(LuceneSearcher.class);
		exec.setSearchTerm(null);
		lucenePat = "\"Datenbasis\\\\\\: * ,\"";
		exec.setSearchQuery(lucenePat);
		exec.setPhraseSlop(0);
		exec.setInputFiles(uris);
		exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		assertEquals(Arrays.asList(uris.get(1), uris.get(2)), exec.getMatchingFiles());
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
	
	//TODO may change if different values for ignoreStudy are set in the config
	@Test
	public void ignoreStudyTest() {
		assertTrue(RegexUtils.ignoreStudy("eigene Erhebung"));
		assertTrue(RegexUtils.ignoreStudy("eigene Erhebungen"));
		assertTrue(RegexUtils.ignoreStudy("eigene Berechnung"));
		assertTrue(RegexUtils.ignoreStudy("eigene Berechnungen"));
		assertTrue(RegexUtils.ignoreStudy("eigene Darstellung"));
		assertTrue(RegexUtils.ignoreStudy("eigene Darstellungen"));
		assertFalse(RegexUtils.ignoreStudy("ALLBUS"));
		assertFalse(RegexUtils.ignoreStudy("eigene Berechnung; ALLBUS"));
		assertFalse(RegexUtils.ignoreStudy("ALLBUS; eigene Berechnung"));
	}


}
