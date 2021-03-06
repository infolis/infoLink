package io.github.infolis.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.github.infolis.datastore.LocalClient;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.EntityLink.EntityRelation;
import io.github.infolis.InfolisConfig;

/**
 * 
 * @author kata
 * 
 * Create links to entities in ontology, if study is included there.
 *
 */
public class OntologyLinker extends MultiMatchesLinker {
	
	private static final Logger log = LoggerFactory.getLogger(OntologyLinker.class);
	
	public OntologyLinker(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private String searchOntologyEntity(Entity entity, boolean onlyOntologyEntities) {
		Multimap<String, String> query = HashMultimap.create();
		// entities in ontology and used repositories have exactly one identifier
		query.put("identifier", entity.getIdentifiers().get(0));
		query.put("tags", "infolis-ontology");
		List<Entity> entitiesInDatabase = getOutputDataStoreClient().search(Entity.class, query);
		for (Entity entityInDatabase : entitiesInDatabase) {
			debug(log, "Found entity in ontology: " + entityInDatabase.getUri());
			return entityInDatabase.getUri();
		}
		debug(log, "Did not find entity in ontology");
		if (onlyOntologyEntities) return null;
		else return entity.getUri();
	}
	
	/**
	 * Return ontology entry for entity if existing.
	 * If not, 
	 * <ul>
	 * <li>return null if onlyOntologyEntities is set</li>
	 * <li>return URI of entity if onlyOntologyEntities is not set</li>
	 * </ul>
	 * @param entity
	 * @param onlyOntologyEntities
	 * @return
	 */
	private String getOntologyEntity(Entity entity, boolean onlyOntologyEntities) {
		String baseUri = "";
		// TODO add "getUriPrefix"-method to AbstractClient and implementing classes; use this here
		if (!getOutputDataStoreClient().getClass().isAssignableFrom(LocalClient.class)) {
			baseUri = InfolisConfig.getFrontendURI() + "/entity/";
		}
		String ontologyUri = baseUri + "dataset_" + entity.getIdentifiers().get(0)
				.replace("/", "")
				.replace(".", "");
		try {
			Entity ontologyEntity = getOutputDataStoreClient().get(Entity.class, ontologyUri);
			
			if (null != ontologyEntity) {
				return ontologyUri;
			} else {
				if (onlyOntologyEntities) return null;
				else return entity.getUri();
			}
		} catch (RuntimeException e) {
			if (onlyOntologyEntities) return null;
			else return entity.getUri();
		}
		
	}
	
	protected List<String> enhanceLinksUsingOntology(List<String> links) {
		List<String> newLinks = new ArrayList<>();
		for (EntityLink link : getOutputDataStoreClient().get(EntityLink.class, links)) {
			Entity toEntity = getOutputDataStoreClient().get(Entity.class, link.getToEntity());
			String entityUri = getOntologyEntity(toEntity, true);
			if (null != entityUri) {
				EntityLink newLink = new EntityLink(toEntity.getUri(), entityUri, 1.0, 
						"", new HashSet<>(Arrays.asList(EntityRelation.same_as)));
				getOutputDataStoreClient().post(EntityLink.class, newLink);
				newLinks.add(newLink.getUri());
			}
		}
		return newLinks;
	}
	
	@Override
	public void execute() {
		super.execute();
		List<String> links = getExecution().getLinks();
		getExecution().getLinks().addAll(enhanceLinksUsingOntology(links));
	}
}