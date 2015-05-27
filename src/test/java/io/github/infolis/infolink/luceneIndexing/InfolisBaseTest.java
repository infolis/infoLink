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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class InfolisBaseTest {
	protected DataStoreClient dataStoreClient = DataStoreClientFactory.local();
	protected FileResolver fileResolver = new TempFileResolver();

	protected static Client jerseyClient = ClientBuilder.newBuilder()
//			.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
			.register(MultiPartFeature.class)
//			.register(JacksonFeature.class)
//			.register(JacksonJsonProvider.class)
			.build();

	
	protected String[] testStrings = {
			"Hallo, please try to find the FOOBAR in this short text snippet. Thank you.",
			"Hallo, please try to find the R2 in this short text snippet. Thank you.",
			"Hallo, please try to find the D2 in this short text snippet. Thank you.",
			"Hallo, please try to find the term in this short text snippet. Thank you.",
			"Hallo, please try to find the _ in this short text snippet. Thank you.",
			"Hallo, please try to find .the term. in this short text snippet. Thank you.",
			"Hallo, please try to find the FOOBAR in this short text snippet. Thank you."
	};
	
	protected List<InfolisFile> createTestFiles(int nrFiles) throws Exception {
		ArrayList<InfolisFile> ret = new ArrayList<>();
		int j = 0;
		for (int i = 0; i < nrFiles; i++) {
			j = i % testStrings.length;
			Path tempFile = Files.createTempFile("infolis-", ".txt");
			String data = testStrings[j];
			FileUtils.write(tempFile.toFile(), data);

			InfolisFile file = new InfolisFile();
			file.setMd5(SerializationUtils.getHexMd5(data));
			file.setFileName(tempFile.toString());

			dataStoreClient.post(InfolisFile.class, file);
			OutputStream os = fileResolver.openOutputStream(file);
			IOUtils.write(data, os);
			os.close();
			ret.add(file);
		}
		return ret;
	}

}
