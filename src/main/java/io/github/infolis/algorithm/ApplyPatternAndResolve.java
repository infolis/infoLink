package io.github.infolis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.resolve.QueryService;

/**
 *
 * This algorithm first searches for a set of given patterns, then extracts the
 * metadata from the detected textual references. Afterwards, this metadata is
 * queried an resolved to get the resulting links.
 *
 * Used alogrithms: PatternApplier - MetaDataExtractor - FederatedSearcher -
 * Resolver
 *
 * @author domi
 * @author kata
 * 
 */
public class ApplyPatternAndResolve extends BaseAlgorithm {

    public ApplyPatternAndResolve(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }
    
    private static final Logger log = LoggerFactory.getLogger(ApplyPatternAndResolve.class);

    @Override
    public void execute() throws IOException {

    	Execution tagExec = new Execution();
    	tagExec.setAlgorithm(TagResolver.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.getInfolisPatternTags().addAll(getExecution().getInfolisPatternTags());
    	tagExec.instantiateAlgorithm(this).run();
    	
    	getExecution().getPatterns().addAll(tagExec.getPatterns());
    	getExecution().getInputFiles().addAll(tagExec.getInputFiles());
        
        List<String> queryServices = getExecution().getQueryServices();        
        List<String> createdLinks = new ArrayList<>();
        List<Class<? extends QueryService>> queryServiceClasses = getExecution().getQueryServiceClasses();
               
        List<String> textualRefs = searchPatterns(getExecution().getPatterns(), getExecution().getInputFiles());        

        //for each textual reference, extract the metadata,
        //query the given repository(ies) and generate links.
        for (String s : textualRefs) {
        	log.debug("Resolving textualReference " + s);
            String searchQuery = extractMetaData(s);
            log.debug("Extracted metadata. SearchQuery: " + searchQuery);
            List<String> searchRes = new ArrayList<>();
            if (null != getExecution().getQueryServiceClasses() && !getExecution().getQueryServiceClasses().isEmpty()) {
                searchRes = searchClassInRepositories(searchQuery, queryServiceClasses);
            }
            if (null != getExecution().getQueryServices() && !getExecution().getQueryServices().isEmpty()) {
                searchRes = searchInRepositories(searchQuery, queryServices);
            }

            if (searchRes.size() > 0) {
                createdLinks.addAll(resolve(searchRes, s));
            }
        }
        //the output of the whole algorithm is again a list with links 
        log.debug("Created links: " + createdLinks);
        getExecution().setLinks(createdLinks);
        getExecution().setStatus(ExecutionStatus.FINISHED);
    }

    private List<String> searchPatterns(List<String> patterns, List<String> input) {
        Execution search = new Execution();
        //search.setAlgorithm(PatternApplier.class);
        search.setAlgorithm(RegexSearcher.class);
        search.setPatternUris(patterns);
        search.setInputFiles(input);
        search.setIndexDirectory(getExecution().getIndexDirectory());
        getOutputDataStoreClient().post(Execution.class, search);
        search.instantiateAlgorithm(this).run();
        updateProgress(1, 4);
        return search.getTextualReferences();
    }

    public String extractMetaData(String textualReference) {
        Execution extract = new Execution();
        extract.setAlgorithm(MetaDataExtractor.class);
        List<String> textRefs = Arrays.asList(textualReference);
        extract.setTextualReferences(textRefs);
        getOutputDataStoreClient().post(Execution.class, extract);
        extract.instantiateAlgorithm(this).run();
        updateProgress(2, 4);
        return extract.getSearchQuery();
    }

    public List<String> searchInRepositories(String query, List<String> queryServices) {
        Execution searchRepo = new Execution();
        searchRepo.setAlgorithm(FederatedSearcher.class);
        searchRepo.setSearchQuery(query);
        searchRepo.setQueryServices(queryServices);
        getOutputDataStoreClient().post(Execution.class, searchRepo);
        searchRepo.instantiateAlgorithm(this).run();
        updateProgress(3, 4);
        log.debug("FederatedSearcher returned " + searchRepo.getSearchResults().size() + " search results");
        return searchRepo.getSearchResults();
    }
    
    public List<String> searchClassInRepositories(String query, List<Class<? extends QueryService>> queryServices) {
    	log.debug("Searching in repository for query: " + query);
        Execution searchRepo = new Execution();
        searchRepo.setAlgorithm(FederatedSearcher.class);
        searchRepo.setSearchQuery(query);
        searchRepo.setQueryServiceClasses(queryServices);
        getOutputDataStoreClient().post(Execution.class, searchRepo);
        searchRepo.instantiateAlgorithm(this).run();
        updateProgress(3, 4);
        log.debug("FederatedSearcher returned " + searchRepo.getSearchResults().size() + " search results");
        return searchRepo.getSearchResults();
    }

    public List<String> resolve(List<String> searchResults, String textRef) {
        Execution resolve = new Execution();
        resolve.setAlgorithm(Resolver.class);
        resolve.setSearchResults(searchResults);
        List<String> textRefs = Arrays.asList(textRef);
        resolve.setTextualReferences(textRefs);
        getOutputDataStoreClient().post(Execution.class, resolve);
        log.debug("Resolving " + searchResults.size() + " search results for textual references: " + textRef);
        resolve.instantiateAlgorithm(this).run();
        updateProgress(4, 4);
        log.debug("Returning links: " + resolve.getLinks());
        return resolve.getLinks();
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        Execution exec = this.getExecution();
		if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) && 
        		(null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())){
            throw new IllegalArgumentException("Must set at least one inputFile!");
        }
        if ((null == exec.getPatterns() || exec.getPatterns().isEmpty()) && 
        		(null == exec.getInfolisPatternTags() || exec.getInfolisPatternTags().isEmpty()))
        {
            throw new IllegalArgumentException("No patterns given.");
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
