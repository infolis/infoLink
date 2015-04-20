package io.github.infolis.datastore;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.model.InfolisFile;

import java.net.URI;

import javax.ws.rs.BadRequestException;

import org.junit.Test;


public class CentralClientTest {

	@Test
	public void test() throws Exception {
		DataStoreClient client = DataStoreClientFactory.global();
		InfolisFile inFile = new InfolisFile();
		inFile.setFileName("foobar.quux");
		inFile.setMediaType("text/plain");
		inFile.setMd5("12345678901234567890123456789012");
//		inFile.setSha1("1234567890123456789012345678901234567890");
		inFile.setFileStatus("AVAILABLE");
		InfolisFile serverFile;
		try {
			client.post(InfolisFile.class, inFile);
			assertTrue("No error posting", true);
			serverFile = client.get(InfolisFile.class, URI.create(inFile.getUri()));
			assertThat(serverFile.getFileName(), equalTo(inFile.getFileName()));
		} catch (BadRequestException e) {
//			e.printStackTrace();
		}
	}
	
}
