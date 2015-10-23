package io.github.infolis.datastore;

import io.github.infolis.model.BaseModel;
import io.github.infolis.util.SerializationUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class LocalClient extends AbstractClient {

	// private static final Logger log =
	// LoggerFactory.getLogger(LocalClient.class);
	public static final Map<UUID, Map<String, String>> jsonDB = new HashMap<>();

	private final UUID storeId;

	public LocalClient(UUID uuid) {
		this.storeId = uuid;
		jsonDB.put(this.storeId, new HashMap<String, String>());
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
		try {
			jsonDB.get(storeId).put(thing.getUri(), SerializationUtils.jacksonMapper.writeValueAsString(thing));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T extends BaseModel> void post(Class<T> clazz, T thing)
			throws BadRequestException {
		String uuid = String.format("%s", UUID.randomUUID().toString());
		thing.setUri(uuid);
		try {
			jsonDB.get(storeId).put(uuid, SerializationUtils.jacksonMapper.writeValueAsString(thing));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void clear() {
		jsonDB.get(storeId).clear();
	}

	@Override
	public void dump(Path directory) {
		for (Entry<String, String> entry : jsonDB.get(storeId).entrySet()) {
			try {
				OutputStream os = Files.newOutputStream(directory.resolve(entry.getKey()));
				IOUtils.write(entry.getValue(), os);
				os.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
