package io.github.infolis.datastore;

import io.github.infolis.model.entity.InfolisFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * {@link FileResolver} using the <code>fileName</code> of an
 * {@link InfolisFile}.
 *
 * @author kba
 */
class LocalFileResolver implements FileResolver {

	@Override
	public void validateFileId(String fileId) {
		// TODO Deliberately do nothing for now
		// TODO We could check whether path is absolute or whether parent dir
		// exists
	}

	@Override
	public OutputStream openOutputStream(InfolisFile file) throws IOException {
		return openOutputStream(file.getFileName());
	}

	@Override
	public InputStream openInputStream(InfolisFile file) throws IOException {
		return openInputStream(file.getFileName());
	}

	@Override
	public InputStream openInputStream(String fileId) throws IOException {
		Path path = Paths.get(fileId);
		return Files.newInputStream(path);
	}

	@Override
	public OutputStream openOutputStream(String fileId) throws IOException {
		Path path = Paths.get(fileId);
		OutputStream newOutputStream = Files.newOutputStream(path,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
		return newOutputStream;
	}

}
