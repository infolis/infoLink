package io.github.infolis.model.file;

import io.github.infolis.model.InfolisFile;

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
public interface FileResolver {

	/**
	 * Validate a file ID
	 * 
	 * @param fileId
	 *            the id to validate
	 * @throws IllegalArgumentException
	 *             if the file id is not valid according to what the
	 *             implementation uses as ID
	 */
	public void validateFileId(String fileId) throws IllegalArgumentException;

	/**
	 * @param outputFile
	 *            the {@link InfolisFile} that should be written to
	 * @return an open {@link OutputStream}
	 * @throws IOException
	 *             if the {@link OutputStream} could not be opened
	 */
	public OutputStream openOutputStream(InfolisFile file) throws IOException;

	/**
	 * @see #openInputStream(String)
	 * @param file
	 *            {@link InfolisFile} referencing the PDF
	 * @return open InputStream
	 * @throws IOException
	 *             if the stream can not be opened
	 */
	public InputStream openInputStream(InfolisFile file) throws IOException;

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
	public InputStream openInputStream(String fileId) throws IOException;

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
