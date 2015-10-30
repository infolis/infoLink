package io.github.infolis.model;

import io.github.infolis.model.entity.InfolisPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.algorithm.bootstrapping.Reliability;
import io.github.infolis.model.entity.Entity;
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
		createTestTextFiles(10, contextStrings.toArray(testStrings));
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
		//String leftText, String reference, String rightText, String textFile, String pattern, String mentionsReference
		List<TextualReference> contexts_pattern = Arrays.asList(
				new TextualReference("foO", "bar", "foO", "textfile1", "pattern", "ref"), 
				new TextualReference("foO", "bar", "foO", "textfile2", "pattern", "ref"), 
				new TextualReference("foO", "bar", "foO", "textfile3", "pattern", "ref"));
		Set<String> reliableInstanceTerms = new HashSet<>();
		Set<Entity> reliableInstances = new HashSet<>();
		Set<TextualReference> contexts = new HashSet<>();;
		Reliability r = new Reliability();
		String seed = "bar";
		reliableInstanceTerms.add(seed);
		r.setSeedTerms(reliableInstanceTerms);

		TextualReference context_bar_0 = new TextualReference("bar", "bar", "bar", "document4", "pattern","ref");
		TextualReference context_bar_1 = new TextualReference("bar", "bar", "bar", "document5", "pattern","ref");
		TextualReference context_bar_2 = new TextualReference("foO", "bar", "foO", "document6", "pattern","ref");
		TextualReference context_bar_3 = new TextualReference("foO", "bar", "foO", "document7", "pattern","ref");
		TextualReference context_bar_4 = new TextualReference("foO", "bar", "foO", "document8", "pattern","ref");
		contexts.add(context_bar_0);
		contexts.add(context_bar_1);
		contexts.add(context_bar_2);
		contexts.add(context_bar_3);
		contexts.add(context_bar_4);
		Entity bar = new Entity(seed);
		bar.setTextualReferences(contexts);
		reliableInstances.add(bar);
		
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
		pat.setTextualReferences(contexts_pattern);
		
		double expectedReliability = r.reliability(pat, "");
		
		pat.isReliable(dataSize, reliableInstances, r);
		assertEquals(expectedReliability, pat.getReliability(), 0.0);
		log.debug("Expected reliability: " + expectedReliability);
		log.debug("Computed reliability: " + pat.getReliability());
	}
	
}
