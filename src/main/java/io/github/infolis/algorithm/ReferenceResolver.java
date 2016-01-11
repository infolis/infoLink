package io.github.infolis.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.resolve.QueryService;

/**
 * This algorithm extracts metadata from textual references and links them 
 * to records in a repository.
 *
 * Used algorithms:  MetaDataExtractor - FederatedSearcher - Resolver
 * 
 * @author kata
 * @author domi
 *
 */
public class ReferenceResolver extends BaseAlgorithm {
	
	public ReferenceResolver(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
    		FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private static final Logger log = LoggerFactory.getLogger(ReferenceResolver.class);
	
	private List<String> resolveReferences(List<String> textualReferences) {
    	List<String> entityLinks = new ArrayList<>();
    	List<String> queryServices = getExecution().getQueryServices();
        List<Class<? extends QueryService>> queryServiceClasses = getExecution().getQueryServiceClasses();

	    for (String s : textualReferences) {
	    	debug(log, "Resolving TextualReference " + s);
	        String searchQuery = extractMetaData(s);
	        debug(log, "Extracted metadata from reference. SearchQuery for QueryService: " + searchQuery);
	        List<String> searchRes = new ArrayList<>();
	        if (null != getExecution().getQueryServiceClasses() && !getExecution().getQueryServiceClasses().isEmpty()) {
	            searchRes = searchClassInRepositories(searchQuery, queryServiceClasses);
	        }
	        if (null != getExecution().getQueryServices() && !getExecution().getQueryServices().isEmpty()) {
	            searchRes = searchInRepositories(searchQuery, queryServices);
	        }
	        if (searchRes.size() > 0) {
	        	entityLinks.addAll(createLinks(searchRes, s));
	        }
	    }
	return entityLinks;
    }

    public String extractMetaData(String textualReference) {
        Execution extract = getExecution().createSubExecution(MetaDataExtractor.class);
        List<String> textRefs = Arrays.asList(textualReference);
        extract.setTextualReferences(textRefs);
        getOutputDataStoreClient().post(Execution.class, extract);
        extract.instantiateAlgorithm(this).run();
        updateProgress(1, 3);
        return extract.getSearchQuery();
    }

    public List<String> searchInRepositories(String query, List<String> queryServices) {
        Execution searchRepo = getExecution().createSubExecution(FederatedSearcher.class);
        searchRepo.setSearchQuery(query);
        searchRepo.setQueryServices(queryServices);
        getOutputDataStoreClient().post(Execution.class, searchRepo);
        searchRepo.instantiateAlgorithm(this).run();
        updateProgress(2, 3);
        debug(log, "FederatedSearcher returned " + searchRepo.getSearchResults().size() + " search results");
        return searchRepo.getSearchResults();
    }

    public List<String> searchClassInRepositories(String query, List<Class<? extends QueryService>> queryServices) {
    	debug(log, "Searching in repository for query: " + query);
        Execution searchRepo = getExecution().createSubExecution(FederatedSearcher.class);;
        searchRepo.setSearchQuery(query);
        searchRepo.setQueryServiceClasses(queryServices);
        getOutputDataStoreClient().post(Execution.class, searchRepo);
        searchRepo.instantiateAlgorithm(this).run();
        updateProgress(2, 3);
        debug(log, "FederatedSearcher returned " + searchRepo.getSearchResults().size() + " search results");
        return searchRepo.getSearchResults();
    }

    public List<String> createLinks(List<String> searchResults, String textRef) {
        Execution resolve = getExecution().createSubExecution(Resolver.class);
        resolve.setSearchResults(searchResults);
        List<String> textRefs = Arrays.asList(textRef);
        resolve.setTextualReferences(textRefs);
        getOutputDataStoreClient().post(Execution.class, resolve);
        debug(log, "Resolving " + searchResults.size() + " search results for textual references: " + textRef);
        resolve.instantiateAlgorithm(this).run();
        updateProgress(3, 3);
        debug(log, "Returning links: " + resolve.getLinks());
        return resolve.getLinks();
    }

    
	@Override
	public void execute() {
		List<String> entityLinks = resolveReferences(getExecution().getTextualReferences());
		getExecution().setLinks(entityLinks);
	}
	
	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
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
		if (null == getExecution().getTextualReferences() || getExecution().getTextualReferences().isEmpty()) {
			throw new IllegalAlgorithmArgumentException(getClass(), "TextualReference", "Required parameter 'textual references' is missing!");
		}
	}
}