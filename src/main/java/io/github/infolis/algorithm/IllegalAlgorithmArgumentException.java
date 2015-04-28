package io.github.infolis.algorithm;

@SuppressWarnings("serial")
public class IllegalAlgorithmArgumentException extends RuntimeException {
	
	public IllegalAlgorithmArgumentException(Class<? extends Algorithm> clazz, String field, String reason) {
		super(clazz.getSimpleName() + ", field '" + field + "' [" + reason + "]");
	}

}
