package io.github.infolis.model.util;

import java.io.StringWriter;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SerializationUtils {

	private final static ObjectMapper jacksonMapper = new ObjectMapper();

	public static String toXML(Object thing) {
		StringWriter sw = new StringWriter();
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(thing.getClass());
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(thing, sw);
			return sw.toString();
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Utility method to JSON-dump a POJO.
	 *
	 * @param object
	 *            the thing to map using {@link ObjectMapper}
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
	
	/**
	 * Change the base dir of a path.
	 * 
	 * <pre>
	 * {@code
	 * String oldPath = "/tmp/foo/quux.bar";
	 * String newPath = SerializationUtils.changeBaseDir(oldPath, "/home/bork")
	 * System.out.println(newPath); // "home/bork.quux.bar"
	 * }
	 * </pre>
	 * <code>/foo/bar/quux.baz</code>
	 * 
	 * @param filename
	 * @param newBaseDir
	 * @return
	 */
	public static String changeBaseDir(String filename, String newBaseDir) {
		return Paths.get(newBaseDir, Paths.get(filename).getFileName().toString()).toString();
	}

}
