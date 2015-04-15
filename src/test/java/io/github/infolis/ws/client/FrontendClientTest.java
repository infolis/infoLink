package io.github.infolis.ws.client;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import javax.ws.rs.BadRequestException;

import io.github.infolis.model.InfolisFile;

import org.junit.Test;


public class FrontendClientTest {

	@Test
	public void test() throws Exception {
//		FrontendClient fc = new FrontendClient();
		InfolisFile inFile = new InfolisFile();
		inFile.setFileName("foobar.quux");
		inFile.setMediaType("text/plain");
		inFile.setMd5("12345678901234567890123456789012");
//		inFile.setSha1("1234567890123456789012345678901234567890");
		inFile.setFileStatus("AVAILABLE");
		InfolisFile serverFile;
		try {
			FrontendClient.post(InfolisFile.class, inFile);
			serverFile = FrontendClient.get(InfolisFile.class, URI.create(inFile.getUri()));
			assertTrue("No error posting", true);
			assertThat(serverFile.getFileName(), equalTo(inFile.getFileName()));
		} catch (BadRequestException e) {
//			e.printStackTrace();
		}
	}
	
}
