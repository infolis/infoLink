package io.github.infolis.infolink.datasetMatcher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import io.github.infolis.algorithm.FrequencyBasedBootstrapping;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.Study;
import io.github.infolis.model.StudyContext;
import io.github.infolis.model.StudyLink;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.json.JsonArray;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class LinkerTest extends BaseTest {
	
	Logger log = LoggerFactory.getLogger(LinkerTest.class);
	private List<String> uris = new ArrayList<>();
	Map<String, String> expectedOutput = new HashMap<>();
	
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
	
	@Test
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
