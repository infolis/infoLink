package io.github.infolis.datastore;

import io.github.infolis.InfolisConfig;
import io.github.infolis.model.entity.InfolisFile;

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

	private static final Logger log = LoggerFactory.getLogger(CentralFileResolver.class);

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
	protected Path resolvePath(String fileId) {
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
		logger.trace("Reading data from file {}", fileName);
		InputStream ret;
		if (!Files.exists(fileName)) {
			throw new FileNotFoundException(fileName.toString());
		} else {
			ret = Files.newInputStream(fileName);
		}
		return ret;
	}

	@Override
	public OutputStream openOutputStream(String fileId) throws IOException {
		Path fileName = resolvePath(fileId);
		logger.trace("Writing data to file {}", fileName);
		OutputStream outStream = Files
				.newOutputStream(fileName,
						StandardOpenOption.TRUNCATE_EXISTING,
						StandardOpenOption.CREATE);
		return outStream;
	}

	@Override
	public InputStream openInputStream(InfolisFile inputFile) throws IOException{
		return openInputStream(inputFile.getMd5());
	}

	@Override
	public OutputStream openOutputStream(InfolisFile outputFile) throws IOException {
		return openOutputStream(outputFile.getMd5());
	}

}
