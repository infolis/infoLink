package io.github.infolis.algorithm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisConfig;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.infolink.querying.QueryService;

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
public class ReferenceLinker extends BaseAlgorithm {
	
	public ReferenceLinker(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
    		FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private static final Logger log = LoggerFactory.getLogger(ReferenceLinker.class);
	
	private List<String> resolveReferences(List<String> textualReferences) throws IOException {
		// create query cache
		File cache = Files.createTempFile(InfolisConfig.getTmpFilePath(), "querycache", ".txt").toFile();
		cache.deleteOnExit();
        String cachePath = cache.getCanonicalPath();
        
    	List<String> entityLinks = new ArrayList<>();
    	List<String> queryServices = getExecution().getQueryServices();
        List<Class<? extends QueryService>> queryServiceClasses = getExecution().getQueryServiceClasses();
        
	    for (String s : textualReferences) {
	    	debug(log, "Resolving TextualReference " + s);
	        String referencedEntity = extractMetaData(s);
	        debug(log, "Extracted metadata from reference");
	        
	        List<String> searchRes = new ArrayList<>();
	        if (null != getExecution().getQueryServiceClasses() && !getExecution().getQueryServiceClasses().isEmpty()) {
	            searchRes = searchClassInRepositories(referencedEntity, queryServiceClasses, cachePath);
	        }
	        if (null != getExecution().getQueryServices() && !getExecution().getQueryServices().isEmpty()) {
	            searchRes = searchInRepositories(referencedEntity, queryServices, cachePath);
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
        String entityUri = extract.getLinkedEntities().get(0);
        updateProgress(1, 3);
        return entityUri;
    }

    public List<String> searchInRepositories(String entityUri, List<String> queryServices, String cachePath) {
        Execution searchRepo = getExecution().createSubExecution(FederatedSearcher.class);
        searchRepo.setSearchResultLinkerClass(getExecution().getSearchResultLinkerClass());
        searchRepo.setLinkedEntities(Arrays.asList(entityUri));
        searchRepo.setQueryServices(queryServices);
        searchRepo.setIndexDirectory(cachePath);
        getOutputDataStoreClient().post(Execution.class, searchRepo);
        searchRepo.instantiateAlgorithm(this).run();
        updateProgress(2, 3);
        debug(log, "FederatedSearcher returned " + searchRepo.getSearchResults().size() + " search results");
        return searchRepo.getSearchResults();
    }

    public List<String> searchClassInRepositories(String entityUri, List<Class<? extends QueryService>> queryServices, String cachePath) {
        Execution searchRepo = getExecution().createSubExecution(FederatedSearcher.class);
        searchRepo.setSearchResultLinkerClass(getExecution().getSearchResultLinkerClass());
        searchRepo.setLinkedEntities(Arrays.asList(entityUri));
        searchRepo.setQueryServiceClasses(queryServices);
        searchRepo.setIndexDirectory(cachePath);
        getOutputDataStoreClient().post(Execution.class, searchRepo);
        searchRepo.instantiateAlgorithm(this).run();
        updateProgress(2, 3);
        debug(log, "FederatedSearcher returned " + searchRepo.getSearchResults().size() + " search results");
        return searchRepo.getSearchResults();
    }

    public List<String> createLinks(List<String> searchResults, String textRef) {
    	Execution linker = getExecution().createSubExecution(getExecution().getSearchResultLinkerClass());
    	linker.setSearchResults(searchResults);
        List<String> textRefs = Arrays.asList(textRef);
        linker.setTextualReferences(textRefs);
        getOutputDataStoreClient().post(Execution.class, linker);
        debug(log, "Creating links based on " + searchResults.size() + " search results for textual references: " + textRef);
        linker.instantiateAlgorithm(this).run();
        updateProgress(3, 3);
        debug(log, "Returning links: " + linker.getLinks());
        return linker.getLinks();
    }

    
	@Override
	public void execute() throws IOException {
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
		if (null == getExecution().getSearchResultLinkerClass()) {
			throw new IllegalAlgorithmArgumentException(getClass(), "searchResultLinkerClass", "Required parameter 'SearchResultLinkerClass' is missing!");
		}
	}
}