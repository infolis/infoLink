package io.github.infolis.datastore;

import io.github.infolis.model.BaseModel;
import io.github.infolis.util.SerializationUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

public class LocalClient extends AbstractClient {

	 private static final Logger log = LoggerFactory.getLogger(LocalClient.class);
	public static final Map<UUID, Map<String, String>> jsonDB = new HashMap<>();
	public static final Map<String, List<String>> endpointIndex = new HashMap<>();

	private final UUID storeId;

	public LocalClient(UUID uuid) {
		this.storeId = uuid;
		jsonDB.put(this.storeId, new HashMap<String, String>());
	}
	
	private <T extends BaseModel> void store(Class<T> clazz, T thing) {
		try {
			jsonDB.get(storeId).put(thing.getUri(), SerializationUtils.jacksonMapper.writeValueAsString(thing));
			String endpoint = CentralClient.getEndpointForClass(clazz);
			if (!endpointIndex.containsKey(endpoint)) {
				endpointIndex.put(endpoint, new ArrayList<String>());
			}
			endpointIndex.get(endpoint).add(thing.getUri());
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T extends BaseModel> T get(Class<T> clazz, String id)
			throws BadRequestException {
		try {
			String json = jsonDB.get(storeId).get(id);
			if (null == json) {
				return null;
			}
			T readValue = SerializationUtils.jacksonMapper.readValue(json, clazz);
			readValue.setUri(id);
			return readValue;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T extends BaseModel> void put(Class<T> clazz, T thing)
			throws BadRequestException {
		store(clazz, thing);
	}

	@Override
	public <T extends BaseModel> void post(Class<T> clazz, T thing)
			throws BadRequestException {
		String uuid = String.format("%s", UUID.randomUUID().toString());
		thing.setUri(uuid);
		store(clazz, thing);
	}

	@Override
	public void clear() {
		jsonDB.get(storeId).clear();
	}

	@Override
	public void dump(Path directory) {
		/*
		 * {
		 *   "file": {
		 *     "1234": {"filename":"foo"}
		 *   },
		 *   "execution": {
		 *     "5678": {"algorithm":"bar"},
		 *   }
		 * }	
		 */
		
		
		Map<String,List<String>> dumpByEndpoint = new HashMap<String, List<String>>();
		for (String endpoint : endpointIndex.keySet()) {
			List<String> uris = endpointIndex.get(endpoint);
			if (uris.isEmpty()) {
				continue;
			}
			List<String> entries = new ArrayList<>();
			dumpByEndpoint.put(endpoint, entries);
			for (String uri : uris) {
				StringBuilder sb = new StringBuilder();
				sb.append("  ");
				sb.append('"');
				sb.append(uri);
				sb.append('"');
				sb.append(": ");
				sb.append(jsonDB.get(storeId).get(uri));
				entries.add(sb.toString());
			}
		}
		List<String> entries = new ArrayList<>();
		for (Entry<String, List<String>> entry : dumpByEndpoint.entrySet())
		{
			StringBuilder sb = new StringBuilder();
			sb.append("  ");
			sb.append('"');
			sb.append(entry.getKey());
			sb.append('"');
			sb.append(": ");
			sb.append("{\n  ");
			sb.append(StringUtils.join(entry.getValue(), ",\n  "));
			sb.append("\n  }");
			entries.add(sb.toString());
		}
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append(StringUtils.join(entries, ",\n"));
		sb.append("\n}");
		try {
			Path outputFile = directory.resolve(storeId + ".json");
			OutputStream os = Files.newOutputStream(outputFile);
			IOUtils.write(sb.toString(), os);
			os.close();
			log.debug("Dumped to {}", outputFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
