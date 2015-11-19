package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashSet;

import org.junit.Test;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TagMap;
import io.github.infolis.model.entity.InfolisPattern;

/**
 * 
 * @author kata
 *
 */
public class TagResolverTest extends InfolisBaseTest {

	@Test
	public void testParseTags() throws IOException {
		InfolisPattern infolisPattern = new InfolisPattern();
		TagMap tagMap = new TagMap();
		tagMap.getInfolisPatternTags().add("test");
        HashSet<String> tags = new HashSet<String>();
        tags.add("test");
        infolisPattern.setTags(tags);

        dataStoreClient.post(InfolisPattern.class, infolisPattern);

        Execution e = new Execution();
        e.setAlgorithm(TagResolver.class);
        e.setTagMap(tagMap);
        e.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        assertEquals(infolisPattern.getUri(), e.getPatterns().get(0));
	}
}