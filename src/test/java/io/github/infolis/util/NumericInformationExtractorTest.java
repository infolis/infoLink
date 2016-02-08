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
	
	@Test
	public void testExtractDOI() {
		String doi1 = "10.4232/1.5126";
		TextualReference ref1 = new TextualReference("left context", doi1, "right context", "textFile", "pattern", "mentionsReference");
		assertEquals(doi1, NumericInformationExtractor.extractDOI(ref1));
		assertEquals(doi1, NumericInformationExtractor.extractRegex(RegexUtils.doiRegex, doi1));
		
		String doi2 = "10.5684/soep.iab-soep-mig.2013";
		TextualReference ref2 = new TextualReference(doi2, "term", "right context", "textFile", "pattern", "mentionsReference");
		assertEquals(doi2, NumericInformationExtractor.extractRegex(RegexUtils.doiRegex, doi2));
		assertEquals(doi2, NumericInformationExtractor.extractDOI(ref2));
		
		String doi3 = "10.5684/soep.v31i";
		TextualReference ref3 = new TextualReference("left context", "term", doi3, "textFile", "pattern", "mentionsReference");
		assertEquals(doi3, NumericInformationExtractor.extractRegex(RegexUtils.doiRegex, doi3));
		assertEquals(doi3, NumericInformationExtractor.extractDOI(ref3));
		
		String noDoi1 = "10,4232/1.5126";
		TextualReference ref4 = new TextualReference("left context", noDoi1, "right context", "textFile", "pattern", "mentionsReference");
		assertEquals("", NumericInformationExtractor.extractRegex(RegexUtils.doiRegex, noDoi1));
		assertEquals("", NumericInformationExtractor.extractDOI(ref4));
		
		String noDoi2 = "10.4232\\1.5126";
		TextualReference ref5 = new TextualReference("left context", noDoi2, "right context", "textFile", "pattern", "mentionsReference");
		assertEquals("", NumericInformationExtractor.extractRegex(RegexUtils.doiRegex, noDoi2));
		assertEquals("", NumericInformationExtractor.extractDOI(ref5));
	}
	
	@Test
	public void testExtractURL() {
		String url1 = "http://www.da-ra.de/";
		assertEquals(url1, NumericInformationExtractor.extractRegex(RegexUtils.urlRegex, url1));
		TextualReference ref1 = new TextualReference("left context", url1, "right context", "textFile", "pattern", "mentionsReference");
		assertEquals(url1, NumericInformationExtractor.extractURL(ref1));
		
		String url2 = "www.da-ra.de";
		assertEquals(url2, NumericInformationExtractor.extractRegex(RegexUtils.urlRegex, url2));
		TextualReference ref2 = new TextualReference(url2, "term", "right context", "textFile", "pattern", "mentionsReference");
		assertEquals(url2, NumericInformationExtractor.extractURL(ref2));
		
		String url3 = "www.gesis.org/en/research/";
		assertEquals(url3, NumericInformationExtractor.extractRegex(RegexUtils.urlRegex, url3));
		TextualReference ref3 = new TextualReference("left context", "term", url3, "textFile", "pattern", "mentionsReference");
		assertEquals(url3, NumericInformationExtractor.extractURL(ref3));
		
		String url4 = "http://www.gesis.org";
		assertEquals(url4, NumericInformationExtractor.extractRegex(RegexUtils.urlRegex, url4));
		TextualReference ref4 = new TextualReference("left context", url4, "right context", "textFile", "pattern", "mentionsReference");
		assertEquals(url4, NumericInformationExtractor.extractURL(ref4));
		
		String noUrl = ".da-ra.de";
		assertEquals("", NumericInformationExtractor.extractRegex(RegexUtils.urlRegex, noUrl));
		TextualReference ref5 = new TextualReference("left context", noUrl, "right context", "textFile", "pattern", "mentionsReference");
		assertEquals("", NumericInformationExtractor.extractURL(ref5));
	}
}