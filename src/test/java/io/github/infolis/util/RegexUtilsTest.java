package io.github.infolis.util;

import io.github.infolis.util.InfolisFileUtils;

import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;


public class RegexUtilsTest {

	@Test
	public void testGetContextMinerYearPatterns() throws Exception {
		Pattern pat = RegexUtils.getContextMinerYearPatterns()[0];
		assertThat(pat, is(not(nullValue())));
//		System.out.println(pat.toString());
		assertThat(pat.matcher("1995").matches(), is(true));
		assertThat(pat.matcher("1995-1998").matches(), is(true));
		assertThat(pat.matcher("1995 bis 1998").matches(), is(true));
		assertThat(pat.matcher("1995 to 1998").matches(), is(true));
		assertThat(pat.matcher("1995       till '98").matches(), is(true));
		
		assertThat(pat.matcher("NaN").matches(), is(false));
		assertThat(pat.matcher("(1998)").matches(), is(false));
	}

}
