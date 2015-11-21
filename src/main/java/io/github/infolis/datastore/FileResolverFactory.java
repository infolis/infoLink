package io.github.infolis.datastore;

/**
 * Factory method to create FileResolvers
 *
 * @author kba
 *
 */
public class FileResolverFactory {

	/**
	 * @see DataStoreStrategy#CENTRAL
	 * @see #create(DataStoreStrategy)
	 * @return a {@link CentralFileResolver} instance
	 */
	public static FileResolver global() {
		return create(DataStoreStrategy.CENTRAL);
	}

	/**
	 * @see DataStoreStrategy#LOCAL
	 * @see #create(DataStoreStrategy)
	 * @return a {@link LocalFileResolver} instance
	 */
	public static FileResolver local() {
		return create(DataStoreStrategy.LOCAL);
	}

	/**
	 * Create {@link FileResolver} using the supplied strategy.
	 *
	 * @param strategy The {@link DataStoreStrategy} to use
	 * @return a {@link FileResolver} instance
	 */
	public static FileResolver create(DataStoreStrategy strategy) {
		Class<? extends FileResolver> clazz = strategy.fileResolverClass;
        FileResolver instance = null;
		try {
			instance = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return instance;
	}

}
