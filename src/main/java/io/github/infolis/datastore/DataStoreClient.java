package io.github.infolis.datastore;

import io.github.infolis.model.BaseModel;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;

import com.google.common.collect.Multimap;

public interface DataStoreClient {

	/**
	 * PUT an resource
	 *
	 * @param clazz
	 * @param thing
	 */
	<T extends BaseModel> void put(Class<T> clazz, T thing) throws BadRequestException;

        /**
	 * PUT a resource with a specific URI
	 *
	 * @param clazz
	 * @param thing
	 */
	<T extends BaseModel> void put(Class<T> clazz, T thing, String uri) throws BadRequestException;
        
	/**
	 * GET a thing with a URI.
	 *
	 * @param clazz
	 *            the class of the thing to retrieve
	 * @param uriStr
	 *            string representing a {@link URI} of the thing to retrieve
	 * @return the server representation of the thing
	 * @throws BadRequestException
	 * @throws ProcessingException
	 */
	<T extends BaseModel> T get(Class<T> clazz, String uriStr) throws BadRequestException, ProcessingException;

	/**
	 * GET a list of things for a list of URI.
	 *
	 * NOTE: It's a {@link List} and not a {@link Set} because we cannot be sure all {@link BaseModel} instances implement equals/hashcode.
	 *
	 * @param clazz
	 * 			the class of the thing to retrieve
	 * @param uriStrList
	 * 			{@link List<String>} of strings of the URI of the things
	 * @return the list of server representations of the things
	 * @throws BadRequestException
	 * @throws ProcessingException
	 */
	<T extends BaseModel> List<T> get(Class<T> clazz, Iterable<String> uriStrList) throws BadRequestException, ProcessingException;

	/**
	 * POST a resource to the frontend web service.
	 *
	 * After successfully creation, {@link CentralClient#get(Class, URI)}
	 * request is made and the current representation returned. The URI of the passed
	 * resource will be set to the newly created URI
	 *
	 * @param clazz
	 *            class of the thing to post
	 * @param thing
	 *            the thing
	 * @return the server representation of the thing
	 */
	<T extends BaseModel> void post(Class<T> clazz, T thing) throws BadRequestException;

	/**
	 * POST a list of things to the frontend web service.
	 *
	 * After successfully creation, {@link CentralClient#get(Class, URI)}
	 * request is made and the current representation returned. The URI of the passed
	 * resource will be set to the newly created URI
	 *
	 * @param clazz
	 *            class of the thing to post
	 * @param thing
	 *            the thing
	 * @return a list of URIs
	 */
	<T extends BaseModel> List<String> post(Class<T> clazz, Iterable<T> thingList) throws BadRequestException;

	/**
	 * DELETE the complete datastore.
	 *
	 */
	void clear();

	/**
	 * GET all things that have any of the key-value pairs (boolean OR).
	 *
	 * @param clazz
	 *             class of the things to get
	 * @param query
	 *             a {@link Multimap}Map of key-value pairs to query for (exact)
	 * @return
	 */
	<T extends BaseModel> List<T> search(Class<T> clazz, Multimap<String, String> query);

	/**
	 * Dump the whole datastore in JSON format.
	 * @param directory Directory to dump to
	 */
	void dump(Path directory, String dumpName);
}
