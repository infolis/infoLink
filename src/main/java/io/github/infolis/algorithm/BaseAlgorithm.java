package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kba
 * @author domi
 */
public abstract class BaseAlgorithm implements Algorithm {

	private static final Logger log = LoggerFactory.getLogger(BaseAlgorithm.class);
	/*
	 * The list of algorithms
	 */
	public static Map<String, Class<? extends BaseAlgorithm>> algorithms = new HashMap<>();
	static {
		algorithms.put(TextExtractorAlgorithm.class.getSimpleName(), TextExtractorAlgorithm.class);
	}
	
	private Execution execution;
	private FileResolver fileResolver;
	private DataStoreClient dataStoreClient;
	
	@Override
	public final void run() {
		baseValidate();
		validate();
		getExecution().setStatus(ExecutionStatus.STARTED);
		getExecution().setStartTime(new Date());
		try {
			execute();
		} catch (Exception e) {
			log.error("Execution threw an Exception: {}" , e);
			getExecution().setStatus(ExecutionStatus.FAILED);
		}
		getExecution().setEndTime(new Date());
		getDataStoreClient().put(Execution.class, getExecution());
	}

	@Override
	public Execution getExecution() {
		return execution;
	}

	@Override
	public void setExecution(Execution execution) {
		this.execution = execution;
	}

	@Override
	public FileResolver getFileResolver() {
		return fileResolver;
	}

	@Override
	public void setFileResolver(FileResolver fileResolver) {
		this.fileResolver = fileResolver;
	}

	@Override
	public DataStoreClient getDataStoreClient() {
		return dataStoreClient;
	}

	@Override
	public void setDataStoreClient(DataStoreClient dataStoreClient) {
		this.dataStoreClient = dataStoreClient;
	}
	
	public void baseValidate() {
		if (null == getExecution()) {
			throw new RuntimeException("Algorithm must have a 'Excecution' set to run().");
		}
		if (null == getFileResolver()) {
			throw new RuntimeException("Algorithm must have a 'FileResolver' set to run().");
		}
		if (null == getDataStoreClient()) {
			throw new RuntimeException("Algorithm must have a 'dataStoreClient' set to run().");
		}
	}
}
