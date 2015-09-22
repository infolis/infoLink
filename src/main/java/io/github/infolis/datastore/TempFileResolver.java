package io.github.infolis.datastore;

import io.github.infolis.InfolisConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an extension of the {@link CentralFileResolver} but resolving from a
 * per-instance temporary directory instead of the directory returned by
 * {@link InfolisConfig#getFileSavePath()}.
 * 
 * @author kba
 *
 */
public class TempFileResolver extends CentralFileResolver {
	
	private static final Logger log = LoggerFactory.getLogger(TempFileResolver.class);
	
	private Path tempDir;

	public TempFileResolver() {
		try {
			this.tempDir = Files.createTempDirectory("infolis-");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected Path resolvePath(String fileId) {
		log.debug("ID to resolve: {}", fileId);
		return this.tempDir.resolve(fileId);
	}
}
