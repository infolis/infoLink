
package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import java.io.IOException;

/**
 *
 * @author domi
 */
public class DumpAlgo extends BaseAlgorithm {

    public DumpAlgo(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }
    @Override
    public void execute() throws IOException {
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        
    }
    
}
