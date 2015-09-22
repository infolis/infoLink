package io.github.infolis.infolink.patternLearner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.entity.Instance;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReliabilityTest {
	
	private static final Logger log = LoggerFactory.getLogger(ReliabilityTest.class);
	private Reliability r = new Reliability();
	InfolisPattern pattern = new InfolisPattern();
	InfolisPattern newPattern = new InfolisPattern();
	Instance instance = new Instance("instance");
	Instance newInstance = new Instance("new instance");
	
	public ReliabilityTest() {
		Set<String> reliableInstances = new HashSet<>();
		reliableInstances.add("instance");
		r.setSeedTerms(reliableInstances);
		pattern.setMinimal("regex");
		newPattern.setMinimal("new regex");
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
		assertTrue(instance.addAssociation(newPattern.getMinimal(), 0.5));
		expectedAssociations.put("new regex", 0.5);
		assertEquals(expectedAssociations, instance.getAssociations());
	}
	
	@Test
	public void testReliability() {
		// first pass: reliability of seed instance with given pmi score should be 1.0
		assertEquals(1.0, r.reliability(instance, ""), 0.0);
		// pattern is inducted based on seed instance -> reliability of 1.0
		assertEquals(1.0, r.reliability(pattern, ""), 0.0);
		r.setMaxPmi(10);
		// TODO @bolandka : expected 0.1 but is 1.0
//		assertEquals(0.1, r.reliability(pattern, ""), 0.0);
		r.maximumPmi = 1.0;
		
		// suppose pattern generates newInstance with pmi of 0.5
		newInstance.addAssociation(pattern.getMinimal(), 0.5);
		pattern.addAssociation(newInstance.getName(), 0.5);//
		r.addInstance(newInstance);
		r.addPattern(pattern);//
		// TODO @bolandka : expected 0.25 but is 0.5
//		assertEquals(0.25, r.reliability(newInstance, ""), 0.0);
		
		// suppose this newInstance leads to induction of newPattern with pmi of 1.0
		newPattern.addAssociation(newInstance.getName(), 1.0);
		newInstance.addAssociation(newPattern.getMinimal(), 1.0);//
		r.addPattern(newPattern);
		r.addInstance(newInstance);
		// TODO @bolandka : expected 0.125 but is 0.25
//		assertEquals(0.125, r.reliability(newPattern, ""), 0.0);
	}

}