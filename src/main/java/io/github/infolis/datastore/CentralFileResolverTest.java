package io.github.infolis.datastore;

import static org.junit.Assert.assertEquals;
import io.github.infolis.ws.server.UploadWebservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CentralFileResolverTest {

	Logger log = LoggerFactory.getLogger(CentralFileResolverTest.class);
	private char[] inData;
	FileResolver fr = new CentralFileResolver();
	String checksum = "cbaeb94798f9a6c6f799daceb8a8726b";

	@Before
	public void setUp() throws IOException {
		inData = new char[]{'a', 'b', '\001'};
		OutputStream out = fr.openOutputStream(checksum);
		IOUtils.write(inData, out);
	}

	@Test
	public void testOpenInputStreamString() throws Exception {
		InputStream in = fr.openInputStream(checksum);
		char[] read = IOUtils.toCharArray(in);
		assertEquals(inData.length, read.length);
		for (int i = 0; i < read.length; i++) {
			assertEquals(inData[i], read[i]);
		}
	}

	@Test
	public void testUploadWebservice() {
		UploadWebservice uws = new UploadWebservice();
		Response resp = uws.getFile(checksum);
		assertEquals(String.copyValueOf(inData), resp.getEntity());
	}

}
