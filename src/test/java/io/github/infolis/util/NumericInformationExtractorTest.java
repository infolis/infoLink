package io.github.infolis.util;

import static org.junit.Assert.assertEquals;

import io.github.infolis.model.TextualReference;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

/**
 * 
 * @author kata
 *
 */
public class NumericInformationExtractorTest {
	
	@Test
	public void testGetBestNumericInfo() {
		List<String> numericInfo = Arrays.asList("56.1", "2000", "'85");
		assertEquals("2000", NumericInformationExtractor.getBestNumericInfo(numericInfo));
		numericInfo = Arrays.asList("1996/08");
		assertEquals("1996/08", NumericInformationExtractor.getBestNumericInfo(numericInfo));
	}
	
	@Test
	public void testExtractNumericInfo() {
		TextualReference reference = new TextualReference("In this snippet, the reference", "Eurobarometer 56.1 2000", "is to be extracted as", "document", "pattern","ref");
		List<String> numericInfo = NumericInformationExtractor.extractNumericInfo(reference);
		assertEquals(new HashSet<>(Arrays.asList("56.1", "2000")), new HashSet<>(numericInfo));
	}
	
	@Test
	public void testGetNumericInfo() {
		List<String> numericInfo = NumericInformationExtractor.getNumericInfo("Eurobarometer 56.1 2000");
		assertEquals(new HashSet<>(Arrays.asList("56.1", "2000")), new HashSet<>(numericInfo));
	}
}