package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.BootstrapStrategy;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.InfolisFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaraLinkerTest extends InfolisBaseTest {
	Logger log = LoggerFactory.getLogger(DaraLinkerTest.class);
	Map<String, Set<String>> expectedLinks;
	private List<String> uris = new ArrayList<>();
	TextualReference[] testContexts = {
			new TextualReference("In this snippet, the reference", "ALLBUS 2000", "is to be extracted as", "document", "pattern","ref"),
			new TextualReference("the reference to the 2000", "ALLBUS", "is to be extracted as", "document", "pattern","ref"),
			new TextualReference("In this snippet, the reference", "ALLBUS", "2000 is to be extracted", "document", "pattern","ref"),

			new TextualReference("In this snippet, the reference", "Eurobarometer 56.1", "is to be extracted as", "document", "pattern","ref"),
			new TextualReference("the reference to the 56.1", "Eurobarometer", "is to be extracted as", "document", "pattern","ref"),
			new TextualReference("In this snippet, the reference", "Eurobarometer", "56.1 is to be extracted", "document", "pattern","ref"),

			new TextualReference("In this snippet, the reference", "Eurobarometer 56.1 2000", "is to be extracted as", "document", "pattern","ref"),
			new TextualReference("reference to the 56.1 2000", "Eurobarometer", "is to be extracted as", "document", "pattern","ref"),
			new TextualReference("In this snippet, the reference", "Eurobarometer", "56.1 2000 is to be", "document", "pattern","ref"),

			new TextualReference("In this snippet, the reference", "ALLBUS 1996/08", "is to be extracted as", "document", "pattern","ref"),
			new TextualReference("the reference to the 1982   -   1983", "ALLBUS", "is to be extracted as", "document", "pattern","ref"),
			new TextualReference("In this snippet, the reference", "ALLBUS", "85/01 is to be extracted", "document", "pattern","ref"),

			new TextualReference("the reference to the 1982 till 1983", "ALLBUS", "is to be extracted as", "document", "pattern","ref"),
			new TextualReference("the reference to the 1982 to 1983", "ALLBUS", "is to be extracted as", "document", "pattern","ref"),
			new TextualReference("the reference to the 1982 bis 1983", "ALLBUS", "is to be extracted as", "document", "pattern","ref"),
			new TextualReference("the reference to the 1982 und 1983", "ALLBUS", "is to be extracted as", "document", "pattern","ref"),

			new TextualReference("the reference to the 2nd wave of the", "2000 Eurobarometer", "56.1 is to be extracted as", "document", "pattern","ref"),
			new TextualReference("the reference to the 2nd wave of the", "Eurobarometer", "2000 is to be extracted as", "document", "pattern","ref")
	};

	// studies/datasets found in generated contexts:
	// Studierendensurvey 1990 x 3 <- 10.4232/1.2417
	// Studierendensurvey '86 x 3 <- 10.4232/1.2416
	// 2010 Studierendensurvey x 3 <- 10.4232/1.11059
	// Studierendensurvey x 6 <- references without year specification match all
	// candidates

	// Studiensituation und studentische Orientierungen 1982/83
	// (Studierenden-Survey) 10.4232/1.1884
	// Studiensituation und studentische Orientierungen 1984/85
	// (Studierenden-Survey) 10.4232/1.1885
	// Studiensituation und studentische Orientierungen 1986/87
	// (Studierenden-Survey) 10.4232/1.2416
	// Studiensituation und studentische Orientierungen 1989/90
	// (Studierenden-Survey) 10.4232/1.2417
	// Studiensituation und studentische Orientierungen 1992/93
	// (Studierenden-Survey) 10.4232/1.3130
	// Studiensituation und studentische Orientierungen 1994/95
	// (Studierenden-Survey) 10.4232/1.3131
	// Studiensituation und studentische Orientierungen 1997/98
	// (Studierenden-Survey) 10.4232/1.3511
	// Studiensituation und studentische Orientierungen 2000/01
	// (Studierenden-Survey) 10.4232/1.4208
	// Studiensituation und studentische Orientierungen 2003/04
	// (Studierenden-Survey) 10.4232/1.4344
	// Studiensituation und studentische Orientierungen 2006/07
	// (Studierenden-Survey) 10.4232/1.4263
	// Studiensituation und studentische Orientierungen 2009/10
	// (Studierenden-Survey) 10.4232/1.11059
	// Studiensituation und studentische Orientierungen 2012/13
	// (Studierenden-Survey) 10.4232/1.5126
	public DaraLinkerTest() {
		expectedLinks = new HashMap<>();
		expectedLinks.put("Studierendensurvey null", new HashSet<>(Arrays.asList("10.4232/1.1884", "10.4232/1.1885",
				"10.4232/1.2416", "10.4232/1.2417", "10.4232/1.3130", "10.4232/1.3131", "10.4232/1.3511",
				"10.4232/1.4208", "10.4232/1.4344", "10.4232/1.4263", "10.4232/1.11059", "10.4232/1.5126")));
		expectedLinks.put("Studierendensurvey '86", new HashSet<>(Arrays.asList("10.4232/1.2416")));
		expectedLinks.put("Studierendensurvey 1990", new HashSet<>(Arrays.asList("10.4232/1.2417")));
		expectedLinks.put("Studierendensurvey 2010", new HashSet<>(Arrays.asList("10.4232/1.11059")));

	}
	protected String[] testStrings = {
			"Hallo, please try to find the FOOBAR in this short text snippet. Thank you.",
			"Hallo, please try to find the Studierendensurvey 1990 in this short text snippet. Thank you.",
			"Hallo, please try to find the Studierendensurvey in this short text snippet. Thank you.",
			"Hallo, please try to find the Studierendensurvey '86 in this short text snippet. Thank you.",
			"Hallo, please try to find the 2010 Studierendensurvey in this short text snippet. Thank you.",
			"Hallo, please try to find .the Studierendensurvey. in this short text snippet. Thank you.",
			"Hallo, please try to find the FOOBAR in this short text snippet. Thank you."
	};

	protected List<InfolisFile> createTestFiles(int nrFiles) throws Exception {
		String[] testStrings = {
				"Hallo, please try to find the FOOBAR in this short text snippet. Thank you.",
				"Hallo, please try to find the R2 in this short text snippet. Thank you.",
				"Hallo, please try to find the D2 in this short text snippet. Thank you.",
				"Hallo, please try to find the term in this short text snippet. Thank you.",
				"Hallo, please try to find the _ in this short text snippet. Thank you.",
				"Hallo, please try to find .the term. in this short text snippet. Thank you.",
				"Hallo, please try to find the FOOBAR in this short text snippet. Thank you."
		};
		return createTestTextFiles(nrFiles, testStrings);
	}
	public void prepareTestFiles() throws IOException, Exception {
		for (InfolisFile file : createTestFiles(20)) {
			uris.add(file.getUri());
			String str = FileUtils.readFileToString(new File(file.getFileName()));
			log.debug(str);
		}
	}

	// TODO: use Learner.learn when implemented properly
	public List<String> generateTestContexts() {
		Execution execution = new Execution();
		execution.setAlgorithm(FrequencyBasedBootstrapping.class);
		execution.getSeeds().add("Studierendensurvey");
		execution.setInputFiles(uris);
		execution.setReliabilityThreshold(0.0);
		execution.setBootstrapStrategy(BootstrapStrategy.mergeAll);
		execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		return execution.getTextualReferences();
	}

	@Ignore
	@Test
	public void linkContextTest() throws Exception {
        Assume.assumeNotNull(System.getProperty("gesisRemoteTest"));
		prepareTestFiles();
		List<String> contextURIs = generateTestContexts();
		Execution execution = new Execution();
		execution.setAlgorithm(DaraLinker.class);
		execution.setTextualReferences(contextURIs);
		execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		List<EntityLink> links = dataStoreClient.get(EntityLink.class, execution.getLinks());
		Map<String, Set<String>> generatedLinks = new HashMap<>();
		for (EntityLink link : links) {
			// log.debug(link.toString());
			Entity toEntity = dataStoreClient.get(Entity.class, link.getToEntity());
			String key = toEntity.getName();
			//String key = link.getAltName() + " " + link.getVersion();
			Set<String> dois = new HashSet<>();
			if (generatedLinks.containsKey(key)) {
				dois = generatedLinks.get(key);
			}
			dois.add(link.getToEntity());
			generatedLinks.put(key, dois);
		}
		assertEquals(expectedLinks, generatedLinks);
	}

	@Test
	public void extractStudyTest() {
		DaraLinker linker = new DaraLinker(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
		assertEquals("2000", linker.extractStudy(testContexts[0]).getNumber());
		assertEquals("2000", linker.extractStudy(testContexts[1]).getNumber());
		assertEquals("2000", linker.extractStudy(testContexts[2]).getNumber());
		assertEquals("56.1", linker.extractStudy(testContexts[3]).getNumber());
		assertEquals("56.1", linker.extractStudy(testContexts[4]).getNumber());
		assertEquals("56.1", linker.extractStudy(testContexts[5]).getNumber());
		assertEquals("56.1", linker.extractStudy(testContexts[6]).getNumber());
		assertEquals("56.1", linker.extractStudy(testContexts[7]).getNumber());
		assertEquals("56.1", linker.extractStudy(testContexts[8]).getNumber());
		assertEquals("1996/08", linker.extractStudy(testContexts[9]).getNumber());
		assertEquals("1982   -   1983", linker.extractStudy(testContexts[10]).getNumber());
		assertEquals("85/01", linker.extractStudy(testContexts[11]).getNumber());
		assertEquals("1982 till 1983", linker.extractStudy(testContexts[12]).getNumber());
		assertEquals("1982 to 1983", linker.extractStudy(testContexts[13]).getNumber());
		assertEquals("1982 bis 1983", linker.extractStudy(testContexts[14]).getNumber());
		assertEquals("1982 und 1983", linker.extractStudy(testContexts[15]).getNumber());
		assertEquals("2000", linker.extractStudy(testContexts[16]).getNumber());
		assertEquals("2000", linker.extractStudy(testContexts[17]).getNumber());
	}

}
