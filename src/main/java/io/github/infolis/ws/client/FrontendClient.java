package io.github.infolis.ws.client;

import io.github.infolis.model.BaseModel;
import io.github.infolis.model.ErrorResponse;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.ws.server.InfolisApplicationConfig;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Client to access the Linked Data frontend web services.
 * 
 * @author kba
 *
 */
public class FrontendClient {

	private static Logger logger = LoggerFactory.getLogger(FrontendClient.class);
	
	private final static ObjectMapper jacksonMapper = new ObjectMapper();

	@SuppressWarnings("rawtypes")
	private final static Map<Class, String> uriForClass = new HashMap<>();
	static {
		/*
		 * Add mappings from class name to uri fragment here, e.g.
		 * http://infolis-frontend/api/file/124
		 *                             \__/
		 *                            map this
		 */
		uriForClass.put(InfolisFile.class, "file");
	}

	private static <T extends BaseModel> String getUriForClass(Class<T> clazz) {
		return uriForClass.get(clazz);
	}

	private static Client jerseyClient = ClientBuilder.newBuilder()
				.register(JacksonFeature.class)
				.register(JacksonJsonProvider.class).build();

	/**
	 * POST a resource to the frontend web service.
	 * 
	 * After successfully creation, {@link FrontendClient#get(Class, URI)}
	 * request is made and the current representation returned.
	 * 
	 * @param clazz
	 *            class of the thing to post
	 * @param thing
	 *            the thing
	 * @return the server representation of the thing
	 */
	public static <T extends BaseModel> T post(Class<T> clazz, T thing) throws BadRequestException {
		WebTarget target = jerseyClient
				.target(InfolisApplicationConfig.getFrontendURI())
				.path(FrontendClient.getUriForClass(clazz));
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
			return get(clazz, URI.create(resp.getHeaderString("Location")));
		}
	}

	/**
	 * GET a thing with a URI.
	 * 
	 * @param clazz
	 *            the class of the thing to retrieve
	 * @param uri
	 *            the {@link URI} of the thing to retrieve
	 * @return the server representation of the thing
	 */
	public static <T extends BaseModel> T get(Class<T> clazz, URI uri) {
		logger.debug("{}", uri);
		WebTarget target = jerseyClient.target(uri);
		Response resp = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		T thing = resp.readEntity(clazz);
		thing.setUri(resp.getHeaderString("Location"));
		return thing;
	}

	/**
	 * Get a thing with a textual ID.
	 * 
	 * Uses the {@link FrontendClient#uriForClass} mapping.
	 * 
	 * @param clazz
	 *            the class of the thing to retrieve
	 * @param id
	 *            the ID part of the URI of the thing to retrieve
	 * @return the server representation of the thing
	 */
	public static <T extends BaseModel> T get(Class<T> clazz, String id) {
		WebTarget target = jerseyClient
				.target(InfolisApplicationConfig.getFrontendURI())
				.path(FrontendClient.getUriForClass(clazz))
				.path(id);
		Response resp = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		T thing = resp.readEntity(clazz);
		thing.setUri(resp.getHeaderString("Location"));
		return thing;
	}

	/**
	 * Utility method to JSON-dump a POJO.
	 *
	 * @param object the thing to map using {@link ObjectMapper}
	 * @return the thing as JSON-encoded String
	 */
	public static String toJSON(Object object) {
		String asString = null;
		try {
			asString = jacksonMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return asString;
	}
}
