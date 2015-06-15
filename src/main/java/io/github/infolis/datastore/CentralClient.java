package io.github.infolis.datastore;

import io.github.infolis.model.BaseModel;
import io.github.infolis.model.ErrorResponse;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.util.SerializationUtils;
import io.github.infolis.ws.server.InfolisConfig;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Client to access the Linked Data frontend web services.
 * 
 * @author kba
 *
 */
class CentralClient implements DataStoreClient {

	private Logger log = LoggerFactory.getLogger(CentralClient.class);

	@SuppressWarnings("rawtypes")
	private static Map<Class, String> uriForClass = new HashMap<>();
	static {
		/*
		 * Add mappings from class name to uri fragment here, e.g.
		 * http://infolis-frontend/api/file/124 \__/ map this
		 */
		uriForClass.put(InfolisFile.class, "file");
		uriForClass.put(Execution.class, "execution");
	}

	private static <T extends BaseModel> String getUriForClass(Class<T> clazz) {
		if (uriForClass.containsKey(clazz)) {
			return uriForClass.get(clazz);
		} else {
			String name = clazz.getSimpleName();
			return name.substring(0,1).toLowerCase() + name.substring(1);
		}
	}

	private static Client jerseyClient = ClientBuilder.newBuilder()
			.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
			.register(JacksonFeature.class)
			.register(MultiPartFeature.class)
			.register(JacksonJsonProvider.class)
			.build();

	@Override
	public <T extends BaseModel> void post(Class<T> clazz, T thing) throws BadRequestException {
		WebTarget target = jerseyClient
				.target(InfolisConfig.getFrontendURI())
				.path(CentralClient.getUriForClass(clazz));
		Entity<T> entity = Entity
				.entity(thing, MediaType.APPLICATION_JSON_TYPE);
		Response resp = target
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(entity);
		log.debug("-> {}", target);
		log.debug("<- HTTP {}", resp.getStatus());
		if (resp.getStatus() >= 400) {
			// TODO check whether resp actually succeeded
			ErrorResponse err = resp.readEntity(ErrorResponse.class);
			log.error(err.getMessage());
			log.error(Arrays.toString(err.getCause().entrySet().toArray()));
			throw new BadRequestException(resp + err.getMessage());
		} else {
			thing.setUri(resp.getHeaderString("Location"));
			log.debug("URI of Posted {}: {}", clazz.getSimpleName(), thing.getUri());
		}
	}

	@Override
	public <T extends BaseModel> void put(Class<T> clazz, T thing) {
		String thingURI = thing.getUri();
		if (null == thingURI) {
			throw new IllegalArgumentException("PUT requires a URI: " + thing);
		}
		WebTarget target = jerseyClient.target(URI.create(thingURI));
		Entity<T> entity = Entity
				.entity(thing, MediaType.APPLICATION_JSON_TYPE);
		Response resp = target
				.request(MediaType.APPLICATION_JSON_TYPE)
				.put(entity);
		if (resp.getStatus() >= 400) {
			// TODO check whether resp actually succeeded
//			ErrorResponse err = resp.readEntity(ErrorResponse.class);
//			log.error(err.getMessage());
//			log.error(Arrays.toString(err.getCause().entrySet().toArray()));
			throw new BadRequestException(resp);
		}

	}


	@Override
	public <T extends BaseModel> T get(Class<T> clazz, String uriStr) {
		URI uri = URI.create(uriStr);
		log.debug("GET from {}", uri);
		if (!uri.isAbsolute()) {
			String msg = "URI must be absolute, " + uri + " is NOT.";
			log.error(msg);
			throw new ProcessingException(msg);
		}
		WebTarget target = jerseyClient.target(uri);
		Response resp = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		log.debug("-> HTTP {}", resp.getStatus());
		if (resp.getStatus() == 404) {
			throw new BadRequestException(String.format("No such '%s': '%s'", clazz.getSimpleName(), target.getUri()));
		} else if (resp.getStatus() >= 400) {
			throw new BadRequestException(resp);
		}
		T thing;
		try {
			thing = resp.readEntity(clazz);
		} catch (Exception e) {
			throw new ProcessingException(e);
		}

		thing.setUri(target.getUri().toString());
		return thing;
	}

	@Override
	public <T extends BaseModel> void patchAdd(Class<T> clazz, T thing, String fieldName, String newValue) {
		try {
			Method method = clazz.getDeclaredMethod("get" + StringUtils.capitalize(fieldName), List.class);
			@SuppressWarnings("unchecked")
			List<String> invoke = (List<String>) method.invoke(thing);
			invoke.add(newValue);
			
			Map<String, String> patch = new HashMap<String, String>();
			patch.put("op", "add");
			patch.put("path", String.format("/%s/-", fieldName));
			patch.put("value", newValue);
			
			WebTarget target = jerseyClient.target(URI.create(thing.getUri()));
			Entity<String> entity = Entity.entity(SerializationUtils.toJSON(patch), MediaType.APPLICATION_JSON_TYPE);
			target.request(MediaType.APPLICATION_JSON_TYPE).method("PATCH", entity);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new ProcessingException(e);
		}
		
	}

	// TODO kba incomplete
	@Override
	@SuppressWarnings("unused")
	public InfolisFile upload(InfolisFile file, InputStream input) throws IOException {
		WebTarget target = jerseyClient
				.target(InfolisConfig.getFrontendURI())
				.path("upload");
		FormDataMultiPart fdm = new FormDataMultiPart();
		fdm.field("mediaType", file.getMediaType());
		fdm.field("fileStatus", file.getFileStatus());
		fdm.field("fileName", file.getFileName());
		StreamDataBodyPart filePart = new StreamDataBodyPart(
				"file",
				input);
		return null;
	}
}
