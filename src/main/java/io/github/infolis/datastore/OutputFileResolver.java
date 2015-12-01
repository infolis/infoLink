package io.github.infolis.datastore;

import io.github.infolis.model.entity.InfolisFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementations of this interface are able to resolve a textual ID or an
 * {@link InfolisFile} to {@link InputStream} / {@link OutputStream}
 *
 * @author kba
 *
 */
public interface OutputFileResolver extends FileResolver {

	/**
	 * @param outputFile
	 *            the {@link InfolisFile} that should be written to
	 * @return an open {@link OutputStream}
	 * @throws IOException
	 *             if the {@link OutputStream} could not be opened
	 */
	public OutputStream openOutputStream(InfolisFile file) throws IOException;

	/**
	 * Return the file contents for an Id as an {@link OutputStream}
	 *
	 * @param fileId
	 *            the id of the file
	 * @return the file contents as an {@link OutputStream}
	 * @throws IOException
	 *             if the stream cannot be opened
	 */
	public OutputStream openOutputStream(String fileId) throws IOException;
}
