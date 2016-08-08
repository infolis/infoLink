package io.github.infolis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.BaseModel;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;

/**
 * Class for searching InfolisFile, InfolisPattern and TextualReference objects with given tags.
 * 
 * @author kata
 *
 */
public class TagSearcher extends BaseAlgorithm {

	public TagSearcher(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

	private void parseTags() {
		Map<Class<?extends BaseModel>, Collection<String>> toSearch = new HashMap<>();

		toSearch.put(InfolisFile.class, getExecution().getInfolisFileTags());
		toSearch.put(InfolisPattern.class, getExecution().getInfolisPatternTags());
		toSearch.put(TextualReference.class, getExecution().getTextualReferenceTags());

		for (Class<? extends BaseModel> clazz : toSearch.keySet()) {
			if (toSearch.get(clazz).isEmpty())
				continue;
			Multimap<String, String> query = HashMultimap.create();
			query.putAll("tags", toSearch.get(clazz));
			List<? extends BaseModel> matchingItemsInDb;
			matchingItemsInDb = getInputDataStoreClient().search(clazz, query);
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

	private void setExecutionParameters(Class<? extends BaseModel> clazz, List<? extends BaseModel> instances) {
		List<String> uris = getUris(instances);
		if (clazz.equals(InfolisPattern.class)) { getExecution().setPatterns(uris); }
		if (clazz.equals(InfolisFile.class)) { getExecution().setInputFiles(uris); }
		if (clazz.equals(TextualReference.class)) { getExecution().setTextualReferences(uris); }
	}

	@Override
	public void execute() throws IOException {
		parseTags();
		getExecution().setStatus(ExecutionStatus.FINISHED);
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// no validation needed, if no tags are specified, TagSearcher simply won't do anything
	}
}