package io.github.infolis.algorithm;

import io.github.infolis.model.Execution;
import io.github.infolis.model.datastore.DataStoreClient;
import io.github.infolis.model.datastore.FileResolver;

public interface Algorithm extends Runnable {

	/**
	 * Execute the algorithm.
	 * 
	 */
	void execute();

	/**
	 * Validate the execution object linking this algorithm with its
	 * input/output parameters
	 */
	void validate();

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
	 * @param fileResolver the {@link FileResolver} for this algorithm.
	 */
	void setFileResolver(FileResolver fileResolver);
	
	/**
	 * @return the {@link DataStoreClient} to use for this algorithm instance.
	 */
	DataStoreClient getDataStoreClient();

	/**
	 * @param dataStoreClient set the {@link DataStoreClient} for this algorithm instance.
	 */
	void setDataStoreClient(DataStoreClient dataStoreClient);
}
