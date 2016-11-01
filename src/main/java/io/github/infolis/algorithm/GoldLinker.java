package io.github.infolis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.InfolisFile;

/**
 * Use links contained in the gold standard for linking new citedData entities. 
 * Use automated linking methods only for items not contained in the gold standard.
 * 
 * @author kata
 *
 */
public class GoldLinker extends SearchResultLinker {

	private static final Logger log = LoggerFactory.getLogger(GoldLinker.class);
	
	public GoldLinker(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
			FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}

	@Override
	public void execute() throws IOException {
		List<String> ruleFileTags = new ArrayList<>();
		log.debug("Loading data from rule file...");
		for (InfolisFile ruleFile : getInputDataStoreClient().get(InfolisFile.class, getExecution().getInputFiles())) {
			loadData(ruleFile);
			ruleFileTags.add(getRuleFileTag(ruleFile));
		}
		log.debug("Creating links...");
		if (null != getExecution().getLinkedEntities() && !getExecution().getLinkedEntities().isEmpty()) {
			String entityUri = getExecution().getLinkedEntities().get(0);
			Entity entity = getInputDataStoreClient().get(Entity.class, entityUri);
			List<String> links = link(entity, ruleFileTags);
			if (links.isEmpty()) {
				Execution exec = getExecution().createSubExecution(OntologyLinker.class);
				exec.setLinkedEntities(Arrays.asList(entityUri));
				exec.setSearchResults(getExecution().getSearchResults());
				exec.instantiateAlgorithm(this).run();
				getExecution().setLinks(exec.getLinks());
			}
			else getExecution().setLinks(links);
		}
	}
	
	private String getRuleFileTag(InfolisFile ruleFile) {
		return "ruleFile_" + ruleFile.getUri();
	}
	
	private void loadData(InfolisFile ruleFile) {
		Execution importerExec = getExecution().createSubExecution(LinkImporter.class);
		importerExec.addTag(getRuleFileTag(ruleFile));
		importerExec.setInputFiles(Arrays.asList(ruleFile.getUri()));
		importerExec.instantiateAlgorithm(this).run();
	}
	
	private List<String> link(Entity entity, List<String> ruleFileTags) {
		List<String> links = new ArrayList<>();
        debug(log, "Searching for matching entity in gold links");
        List<String> fromEntityUris = getEntitiesFromDatastore(entity, ruleFileTags);
        if (fromEntityUris.isEmpty()) return new ArrayList<>();
		// get all links with fromEntityUri as fromEntity and make copies with entity as fromEntity
        List<String> tags = new ArrayList<>();
        tags.addAll(ruleFileTags);
        tags.add("infolis-ontology");
        for (String fromEntityUri : fromEntityUris) {
        	for (EntityLink goldLink : getLinksFromDatastore(fromEntityUri, tags)) {
        		EntityLink newLink = new EntityLink();
        		newLink.setConfidence(goldLink.getConfidence());
        		newLink.setEntityRelations(goldLink.getEntityRelations());
        		newLink.setFromEntity(entity.getUri());
        		newLink.setTags(goldLink.getTags());
        		newLink.addAllTags(getExecution().getTags());
        		newLink.addAllTags(ruleFileTags);
        		newLink.setToEntity(goldLink.getToEntity());
        		getOutputDataStoreClient().post(EntityLink.class, newLink);
        		links.add(newLink.getUri());
        	}
        }
		return links;
	}
	
	
	//TODO do all clients use boolean OR? 
	/**
	 * Search entity in datastore - if an entity with the same properties can be found, 
	 * return its uri. Else, post the tempEntity to the datastore and return its uri.
	 * 
	 * Note: this method assumes the search method searches for entities having at least 
	 * one of the specified properties (OR), not all them (AND). 
	 * 
	 * @param tempEntity
	 * @return
	 */
	private List<String> getEntitiesFromDatastore(Entity entity, List<String> tags) {
		List<String> entities = new ArrayList<>();
		
		Multimap<String, String> query = HashMultimap.create();
		query.put("name", entity.getName());
		
		/*
		for (int i = 0; i < entity.getNumericInfo().size(); i++) {
			query.put("numericInfo", entity.getNumericInfo().get(i));
		}
		for (int i = 0; i < tags.size(); i++) {
			query.put("tags", tags.get(i));
		}*/
		
		List<Entity> entitiesInDatabase = getOutputDataStoreClient().search(Entity.class, query);
		for (Entity entityInDatabase : entitiesInDatabase) {
			if (((new HashSet<>(entityInDatabase.getNumericInfo()).equals(new HashSet<>(entity.getNumericInfo())))
						|| (CollectionUtils.intersection(entityInDatabase.getNumericInfo(), entity.getNumericInfo()).isEmpty())
						|| (entityInDatabase.getNumericInfo().isEmpty() && entity.getNumericInfo().isEmpty())) 
					&& (entityInDatabase.getName().equals(entity.getName()))
					&& (!CollectionUtils.intersection(entityInDatabase.getTags(), tags).isEmpty())) {
				debug(log, "Found entity in datastore: " + entityInDatabase.getUri());
				entities.add(entityInDatabase.getUri());
			}
		}
		debug(log, "found {} matching entities in data store", entities.size());
		return entities;
	}
	
	//Note: this method assumes the search method searches for entities having at least 
	//one of the specified properties (OR), not all them (AND). 
	// TODO checking tag should not be necessary
	private List<EntityLink> getLinksFromDatastore(String fromEntityUri, List<String> tags) {
		List<EntityLink> links = new ArrayList<>();
		
		Multimap<String, String> query = HashMultimap.create();
		query.put("fromEntity", fromEntityUri);
		for (EntityLink link : getOutputDataStoreClient().search(EntityLink.class, query)) {
			if (!CollectionUtils.intersection(link.getTags(), tags).isEmpty()) {
				debug(log, "Found link in datastore: " + link.getUri());
				links.add(link);
			}
		}
		debug(log, "found {} matching links in data store", links.size());
		return links;
	}
	
}