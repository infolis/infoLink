package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisPattern;

/**
 *
 * @author kata
 *
 */
public class TagSearcherTest extends InfolisBaseTest {

	@Test
	public void testParseTags() throws IOException {
		InfolisPattern infolisPattern = new InfolisPattern();
        infolisPattern.getTags().add("test");

        dataStoreClient.post(InfolisPattern.class, infolisPattern);

        Execution e = new Execution();
		e.getInfolisPatternTags().add("test");
        e.setAlgorithm(TagSearcher.class);
        e.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        assertEquals(infolisPattern.getUri(), e.getPatterns().get(0));
	}
}