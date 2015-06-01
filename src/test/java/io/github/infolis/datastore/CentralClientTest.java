package io.github.infolis.datastore;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import io.github.infolis.model.InfolisFile;

import java.net.URI;

import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CentralClientTest {
	
	Logger log = LoggerFactory.getLogger(CentralClientTest.class);

	@Test
	public void test() throws Exception {
		Assume.assumeNotNull(System.getProperty("infolisRemoteTest", "false"));

		DataStoreClient client = DataStoreClientFactory.global();
		InfolisFile inFile = new InfolisFile();
		inFile.setFileName("foobar.quux");
		inFile.setMediaType("text/plain");
		inFile.setMd5("12345678901234567890123456789012");
		inFile.setFileStatus("AVAILABLE");
		InfolisFile serverFile;
		client.post(InfolisFile.class, inFile);
		assertTrue("No error posting", true);
		serverFile = client.get(InfolisFile.class, URI.create(inFile.getUri()));
		assertThat(serverFile.getFileName(), equalTo(inFile.getFileName()));
	}
	
}
