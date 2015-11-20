package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.resolve.QueryService;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class FederatedSearcher extends BaseAlgorithm {

    public FederatedSearcher(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }
    
    private static final Logger log = LoggerFactory.getLogger(FederatedSearcher.class);

    @Override
    public void execute() throws IOException {
        List<SearchResult> allResults = new ArrayList<>();

        SearchQuery query = getInputDataStoreClient().get(SearchQuery.class, getExecution().getSearchQuery());
        int counter = 0, size =0;

        if (null != getExecution().getQueryServiceClasses() && !getExecution().getQueryServiceClasses().isEmpty()) {
            size = getExecution().getQueryServiceClasses().size();
            for (Class<? extends QueryService> qs : getExecution().getQueryServiceClasses()) {
                QueryService queryService;
                try {
                    Constructor<? extends QueryService> constructor = qs.getDeclaredConstructor();
                    queryService = constructor.newInstance();
                    debug(log, "Calling QueryService %s to execute query %s", queryService, query);
                    List<SearchResult> results = queryService.executeQuery(query);
                    allResults.addAll(results);
                    updateProgress(counter, size);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (null != getExecution().getQueryServices() && !getExecution().getQueryServices().isEmpty()) {
            size = getExecution().getQueryServices().size();
            for (QueryService queryService : getInputDataStoreClient().get(QueryService.class, getExecution().getQueryServices())) {

                List<SearchResult> results = queryService.executeQuery(query);
                allResults.addAll(results);
                updateProgress(counter, size);

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
        if (null == getExecution().getSearchQuery()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "searchQuery", "Required parameter 'query for query service' is missing!");
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
    }

}
