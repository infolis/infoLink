package io.github.infolis.model.datastore;

import io.github.infolis.model.InfolisFile;

/**
 * Predefined strategies for resolving {@link InfolisFile} and instantiating {@link DataStoreClient}.
 * 
 * @author kba
 *
 */
public enum DataStoreStrategy {
	
	/**
	 *  @see CentralFileResolver
	 */
	CENTRAL(CentralFileResolver.class, CentralClient.class),
	/**
	 *  @see LocalFileResolver
	 */
	LOCAL(LocalFileResolver.class, LocalClient.class)
	;

	protected Class<? extends FileResolver> fileResolverClass;
	protected Class<? extends DataStoreClient> dataStoreClientClass;

	private DataStoreStrategy(Class<? extends FileResolver> fileResolverClass, Class<? extends DataStoreClient> dataStoreClientClass) {
		this.fileResolverClass = fileResolverClass;
		this.dataStoreClientClass = dataStoreClientClass;
	}

}
