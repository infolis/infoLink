package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.util.SerializationUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.BadRequestException;

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
		log.debug("{}", SerializationUtils.toJSON(getExecution()));
		try {
			baseValidate();
			validate();
		} catch (IllegalAlgorithmArgumentException | RuntimeException e) {
			getExecution().setStatus(ExecutionStatus.FAILED);
			getExecution().getLog().add(e.getMessage());
			getExecution().setEndTime(new Date());
			return;
		} finally {
			persistExecution();
		}
		getExecution().setStatus(ExecutionStatus.STARTED);
		getExecution().setStartTime(new Date());
		try {
			execute();
		} catch (Exception e) {
			log.error("Execution threw an Exception: {}" , e);
			getExecution().setStatus(ExecutionStatus.FAILED);
			getExecution().setEndTime(new Date());
		} finally {
			persistExecution();
		}
	}

	protected void persistExecution() throws BadRequestException {
		if (null != getExecution().getUri()) {
			getDataStoreClient().put(Execution.class, getExecution());
		} else {
			getDataStoreClient().post(Execution.class, getExecution());
		}
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
	
	public void baseValidate() throws IllegalAlgorithmArgumentException {
		if (null == getExecution()) {
			throw new IllegalAlgorithmArgumentException(getClass(), "execution", "Algorithm must have a 'Excecution' set to run().");
		}
		if (null == getFileResolver()) {
			throw new IllegalAlgorithmArgumentException(getClass(), "fileResolver", "Algorithm must have a 'FileResolver' set to run().");
		}
		if (null == getDataStoreClient()) {
			throw new IllegalAlgorithmArgumentException(getClass(), "dataStoreClient", "Algorithm must have a 'dataStoreClient' set to run().");
		}
	}
}
