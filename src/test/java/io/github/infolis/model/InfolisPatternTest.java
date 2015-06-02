package io.github.infolis.model;

import io.github.infolis.infolink.luceneIndexing.InfolisBaseTest;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfolisPatternTest extends InfolisBaseTest {

	private static final Logger log = LoggerFactory.getLogger(InfolisPatternTest.class);

	@Test
	public void testInfolisPatternStringString() throws Exception {
		InfolisPattern pat = new InfolisPattern();
		pat.setPatternRegex("foo");
		dataStoreClient.post(InfolisPattern.class, pat);
		log.debug(pat.getUri());
	}

}
