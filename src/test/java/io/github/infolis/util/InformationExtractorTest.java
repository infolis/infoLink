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
public class InformationExtractorTest {
	
	@Test
	public void testGetBestNumericInfo() {
		List<String> numericInfo = Arrays.asList("56.1", "2000", "'85");
		assertEquals("2000", InformationExtractor.getBestNumericInfo(numericInfo));
		numericInfo = Arrays.asList("1996/08");
		assertEquals("1996/08", InformationExtractor.getBestNumericInfo(numericInfo));
	}
	
	@Test
	public void testExtractNumericInfo() {
		TextualReference reference = new TextualReference("In this snippet, the reference", "Eurobarometer 56.1 2000", "is to be extracted as", "document", "pattern","ref");
		List<String> numericInfo = InformationExtractor.extractNumericInfo(reference);
		assertEquals(new HashSet<>(Arrays.asList("56.1", "2000")), new HashSet<>(numericInfo));
	}
	
	@Test
	public void testGetNumericInfo() {
		List<String> numericInfo = InformationExtractor.getNumericInfo("Eurobarometer 56.1 2000");
		assertEquals(new HashSet<>(Arrays.asList("56.1", "2000")), new HashSet<>(numericInfo));
	}
	
	@Test
	public void testExtractDOI() {
		String doi1 = "10.4232/1.5126";
		TextualReference ref1 = new TextualReference("left context", doi1, "right context", "textFile", "pattern", "mentionsReference");
		assertEquals(doi1, InformationExtractor.extractDOI(ref1));
		assertEquals(doi1, InformationExtractor.extractRegex(RegexUtils.doiRegex, doi1));
		
		String doi2 = "10.5684/soep.iab-soep-mig.2013";
		TextualReference ref2 = new TextualReference(doi2, "term", "right context", "textFile", "pattern", "mentionsReference");
		assertEquals(doi2, InformationExtractor.extractRegex(RegexUtils.doiRegex, doi2));
		assertEquals(doi2, InformationExtractor.extractDOI(ref2));
		
		String doi3 = "10.5684/soep.v31i";
		TextualReference ref3 = new TextualReference("left context", "term", doi3, "textFile", "pattern", "mentionsReference");
		assertEquals(doi3, InformationExtractor.extractRegex(RegexUtils.doiRegex, doi3));
		assertEquals(doi3, InformationExtractor.extractDOI(ref3));
		
		String noDoi1 = "10,4232/1.5126";
		TextualReference ref4 = new TextualReference("left context", noDoi1, "right context", "textFile", "pattern", "mentionsReference");
		assertEquals("", InformationExtractor.extractRegex(RegexUtils.doiRegex, noDoi1));
		assertEquals("", InformationExtractor.extractDOI(ref4));
		
		String noDoi2 = "10.4232\\1.5126";
		TextualReference ref5 = new TextualReference("left context", noDoi2, "right context", "textFile", "pattern", "mentionsReference");
		assertEquals("", InformationExtractor.extractRegex(RegexUtils.doiRegex, noDoi2));
		assertEquals("", InformationExtractor.extractDOI(ref5));
	}
	
	@Test
	public void testExtractURL() {
		String url1 = "http://www.da-ra.de/";
		assertEquals(url1, InformationExtractor.extractRegex(RegexUtils.urlRegex, url1));
		TextualReference ref1 = new TextualReference("left context", url1, "right context", "textFile", "pattern", "mentionsReference");
		assertEquals(url1, InformationExtractor.extractURL(ref1));
		
		String url2 = "www.da-ra.de";
		assertEquals(url2, InformationExtractor.extractRegex(RegexUtils.urlRegex, url2));
		TextualReference ref2 = new TextualReference(url2, "term", "right context", "textFile", "pattern", "mentionsReference");
		assertEquals(url2, InformationExtractor.extractURL(ref2));
		
		String url3 = "www.gesis.org/en/research/";
		assertEquals(url3, InformationExtractor.extractRegex(RegexUtils.urlRegex, url3));
		TextualReference ref3 = new TextualReference("left context", "term", url3, "textFile", "pattern", "mentionsReference");
		assertEquals(url3, InformationExtractor.extractURL(ref3));
		
		String url4 = "http://www.gesis.org";
		assertEquals(url4, InformationExtractor.extractRegex(RegexUtils.urlRegex, url4));
		TextualReference ref4 = new TextualReference("left context", url4, "right context", "textFile", "pattern", "mentionsReference");
		assertEquals(url4, InformationExtractor.extractURL(ref4));
		
		String noUrl = ".da-ra.de";
		assertEquals("", InformationExtractor.extractRegex(RegexUtils.urlRegex, noUrl));
		TextualReference ref5 = new TextualReference("left context", noUrl, "right context", "textFile", "pattern", "mentionsReference");
		assertEquals("", InformationExtractor.extractURL(ref5));
	}
}