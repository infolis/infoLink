package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.datasetMatcher.QueryService;
import io.github.infolis.infolink.datasetMatcher.SolrQueryService;
import io.github.infolis.model.Execution;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.entity.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Set<List<SearchResult>> allResults = new HashSet<>();

        String queryString = getExecution().getQueryForQueryService();
        //TODO: query services have to be postet first!
        for (QueryService queryService : getInputDataStoreClient().get(QueryService.class, getExecution().getQueryServices())) {
            List<SearchResult> results = queryService.executeQuery(queryString);
            allResults.add(results);
        }
        
        //TODO: consolidate results

    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        if (null == getExecution().getQueryForQueryService()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "searchQuery", "Required parameter 'query for query service' is missing!");
        }
        if (null == getExecution().getQueryServices() || getExecution().getQueryServices().isEmpty()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "queryService", "Required parameter 'query services' is missing!");
        }
    }

}
