package io.github.infolis.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;

public class MathUtilsTest extends InfolisBaseTest {
	
	private static final Logger log = LoggerFactory.getLogger(MathUtilsTest.class);
	// see http://www.ijfcc.org/papers/43-T00053.pdf for example values
	// apparently, they forgot to apply the log though...
	private static final double dataSize = 10000000.0;
	private static final double p_x = 24200 / dataSize; // occurrence of term x in all contexts
	private static final double p_y = 38900 / dataSize; // occurrence of pattern (or term) y 
	private static final double p_xy = 169 / dataSize; // joint occurrence of x and y
	
	@Test
	public void testPmi() {
		log.debug("p_x: " + p_x);
		log.debug("p_y: " + p_y);
		log.debug("p_xy: " + p_xy);
		log.debug("p_x_y: " + p_x * p_y);
		log.debug("p_xy / p_x_y: " + p_xy / (p_x * p_y));
		double pmi =  MathUtils.pmi(p_xy, p_x, p_y);
		double expectedPmi = MathUtils.log2(1.8);
		log.debug("pmi: " + pmi);
		log.debug("expected pmi (rounded): " + expectedPmi);
		assertEquals(expectedPmi, pmi, 0.01);
	}
}