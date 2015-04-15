package io.github.infolis.model.file;

import io.github.infolis.model.InfolisFile;

/**
 * Predefined strategies for resolving {@link InfolisFile}.
 * 
 * @author kba
 *
 */
public enum FileResolverStrategy {
	
	/**
	 *  @see CentralFileResolver
	 */
	CENTRAL(CentralFileResolver.class),
	/**
	 *  @see LocalFileResolver
	 */
	LOCAL(LocalFileResolver.class)
	;

	protected Class<? extends FileResolver> implementation;

	private FileResolverStrategy(Class<? extends FileResolver> clazz) {
		this.implementation = clazz;
	}

}
