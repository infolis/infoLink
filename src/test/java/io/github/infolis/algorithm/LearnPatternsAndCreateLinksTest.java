package io.github.infolis.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.infolink.querying.QueryService;
import io.github.infolis.infolink.querying.DaraHTMLQueryService;
import io.github.infolis.model.BootstrapStrategy;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.InfolisFile;

/**
 * 
 * @author kata
 *
 */
public class LearnPatternsAndCreateLinksTest extends InfolisBaseTest {
	
	Logger log = LoggerFactory.getLogger(LearnPatternsAndCreateLinksTest.class);
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

	public LearnPatternsAndCreateLinksTest() throws Exception {
		for (InfolisFile file : createTestTextFiles(7, testStrings)) {
			uris.add(file.getUri());
		}
	}
	
	@Test
	public void testExecute() {
		Execution execution = new Execution();
		execution.setAlgorithm(LearnPatternsAndCreateLinks.class);
		execution.getSeeds().addAll(terms);
		execution.setInputFiles(uris);
		execution.setSearchTerm(terms.get(0));
		execution.setReliabilityThreshold(0.0);
		execution.setBootstrapStrategy(BootstrapStrategy.mergeAll);
		QueryService queryService = new DaraHTMLQueryService();
		queryService.setMaxNumber(10);
        dataStoreClient.post(QueryService.class, queryService);
        execution.setSearchResultLinkerClass(BestMatchLinker.class);
		execution.setQueryServices(Arrays.asList(queryService.getUri()));
		execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		for (String textRefUri: execution.getTextualReferences()) {
			log.debug(textRefUri);
			log.debug(dataStoreClient.get(TextualReference.class, textRefUri).toString());
			//TODO add assert statements
		}
		for (String linkUri: execution.getLinks()) {
			log.debug(linkUri);
			log.debug(dataStoreClient.get(EntityLink.class, linkUri).getToEntity());
			log.debug(dataStoreClient.get(EntityLink.class, linkUri).getLinkReason());
			//TODO add assert statements
		}
	}
}