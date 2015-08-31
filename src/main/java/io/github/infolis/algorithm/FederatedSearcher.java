package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.datasetMatcher.QueryService;
import io.github.infolis.infolink.datasetMatcher.SolrQueryService;
import io.github.infolis.model.Execution;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

        List<SearchQuery> searchQueries = new ArrayList<>();
        for (String queryString : getExecution().getQueriesForQueryService()) {
            searchQueries.add(getInputDataStoreClient().get(SearchQuery.class, queryString));
        }
        //TODO: how to register QueryServices?
        for (String queryService : getExecution().getQueryServices()) {
            
            Execution convertExec = new Execution();
            convertExec.setAlgorithm(SolrQueryService.class);
            // TODO wire this more efficiently so files are stored temporarily
            Algorithm algo = convertExec.instantiateAlgorithm(this);
            // do the actual conversion
            algo.run();
           // Set<SearchResult> results = algo.getOutputDataStoreClient().get(SearchResult.class, convertExec.getSearchResults());

        }

    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
