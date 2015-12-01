package io.github.infolis.datastore;

import java.util.UUID;

public class DataStoreClientFactory {

	/**
	 * @see DataStoreStrategy#CENTRAL
	 * @see #create(DataStoreStrategy)
	 * @return a {@link CentralFileResolver} instance
	 */
	public static DataStoreClient global() {
		return create(DataStoreStrategy.CENTRAL);
	}

	/**
	 * @see DataStoreStrategy#LOCAL
	 * @see #create(DataStoreStrategy)
	 * @return a {@link LocalFileResolver} instance
	 */
	public static DataStoreClient local() {
		return create(DataStoreStrategy.LOCAL);
	}

	/**
	 * Create {@link FileResolver} using the supplied strategy.
	 *
	 * @param strategy The {@link DataStoreStrategy} to use
	 * @return a {@link FileResolver} instance
	 */
	public static DataStoreClient create(DataStoreStrategy strategy) {
		Class<? extends DataStoreClient> clazz = strategy.dataStoreClientClass;
        final DataStoreClient instance;
		if (clazz.equals(CentralClient.class)) {
			instance = new CentralClient();
		} else if (clazz.equals(LocalClient.class)) {
			instance = new LocalClient(UUID.randomUUID());
		} else {
			throw new RuntimeException("Unhandled DataStoreClient class " + clazz.getName());
		}
		return instance;
	}

}
