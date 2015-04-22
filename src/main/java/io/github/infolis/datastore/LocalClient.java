package io.github.infolis.datastore;

import io.github.infolis.model.BaseModel;
import io.github.infolis.util.SerializationUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

public class LocalClient implements DataStoreClient {

	// private static final Logger log =
	// LoggerFactory.getLogger(LocalClient.class);
	private static final Map<String, String> jsonDB = new HashMap<>();

	@Override
	public <T extends BaseModel> T get(Class<T> clazz, String id) throws BadRequestException {
		try {
			String json = jsonDB.get(id);
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
	public <T extends BaseModel> T get(Class<T> clazz, URI uri) throws BadRequestException {
		return get(clazz, uri.toString());
	}

	@Override
	public <T extends BaseModel> void put(Class<T> clazz, T thing) throws BadRequestException {
		try {
			jsonDB.put(thing.getUri(), SerializationUtils.jacksonMapper.writeValueAsString(thing));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T extends BaseModel> void post(Class<T> clazz, T thing) throws BadRequestException {
		String uuid = String.format("%s", UUID.randomUUID().toString());
		thing.setUri(uuid);
		try {
			jsonDB.put(uuid, SerializationUtils.jacksonMapper.writeValueAsString(thing));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T extends BaseModel> void patchAdd(Class<T> clazz, T thing, String fieldName, String newValue) {
		try {
			T foundThing = SerializationUtils.jacksonMapper.readValue(jsonDB.get(thing.getUri()), clazz);
			Method getter = clazz.getDeclaredMethod("get" + StringUtils.capitalize(fieldName));
			@SuppressWarnings("unchecked")
			List<String> invoke = (List<String>) getter.invoke(foundThing);
			// TODO NPE if list is undefined
			invoke.add(newValue);
			jsonDB.put(thing.getUri(), SerializationUtils.jacksonMapper.writeValueAsString(foundThing));
		} catch (IOException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
}
