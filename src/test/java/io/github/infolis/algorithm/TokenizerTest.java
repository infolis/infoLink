package io.github.infolis.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;
import opennlp.tools.util.InvalidFormatException;

/**
 * 
 * @author kata
 *
 */
public class TokenizerTest extends InfolisBaseTest {
	
	private static final Logger log = LoggerFactory.getLogger(TokenizerTest.class);
	
	List<InfolisFile> testFiles;
	String[] testStrings = {
			"On the one hand, the granularity (what is the smallest element of research data in need of description?) and the possible, aggregating intermediary steps vary widely." + System.getProperty("line.separator") + "On the other-hand, ...",
			"Funktioniert der \nTokenizer auch gut für z.B. deutsch?"
			};
	List<String> uris = new ArrayList<>();
	
	public TokenizerTest() throws Exception {
		testFiles = createTestTextFiles(2, testStrings);
		for (InfolisFile file : testFiles) {
            uris.add(file.getUri());
		}
	}
	
	@Test
	public void testStanfordTokenize() throws InvalidFormatException, IOException{
		Execution exec = new Execution();
		exec.setInputFiles(uris);
		exec.setAlgorithm(TokenizerStanford.class);
		exec.setTokenizeNLs(true);
		exec.setPtb3Escaping(true);
		exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		List<InfolisFile> outFiles = dataStoreClient.get(InfolisFile.class, exec.getOutputFiles());
		for (InfolisFile outFile : outFiles) {
			InputStream is = fileResolver.openInputStream(outFile);
			String content = IOUtils.toString(is);
			log.debug("output stanford: " + content);
		}
	}
	
	// TODO path to model as param
	// TODO download script for model...
	@Test
	public void testOpenNLPTokenize() throws InvalidFormatException, IOException {
		Execution exec = new Execution();
		exec.setInputFiles(uris);
		exec.setAlgorithm(TokenizerOpenNLP.class);
		exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		List<InfolisFile> outFiles = dataStoreClient.get(InfolisFile.class, exec.getOutputFiles());
		for (InfolisFile outFile : outFiles) {
			InputStream is = fileResolver.openInputStream(outFile);
			String content = IOUtils.toString(is);
			log.debug("output openNLP: " + content);
		}
	}
	
}