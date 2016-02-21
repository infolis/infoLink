package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.infolink.querying.QueryService;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public void execute() throws IOException {
        List<SearchResult> allResults = new ArrayList<>();

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
                    if (!Modifier.isAbstract(getExecution().getSearchResultLinkerClass().getModifiers())) {
                    	SearchResultLinker linker = linkerConstructor.newInstance(initArgs);
                    	queryService.setQueryStrategy(linker.getQueryStrategy());
                    }
                    //TODO else?
                    debug(log, "Calling QueryService %s to find entity %s", queryService, entity);
                    List<SearchResult> results = queryService.find(entity);
                    allResults.addAll(results);
                    updateProgress(counter, size);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (null != getExecution().getQueryServices() && !getExecution().getQueryServices().isEmpty()) {
            size = getExecution().getQueryServices().size();
            try {
            	for (QueryService queryService : getInputDataStoreClient().get(QueryService.class, getExecution().getQueryServices())) {
	            	Constructor<? extends SearchResultLinker> linkerConstructor = getExecution().getSearchResultLinkerClass().getDeclaredConstructor(parameterTypes);
	            	if (!Modifier.isAbstract(getExecution().getSearchResultLinkerClass().getModifiers())) {
                    	SearchResultLinker linker = linkerConstructor.newInstance(initArgs);
                    	queryService.setQueryStrategy(linker.getQueryStrategy());
                    }
                    //TODO else?
	            	List<SearchResult> results = queryService.find(entity);
	                allResults.addAll(results);
	                updateProgress(counter, size);
            	}
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        getOutputDataStoreClient().post(SearchResult.class, allResults);
        List<String> searchResultUris = new ArrayList<>();
        for (SearchResult sr : allResults) {
            searchResultUris.add(sr.getUri());
            log.debug("Found search result " + sr.getUri());
        }
        getExecution().setSearchResults(searchResultUris);
        getExecution().setStatus(ExecutionStatus.FINISHED);
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
