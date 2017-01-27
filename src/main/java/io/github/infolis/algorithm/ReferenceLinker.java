package io.github.infolis.algorithm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisConfig;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.infolink.querying.QueryService;

/**
 * This algorithm extracts metadata from textual references to create or reuse a 
 * corresponding entity and creates links between entities:
 * <ol>
 * <li>links between the mentionsReference of the textualReference and the referencedEntity</li>
 * <li>links between the referenced entity and entities in an external repository</li>
 * </ol>
 *
 * Used algorithms:  MetaDataExtractor - FederatedSearcher - SearchResultLinker
 * 
 * @author kata
 *
 */
public class ReferenceLinker extends BaseAlgorithm {
	
	public ReferenceLinker(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
    		FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private static final Logger log = LoggerFactory.getLogger(ReferenceLinker.class);
	
	private String createLinkToEntity(String fromEntityUri, String toEntityUri, TextualReference textualReference) {
		EntityLink link = new EntityLink();
		Entity referencedEntity = getOutputDataStoreClient().get(Entity.class, toEntityUri);
		link.setConfidence(referencedEntity.getEntityReliability());
		link.setFromEntity(fromEntityUri);
		link.setToEntity(toEntityUri);
		Set<EntityLink.EntityRelation> entityRelations = new HashSet<>();
		entityRelations.add(EntityLink.EntityRelation.references);
		link.setEntityRelations(entityRelations);
		link.setLinkReason(textualReference.getUri());
		link.setTags(textualReference.getTags());
		link.addAllTags(getExecution().getTags());
		
		getOutputDataStoreClient().post(EntityLink.class, link);
		return link.getUri();
	}
	
	private List<String> linkEntity(String referencedEntity, List<String> queryServices, 
			List<Class<? extends QueryService>> queryServiceClasses, String cachePath) {
		List<String> links = new ArrayList<>();
		List<String> searchResults = new ArrayList<>();
        if (null != getExecution().getQueryServiceClasses() && !getExecution().getQueryServiceClasses().isEmpty()) {
            searchResults = searchClassInRepositories(referencedEntity, queryServiceClasses, cachePath);
        }
        if (null != getExecution().getQueryServices() && !getExecution().getQueryServices().isEmpty()) {
            searchResults = searchInRepositories(referencedEntity, queryServices, cachePath);
        }
        if (searchResults.size() > 0) {
        	// TODO check. Previously used textual reference
        	links.addAll(createLinksForSearchResults(searchResults, referencedEntity));
        }
        return links;
	}
	
	public List<String> linkEntities(List<String> entities) throws IOException {
		// create query cache
		Path generalCachePath = Paths.get(InfolisConfig.getTmpFilePath().toString(), "cache");
		if (!generalCachePath.toFile().exists()) Files.createDirectories(generalCachePath);
		Path privateCachePath = Files.createTempDirectory(generalCachePath, Long.toString(System.nanoTime()));
		File cache = Files.createTempFile(privateCachePath, "querycache", ".txt").toFile();
        String cachePath = cache.getCanonicalPath();
        
        // update links for entities only once per execution
        Set<String> entitiesWithUpdatedLinks = new HashSet<>(); 
        
    	List<String> entityLinks = new ArrayList<>();
        
	    for (String toEntityUri : entities) {
	        // create links from referenced entity to entities in repository, if not already linked
	        if (!entitiesWithUpdatedLinks.contains(toEntityUri)) {
	        	List<String> queryServices = getExecution().getQueryServices();
	            List<Class<? extends QueryService>> queryServiceClasses = getExecution().getQueryServiceClasses();
	            entityLinks.addAll(linkEntity(toEntityUri, queryServices, queryServiceClasses, cachePath));
	        	entitiesWithUpdatedLinks.add(toEntityUri);
	        }
	        // TODO else return all existing links of entity
	        //else
	    }
	    cache.delete();
		privateCachePath.toFile().delete();
		log.debug("Returning entity links: " + entityLinks);
	    return entityLinks;
    }
	
	private List<String> linkReferences(List<String> textualReferences) throws IOException {
		// create query cache
		Path generalCachePath = Paths.get(InfolisConfig.getTmpFilePath().toString(), "cache");
		if (!generalCachePath.toFile().exists()) Files.createDirectories(generalCachePath);
		Path privateCachePath = Files.createTempDirectory(generalCachePath, Long.toString(System.nanoTime()));
		File cache = Files.createTempFile(privateCachePath, "querycache", ".txt").toFile();
        String cachePath = cache.getCanonicalPath();
        
        // update links for entities only once per execution
        Set<String> entitiesWithUpdatedLinks = new HashSet<>(); 
        
    	List<String> entityLinks = new ArrayList<>();
        
	    for (String s : textualReferences) {
	    	debug(log, "Extracted metadata from reference");
	    	String toEntityUri = extractMetaData(s, getOutputDataStoreClient());
	        
	        TextualReference textRef = getOutputDataStoreClient().get(TextualReference.class, s);
	        String linkFromSourceToReferencedEntity = createLinkToEntity(
	        		textRef.getMentionsReference(), toEntityUri, textRef);
	        entityLinks.add(linkFromSourceToReferencedEntity);
	        
	        // create links from referenced entity to entities in repository, if not already linked
	        if (!entitiesWithUpdatedLinks.contains(toEntityUri)) {
	        	List<String> queryServices = getExecution().getQueryServices();
	            List<Class<? extends QueryService>> queryServiceClasses = getExecution().getQueryServiceClasses();
	            entityLinks.addAll(linkEntity(toEntityUri, queryServices, queryServiceClasses, cachePath));
	        	entitiesWithUpdatedLinks.add(toEntityUri);
	        }
	        // TODO else return all existing links of entity
	        //else
	    }
	    cache.delete();
		privateCachePath.toFile().delete();
		log.debug("Returning entity links: " + entityLinks);
	    return entityLinks;
    }

    public String extractMetaData(String textualReference, DataStoreClient client) {
        Execution extract = getExecution().createSubExecution(MetaDataExtractor.class);
        List<String> textRefs = Arrays.asList(textualReference);
        extract.setTextualReferences(textRefs);
        getOutputDataStoreClient().post(Execution.class, extract);
        extract.instantiateAlgorithm(getInputDataStoreClient(), client,
        		getInputFileResolver(), getOutputFileResolver()).run();
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

    public List<String> createLinksForSearchResults(List<String> searchResults, String entityUri) {
    	Execution linker = getExecution().createSubExecution(getExecution().getSearchResultLinkerClass());
    	linker.setSearchResults(searchResults);
        linker.setLinkedEntities(Arrays.asList(entityUri));
        if (null != getExecution().getInputFiles() && !getExecution().getInputFiles().isEmpty()) linker.setInputFiles(getExecution().getInputFiles());
        getOutputDataStoreClient().post(Execution.class, linker);
        debug(log, "Creating links based on " + searchResults.size() + " search results");
        linker.instantiateAlgorithm(this).run();
        updateProgress(3, 3);
        debug(log, "Returning links: " + linker.getLinks());
        return linker.getLinks();
    }

    
	@Override
	public void execute() throws IOException {
		Execution tagSearcher = getExecution().createSubExecution(TagSearcher.class);
		tagSearcher.setTextualReferenceTags(getExecution().getTextualReferenceTags());
		tagSearcher.instantiateAlgorithm(this).run();
		getExecution().getTextualReferences().addAll(tagSearcher.getTextualReferences());
		getExecution().setLinks(new ArrayList<>());
		
		if (!getExecution().getTextualReferences().isEmpty()) {
			List<String> entityLinks = linkReferences(getExecution().getTextualReferences());
			getExecution().setLinks(entityLinks);
		}
		if (null != getExecution().getLinkedEntities() && !getExecution().getLinkedEntities().isEmpty()) {
			getExecution().getLinks().addAll(linkEntities(getExecution().getLinkedEntities()));
		}
		
		getExecution().setStatus(ExecutionStatus.FINISHED);
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

		// If textualReferences is empty, do not throw an exception. If used automatically after searching for 
		// patterns, the list of textual references may be empty, it is not an error.
		// If, however, ReferenceLinker is applied directly on existing textual references specified by their tags, the 
		// list should not be empty and throwing an error is assumed to be helpful for the user.
		if (null == getExecution().getTextualReferences()
				&& (null == getExecution().getTextualReferenceTags() || getExecution().getTextualReferenceTags().isEmpty())) {
			if (null == getExecution().getLinkedEntities() || getExecution().getLinkedEntities().isEmpty()) {
				throw new IllegalAlgorithmArgumentException(getClass(), "TextualReference", "Required parameter 'textual references' (or linked entities) is missing!");
			}
		}
		if (!queryServiceSet) {
            throw new IllegalAlgorithmArgumentException(getClass(), "queryService", "Required parameter 'query services' is missing!");
        }
		if (null == getExecution().getSearchResultLinkerClass()) {
			throw new IllegalAlgorithmArgumentException(getClass(), "searchResultLinkerClass", "Required parameter 'SearchResultLinkerClass' is missing!");
		}
	}
}