package io.github.infolis.model.file;

/**
 * Factory method to create FileResolvers
 * 
 * @author kba
 *
 */
public class FileResolverFactory {
	
	/**
	 * @see FileResolverStrategy#CENTRAL
	 * @see #create(FileResolverStrategy)
	 * @return a {@link CentralFileResolver} instance
	 */
	public static FileResolver global() {
		return create(FileResolverStrategy.CENTRAL);
	}

	/**
	 * @see FileResolverStrategy#LOCAL
	 * @see #create(FileResolverStrategy)
	 * @return a {@link LocalFileResolver} instance
	 */
	public static FileResolver local() {
		return create(FileResolverStrategy.LOCAL);
	}
	
	/**
	 * Create {@link FileResolver} using the supplied strategy. 
	 * 
	 * @param strategy The {@link FileResolverStrategy} to use
	 * @return a {@link FileResolver} instance
	 */
	public static FileResolver create(FileResolverStrategy strategy) {
		Class<? extends FileResolver> clazz = strategy.implementation;
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
