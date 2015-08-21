package io.github.infolis.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.infolink.patternLearner.Reliability;
import io.github.infolis.infolink.patternLearner.Reliability.Instance;
import io.github.infolis.util.MathUtils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfolisPatternTest extends InfolisBaseTest {

	private static final Logger log = LoggerFactory.getLogger(InfolisPatternTest.class);
	private static final List<String> contextStrings = Arrays.asList(
			"bar foo bar", "bar foo bar", "bar foo bar", "bar foo bar",
			"foO bar foO", "foO bar foO", "foO bar foO", 
			"fOo bAr fOo", 
			"bar bar bar", "bar bar bar");
	private static InfolisPattern pat = new InfolisPattern();
	

	public InfolisPatternTest() throws Exception {
		pat.setMinimal("foO\\s(.*?)\\sfoO");
		String[] testStrings = new String[contextStrings.size()];
		createTestFiles(10, contextStrings.toArray(testStrings));
	}

	@Test
	public void testInfolisPatternStringString() throws Exception {
		dataStoreClient.post(InfolisPattern.class, pat);
		log.debug(pat.getUri());
	}
	
	@Test
	public void testIsRelevant() throws Exception {
		pat.setThreshold(0.0);
		assertTrue(pat.isRelevant(contextStrings));
		pat.setThreshold(0.3);
		assertTrue(pat.isRelevant(contextStrings));
		pat.setThreshold(1.0);
		assertFalse(pat.isRelevant(contextStrings));
	}

	@Test
	public void testIsReliable() throws Exception {
		int dataSize = contextStrings.size();
		List<String> contexts_pattern = Arrays.asList(
				"foO bar foO", "foO bar foO", "foO bar foO");
		Set<String> reliableInstances = new HashSet<>();
		Set<StudyContext> contexts_seed = new HashSet<>();
		Map<String, Set<StudyContext>> contexts = new HashMap<>();;
		Reliability r = new Reliability();
		String seed = "bar";
		reliableInstances.add(seed);
		r.setSeedInstances(reliableInstances);

		StudyContext context_bar_0 = new StudyContext("bar", "bar", "bar", "document4", new InfolisPattern(), "version");
		StudyContext context_bar_1 = new StudyContext("bar", "bar", "bar", "document5", new InfolisPattern(), "version");
		StudyContext context_bar_2 = new StudyContext("foO", "bar", "foO", "document6", new InfolisPattern(), "version");
		StudyContext context_bar_3 = new StudyContext("foO", "bar", "foO", "document7", new InfolisPattern(), "version");
		StudyContext context_bar_4 = new StudyContext("foO", "bar", "foO", "document8", new InfolisPattern(), "version");
		
		contexts_seed.add(context_bar_0);
		contexts_seed.add(context_bar_1);
		contexts_seed.add(context_bar_2);
		contexts_seed.add(context_bar_3);
		contexts_seed.add(context_bar_4);
		
		contexts.put(seed, contexts_seed);

		Instance bar = r.new Instance(seed);
		
		double p_x = 5 / 10.0; // "bar" occurs 5 times as instance in all data
		double p_y = 3 / 10.0; // bar_patt occurs 3 times
		double p_xy = 3 / 10.0; // "bar" instance and bar_pat occur jointly 3 times
		double pmi_score = MathUtils.pmi(p_xy, p_x, p_y);
		log.debug("initial pmi_score: " + pmi_score);
		pat.addAssociation("bar", pmi_score);
		bar.addAssociation(pat.getMinimal(), pmi_score);
		r.addInstance(bar);
		r.addPattern(pat);
		r.setMaxPmi(pmi_score);
		double expectedReliability = r.reliability(pat, "");
		pat.isReliable(contexts_pattern, dataSize, reliableInstances, contexts, r);
		assertEquals(expectedReliability, pat.getReliability(), 0.0);
		log.debug("Expected reliability: " + expectedReliability);
		log.debug("Computed reliability: " + pat.getReliability());
	}
	
}
