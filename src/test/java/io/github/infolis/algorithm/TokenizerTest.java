package io.github.infolis.algorithm;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.entity.InfolisFile;

/**
 * 
 * @author kata
 *
 */
public class TokenizerTest extends InfolisBaseTest {
	
	private static final Logger log = LoggerFactory.getLogger(TokenizerTest.class);
	
	List<InfolisFile> testFiles;
	
	public TokenizerTest() throws Exception {
		String[] testStrings = {"On the one hand, the granularity (what is the smallest element of research data in need of description?) and the possible, aggregating intermediary steps vary widely."};
		testFiles = createTestTextFiles(1, testStrings);
	}
	
	@Test
	public void testGetTokenizedSentences() {
		List<String> sentences = Tokenizer.getTokenizedSentences(testFiles.get(0).getFileName());
		for (String sentence : sentences) { 
			log.debug("Sentence: " + sentence); 
		}
	}
	
	@Test
	public void testPrintCoreLabels() throws FileNotFoundException {
		Tokenizer.printCoreLabels(testFiles.get(0).getFileName());
	}
	
}