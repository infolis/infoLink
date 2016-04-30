package io.github.infolis.infolink.patternLearner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.util.RegexUtils;

/**
 * 
 * @author kata
 *
 */
public class StandardPatternInducerTest {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(StandardPatternInducerTest.class);
	
	@Test
	public void testInduce() {
		StandardPatternInducer inducer = new StandardPatternInducer();
		Double[] thresholds = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		
		TextualReference ref = new TextualReference("15757 41727 5743 10877 10014 30850 Sozialstaatssurvey/", "ALLBUS", " .", "textfile", "pattern", "mentionsReference");
		List<InfolisPattern> patterns = inducer.induce(ref, thresholds);
		assertEquals("\"Sozialstaatssurvey\\\\\\/* .\"", patterns.get(0).getLuceneQuery());
		assertEquals("\"Sozialstaatssurvey\\\\\\/* .\"", patterns.get(5).getLuceneQuery());
		
		TextualReference ref0 = new TextualReference("this is a ref 1998 1999 2000 ", "ALLBUS", " dataset .", "textfile", "pattern", "mentionsReference");
		patterns = inducer.induce(ref0, thresholds);
		assertEquals("\"dataset\"", patterns.get(0).getLuceneQuery());
		assertEquals("\"dataset\"", patterns.get(3).getLuceneQuery());
		assertEquals("\"ref * * * * dataset\"", patterns.get(5).getLuceneQuery());
		
		TextualReference ref1 = new TextualReference("this is a ref to the 2000 ", "ALLBUS", " dataset .", "textfile", "pattern", "mentionsReference");
		patterns = inducer.induce(ref1, thresholds);
		assertEquals("\"dataset\"", patterns.get(0).getLuceneQuery());
		assertEquals(new HashSet<String>(Arrays.asList("2000", "dataset")), patterns.get(0).getWords());
		assertEquals(RegexUtils.complexNumericInfoRegex + "\\s" + RegexUtils.studyRegex_ngram + "\\s\\Qdataset\\E", patterns.get(0).getPatternRegex());
		assertEquals(null, patterns.get(2).getLuceneQuery());
		assertEquals(null, patterns.get(2).getPatternRegex());
		assertEquals("\"to the * * dataset\"", patterns.get(3).getLuceneQuery());
		assertEquals("\\Qto\\E\\s\\Qthe\\E\\s" + RegexUtils.complexNumericInfoRegex + "\\s" + RegexUtils.studyRegex_ngram +"\\s\\Qdataset\\E", patterns.get(3).getPatternRegex());
		
		TextualReference ref2 = new TextualReference("dies ist eine Referenz auf den 2000er ", "ALLBUS", "-Datensatz .", "textfile", "pattern", "mentionsReference");
		patterns = inducer.induce(ref2, thresholds);
		assertEquals("\"*er *\\\\\\-Datensatz\"", patterns.get(0).getLuceneQuery());
		assertEquals(RegexUtils.complexNumericInfoRegex + "\\Qer\\E\\s" + RegexUtils.studyRegex_ngram + "\\s?\\Q-Datensatz\\E", patterns.get(0).getPatternRegex());
		
		TextualReference ref3 = new TextualReference("dies ist eine Referenz auf den 2000er Wohlfahrtssurvey/", "ALLBUS", "-Datensatz .", "textfile", "pattern", "mentionsReference");
		patterns = inducer.induce(ref3, thresholds);
		assertEquals("\"Wohlfahrtssurvey\\\\\\/*\\\\\\-Datensatz\"", patterns.get(0).getLuceneQuery());
		assertEquals("\\QWohlfahrtssurvey/\\E\\s?" + RegexUtils.studyRegex_ngram + "\\s?\\Q-Datensatz\\E", patterns.get(0).getPatternRegex());
		
		String text = "dies ist eine Referenz auf den 2000er Wohlfahrtssurvey/ALLBUS-Datensatz .";
		Pattern p = Pattern.compile(patterns.get(0).getPatternRegex());
		Matcher m = p.matcher(text);
		boolean matchFound = false;
		while (m.find()) {
			matchFound = true;
			assertEquals("Wohlfahrtssurvey/ALLBUS-Datensatz", m.group());
		}
		assertTrue(matchFound);
		
		p = Pattern.compile("(.*?" + System.getProperty("line.separator") + "+)?.*?[-—\\s/]" + Pattern.quote("ALLBUS") + "[-—\\s/].*(" + System.getProperty("line.separator") + "+.*)?");
		m = p.matcher(text);
		matchFound = false;
		while (m.find()) {
			matchFound = true;
			assertEquals(text, m.group());
		}
		assertTrue(matchFound);
	}
}