package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * This algorithm first searches for a set of given patterns, then extracts the 
 * metadata from the detected textual references. Afterwards, this metadata
 * is queried an resolved to get the resulting links.
 * 
 * Used alogrithms: PatternApplier - MetaDataExtractor - FederatedSearcher - Resolver
 * 
 * @author domi
 */
public class ApplyPatternAndResolve extends BaseAlgorithm {

    public ApplyPatternAndResolve(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    @Override
    public void execute() throws IOException {
        List<String> pattern = getExecution().getPatterns();
        List<String> inputFiles = getExecution().getInputFiles();
        List<String> queryServices = getExecution().getQueryServices();        
        List<String> createdLinks = new ArrayList<>();
               
        List<String> textualRefs = searchPattern(pattern, inputFiles);        
        
        //for each textual reference, extract the metadata,
        //query the given repository(ies) and generate links.
        
        for (String s : textualRefs) {
            String searchQuery = extractMetaData(s);
            List<String> searchRes = searchInRepositories(searchQuery, queryServices);

            if (searchRes.size() > 0) {
                createdLinks.addAll(resolve(searchRes, s));
            }
        }
        //the output of the whole algorithm is again a list with links 
        getExecution().setLinks(createdLinks);
    }

    private List<String> searchPattern(List<String> pattern, List<String> input) {
        Execution search = new Execution();
        search.setAlgorithm(PatternApplier.class);
        search.setPatternUris(pattern);
        search.setInputFiles(input);
        getOutputDataStoreClient().post(Execution.class, search);
        search.instantiateAlgorithm(this).run();
        updateProgress(25);
        return search.getTextualReferences();
    }

    public String extractMetaData(String textualReference) {
        Execution extract = new Execution();
        extract.setAlgorithm(MetaDataExtractor.class);
        List<String> textRefs = Arrays.asList(textualReference);
        extract.setTextualReferences(textRefs);
        getOutputDataStoreClient().post(Execution.class, extract);
        extract.instantiateAlgorithm(this).run();
        updateProgress(50);
        return extract.getSearchQuery();
    }

    public List<String> searchInRepositories(String query, List<String> queryServices) {
        Execution searchRepo = new Execution();
        searchRepo.setAlgorithm(FederatedSearcher.class);
        searchRepo.setSearchQuery(query);
        searchRepo.setQueryServices(queryServices);
        getOutputDataStoreClient().post(Execution.class, searchRepo);
        searchRepo.instantiateAlgorithm(this).run();
        updateProgress(75);
        return searchRepo.getSearchResults();
    }

    public List<String> resolve(List<String> searchResults, String textRef) {
        Execution resolve = new Execution();
        resolve.setAlgorithm(Resolver.class);
        resolve.setSearchResults(searchResults);
        List<String> textRefs = Arrays.asList(textRef);
        resolve.setTextualReferences(textRefs);
        getOutputDataStoreClient().post(Execution.class, resolve);
        resolve.instantiateAlgorithm(this).run();
        updateProgress(100);
        return resolve.getLinks();
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        if (null == this.getExecution().getInputFiles()
                || this.getExecution().getInputFiles().isEmpty()) {
            throw new IllegalArgumentException("Must set at least one inputFile!");
        }
        if (null == this.getExecution().getPatterns() || this.getExecution().getPatterns().isEmpty()) {
            throw new IllegalArgumentException("No patterns given.");
        }
        if (null == this.getExecution().getQueryServices() || this.getExecution().getQueryServices().isEmpty()) {
            throw new IllegalArgumentException("No query services specified.");
        }
    }
}
