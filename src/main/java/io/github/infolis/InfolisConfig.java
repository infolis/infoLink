package io.github.infolis;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration of the Infolis Web Services.
 * 
 * @author kba
 */
public class InfolisConfig {

	private static final String CONFIG_PROPERTIES_NAME = "infolis-config.properties";
	private static final Logger log = LoggerFactory.getLogger(InfolisConfig.class);
	private static final ArrayList<String> pathsToSearch = new ArrayList<>();

	private static final InfolisConfig INSTANCE;

	static {
		// Setup paths to look for config
		pathsToSearch.add("/etc");
		pathsToSearch.add(System.getProperty("user.home"));
		pathsToSearch.add(System.getProperty("user.dir"));

		// Instantiate
		INSTANCE = new InfolisConfig();
		// Make sure the configuration is loaded and valid
		try {
			validate();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	Properties prop;
	String loadedFrom = null;

	/**
	 * Looks for a properties file first in /etc/infolis-ws.properties. If not
	 * found, loads defaults from classpath.
	 * 
	 */
	private InfolisConfig() {
		prop = new Properties();

		// Load default config
		try {
			InputStream inStream = InfolisConfig.class.getClassLoader().getResourceAsStream(CONFIG_PROPERTIES_NAME);
			String classPath = InfolisConfig.class.getClassLoader().getResource(CONFIG_PROPERTIES_NAME).getFile().toString();
			log.debug("Classpath: " + classPath);
			prop.load(inStream);
		} catch (IOException | NullPointerException e) {
			System.err.println("Couldn't load properties from classpath, deployment broken.");
			e.printStackTrace();
			System.exit(100);
		}

		// Merge configs found in search paths
		for (String dir : pathsToSearch) {
			Path path = Paths.get(dir, CONFIG_PROPERTIES_NAME);
			try {
				Properties configFound = new Properties();
				configFound.load(Files.newInputStream(path));
				prop.putAll(configFound);
				log.debug("Loaded properties from '{}'", path);
			} catch (IOException e) {
				log.debug("Couldn't load properties from '{}'.", path);
			}
		}
		// TODO debug output
		log.debug("Found config: {}", prop);
	}

	/**
	 * Ensures that the config options are valid, directories exist and such.
	 * 
	 * @throws IOException
	 */
	public static void validate() throws IOException {
		if (!Files.exists(getFileSavePath())) {
			Files.createDirectories(getFileSavePath());
		}
		if (!Files.exists(getTmpFilePath())) {
			Files.createDirectories(getTmpFilePath());
		}
	}

	/**
	 * Property "fileSavePath"
	 * 
	 * @return {@link Path} to the directory where files are to be saved
	 */
	public static Path getFileSavePath() {
		Path path = Paths.get(INSTANCE.prop.getProperty("fileSavePath"));
		return path;
	}
	
	/**
	 * Property "tmpFilePath"
	 * 
	 * @return {@link Path} to the directory where temporary files are to be saved
	 */
	public static Path getTmpFilePath() {
		Path path = Paths.get(INSTANCE.prop.getProperty("tmpFilePath"));
		return path;
	}

	/**
	 * Property "frontendURI"
	 * 
	 * @return {@link URI} of the frontend Linked Data web service
	 */
	public static URI getFrontendURI() {
		return URI.create(INSTANCE.prop.getProperty("frontendURI"));
	}

	/**
	 * Property "ignoreStudy"
	 * 
	 * @return
	 */
	public static List<String> getIgnoreStudy() {
		return Arrays.asList(INSTANCE.prop.getProperty("ignoreStudy").trim().split("\\s*,\\s*"));
	}

	/**
	 * Property "bibliographyCues"
	 * 
	 * @return
	 */
	public static List<String> getBibliographyCues() {
		return Arrays.asList(INSTANCE.prop.getProperty("bibliographyCues").trim().split("\\s*,\\s*"));
	}

	/**
	 * Property "tagCommand"
	 * 
	 * @return
	 */
	public static String getTagCommand() {
		return INSTANCE.prop.getProperty("tagCommand");
	}

	/**
	 * Property "chunkCommand"
	 * 
	 * @return
	 */
	public static String getChunkCommand() {
		return INSTANCE.prop.getProperty("chunkCommand");
	}
}
