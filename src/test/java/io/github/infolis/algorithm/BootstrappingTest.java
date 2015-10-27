package io.github.infolis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
//import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;

import static org.junit.Assert.*;

/**
 * 
 * @author kata
 */
public class BootstrappingTest extends InfolisBaseTest {
	
	//private static final org.slf4j.Logger log = LoggerFactory.getLogger(BootstrappingTest.class);
	Execution indexerExecution = new Execution();
	
	private List<String> uris = new ArrayList<>();
	private static InfolisPattern pat = new InfolisPattern();
	private static InfolisPattern pat2 = new InfolisPattern();

	public BootstrappingTest() throws Exception {
		String[] testStrings = {
				"Hallo, please try to find the FOOBAR in this short text snippet. Thank you.",
				"Hallo, please try to find the R2 in this short text snippet. Thank you.",
				"Hallo, please try to find the D2 in this short text snippet. Thank you.",
				"Hallo, please try to find the term in this short text snippet. Thank you.",
				"Hallo, please try to find the _ in this short text snippet. Thank you.",
				"Hallo, please try to find .the term. in this short text snippet. Thank you.",
				"Hallo, please try to find the FOOBAR in this short text snippet. Thank you."
		};
		for (InfolisFile file : createTestTextFiles(7, testStrings)) 
			uris.add(file.getUri());
		pat.setPatternRegex("\\S++\\s\\S++\\s\\S++\\s\\S++\\s\\Q.the\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E\\s\\S+?\\s\\S+?\\s\\S+?\\s\\S+");
		pat.setLuceneQuery("the * in");
		pat2.setPatternRegex("\\S++\\s\\S++\\s\\S++\\s\\S++\\s\\Qthe\\E\\s\\s?(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)\\s?\\s\\Qin\\E\\s\\S+?\\s\\S+?\\s\\S+?\\s\\S+");
		pat2.setLuceneQuery("the * in");
		dataStoreClient.post(InfolisPattern.class, pat);
		dataStoreClient.post(InfolisPattern.class, pat2);
		indexerExecution = createIndex();
	}
	
	
	public Execution createIndex() throws IOException {
		Execution execution = new Execution();
		execution.setAlgorithm(Indexer.class);
		execution.setInputFiles(uris);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		return execution;
	}

    public List<String> getContextsForPattern(InfolisPattern pattern) {
        Execution execution = new Execution();
        execution.getPatterns().add(pattern.getUri());
        execution.setAlgorithm(PatternApplier.class);
        execution.getInputFiles().addAll(uris);
        Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, fileResolver);
        algo.run();
        return execution.getTextualReferences();
    }
    
    private List<String> getReferenceStrings(Collection<String> URIs) {
        List<String> contexts = new ArrayList<>();
        for (String uri : URIs) {
            contexts.add(dataStoreClient.get(TextualReference.class, uri).toPrettyString());
        }
        return contexts;
    }
    
    @Test
    /**
     * Tests whether optimized search using lucene yields the same result as 
     * searching the regular expressions directly without prior filtering.
     * 
     * @throws IOException
     */
    public void testGetContextsForPatterns() throws IOException {
    	Set<String> references1 = new HashSet<>(getReferenceStrings(getContextsForPattern(pat)));
    	references1.addAll(getReferenceStrings(getContextsForPattern(pat2)));
    	Execution e = new Execution();
    	e.setInputFiles(uris);
    	Bootstrapping b = new FrequencyBasedBootstrapping(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
    	b.indexerExecution = indexerExecution;
    	b.setExecution(e);
    	Set<String> references2 = new HashSet<>(getReferenceStrings(b.getContextsForPatterns(Arrays.asList(pat, pat2))));
    	assertEquals(references1, references2);
    }
}