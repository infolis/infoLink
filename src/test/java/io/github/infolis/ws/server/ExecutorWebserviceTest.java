package io.github.infolis.ws.server;

import static org.junit.Assert.assertNotNull;
import io.github.infolis.algorithm.TextExtractorAlgorithm;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.infolink.luceneIndexing.InfolisBaseTest;
import io.github.infolis.model.Execution;

import java.net.URI;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExecutorWebserviceTest extends InfolisBaseTest {
	
	Logger log = LoggerFactory.getLogger(ExecutorWebserviceTest.class);

	@Test
	public void testStartExecution() throws Exception {
		DataStoreClient centralClient = DataStoreClientFactory.create(DataStoreStrategy.CENTRAL);
		Execution e = new Execution();
		e.setAlgorithm(TextExtractorAlgorithm.class);
		centralClient.post(Execution.class, e);
		assertNotNull(e.getUri());
		
		Execution e2 = centralClient.get(Execution.class, URI.create(e.getUri()));
//		log.debug("E2: {}", e2.getInputFiles());
		
		ExecutorWebservice ws = new ExecutorWebservice();
		Response resp = ws.startExecution(e.getUri());
		log.error("{}", resp);
		while (true) {
			Thread.sleep(1000);
			e = centralClient.get(Execution.class, URI.create(e.getUri()));
			log.debug("Status: {}", e.getStatus());
			log.debug("log: {}", e.getLog());
		}
	}

}
