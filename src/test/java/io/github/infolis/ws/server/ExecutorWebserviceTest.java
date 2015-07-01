package io.github.infolis.ws.server;

import static org.junit.Assert.assertNotNull;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.algorithm.TextExtractorAlgorithm;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.model.Execution;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExecutorWebserviceTest extends InfolisBaseTest {
	
	Logger log = LoggerFactory.getLogger(ExecutorWebserviceTest.class);

	@Test
	public void testStartFrontendExecution() throws Exception {
        Assume.assumeNotNull(System.getProperty("infolisRemoteTest"));
		
		FormDataMultiPart fdm = new FormDataMultiPart();
		fdm.field("algorithm", TextExtractorAlgorithm.class.getName());
		WebTarget target = jerseyClient
				.target(InfolisConfig.getFrontendURI())
				.path("/execute");
		log.debug("{}", target);
		Entity<FormDataMultiPart> entity = Entity.entity(fdm, fdm.getMediaType());
		log.debug("{}", entity);
		log.debug("{}", fdm.getField("algorithm").getValue());
		
		// Why TF does this hang???
		Response post = target
				.request(MediaType.APPLICATION_JSON)
				.post(entity);
		log.debug("{}", post.getHeaders());
		log.debug("{}", post.readEntity(String.class));
	}

	@Ignore
	public void testStartExecution() throws Exception {
		DataStoreClient centralClient = DataStoreClientFactory.create(DataStoreStrategy.CENTRAL);
		Execution e = new Execution();
		e.setAlgorithm(TextExtractorAlgorithm.class);
		centralClient.post(Execution.class, e);
		assertNotNull(e.getUri());
		
//		Execution e2 = centralClient.get(Execution.class, e.getUri());
//		log.debug("E2: {}", e2.getInputFiles());
		
		ExecutorWebservice ws = new ExecutorWebservice();
		Response resp = ws.startExecution(e.getUri());
		log.error("{}", resp);
		while (true) {
			Thread.sleep(1000);
			e = centralClient.get(Execution.class, e.getUri());
			log.debug("Status: {}", e.getStatus());
			log.debug("log: {}", e.getLog());
		}
	}

}
