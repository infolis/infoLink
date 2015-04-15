package io.github.infolis.model.file;

public class FileResolverFactory {
	
	public static FileResolver create(FileResolveStrategy strategy) {
		
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
