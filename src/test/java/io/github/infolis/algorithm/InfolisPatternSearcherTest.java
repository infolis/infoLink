package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;

/**
 * 
 * @author kata
 *
 */
public class InfolisPatternSearcherTest extends InfolisBaseTest {

	//@Test
    /**
     * Tests whether optimized search using lucene yields the same result as
     * searching the regular expressions directly without prior filtering.
     *
     * @throws IOException
     */
    /*public void testExecute() throws IOException {
    	
    }*/
	
	@Test
	public void testSettingOfTags() throws Exception {
		InfolisPattern pattern = new InfolisPattern();
		pattern.setLuceneQuery("test * test");
		pattern.setPatternRegex("test (.*) test");
		dataStoreClient.post(InfolisPattern.class, pattern);
		
		InfolisFile testFile = createTestTextFiles(1, new String[] {"test foo test"}).get(0);
		Set<String> fileTagsToSet = new HashSet<>();
		fileTagsToSet.add("fileTag1");
		testFile.setTags(fileTagsToSet);
		dataStoreClient.put(InfolisFile.class, testFile, testFile.getUri());
		
		Execution exec = new Execution();
		exec.setInputFiles(Arrays.asList(testFile.getUri()));
		exec.setPatterns(Arrays.asList(pattern.getUri()));
		exec.setAlgorithm(InfolisPatternSearcher.class);
		exec.setUpperCaseConstraint(false);
		Set<String> tagsToSet = new HashSet<>();
		tagsToSet.add("test1");
		tagsToSet.add("test2");
		exec.setTags(tagsToSet);
		exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		TextualReference textRef = dataStoreClient.get(TextualReference.class, exec.getTextualReferences().get(0));
		assertEquals(new HashSet<>(Arrays.asList("fileTag1", "test1", "test2")), textRef.getTags()); 
	}
}
