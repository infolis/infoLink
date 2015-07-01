package io.github.infolis;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.FileResolverFactory;
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
import org.glassfish.jersey.media.multipart.MultiPartFeature;

public class InfolisBaseTest {
	protected final DataStoreClient dataStoreClient;
	protected final FileResolver fileResolver;
	{
		DataStoreStrategy strategy;
		if (Boolean.valueOf(System.getProperty("infolisRemoteTests", "false"))) {
			strategy = DataStoreStrategy.CENTRAL;
		} else {
			strategy = DataStoreStrategy.TEMPORARY;
		}
		dataStoreClient = DataStoreClientFactory.create(strategy);
		fileResolver =  FileResolverFactory.create(strategy);
	}

	protected static Client jerseyClient = ClientBuilder.newBuilder()
			// .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
			.register(MultiPartFeature.class)
			// .register(JacksonFeature.class)
			// .register(JacksonJsonProvider.class)
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
		return createTestFiles(this.testStrings, nrFiles);
	}

	protected List<InfolisFile> createTestFiles(String[] strings, int nrFiles) throws Exception {
		ArrayList<InfolisFile> ret = new ArrayList<>();
		int j = 0;
		for (int i = 0; i < nrFiles; i++) {
			j = i % strings.length;
			Path tempFile = Files.createTempFile("infolis-", ".txt");
			String data = strings[j];
			FileUtils.write(tempFile.toFile(), data);

			InfolisFile file = new InfolisFile();
			file.setMd5(SerializationUtils.getHexMd5(data));
			file.setFileName(tempFile.toString());
			file.setFileStatus("AVAILABLE");

			dataStoreClient.post(InfolisFile.class, file);
			OutputStream os = fileResolver.openOutputStream(file);
			IOUtils.write(data, os);
			os.close();
			ret.add(file);
		}
		return ret;
	}

}