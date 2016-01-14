package io.github.infolis.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.resolve.QueryService;
import io.github.infolis.resolve.HTMLQueryService;
import io.github.infolis.model.BootstrapStrategy;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;

/**
 * 
 * @author kata
 *
 */
public class LearnAndResolveTest extends InfolisBaseTest {
	
	Logger log = LoggerFactory.getLogger(LearnAndResolve.class);
	private List<String> uris = new ArrayList<>();
	private final static String term = "ALLBUS";
	private final static List<String> terms = Arrays.asList(term);
	private String[] testStrings = {
			"Hallo, please try to find the ALLBUS 2000 in this short text snippet. Thank you.",
			"Hallo, please try to find the R2 in this short text snippet. Thank you.",
			"Hallo, please try to find the D2 in this short text snippet. Thank you.",
			//"Hallo, please try to find the term in this short text snippet. Thank you.",
			"Hallo, please try to find the _ in this short text snippet. Thank you.",
			//"Hallo, please try to find .the term. in this short text snippet. Thank you.",
			"Hallo, please try to find the ALLBUS in this short text snippet. Thank you."
	};

	public LearnAndResolveTest() throws Exception {
		for (InfolisFile file : createTestTextFiles(7, testStrings)) {
			uris.add(file.getUri());
		}
	}
	
	@Test
	public void testInfoLink() {
		Execution execution = new Execution();
		execution.setAlgorithm(LearnAndResolve.class);
		execution.getSeeds().addAll(terms);
		execution.setInputFiles(uris);
		execution.setSearchTerm(terms.get(0));
		execution.setReliabilityThreshold(0.0);
		execution.setBootstrapStrategy(BootstrapStrategy.mergeAll);
		HTMLQueryService queryService = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show", 0.5);
		queryService.setMaxNumber(10);
        dataStoreClient.post(QueryService.class, queryService);
		execution.setQueryServices(Arrays.asList(queryService.getUri()));
		execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		for (String textRefUri: execution.getTextualReferences()) {
			log.debug(textRefUri);
			//TODO resolve uris; add assert statements
		}
		for (String linkUri: execution.getLinks()) {
			log.debug(linkUri);
			//TODO resolve uris; add assert statements
		}
	}
}