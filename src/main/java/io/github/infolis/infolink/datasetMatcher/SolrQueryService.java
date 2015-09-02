/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.infolink.datasetMatcher;

import io.github.infolis.algorithm.BaseAlgorithm;
import io.github.infolis.algorithm.IllegalAlgorithmArgumentException;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.entity.SearchResult;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author domi
 */
public class SolrQueryService extends BaseAlgorithm implements QueryService {
    
    
    
    public SolrQueryService(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    @Override
    public void execute() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String adaptQuery(String solrQuery) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<SearchResult> executeQuery(String adaptedQuery) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
