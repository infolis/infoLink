package io.github.infolis.algorithm;

import java.util.HashSet;
import java.util.Set;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.querying.QueryService.QueryField;

/**
 * 
 * @author kata
 *
 */
public class DoiLinker extends BestMatchLinker {
	
	public DoiLinker(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
        Set<QueryField> queryStrategy = new HashSet<>();
        queryStrategy.add(QueryField.doi);
        setQueryStrategy(queryStrategy); 
	}
	
	
}