package io.github.infolis.datastore;

import io.github.infolis.model.BaseModel;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.BadRequestException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LocalClient implements DataStoreClient {

	// private static final Logger log =
	// LoggerFactory.getLogger(LocalClient.class);
	private static final Map<String, String> jsonDB = new HashMap<>();
	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public <T extends BaseModel> T get(Class<T> clazz, String id) throws BadRequestException {
		try {
			String json = jsonDB.get(id);
			if (null == json) {
				return null;
			}
			T readValue = objectMapper.readValue(json, clazz);
			readValue.setUri(id);
			return readValue;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T extends BaseModel> T get(Class<T> clazz, URI uri) throws BadRequestException {
		return get(clazz, uri.toString());
	}

	@Override
	public <T extends BaseModel> void put(Class<T> clazz, T thing) throws BadRequestException {
		try {
			jsonDB.put(thing.getUri(), objectMapper.writeValueAsString(thing));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T extends BaseModel> void post(Class<T> clazz, T thing) throws BadRequestException {
		String uuid = String.format("%s", UUID.randomUUID().toString());
		thing.setUri(uuid);
		try {
			jsonDB.put(uuid, objectMapper.writeValueAsString(thing));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
