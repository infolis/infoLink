package io.github.infolis.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import io.github.infolis.algorithm.TextExtractorAlgorithm;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.util.SerializationUtils;

import java.net.URI;
import java.util.Date;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExecutionTest {

	Logger log = LoggerFactory.getLogger(ExecutionTest.class);
	
	@Test
	public void testRoundTrip() {
		
		if (Boolean.parseBoolean(System.getProperty("infolisRemoteTest", "false"))) {
			log.debug("Skipping because 'infolisRemoteTest' is not 'true'.");
			return;
		}
		
		DataStoreClient client = DataStoreClientFactory.global();
		
		Execution execution = new Execution();
		execution.setAlgorithm(TextExtractorAlgorithm.class);
		execution.getInputFiles().add("urn:foo");
		execution.getOutputFiles().add("urn:bar");
		execution.setRemoveBib(true);
		execution.setStatus(ExecutionStatus.FINISHED);
		execution.setEndTime(new Date());
		
		client.post(Execution.class, execution);
		
		Execution executionRetrieved = client.get(Execution.class, URI.create(execution.getUri()));
		
		assertThat(SerializationUtils.toJSON(executionRetrieved), equalTo(SerializationUtils.toJSON(execution)));
//		log.debug(SerializationUtils.toJSON(execution));
//		log.debug(SerializationUtils.toJSON(executionRetrieved));
	}
	
}
