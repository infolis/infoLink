package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.datasetMatcher.QueryService;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.entity.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author domi
 */
public class FederatedSearcher extends BaseAlgorithm {

    public FederatedSearcher(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }
    
    @Override
    public void execute() throws IOException {
        List<SearchResult> allResults = new ArrayList<>();
        
        SearchQuery query = getInputDataStoreClient().get(SearchQuery.class, getExecution().getSearchQuery());
        int counter =0;
        for (QueryService queryService : getInputDataStoreClient().get(QueryService.class, getExecution().getQueryServices())) {
            List<SearchResult> results = queryService.executeQuery(query);
            allResults.addAll(results);
            if(counter%10==0) {
                updateProgress(counter/getExecution().getQueryServices().size());
            }
        }
        getOutputDataStoreClient().post(SearchResult.class, allResults);
        List<String> searchResultUris = new ArrayList<>();
        for(SearchResult sr : allResults) {
            searchResultUris.add(sr.getUri());
        }
        getExecution().setSearchResults(searchResultUris);
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        if (null == getExecution().getSearchQuery()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "searchQuery", "Required parameter 'query for query service' is missing!");
        }
        if (null == getExecution().getQueryServices() || getExecution().getQueryServices().isEmpty()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "queryService", "Required parameter 'query services' is missing!");
        }
    }

}
