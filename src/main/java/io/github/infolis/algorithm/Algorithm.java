package io.github.infolis.algorithm;

import io.github.infolis.datastore.AbstractClient;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.LocalClient;
import io.github.infolis.datastore.TempFileResolver;
import io.github.infolis.model.Execution;

import java.io.IOException;

import org.slf4j.Logger;

public interface Algorithm  {

	/**
	 * Execute the algorithm.
	 * 
	 * @throws IOException
	 * 
	 */
	void execute()
			throws IOException;

        /**
	 * Run the algorithm.
	 * 
	 * 
	 */
	public void run();
        
	/**
	 * Validate the execution object linking this algorithm with its
	 * input/output parameters
         * 
         * @throws io.github.infolis.algorithm.IllegalAlgorithmArgumentException
	 */
	void validate()
			throws IllegalAlgorithmArgumentException;

        
        void updateProgress(int done, int total);
        
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
	FileResolver getOutputFileResolver();

	/**
	 * @return the {@link FileResolver} of this algorithm
	 */
	FileResolver getInputFileResolver();

	/**
	 * @return the {@link DataStoreClient} to use for this algorithm instance.
	 */
	DataStoreClient getInputDataStoreClient();

	/**
	 * @return the {@link DataStoreClient} to use for this algorithm instance.
	 */
	DataStoreClient getOutputDataStoreClient();
	
	/**
	 * @return a temporary {@link TempFileResolver}
	 */
	TempFileResolver getTempFileResolver();


	/**
	 * @return the {@link LocalClient} for temporary storage.
	 */
	AbstractClient getTempDataStoreClient();

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
	
	/**
	 * Log an ERROR message to both the system logger and the execution.
	 * 
	 * @param log
	 *            {@link Logger} to log to
	 * @param fmt
	 *            Format string for {@link String#format(String, Object...)}
	 * @param args
	 *            Objects for {@link String#format(String, Object...)}
	 */
	void error(Logger log, String fmt, Object... args);

}
