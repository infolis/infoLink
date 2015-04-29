/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.algorithm;

import io.github.infolis.infolink.luceneIndexing.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.StudyContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class FrequencyBasedBootstrappingTest extends InfolisBaseTest {

    Logger log = LoggerFactory.getLogger(SearchTermPositionTest.class);
	private List<String> uris = new ArrayList<>();
    private final static List<String> terms = Arrays.asList("FOOBAR");
    
    public FrequencyBasedBootstrappingTest() throws Exception {
		for (InfolisFile file : createTestFiles()) {
            uris.add(file.getUri());
		}
	}

    @Test
    public void testBootstrapping() throws Exception {

        Execution execution = new Execution();
        execution.setAlgorithm(FrequencyBasedBootstrapping.class);
        execution.getTerms().addAll(terms);
        execution.setInputFiles(uris);
        execution.setSearchTerm(terms.get(0));
	execution.setThreshold(0.0);

        Algorithm algo = new FrequencyBasedBootstrapping();
        algo.setDataStoreClient(localClient);
        algo.setFileResolver(tempFileResolver);
        algo.setExecution(execution);
        algo.run();

        for(String s : execution.getStudyContexts()) {
			System.out.println("found: " + localClient.get(StudyContext.class, s));
			System.out.println("term: " + localClient.get(StudyContext.class, s).getTerm());
			System.out.println("pattern: " + localClient.get(StudyContext.class, s).getPattern());
        }
    }

}
