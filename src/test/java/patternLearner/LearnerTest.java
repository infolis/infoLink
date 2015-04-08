package patternLearner;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;


public class LearnerTest {
	
	// TODO: Do actual tests
	@Test
	public void test() {
		assertThat(true, is(true));
		boolean constraint_upperCase = false;
		Learner l = new Learner("taggingCMD", "chunkingCMD", constraint_upperCase, "corpusPath", "indexPath", "trainPath", "contextPath", "arffPath", "outputPath");
		assertThat(l, is(not(nullValue())));
	}

}
