package io.github.infolis.util;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.InfolisFile;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.StringJoiner;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang3.StringEscapeUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;

public class SerializationUtils {

	public final static ObjectMapper jacksonMapper = new ObjectMapper();

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
	 *            String to calculate the MD5 {@link MessageDigest} for
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
                Path p = null;
                //problems with leading slashs if using Windows...
                if (filename.startsWith("/")) {
                    p = Paths.get(filename.substring(1));
                } else {
                    p = Paths.get(filename);
                }
		return Paths.get(newBaseDir, p.getFileName().toString()).toString();
	}

	/**
	 * Escapes a string for integration into XML files.
	 *
	 * @param string	the string to be escaped
	 * @return			the escaped string
	 */
	public static String escapeXML(String string)
	{
	    String xml10pattern = "[^"
	                + "\u0009\r\n"
	                + "\u0020-\uD7FF"
	                + "\uE000-\uFFFD"
	                + "\ud800\udc00-\udbff\udfff"
	                + "]";
		return StringEscapeUtils.escapeXml(string).replaceAll(xml10pattern,"");
	}

	/**
	 * Returns an escaped XML string into its normal string representation.
	 *
	 * @param string	the string to be transformed
	 * @return			the transformed string
	 */
	public static String unescapeXML(String string)
	{
		return StringEscapeUtils.unescapeXml(string);
	}

	public static void writeContextToXML(TextualReference context, String filename) throws IOException {
		try {
			InfolisFileUtils.prepareOutputFile(filename);
			InfolisFileUtils.writeToFile(new File(filename), "UTF-8", context.toXML(), true);
			InfolisFileUtils.completeOutputFile(filename);
		}
		catch (IOException ioe) { ioe.printStackTrace(); throw new IOException();}
	}

	public static String dumpExecutionLog(Execution execution) {
		StringBuilder sb = new StringBuilder();
		sb.append("Log of ");
		sb.append(execution.getUri());
		sb.append("\n");
		for (String msg : execution.getLog()) {
			sb.append(msg);
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Create csv file from entity links. 
	 * Format: fromEntity.getIdentifiers()|fromEntity.getName()|linkReason.getTextFile()
	 * 			|toEntity.getIdentifiers()|toEntity.getName()|toEntity.getNumericInfo()|ref.toPrettyString()
	 * 
	 * @param entityLinks
	 * @param client
	 * @return
	 */
	public static String toCsv(List<String> entityLinks, DataStoreClient client) {
		String csv = "";
		for (String linkUri : entityLinks) {
			StringJoiner row = new StringJoiner("|", "", "");
			EntityLink link = client.get(EntityLink.class, linkUri);
			Entity fromEntity = client.get(Entity.class, link.getFromEntity());
			Entity toEntity = client.get(Entity.class, link.getToEntity());
			row.add(String.join(";", fromEntity.getIdentifiers()));
			row.add(fromEntity.getName());
			String textualReferenceString = " ";
			if (null != link.getLinkReason() && !link.getLinkReason().isEmpty()) {
				TextualReference ref = client.get(TextualReference.class, link.getLinkReason());
				row.add(client.get(InfolisFile.class, ref.getTextFile()).getFileName());
				textualReferenceString = ref.toPrettyString();
			} else row.add(" ");
			row.add(String.join(";", toEntity.getIdentifiers()));
			row.add(toEntity.getName());
			row.add(toEntity.getNumericInfo().toString());
			row.add(textualReferenceString);
			csv += row.toString() + "\n";
		}
		return csv.toString();
	}

}
