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
		// call LinkImporter as subexecution
		// tags: "ruleFile_" + ruleFile.getUri()
	}
	
	private List<String> link(Entity entity, List<String> ruleFileTags) {
		List<EntityLink> links = new ArrayList<>();
        debug(log, "Searching for matching entity in gold links");
        String fromEntityUri = getEntityFromDatastore(entity, ruleFileTags);
        if (null == fromEntityUri) return new ArrayList<>();
		// TODO get all links with fromEntityUri as fromEntity and make copies with entity as fromEntity
		return new ArrayList<>();
	}
	
	
	//TODO do all clients use boolean OR? 
	/**
	 * Search entity in datastore - if an entity with the same properties can be found, 
	 * return its uri. Else, post the tempEntity to the datastore and return its uri.
	 * 
	 * Note: this method assumes the search method searches for entities having at least 
	 * one of the specified properties (AND), not all them (OR). 
	 * 
	 * @param tempEntity
	 * @return
	 */
	private String getEntityFromDatastore(Entity entity, List<String> tags) {
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
						|| (CollectionUtils.intersection(entityInDatabase.getNumericInfo(), entity.getNumericInfo()).isEmpty())) 
					&& (entityInDatabase.getName().equals(entity.getName()))
					&& (!CollectionUtils.intersection(entityInDatabase.getTags(), tags).isEmpty())) {
				debug(log, "Found entity in datastore: " + entityInDatabase.getUri());
				return entityInDatabase.getUri();
			}
		}
		return null;
	}
	
}