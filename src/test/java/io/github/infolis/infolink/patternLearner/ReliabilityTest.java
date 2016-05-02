package io.github.infolis.infolink.patternLearner;

import io.github.infolis.model.entity.Entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.infolis.model.entity.InfolisPattern;

import org.junit.Test;

/**
 * 
 * @author kata
 *
 */
public class ReliabilityTest {

	private Reliability r = new Reliability();
	InfolisPattern pattern = new InfolisPattern();
	InfolisPattern newPattern = new InfolisPattern();
	Entity instance = new Entity("instance");
	Entity newInstance = new Entity("new instance");

	public ReliabilityTest() {
		Set<String> reliableInstances = new HashSet<>();
		reliableInstances.add("instance");
		r.setSeedTerms(reliableInstances);
		pattern.setPatternRegex("regex");
		newPattern.setPatternRegex("new regex");
		double pmi = 1.0;
		pattern.addAssociation(instance.getName(), pmi);
		r.addInstance(instance);
		r.addPattern(pattern);
		r.setMaxPmi(pmi);
	}

	@Test
	public void testSetMaxPmi() {
		assertTrue(r.setMaxPmi(1.0));
		assertEquals(1.0, r.getMaxPmi(), 0.0);
		assertFalse(r.setMaxPmi(0.0));
		assertEquals(1.0, r.getMaxPmi(), 0.0);
		assertTrue(r.setMaxPmi(10.0));
		assertEquals(10.0, r.getMaxPmi(), 0.0);
	}

	@Test
	public void testAddAssociation() {
		Map<String, Double> expectedAssociations = new HashMap<>();
		expectedAssociations.put("instance", 1.0);
		assertEquals(expectedAssociations, pattern.getAssociations());
		assertTrue(pattern.addAssociation(newInstance.getName(), 0.5));
		expectedAssociations.put(newInstance.getName(), 0.5);
		assertEquals(expectedAssociations, pattern.getAssociations());

		expectedAssociations = new HashMap<>();
		assertEquals(expectedAssociations, instance.getAssociations());
		assertTrue(instance.addAssociation(newPattern.getPatternRegex(), 0.5));
		expectedAssociations.put("new regex", 0.5);
		assertEquals(expectedAssociations, instance.getAssociations());
	}

	@Test
	public void testReliability() {
		// first pass: reliability of seed instance with given pmi score should be 1.0
		assertEquals(1.0, r.reliability(instance, new HashSet<String>()), 0.0);
		// pattern is inducted based on seed instance -> reliability of 1.0
		assertEquals(1.0, r.reliability(pattern, new HashSet<String>()), 0.0);
		r.setMaxPmi(10);
		r.deleteScoreCache();
		assertEquals(0.1, r.reliability(pattern, new HashSet<String>()), 0.0);
		r.maximumPmi = 1.0;
		r.deleteScoreCache();
		
		// suppose pattern generates newInstance with pmi of 0.5
		newInstance.addAssociation(pattern.getPatternRegex(), 0.5);
		pattern.addAssociation(newInstance.getName(), 0.5);
		r.addInstance(newInstance);
		r.addPattern(pattern);//
		assertEquals(0.25, r.reliability(newInstance, new HashSet<String>()), 0.0);

		// suppose this newInstance leads to induction of newPattern with pmi of 1.0
		newPattern.addAssociation(newInstance.getName(), 1.0);
		newInstance.addAssociation(newPattern.getPatternRegex(), 1.0);
		r.deleteScoreCache();
		r.addPattern(newPattern);
		r.addInstance(newInstance);
		assertEquals(0.125, r.reliability(newPattern, new HashSet<String>()), 0.0);
	}

}