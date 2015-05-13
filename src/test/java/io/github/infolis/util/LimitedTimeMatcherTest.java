package io.github.infolis.util; 
import java.util.regex.Pattern;

import org.junit.Test;


public class LimitedTimeMatcherTest {

	@Test
	public void testLimitedTimeMatcher() throws Exception {
		
		Pattern pat = Pattern.compile("(x+x+)+y");
		
		LimitedTimeMatcher ltm = new LimitedTimeMatcher(pat, "xxxxxxxxxxxxxxxxxxxxxxx", 1_000, "Test");
		ltm.run();

		
	}

}
