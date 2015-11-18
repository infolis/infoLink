package io.github.infolis.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.algorithm.TextExtractor;
import io.github.infolis.util.SerializationUtils;

import java.util.Date;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionTest extends InfolisBaseTest {

	Logger log = LoggerFactory.getLogger(ExecutionTest.class);

	@Test
	public void testRoundTrip() {

		// Assume.assumeNotNull(System.getProperty("infolisRemoteTest",
		// "false"));

		Execution execution = new Execution();
		execution.setAlgorithm(TextExtractor.class);
		execution.getInputFiles().add("urn:foo");
		execution.getOutputFiles().add("urn:bar");
		execution.setRemoveBib(true);
		execution.setStatus(ExecutionStatus.FINISHED);
		execution.setEndTime(new Date());

		dataStoreClient.post(Execution.class, execution);

		Execution executionRetrieved = dataStoreClient.get(Execution.class, execution.getUri());

		assertThat(SerializationUtils.toJSON(executionRetrieved), equalTo(SerializationUtils.toJSON(execution)));
		// log.debug(SerializationUtils.toJSON(execution));
		// log.debug(SerializationUtils.toJSON(executionRetrieved));
	}

}
