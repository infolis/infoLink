package io.github.infolis.datastore;

import static org.junit.Assert.*;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.model.InfolisFile;

import org.junit.Test;

public class LocalClientTest {

	@Test
	public void testCRUD() {
		
		DataStoreClient client = DataStoreClientFactory.create(DataStoreStrategy.LOCAL);
		
		InfolisFile file = new InfolisFile();
		file.setFileName("foo");
		
		assertNull(file.getUri());
		client.post(InfolisFile.class, file);
		assertNotNull(file.getUri());
		
		InfolisFile file2 = client.get(InfolisFile.class, file.getUri());
		assertEquals(file.getUri(), file2.getUri());
		assertEquals(file.getFileName(), file2.getFileName());
		
	}
}
