package io.github.infolis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.github.infolis.Registry;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.BaseModel;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;

public class TagResolver extends BaseAlgorithm {
	
	private static final Logger log = LoggerFactory.getLogger(TagResolver.class);
	
	public TagResolver(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }
	
	private <T extends BaseModel> List<T> resolve(Class<T> clazz, Multimap<String, String> query) {
		return getInputDataStoreClient().search(clazz, query);
	}
	
	private void parseTags() {
		Multimap<String, String> tagMap = getExecution().getTagMap().asMultimap(); 
		for (String className : tagMap.keys()) {
			log.debug("Resolving class name \"" + className + "\"");
			Collection<String> tags = tagMap.get(className);
			Class<? extends BaseModel> clazz = Registry.getClass(className);
			
			Multimap<String, String> query = HashMultimap.create();
	    	for (String tag : tags) query.put("tags", tag);
	    	
	    	List<? extends BaseModel> matchingItemsInDb;
	    	matchingItemsInDb = resolve(clazz, query);
	    	
	    	setExecutionParameters(clazz, matchingItemsInDb);
		}
	}
	
	private List<String> getUris(List<? extends BaseModel> itemList) {
    	List<String> uris = new ArrayList<String>();
    	for (BaseModel item : itemList) {
	    	if (item.getUri() == null)
	    		throw new RuntimeException("missing URI!");
	    	uris.add(item.getUri());
    	}
    	return uris;
    }
	
	// TODO more generic way to do this?
	private void setExecutionParameters(Class<? extends BaseModel> clazz, List<? extends BaseModel> instances) {
		List<String> uris = getUris(instances);
		if (clazz.equals(InfolisPattern.class)) { getExecution().setPatternUris(uris); }
		if (clazz.equals(InfolisFile.class)) { getExecution().setInputFiles(uris); }
		//if (clazz.equals(Entity.class)) { getExecution().set?(uris); }
	}
	
	@Override
	public void execute() throws IOException { 
		parseTags();
		getExecution().setStatus(ExecutionStatus.FINISHED);
	}
	 
	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		if (null == getExecution().getTags()) {
			throw new IllegalAlgorithmArgumentException(getClass(), "tags", "Required parameter 'tags' is missing!");
		}
	}
}