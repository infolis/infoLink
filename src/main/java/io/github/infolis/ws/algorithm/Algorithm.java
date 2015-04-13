package io.github.infolis.ws.algorithm;

import io.github.infolis.model.Execution;

public interface Algorithm {
	
	public void execute();

	/**
	 * Validate the execution object linking this algorithm with its
	 * input/output parameters
	 */
	public void validate();

	/**
	 * Get the execution context of this algorithm instance.
	 * @return
	 */
	public Execution getExecution();

	/**
	 * Set the execution context of this algorithm instance.
	 * @param execution
	 */
	public void setExecution(Execution execution);

}
