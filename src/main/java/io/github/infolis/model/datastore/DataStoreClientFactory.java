package io.github.infolis.model.datastore;

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
        DataStoreClient instance = null;
		try {
			instance = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return instance;
	}

}
