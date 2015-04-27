package io.github.infolis.infolink.patternLearner;

import io.github.infolis.infolink.patternLearner.Learner;
import io.github.infolis.model.Execution;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;


public class LearnerTest {
	
	// TODO: Do actual tests
	@Test
	public void test() {
		assertThat(true, is(true));
		boolean constraint_upperCase = false;
		Learner l = new Learner(constraint_upperCase, "corpusPath", "indexPath", "trainPath", "contextPath", "arffPath", "outputPath", Execution.Strategy.separate);
		assertThat(l, is(not(nullValue())));
	}

}
