package io.github.infolis.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;

import org.junit.Test;

public class LocalClientTest {
	
//	public class Execution extends BaseModel {
//		public Execution() { }
//		private List<String> log;
//		public List<String> getLog() { return log; }
//		public void setLog(List<String> log) { this.log = log; }
//	}
//	

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
