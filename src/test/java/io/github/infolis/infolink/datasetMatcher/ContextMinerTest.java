package io.github.infolis.infolink.datasetMatcher;

import static org.junit.Assert.*;

import org.junit.Test;


public class ContextMinerTest {

	@Test
	public void testIgnoreStudy() throws Exception {
		
		assertFalse(ContextMiner.ignoreStudy("Eigenen Erhebung"));
		assertTrue(ContextMiner.ignoreStudy("Eigene Erhebung"));
		assertTrue(ContextMiner.ignoreStudy("eigene Erhebung"));
		assertFalse(ContextMiner.ignoreStudy("eigene erhebung"));
		assertTrue(ContextMiner.ignoreStudy("1994 2"));
	}

}
