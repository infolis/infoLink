package io.github.infolis.infolink.datasetMatcher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import io.github.infolis.algorithm.FrequencyBasedBootstrapping;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.StudyContext;
import io.github.infolis.model.StudyLink;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class LinkerTest extends BaseTest {
	
	Logger log = LoggerFactory.getLogger(LinkerTest.class);
	private List<String> uris = new ArrayList<>();
	Map<String, String> expectedOutput = new HashMap<>();
	StudyContext[] testContexts = {
			new StudyContext("In this snippet, the reference", "ALLBUS 2000", "is to be extracted as", "document", new InfolisPattern()),
			new StudyContext("the reference to the 2000", "ALLBUS", "is to be extracted as", "document", new InfolisPattern()),
			new StudyContext("In this snippet, the reference", "ALLBUS", "2000 is to be extracted", "document", new InfolisPattern()),
			
			new StudyContext("In this snippet, the reference", "Eurobarometer 56.1", "is to be extracted as", "document", new InfolisPattern()),
			new StudyContext("the reference to the 56.1", "Eurobarometer", "is to be extracted as", "document", new InfolisPattern()),
			new StudyContext("In this snippet, the reference", "Eurobarometer", "56.1 is to be extracted", "document", new InfolisPattern()),
			
			new StudyContext("In this snippet, the reference", "Eurobarometer 56.1 2000", "is to be extracted as", "document", new InfolisPattern()),
			new StudyContext("reference to the 56.1 2000", "Eurobarometer", "is to be extracted as", "document", new InfolisPattern()),
			new StudyContext("In this snippet, the reference", "Eurobarometer", "56.1 2000 is to be", "document", new InfolisPattern()),
	
			new StudyContext("In this snippet, the reference", "ALLBUS 1996/08", "is to be extracted as", "document", new InfolisPattern()),
			new StudyContext("the reference to the 1982   -   1983", "ALLBUS", "is to be extracted as", "document", new InfolisPattern()),
			new StudyContext("In this snippet, the reference", "ALLBUS", "85/01 is to be extracted", "document", new InfolisPattern()),
	
			new StudyContext("the reference to the 1982 till 1983", "ALLBUS", "is to be extracted as", "document", new InfolisPattern()),
			new StudyContext("the reference to the 1982 to 1983", "ALLBUS", "is to be extracted as", "document", new InfolisPattern()),
			new StudyContext("the reference to the 1982 bis 1983", "ALLBUS", "is to be extracted as", "document", new InfolisPattern()),
			new StudyContext("the reference to the 1982 und 1983", "ALLBUS", "is to be extracted as", "document", new InfolisPattern()),
	
			new StudyContext("the reference to the 2nd wave of the", "2000 Eurobarometer", "56.1 is to be extracted as", "document", new InfolisPattern()),
			new StudyContext("the reference to the 2nd wave of the", "Eurobarometer", "2000 is to be extracted as", "document", new InfolisPattern())};
	
	public LinkerTest() {

	}
	
	public void prepareTestFiles() throws IOException, Exception {
		for (InfolisFile file : createTestFiles(20)) {
            uris.add(file.getUri());
            String str = FileUtils.readFileToString(new File(file.getFileName()));
            log.debug(str);
		}
	}
	
	//TODO: use Learner.learn when implemented properly
	public List<String> generateTestContexts() {
		Execution execution = new Execution();
        execution.setAlgorithm(FrequencyBasedBootstrapping.class);
        execution.getTerms().add("Studierendensurvey");
        execution.setInputFiles(uris);
        execution.setThreshold(0.0);
        execution.setBootstrapStrategy(Execution.Strategy.mergeAll);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        return execution.getStudyContexts();
	}
	
	@Ignore
	public void linkContextTest() throws IOException, Exception {
		prepareTestFiles();
		List<String> contextURIs = generateTestContexts();
        Execution execution = new Execution();
        execution.setAlgorithm(Linker.class);
        execution.setStudyContexts(contextURIs);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        Set<StudyLink> links = execution.getLinks();
        for (StudyLink link : links) {
        	log.debug(link.toString());
        }
        //TODO: compare to gold set
	}
	
	@Test
	public void ignoreStudyTest() {
		Linker linker = new Linker();
		assertTrue(linker.ignoreStudy("eigene Erhebung"));
		assertTrue(linker.ignoreStudy("eigene Erhebungen"));
		assertTrue(linker.ignoreStudy("eigene Berechnung"));
		assertTrue(linker.ignoreStudy("eigene Berechnungen"));
		assertTrue(linker.ignoreStudy("eigene Darstellung"));
		assertTrue(linker.ignoreStudy("eigene Darstellungen"));
		assertFalse(linker.ignoreStudy("ALLBUS"));
		assertFalse(linker.ignoreStudy("eigene Berechnung; ALLBUS"));
		assertFalse(linker.ignoreStudy("ALLBUS; eigene Berechnung"));
	}
	
	@Test
	public void extractStudyTest() {
		Linker linker = new Linker();
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
	
	@Test
	public void testStudyYearPattern() throws Exception {
		Pattern pat = Pattern.compile(Linker.complexNumericInfoRegex);
		assertThat(pat, is(not(nullValue())));
		assertThat(pat.matcher("1995").matches(), is(true));
		assertThat(pat.matcher("1995-1998").matches(), is(true));
		assertThat(pat.matcher("1995 bis 1998").matches(), is(true));
		assertThat(pat.matcher("1995 to 1998").matches(), is(true));
		assertThat(pat.matcher("1995       till '98").matches(), is(true));
		
		assertThat(pat.matcher("NaN").matches(), is(false));
		assertThat(pat.matcher("(1998)").matches(), is(false));
	}
	
}
