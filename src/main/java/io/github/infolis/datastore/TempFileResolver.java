package io.github.infolis.datastore;

import io.github.infolis.ws.server.InfolisConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This is an extension of the {@link CentralFileResolver} but resolving from a
 * per-instance temporary directory instead of the directory returned by
 * {@link InfolisConfig#getFileSavePath()}.
 * 
 * @author kba
 *
 */
public class TempFileResolver extends CentralFileResolver {
	
	private Path tempDir;

	public TempFileResolver() throws IOException {
		this.tempDir = Files.createTempDirectory("infolis-");
	}
	
	@Override
	protected Path resolvePath(String fileId) {
		// TODO Auto-generated method stub
		return this.tempDir.resolve(fileId);
	}
}
