package io.github.infolis.datastore;

import io.github.infolis.model.BaseModel;
import io.github.infolis.model.ErrorResponse;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.ws.server.InfolisConfig;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.JacksonFeature;
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

	private Logger logger = LoggerFactory.getLogger(CentralClient.class);

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
		return uriForClass.get(clazz);
	}

	private static Client jerseyClient = ClientBuilder.newBuilder()
			.register(JacksonFeature.class)
			.register(JacksonJsonProvider.class).build();

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
		if (resp.getStatus() != 201) {
			// TODO check whether resp actually succeeded
			ErrorResponse err = resp.readEntity(ErrorResponse.class);
			logger.error(err.getMessage());
			logger.error(Arrays.toString(err.getCause().entrySet().toArray()));
			throw new BadRequestException(resp);
		} else {
			thing.setUri(resp.getHeaderString("Location"));
			logger.debug("URI of Posted {}: {}", clazz.getSimpleName(), thing.getUri());
		}
	}

	@Override
	public <T extends BaseModel> T get(Class<T> clazz, URI uri) {
		logger.debug("{}", uri);
		if (!uri.isAbsolute()) {
			String msg = "URI must be absolute, " + uri + " is NOT.";
			logger.error(msg);
			throw new RuntimeException(msg);
		}
		WebTarget target = jerseyClient.target(uri);
		Response resp = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		T thing = resp.readEntity(clazz);

		thing.setUri(target.getUri().toString());
		return thing;
	}

	@Override
	public <T extends BaseModel> T get(Class<T> clazz, String id) {
		WebTarget target = jerseyClient
				.target(InfolisConfig.getFrontendURI())
				.path(CentralClient.getUriForClass(clazz))
				.path(id);
		Response resp = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		T thing = resp.readEntity(clazz);

		thing.setUri(target.getUri().toString());
		return thing;
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
		Response resp = target.request(MediaType.APPLICATION_JSON_TYPE).put(entity);
		if (resp.getStatus() != 201) {
			// TODO check whether resp actually succeeded
			ErrorResponse err = resp.readEntity(ErrorResponse.class);
			logger.error(err.getMessage());
			logger.error(Arrays.toString(err.getCause().entrySet().toArray()));
			throw new BadRequestException(resp);
		}

	}
}
