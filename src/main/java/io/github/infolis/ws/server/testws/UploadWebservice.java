package io.github.infolis.ws.server.testws;

import io.github.infolis.model.util.FileResolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple upload/download handler.
 * 
 * @author kba
 *
 */
@Path("upload")
public class UploadWebservice {
	
    Logger logger = LoggerFactory.getLogger(UploadWebservice.class);
	
	/**
	 * PUT /upload/d3b07384d113edec49eaa6238ad5ff00  -
	 * @param fileId The id of the file, e.g. it's sha1 or md5 checksum
	 * @param requestBody The file data, directly in the body
	 */
	@PUT
	@Path("{id}")
	public Response putFile(
			@PathParam("id") String fileId,
			InputStream requestBody) {
		Response ret = null;
		try {
			FileResolver.validateFileId(fileId);
            OutputStream outStream = FileResolver.getOutputStream(fileId);
            IOUtils.copy(requestBody, outStream);
            outStream.close();
            ret = Response.created(new URI(fileId)).build();
		} catch (IllegalArgumentException | IOException | URISyntaxException e) {
			ret = Response.status(400).entity("Error creating file: " + e.getMessage()).build();
		}
		return ret;
	}
	
	/**
	 * GET /upload/d3b07384d113edec49eaa6238ad5ff00  -
	 * @param fileId The id of the file
	 * @return the contents of the file
	 */
	@GET
	@Path("{id}")
	public Response getFile(@PathParam("id") String fileId) {
		Response ret = null;
		try {
			FileResolver.validateFileId(fileId);
			InputStream inputStream = FileResolver.getInputStream(fileId);
            ret = Response.ok(inputStream).build();
		} catch (FileNotFoundException fnfe) {
			ret = Response.status(404).entity(String.format("No such file '%s'.", fileId)).build();
		} catch (IllegalArgumentException | IOException e) {
			ret = Response.status(400).entity("Error reading file: " + e.getMessage()).build();
		}
		return ret;
	}
		
}
