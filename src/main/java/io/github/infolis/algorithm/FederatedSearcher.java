package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.infolink.querying.QueryService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 * @author kata
 */
public class FederatedSearcher extends BaseAlgorithm {

    public FederatedSearcher(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(FederatedSearcher.class);

    private List<String> search(QueryService queryService, SearchResultLinker linker, 
    		Entity entity, File cache) throws IOException {
	    queryService.setMaxNumber(linker.getMaxNum());
	    String query = queryService.createQuery(entity).toString();
		List<String> searchResultUris = readFromCache(cache, query);
		
		// searchResultUris is null iff query is not contained in the cache
		if (null == searchResultUris) {
	    	List<SearchResult> results = queryService.find(entity);
	    	searchResultUris = getInputDataStoreClient().post(SearchResult.class, results);
	        writeToCache(cache, query, searchResultUris);
		}
	    return searchResultUris;
    }
    
    @Override
    public void execute() throws IOException {
        List<String> allResults = new ArrayList<>();
        
        File cache = null;
        if (null != getExecution().getIndexDirectory() && !getExecution().getIndexDirectory().isEmpty()) {
        	cache = new File(getExecution().getIndexDirectory());
        	log.debug("using cache at " + getExecution().getIndexDirectory());
        }
        
        Entity entity = getInputDataStoreClient().get(Entity.class, getExecution().getLinkedEntities().get(0));
        int counter = 0, size =0;
        
        Class[] parameterTypes = { DataStoreClient.class, DataStoreClient.class, FileResolver.class, FileResolver.class };
        Object[] initArgs = { getInputDataStoreClient(), getOutputDataStoreClient(), getInputFileResolver(), getOutputFileResolver() };

        if (null != getExecution().getQueryServiceClasses() && !getExecution().getQueryServiceClasses().isEmpty()) {
            size = getExecution().getQueryServiceClasses().size();
            for (Class<? extends QueryService> qs : getExecution().getQueryServiceClasses()) {
                QueryService queryService;
                try {
                    Constructor<? extends QueryService> constructor = qs.getDeclaredConstructor();
                    queryService = constructor.newInstance();
                    Constructor<? extends SearchResultLinker> linkerConstructor = getExecution().getSearchResultLinkerClass().getDeclaredConstructor(parameterTypes);
                    SearchResultLinker linker = linkerConstructor.newInstance(initArgs);
                    queryService.setQueryStrategy(linker.getQueryStrategy());
                    queryService.setTags(getExecution().getTags());
                    getOutputDataStoreClient().post(QueryService.class, queryService);
                    debug(log, "Calling QueryService {} to find entity {}", queryService.getUri(), entity.getUri());
                    
                    List<String> searchResultUris = search(queryService, linker, entity, cache);
                    if (!searchResultUris.isEmpty()) allResults.addAll(searchResultUris);
                    
                    updateProgress(counter, size);

                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        } else if (null != getExecution().getQueryServices() && !getExecution().getQueryServices().isEmpty()) {
            size = getExecution().getQueryServices().size();
            try {
            	for (QueryService queryService : getInputDataStoreClient().get(QueryService.class, getExecution().getQueryServices())) {
	            	Constructor<? extends SearchResultLinker> linkerConstructor = getExecution().getSearchResultLinkerClass().getDeclaredConstructor(parameterTypes);
	            	SearchResultLinker linker = linkerConstructor.newInstance(initArgs);
                    queryService.setQueryStrategy(linker.getQueryStrategy());
	            	debug(log, "Calling QueryService {} to find entity {}", queryService.getUri(), entity.getUri());
	            	
	            	List<String> searchResultUris = search(queryService, linker, entity, cache);
                    if (!searchResultUris.isEmpty()) allResults.addAll(searchResultUris);
	            	
	                updateProgress(counter, size);
            	}
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        
        for (String uri : allResults) {
            log.debug("Found search result {}", uri);
        }
        getExecution().setSearchResults(allResults);
        getExecution().setStatus(ExecutionStatus.FINISHED);
    }
    
    private void writeToCache(File cache, String query, List<String> uris) throws IOException {
    	if (null == cache || !cache.exists()) {
    		log.debug("no cache file given or cache file does not exist, continuing without cache");
    		return;
    	}
    	
    	String entry = query + "@@@";
    	for (String uri : uris) entry += uri + "!!!";
    	if (uris.isEmpty()) entry += " ";
    	FileUtils.write(cache, entry.substring(0, entry.length() - 3) + "\n", true);
    	log.debug("wrote query to cache {}", entry.substring(0, entry.length() - 3));
    }
    
    private List<String> readFromCache(File cache, String query) throws IOException {
    	if (null == cache || !cache.exists()) {
    		log.debug("no cache file given or cache file does not exist, continuing without cache");
    		return null;
    	}
    	
    	for (String line : FileUtils.readLines(cache)) {
    		String[] entry = line.split("@@@");
    		if (entry[0].equals(query)) {
    			log.debug("found query in cache");
    			try {
    				return Arrays.asList(entry[1].split("!!!"));
    			} catch (ArrayIndexOutOfBoundsException e) {
    				if (null == entry[1] || entry[1].isEmpty()) return new ArrayList<String>();
    				else return Arrays.asList(entry[1]);
    			}
    		}
    	}
    	// query was not found in cache
    	return null;
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        if (null == getExecution().getLinkedEntities()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "entity", "Required parameter 'entity for query service' is missing!");
        }
        boolean queryServiceSet = false;
        if (null != getExecution().getQueryServiceClasses() && !getExecution().getQueryServiceClasses().isEmpty()) {
            queryServiceSet = true;
        }
        if (null != getExecution().getQueryServices() && !getExecution().getQueryServices().isEmpty()) {
            queryServiceSet = true;
        }
        if (!queryServiceSet) {
            throw new IllegalAlgorithmArgumentException(getClass(), "queryService", "Required parameter 'query services' is missing!");
        }
        if (null == getExecution().getSearchResultLinkerClass()) {
        	throw new IllegalAlgorithmArgumentException(getClass(), "SearchResultLinkerClass", "Required parameter 'SearchResultLinkerClass' is missing!");
        }
    }

}
