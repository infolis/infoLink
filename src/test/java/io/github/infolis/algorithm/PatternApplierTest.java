package io.github.infolis.algorithm;

import static org.junit.Assert.*;
import io.github.infolis.infolink.luceneIndexing.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.InfolisPattern;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 * @author domi
 */
public class PatternApplierTest extends InfolisBaseTest {

    Logger log = LoggerFactory.getLogger(SearchTermPositionTest.class);
    private List<String> uris = new ArrayList<>();
    // left context: <word> <word> to find the 
    // study title: <word>* <word> <word> <word> <word>* 
    // right context: in <word> <word> <word> <word>
    // where word is an arbitrary string consisting of at least one character
    private final static InfolisPattern testPattern = new InfolisPattern(
    		"\\S++\\s\\S++\\s\\Qto\\E\\s\\Qfind\\E\\s\\Qthe\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E\\s\\S+?\\s\\S+?\\s\\S+?\\s\\S+");
    
    public PatternApplierTest() throws Exception {
	    for (InfolisFile file : createTestFiles(7)) {
			uris.add(file.getUri());
	    }
	    dataStoreClient.post(InfolisPattern.class, testPattern);
    }

    @Test
    public void testPatternApplier() throws Exception {
    	
    	Execution execution = new Execution();   
    	execution.getPattern().add(testPattern.getUri());
    	execution.setAlgorithm(PatternApplier.class);
    	execution.getInputFiles().addAll(uris);
    	
    	Algorithm algo = new PatternApplier();
    	algo.setDataStoreClient(dataStoreClient);
    	algo.setFileResolver(fileResolver);
    	algo.setExecution(execution);
    	algo.run();
    	
    	// find the contexts of "FOOBAR" and "term" (see also FrequencyBasedBootstrappingTest)
    	assertEquals(3, execution.getStudyContexts().size());
    	log.debug("LOG: {}", execution.getLog());
    }

}
