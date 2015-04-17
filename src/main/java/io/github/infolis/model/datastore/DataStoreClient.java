package io.github.infolis.model.datastore;

import io.github.infolis.model.BaseModel;

import java.net.URI;

import javax.ws.rs.BadRequestException;

public interface DataStoreClient {

	/**
	 * GET a thing with a textual ID.
	 * 
	 * Uses the {@link CentralClient#uriForClass} mapping.
	 * 
	 * @param clazz
	 *            the class of the thing to retrieve
	 * @param id
	 *            the ID part of the URI of the thing to retrieve
	 * @return the server representation of the thing
	 */
	<T extends BaseModel> T get(Class<T> clazz, String id) throws BadRequestException;

	/**
	 * PUT an resource
	 * 
	 * @param clazz
	 * @param thing
	 */
	<T extends BaseModel> void put(Class<T> clazz, T thing) throws BadRequestException;

	/**
	 * GET a thing with a URI.
	 * 
	 * @param clazz
	 *            the class of the thing to retrieve
	 * @param uri
	 *            the {@link URI} of the thing to retrieve
	 * @return the server representation of the thing
	 */
	<T extends BaseModel> T get(Class<T> clazz, URI uri) throws BadRequestException;

	/**
	 * POST a resource to the frontend web service.
	 * 
	 * After successfully creation, {@link CentralClient#get(Class, URI)}
	 * request is made and the current representation returned.
	 * 
	 * @param clazz
	 *            class of the thing to post
	 * @param thing
	 *            the thing
	 * @return the server representation of the thing
	 */
	<T extends BaseModel> void post(Class<T> clazz, T thing) throws BadRequestException;

//	/**
//	 * GET a resource from the data store. If it does not exist, create it.
//	 * 
//	 * @param clazz
//	 * @param id
//	 * @return
//	 */
//	<T extends BaseModel> T getOrPost(Class<T> clazz, String id);

}
