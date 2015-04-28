package io.github.infolis.infolink.luceneIndexing;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.TempFileResolver;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.util.SerializationUtils;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class InfolisBaseTest {
	protected DataStoreClient localClient = DataStoreClientFactory.local();
	protected FileResolver tempFileResolver = new TempFileResolver();
	
	protected String[] testStrings = {
			"Hallo, please try to find the FOOBAR in this short text snippet. Thank you.",
			"Hallo, please try to find the R2 in this short text snippet. Thank you.",
			"Hallo, please try to find the D2 in this short text snippet. Thank you.",
			"Hallo, please try to find the term in this short text snippet. Thank you.",
			"Hallo, please try to find the _ in this short text snippet. Thank you.",
			"Hallo, please try to find .the term. in this short text snippet. Thank you.",
			"Hallo, please try to find the FOOBAR in this short text snippet. Thank you."
	};
	
	protected List<InfolisFile> createTestFiles() throws Exception {
		ArrayList<InfolisFile> ret = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			Path tempFile = Files.createTempFile("infolis-", ".txt");
			String data = testStrings[i % 3];
			FileUtils.write(tempFile.toFile(), data);

			InfolisFile file = new InfolisFile();
			file.setMd5(SerializationUtils.getHexMd5(data));
			file.setFileName(tempFile.toString());

			localClient.post(InfolisFile.class, file);
			OutputStream os = tempFileResolver.openOutputStream(file);
			IOUtils.write(data, os);
			os.close();
			ret.add(file);
		}
		return ret;
	}

}
