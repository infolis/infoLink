package io.github.infolis.infolink.datasetMatcher;

import io.github.infolis.algorithm.BaseAlgorithm;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.BaseModel;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.entity.SearchResult;
import java.util.Set;

/**
 *
 * @author domi
 */
public abstract class QueryService extends BaseAlgorithm {
    
    private String target;

    public QueryService(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    /**
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(String target) {
        this.target = target;
    }
    
}
