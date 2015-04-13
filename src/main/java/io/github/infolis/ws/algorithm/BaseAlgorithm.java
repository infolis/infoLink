/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.ws.algorithm;

import io.github.infolis.model.Execution;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author domi
 */
@SuppressWarnings("unchecked")
public abstract class BaseAlgorithm implements Runnable,Algorithm {

	/*
	 * The list of algorithms
	 */
	public static Map<String, Class<? extends BaseAlgorithm>> algorithms = new HashMap<>();
	@SuppressWarnings("rawtypes")
	private final static Class[] algoList = {
		PDF2TextAlgorithm.class
    };
	static {
		for (int i = 0 ; i < algoList.length ; i++) {
            algorithms.put(algoList[i].getSimpleName(), algoList[i]);
		}
	}
	
	private Execution execution;


	@Override
	public final void run() {
		validate();
		getExecution().setStatus(Execution.Status.STARTED);
		execute();
	}

	@Override
	public Execution getExecution() {
		return execution;
	}

	@Override
	public void setExecution(Execution execution) {
		this.execution = execution;
	}

}
