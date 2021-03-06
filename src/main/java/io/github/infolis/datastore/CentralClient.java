package io.github.infolis.datastore;

import io.github.infolis.InfolisConfig;
import io.github.infolis.model.BaseModel;
import io.github.infolis.model.ErrorResponse;
import io.github.infolis.util.SerializationUtils;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * Client to access the Linked Data frontend web services.
 *
 * @author kba
 *
 */
class CentralClient extends AbstractClient {

	private Logger log = LoggerFactory.getLogger(CentralClient.class);

	/*
	 * <pre>
	 * Add mappings from class name to uri fragment here, e.g.
	 * http://infolis-frontend/api/infolisFile/124
	 *                             \__/
	 *                            map this
	 * </pre>
	 */
	private static Map<Class<?>, String> uriForClass = new ImmutableMap.Builder<Class<?>, String>()
//		.put(InfolisFile.class, "file")
		.build();
                
	public static String getEndpointForClass(Class<?> clazz) {
		// if explicitly mapped
		if (uriForClass.containsKey(clazz)) {
			return uriForClass.get(clazz);
		}
		// otherwise use lcfirst-version of simplename ("QueryService" -> "queryService")
		String name = clazz.getSimpleName();
		char c[] = name.toCharArray();
		c[0] = Character.toLowerCase(c[0]);
		return new String(c);
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
		String err = resp.readEntity(String.class);
		if (resp.getStatus() >= 400) {
			//String err = resp.readEntity(String.class);
			log.error(err);
			throw new RuntimeException(err);
		} else {
			thing.setUri(resp.getHeaderString("Location"));
			log.debug("URI of Posted {}: {}", clazz.getSimpleName(), thing.getUri());
		}
	}

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
		String err = resp.readEntity(String.class);
		if (resp.getStatus() >= 400) {
			// TODO check whether resp actually succeeded
//			ErrorResponse err = resp.readEntity(ErrorResponse.class);
			log.error(err);
//			log.error(Arrays.toString(err.getCause().entrySet().toArray()));
			log.error(resp.toString());
			throw new BadRequestException(resp);
		}
	}
        
        public <T extends BaseModel> void put(Class<T> clazz, T thing, String uri) {            
            String id = uri;
		WebTarget target = jerseyClient.target(id);
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
			throw new RuntimeException(String.format("No such '%s': '%s'", clazz.getSimpleName(), target.getUri()));
		} else if (resp.getStatus() >= 400) {
			throw new WebApplicationException(resp);
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
	public void clear() {
		log.error("clear not supported");
		return;
	}

	@Override
	public void dump(Path directory, String basename) {
		log.error("dump not supported");
		return;
	}

    @Override
    public <T extends BaseModel> List<T> search(Class<T> clazz, Multimap<String, String> query) {
        StringBuilder qParamSB = new StringBuilder();
        for (Entry<String, String> entry : query.entries()) {
        	qParamSB.append(entry.getKey());
        	qParamSB.append(":");
        	try
			{
				qParamSB.append(URLEncoder.encode(entry.getValue().replace(":", "\\:"), "UTF-8"));
			} catch (UnsupportedEncodingException e)
			{
				throw new RuntimeException(e);
			}
			qParamSB.append(",");
        }
        String baseURI = InfolisConfig.getFrontendURI() + "/" + getUriForClass(clazz);
        String uri = baseURI;
        uri += "?q=" + qParamSB.toString();
        uri += "&max=" + Integer.MAX_VALUE;
        log.debug("Search for {}", uri);
        WebTarget target = jerseyClient.target(uri);
		Response resp = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		log.debug("-> HTTP {}", resp.getStatus());
		if (resp.getStatus() == 404) {
			throw new RuntimeException(String.format("No such '%s': '%s'", clazz.getSimpleName(), target.getUri()));
		} else if (resp.getStatus() >= 400) {
			throw new WebApplicationException(resp);
		}
		try {
			ObjectMapper mapper = SerializationUtils.jacksonMapper;
			CollectionType listOfTType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
			CollectionType listOfMapType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, HashMap.class);
			String readEntity = resp.readEntity(String.class);
//			log.debug("JSON before: {}", readEntity);
//			readEntity = readEntity.replaceAll("\"_id\":\"", "\"uri\":\""+baseURI+"/");
//			log.debug("JSON after: {}", readEntity);
			List<T> listOfTs = mapper.<List<T>>readValue(readEntity, listOfTType);
			List<Map> flatList = mapper.readValue(readEntity, listOfMapType);
			for (int i = 0; i < flatList.size(); i++)
			{
				listOfTs.get(i).setUri(baseURI + "/" + flatList.get(i).get("_id"));
			}
			return listOfTs;
		} catch (Exception e) {
			throw new ProcessingException(e);
		}

    }

}
