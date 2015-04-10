package io.github.infolis.ws.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration of the Infolis Web Services.
 * 
 * @author kba
 */
public class InfolisApplicationConfig {
	
	private static final InfolisApplicationConfig INSTANCE = new InfolisApplicationConfig();
	
	static {
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
	 * Looks for a properties file first in /etc/infolis-ws.properties. If not found, loads defaults from classpath.
	 * 
	 */
	private InfolisApplicationConfig() {
		prop = new Properties();
		try {
			String etcPath = "/etc/infolis-ws.properties";
			FileInputStream inStream = new FileInputStream(etcPath);
			prop.load(inStream);
			loadedFrom = etcPath;
		} catch (IOException fnfe) {
			System.out.println("Couldn't load properties from '/etc/infolis-ws.properties', reverting to default");
			try {
				String resourceName = "infolis-ws.properties";
                InputStream inStream = InfolisApplicationConfig.class.getClassLoader().getResourceAsStream(resourceName);
				prop.load(inStream);
				loadedFrom = resourceName;
			} catch (IOException | NullPointerException e) {
				System.out.println("Couldn't load properties from classpath, deployment broken.");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Ensures that the config options are valid, directories exist and such.
	 * @throws IOException 
	 */
	public static void validate() throws IOException {
		if (null == INSTANCE.loadedFrom) {
			throw new IOException("Properties weren't loaded!");
		}
		if (! Files.exists(getFileSavePath())) {
			Files.createDirectories(getFileSavePath());
		}
	}

	/**
	 * Property "fileSavePath"
	 * @return Path to the directory where files are to be saved
	 */
	public static Path getFileSavePath() {
		Path path = Paths.get(INSTANCE.prop.getProperty("fileSavePath"));
		return path;
	}

}
