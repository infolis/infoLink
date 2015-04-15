package io.github.infolis.model.file;

public enum FileResolveStrategy {
	
	CENTRAL(CentralFileResolver.class),
	LOCAL(LocalFileResolver.class)
	;

	protected Class<? extends FileResolver> implementation;

	private FileResolveStrategy(Class<? extends FileResolver> clazz) {
		this.implementation = clazz;
	}

}
