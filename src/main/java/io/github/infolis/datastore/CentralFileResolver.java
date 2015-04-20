package io.github.infolis.datastore;

import io.github.infolis.model.InfolisFile;
import io.github.infolis.ws.server.InfolisConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileResolver encapsulating file storage in a central directory with files
 * named with the MD5 sum of the {@link InfolisFile}
 * 
 * @author kba
 *
 */
class CentralFileResolver implements FileResolver {

	private static Logger logger = LoggerFactory.getLogger(CentralFileResolver.class);
	/*
	 * File IDs should be hexadecimal hashsums, i.e. only alnum, either 32 (md5)
	 * or 40 (sha1) bytes long
	 */
	private static Pattern VALID_ID_RE = Pattern
			.compile("^(?:\\p{Alnum}{32}|\\p{Alnum}{40})$");

	/**
	 * Resolve a filename to a {@link java.nio.file.Path} relative to the base
	 * directory defined by the {@link InfolisConfig}
	 * 
	 * @param fileId
	 *            the id to resolve
	 * @return the absolute path to the file
	 */
	private static Path resolvePath(String fileId) {
		return InfolisConfig.getFileSavePath().resolve(fileId);
	}

	@Override
	public void validateFileId(String fileId) {
		Matcher matcher = VALID_ID_RE.matcher(fileId);
		if (!matcher.find())
			throw new IllegalArgumentException("Bad File ID: " + fileId);
	}

	@Override
	public InputStream openInputStream(String fileId) throws IOException {
		Path fileName = resolvePath(fileId);
		logger.debug("Reading data from file {}", fileName);
		InputStream ret;
		if (!Files.exists(fileName)) {
			throw new FileNotFoundException();
		} else {
			ret = Files.newInputStream(fileName);
		}
		return ret;
	}

	@Override
	public OutputStream openOutputStream(String fileId) throws IOException {
		Path fileName = resolvePath(fileId);
		logger.debug("Writing data to file {}", fileName);
		OutputStream outStream = Files
				.newOutputStream(fileName,
						StandardOpenOption.TRUNCATE_EXISTING,
						StandardOpenOption.CREATE);
		return outStream;
	}

	@Override
	public InputStream openInputStream(InfolisFile file) throws IOException{
		return openInputStream(file.getMd5());
	}

	@Override
	public OutputStream openOutputStream(InfolisFile outputFile) throws IOException {
		return openOutputStream(outputFile.getMd5());
	}

}
