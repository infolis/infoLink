package io.github.infolis.ws.algorithm;

import io.github.infolis.model.Execution;
import io.github.infolis.model.file.FileResolver;
import io.github.infolis.ws.client.FrontendClient;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author kba
 * @author domi
 */
public abstract class BaseAlgorithm implements Runnable,Algorithm {

	/*
	 * The list of algorithms
	 */
	public static Map<String, Class<? extends BaseAlgorithm>> algorithms = new HashMap<>();
	static {
		algorithms.put(TextExtractorAlgorithm.class.getSimpleName(), TextExtractorAlgorithm.class);
	}
	
	private Execution execution;
	private FileResolver fileResolver;

	@Override
	public final void run() {
		validate();
		getExecution().setStatus(Execution.Status.STARTED);
		execute();
		FrontendClient.put(Execution.class, getExecution());
	}

	@Override
	public Execution getExecution() {
		return execution;
	}

	@Override
	public void setExecution(Execution execution) {
		this.execution = execution;
	}

	public FileResolver getFileResolver() {
		return fileResolver;
	}

	public void setFileResolver(FileResolver fileResolver) {
		this.fileResolver = fileResolver;
	}

}
