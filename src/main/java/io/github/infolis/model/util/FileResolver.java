package io.github.infolis.model.util;

import io.github.infolis.model.InfolisFile;
import io.github.infolis.ws.server.InfolisApplicationConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static class encapsulating file storage.
 * 
 * @author kba
 *
 */
public class FileResolver {

	private static Logger logger = LoggerFactory.getLogger(FileResolver.class);
	/*
	 * File IDs should be hexadecimal hashsums, i.e. only alnum, either 32 (md5)
	 * or 40 (sha1) bytes long
	 */
	private static Pattern VALID_ID_RE = Pattern
			.compile("^(?:\\p{Alnum}{32}|\\p{Alnum}{40})$");

	/**
	 * Resolve a filename to a {@link java.nio.file.Path} relative to the base
	 * directory defined by the {@link InfolisApplicationConfig}
	 * 
	 * @param fileId
	 *            the id to resolve
	 * @return the absolute path to the file
	 */
	private static Path resolvePath(String fileId) {
		return InfolisApplicationConfig.getFileSavePath().resolve(fileId);
	}

	/**
	 * Validate a file ID
	 * 
	 * @param fileId
	 *            the id to validate
	 * @throws IllegalArgumentException
	 *             if the file id is not a hexadecimal MD5/SHA-1 checksum
	 */
	public static void validateFileId(String fileId)
			throws IllegalArgumentException {
		Matcher matcher = VALID_ID_RE.matcher(fileId);
		if (!matcher.find())
			throw new IllegalArgumentException("Bad File ID: " + fileId);
	}

	/**
	 * Get the file contents as an {@link OutputStream}.
	 * 
	 * @param fileId
	 *            the id of the file
	 * @return InputStream the file contents
	 * @throws FileNotFoundException
	 *             if the file doesn't exist
	 * @throws IOException
	 *             if the file can't be read
	 */
	public static InputStream getInputStream(String fileId) throws IOException {
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

	/**
	 * Return the file contents for an Id as an {@link OutputStream}
	 * 
	 * @param fileId
	 *            the id of the file
	 * @return the file contents as an {@link OutputStream}
	 * @throws IOException
	 *             if the stream cannot be opened
	 */
	public static OutputStream getOutputStream(String fileId)
			throws IOException {
		Path fileName = resolvePath(fileId);
		logger.debug("Writing data to file {}", fileName);
		OutputStream outStream = Files
				.newOutputStream(fileName,
						StandardOpenOption.TRUNCATE_EXISTING,
						StandardOpenOption.CREATE);
		return outStream;
	}

	/**
	 * @see #getInputStream(String)
	 * @param pdfFile
	 *            {@link InfolisFile} referencing the PDF
	 * @return open InputStream
	 * @throws IOException
	 *             if the stream can not be opened
	 */
	public static InputStream getInputStream(InfolisFile pdfFile)
			throws IOException {
		return getInputStream(pdfFile.getMd5());
	}

	/**
	 * Get a hex representation of the MD5 checksum of an array of bytes.
	 * 
	 * @param bytes
	 *            array of bytes
	 * @return lower-case hex digest of the input
	 */
	public static final String getHexMd5(byte[] bytes) {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			/**
			 * This really, really cannot happen, because "MD5" = String
			 * constant.
			 */
		}
		digest.update(bytes);
		String md5 = DatatypeConverter.printHexBinary(digest.digest()).toLowerCase();
		return md5;
	}

	/**
	 * @see #getHexMd5(byte[])
	 * @param asText
	 *            String to calulate the MD5 {@link MessageDigest} for
	 */
	public static final String getHexMd5(String asText) {
		return getHexMd5(asText.getBytes());
	}

	/**
	 * @param outputFile
	 *            the {@link InfolisFile} that should be written to
	 * @return an open {@link OutputStream}
	 * @throws IOException
	 *             if the {@link OutputStream} could not be opened
	 */
	public static OutputStream getOutputStream(InfolisFile outputFile)
			throws IOException {
		return getOutputStream(outputFile.getMd5());
	}

	/**
	 * Replace the file extension of a file with a new extension
	 * 
	 * @param fileName
	 *            the original file name
	 * @param ext
	 *            the new extension
	 * @return the file name with a new extension
	 */
	public static String changeFileExtension(String fileName, String ext) {
		return fileName.replaceFirst("\\.[^\\.]+$", "." + ext);
	}

}
