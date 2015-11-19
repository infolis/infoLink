package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashSet;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisPattern;

import org.junit.Test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * 
 * @author kata
 *
 */
public class TagResolverTest extends InfolisBaseTest {

	@Test
	public void testParseTags() throws IOException {
		InfolisPattern infolisPattern = new InfolisPattern();
        Multimap<String, String> tagMap = HashMultimap.create();
        tagMap.put("InfolisPattern","test");
        HashSet<String> tags = new HashSet<String>();
        tags.add("test");
        infolisPattern.setTags(tags);

        dataStoreClient.post(InfolisPattern.class, infolisPattern);

        Execution e = new Execution();
        e.setAlgorithm(TagResolver.class);
        e.useTagMap(tagMap);
        e.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        assertEquals(infolisPattern.getUri(), e.getPatterns().get(0));
	}
}