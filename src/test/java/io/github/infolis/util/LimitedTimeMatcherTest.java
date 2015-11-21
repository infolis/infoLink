package io.github.infolis.util;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;


public class LimitedTimeMatcherTest {

	@Test
	public void testTimeout() {

		Pattern pat = Pattern.compile("(x+x+)+y");

		LimitedTimeMatcher ltm = new LimitedTimeMatcher(pat, "xxxxxxxxxxxxxxxxxxxxxxx", 1_000, "Test");
		ltm.run();
		assertTrue(ltm.timedOut());
	}

	@Test
	public void testFind() throws Exception {

		Pattern pat = Pattern.compile("(a[bB]c)");

		LimitedTimeMatcher ltm = new LimitedTimeMatcher(pat, "foobar abc xyzzy aBc foobar", 1_000, "Test2");
		ltm.run();
		assertTrue(ltm.matched());
		assertTrue(ltm.finished());
		assertFalse(ltm.timedOut());
		assertEquals("abc", ltm.group(1));
		ltm.run();
		assertTrue(ltm.matched());
		assertTrue(ltm.finished());
		assertFalse(ltm.timedOut());
		assertEquals("aBc", ltm.group(1));
		ltm.run();
		assertFalse(ltm.matched());
		assertTrue(ltm.finished());
		assertFalse(ltm.timedOut());
		try {
			ltm.group();
		} catch (IllegalStateException e) {
			assertTrue("Should throw this exception", true);
		}
	}

}
