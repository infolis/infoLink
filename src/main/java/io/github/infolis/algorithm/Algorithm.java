package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;

import java.io.IOException;

import org.slf4j.Logger;

public interface Algorithm extends Runnable {

	/**
	 * Execute the algorithm.
	 * 
	 * @throws IOException
	 * 
	 */
	void execute()
			throws IOException;

	/**
	 * Validate the execution object linking this algorithm with its
	 * input/output parameters
	 */
	void validate()
			throws IllegalAlgorithmArgumentException;

	/**
	 * Get the execution context of this algorithm instance.
	 * 
	 * @return the {@link Execution} for this instance of the algorithm
	 */
	Execution getExecution();

	/**
	 * Set the execution context of this algorithm instance.
	 * 
	 * @param execution
	 */
	void setExecution(Execution execution);

	/**
	 * @return the {@link FileResolver} of this algorithm
	 */
	FileResolver getFileResolver();

	/**
	 * @param fileResolver
	 *            the {@link FileResolver} for this algorithm.
	 */
	void setFileResolver(FileResolver fileResolver);

	/**
	 * @return the {@link DataStoreClient} to use for this algorithm instance.
	 */
	DataStoreClient getDataStoreClient();

	/**
	 * @param dataStoreClient
	 *            set the {@link DataStoreClient} for this algorithm instance.
	 */
	void setDataStoreClient(DataStoreClient dataStoreClient);

	/**
	 * Log a DEBUG message to both the system logger and the execution.
	 * 
	 * @param log
	 *            {@link Logger} to log to
	 * @param fmt
	 *            Format string for {@link String#format(String, Object...)}
	 * @param args
	 *            Objects for {@link String#format(String, Object...)}
	 */
	void debug(Logger log, String fmt, Object... args);

	/**
	 * Log an INFO message to both the system logger and the execution.
	 * 
	 * @param log
	 *            {@link Logger} to log to
	 * @param fmt
	 *            Format string for {@link String#format(String, Object...)}
	 * @param args
	 *            Objects for {@link String#format(String, Object...)}
	 */
	void info(Logger log, String fmt, Object... args);

	/**
	 * Log a FATAL message to both the system logger and the execution.
	 * 
	 * @param log
	 *            {@link Logger} to log to
	 * @param fmt
	 *            Format string for {@link String#format(String, Object...)}
	 * @param args
	 *            Objects for {@link String#format(String, Object...)}
	 */
	void fatal(Logger log, String fmt, Object... args);

}
