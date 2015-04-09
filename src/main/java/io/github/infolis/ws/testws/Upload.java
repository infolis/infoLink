package io.github.infolis.ws.testws;

import io.github.infolis.ws.InfolisApplicationConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

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
public class Upload {
	
    Logger logger = LoggerFactory.getLogger(Upload.class);
	
	/**
	 * PUT /upload/d3b07384d113edec49eaa6238ad5ff00  -
	 * @param fileId The id of the file, e.g. it's sha1 or md5 checksum
	 * @param requestBody The file data, directly in the body
	 * @throws IOException
	 */
	@PUT
	@Path("{id}")
	public Response putFile(
			@PathParam("id") String fileId,
			InputStream requestBody) throws IOException {
		java.nio.file.Path fileName = InfolisApplicationConfig.getFileSavePath().resolve(fileId);
		logger.debug("Writing data to file {}", fileName);
		OutputStream outStream = Files.newOutputStream(fileName, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
		IOUtils.copy(requestBody, outStream);
		outStream.close();
		return Response.status(200).build();
	}
	
	/**
	 * GET /upload/d3b07384d113edec49eaa6238ad5ff00  -
	 * @param fileId The id of the file
	 * @return
	 */
	@GET
	@Path("{id}")
	public Response getFile(
			@PathParam("id") String fileId
			) {
		java.nio.file.Path fileName = InfolisApplicationConfig.getFileSavePath().resolve(fileId);
		if (! Files.exists(fileName)) {
			return Response.status(404).build();
		} else {	
            try {
				return Response.ok(Files.newInputStream(fileName)).build();
			} catch (IOException e) {
                return Response.status(400).entity("Couldn't read " + fileName + "!").build();
			}
		}
	}
		
}
