package io.github.infolis.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.entity.InfolisFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class LocalClientTest extends InfolisBaseTest {

    private static final Logger log = LoggerFactory.getLogger(LocalClientTest.class);

	@Test
	public void testCRUD() throws IOException {

		InfolisFile file = new InfolisFile();
		file.setFileName("foo");

		assertNull(file.getUri());
		dataStoreClient.post(InfolisFile.class, file);
		assertNotNull(file.getUri());

		InfolisFile file2 = dataStoreClient.get(InfolisFile.class, file.getUri());
		assertEquals(file.getUri(), file2.getUri());
		assertEquals(file.getFileName(), file2.getFileName());

		file2.setFileName("bar");
		dataStoreClient.put(InfolisFile.class, file2);

		Path dumpPath = Paths.get("/tmp/infolis-test");
		Files.createDirectories(dumpPath);
		dataStoreClient.dump(dumpPath, "test");

	}

	@Test
	public void testSearch()
	{
	    String[] testTags = new String[] {"noplay", "foobar", "jack", "barfoo", "allwork", "noplay", "allwork", "noplay" };
	    for (String tag : testTags) {
	        InfolisFile file = new InfolisFile();
	        file.setFileName("dummy-" + tag);
	        file.getTags().add(tag);
	        dataStoreClient.post(InfolisFile.class, file);
	    }
	    {
	        Multimap<String, String> query = HashMultimap.create();
	        query.put("tags", "allwork");
	        List<InfolisFile> found = dataStoreClient.search(InfolisFile.class, query);
	        assertEquals(2, found.size());
	        for (InfolisFile file : found) {
	            assertEquals("[allwork]", file.getTags().toString());
	        }
	    }
	    {
	        Multimap<String, String> query = HashMultimap.create();
	        query.put("tags", "allwork");
	        query.put("tags", "noplay");
	        List<InfolisFile> found = dataStoreClient.search(InfolisFile.class, query);
	        assertEquals(5, found.size());
	    }
	    {
	        Multimap<String, String> query = HashMultimap.create();
	        query.put("fileName", "dummy-noplay");
	        List<InfolisFile> found = dataStoreClient.search(InfolisFile.class, query);
	        assertEquals(3, found.size());
	        for (InfolisFile file : found) {
	            assertEquals("dummy-noplay", file.getFileName());
	        }
	    }
	}

}
