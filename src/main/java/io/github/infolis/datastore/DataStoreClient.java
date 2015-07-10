package io.github.infolis.datastore;

import io.github.infolis.model.BaseModel;

import java.net.URI;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;

public interface DataStoreClient {

//	/**
//	 * GET a thing with a textual ID.
//	 * 
//	 * Uses the {@link CentralClient#uriForClass} mapping.
//	 * 
//	 * @param clazz
//	 *            the class of the thing to retrieve
//	 * @param id
//	 *            the ID part of the URI of the thing to retrieve
//	 * @return the server representation of the thing
//	 * @throws BadRequestException
//	 * @throws ProcessingException
//	 */
//	<T extends BaseModel> T getById(Class<T> clazz, String id) throws BadRequestException, ProcessingException;

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
	 * @param uriStr
	 *            string representing a {@link URI} of the thing to retrieve
	 * @return the server representation of the thing
	 * @throws BadRequestException
	 * @throws ProcessingException
	 */
	<T extends BaseModel> T get(Class<T> clazz, String uriStr) throws BadRequestException, ProcessingException;

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
	
	<T extends BaseModel> void patchAdd(Class<T> clazz, T thing, String fieldName, String newValue);

}
