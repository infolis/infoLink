package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.InfolisConfig;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.EvaluationUtils;
import io.github.infolis.util.SerializationUtils;

/**
 *
 * @author kata
 *
 */
public class BibliographyExtractorTest extends InfolisBaseTest {

	private static final Logger log = LoggerFactory.getLogger(BibliographyExtractorTest.class);
	File inputDir;
	File goldDir;
	List<String> inputFiles;
	List<String> goldFiles;

	public BibliographyExtractorTest() throws URISyntaxException, IOException {
		inputDir = getResource("/bibExtractor/test/");
		goldDir = getResource("/bibExtractor/gold/");
		inputFiles = postFiles(inputDir, "text/plain");
		goldFiles = postFiles(goldDir, "text/plain");
	}

	private File getResource(String resName) {
    	return new File(getClass().getResource(resName).getFile());
    }

	public List<String> postFiles(File dir, String mimetype) throws IOException {
        List<InfolisFile> infolisFiles = new ArrayList<>();
        for (File file : dir.listFiles()) {
            InfolisFile infolisFile = new InfolisFile();
            InputStream inputStream = Files.newInputStream(Paths.get(file.getAbsolutePath()));
            byte[] bytes = IOUtils.toByteArray(inputStream);
            infolisFile.setMd5(SerializationUtils.getHexMd5(bytes));
            infolisFile.setFileName(file.toString());
            infolisFile.setMediaType(mimetype);
            infolisFile.setFileStatus("AVAILABLE");
            infolisFiles.add(infolisFile);
        }
        return dataStoreClient.post(InfolisFile.class, infolisFiles);
    }

	@Test
	public void testBibExtractor() throws URISyntaxException, IOException {
		Execution exec = new Execution();
		exec.setInputFiles(inputFiles);
		exec.setAlgorithm(BibliographyExtractor.class);
		exec.instantiateAlgorithm(dataStoreClient, dataStoreClient, FileResolverFactory.local(), fileResolver).run();

		log.debug("output files: " + exec.getOutputFiles());
		assertEquals(exec.getInputFiles().size(), exec.getOutputFiles().size());
		assertTrue(Paths.get(exec.getOutputDirectory()).startsWith(InfolisConfig.getTmpFilePath()));
		InfolisFile outFile = dataStoreClient.get(InfolisFile.class, exec.getFirstOutputFile());
		assertTrue(Paths.get(outFile.getFileName()).getParent().startsWith(InfolisConfig.getTmpFilePath()));
	}

	private Map<String, String> getGoldTexts() throws IOException {
		Map<String, String> txtBibless = new HashMap<>();
		for (String uri : goldFiles) {
			InfolisFile infolisFile = dataStoreClient.get(InfolisFile.class, uri);
			File file = new File(infolisFile.getFileName());
			String text = FileUtils.readFileToString(file, "utf-8");
			txtBibless.put(mapFilename(infolisFile), text);
		}
		return txtBibless;
	}

	private String mapFilename(InfolisFile file) {
		Path p = Paths.get(file.getFileName());
		return p.getFileName().toString();
	}

	private Map<String, String> getExtractedTexts() throws IOException {
		BibliographyExtractor bibExtractor = new BibliographyExtractor(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
		Map<String, String> txtBibless = new HashMap<>();
		for (String uri : inputFiles) {
			InfolisFile infolisFile = dataStoreClient.get(InfolisFile.class, uri);
			File file = new File(infolisFile.getFileName());
			String text = FileUtils.readFileToString(file, "utf-8");
			String bibless = bibExtractor.removeBibliography(bibExtractor.tokenizeSections(text, 10));
			txtBibless.put(mapFilename(infolisFile), bibless);
		}
		return txtBibless;
	}

	@Test
	public void testRemoveBibliography() throws IOException {
		Map<String, String> output = getExtractedTexts();
		Map<String, String> gold = getGoldTexts();
		double precision_avg = 0;
		double recall_avg = 0;
		BibliographyExtractor bibExtractor = new BibliographyExtractor(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
		for (String textfile : gold.keySet()) {
			String goldText = gold.get(textfile);
			String outputText = output.get(textfile);
			log.debug("length of goldText: " + goldText.length());
			log.debug("length of outputText: " + outputText.length());
			Collection<String> goldSentences = bibExtractor.tokenizeSections(goldText, 1);
			Collection<String> outputSentences = bibExtractor.tokenizeSections(outputText, 1);
			double precision = EvaluationUtils.getPrecision(goldSentences, outputSentences);
			double recall = EvaluationUtils.getRecall(goldSentences, outputSentences);
			log.debug("precision: " + precision + " (" + textfile + ")");
			log.debug("recall: " + recall + " (" + textfile + ")");
			precision_avg += precision;
			recall_avg += recall;
		}
		precision_avg = precision_avg / (double) gold.size();
		recall_avg = recall_avg / (double) gold.size();
		log.debug("Average precision: " + precision_avg);
		log.debug("Average recall: " + recall_avg);
		double f1 = EvaluationUtils.getF1Measure(precision_avg, recall_avg);
		log.debug("F1-measure: " + f1);
	}
}