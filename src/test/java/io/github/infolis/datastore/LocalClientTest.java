package io.github.infolis.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import io.github.infolis.infolink.datasetMatcher.HTMLQueryService;
import io.github.infolis.infolink.datasetMatcher.QueryService;
import io.github.infolis.model.entity.InfolisFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
	public void testCRUD() throws IOException {
		
		DataStoreClient client = DataStoreClientFactory.create(DataStoreStrategy.LOCAL);
		
		InfolisFile file = new InfolisFile();
		file.setFileName("foo");
		
		assertNull(file.getUri());
		client.post(InfolisFile.class, file);
		assertNotNull(file.getUri());
		
		InfolisFile file2 = client.get(InfolisFile.class, file.getUri());
		assertEquals(file.getUri(), file2.getUri());
		assertEquals(file.getFileName(), file2.getFileName());
		
		file2.setUri("foo");
		client.post(InfolisFile.class, file2);
		
		QueryService qs = new HTMLQueryService();
		client.post(QueryService.class, qs);
		
		Path dumpPath = Paths.get("/tmp/infolis-test");
		Files.createDirectories(dumpPath);
		client.dump(dumpPath);
		
	}
}
