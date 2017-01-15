package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.EntityLink.EntityRelation;

/**
 * 
 * @author kata
 *
 */
public class OntologyLinkerTest extends InfolisBaseTest {
	
	private static final Logger log = LoggerFactory.getLogger(OntologyLinkerTest.class);
	
	@Before
	public void loadTestOntology() {
		Entity dataset1 = new Entity();
		Entity dataset2 = new Entity();
		dataset1.addIdentifier("id1");
		dataset2.addIdentifier("id2");
		dataset1.addTag("infolis-ontology");
		dataset2.addTag("infolis-ontology");
		dataset1.setName("ontologyDataset1");
		dataset1.setSpatial(new HashSet<>(Arrays.asList("spatial")));
		dataStoreClient.put(Entity.class, dataset1, "dataset_id1");
		dataStoreClient.put(Entity.class, dataset2, "dataset_id2");
		EntityLink link = new EntityLink();
		link.setFromEntity(dataset1.getUri());
		link.setToEntity(dataset2.getUri());
		link.addTag("infolis-ontology");
		link.addEntityRelation(EntityRelation.part_of);
		dataStoreClient.post(EntityLink.class, link);
	}
	
	@Test
	public void test() {
		Entity fromEntity1 = new Entity();
		Entity toEntity1 = new Entity();
		toEntity1.addIdentifier("id1");
		toEntity1.setName("dataset1");
		dataStoreClient.post(Entity.class, fromEntity1);
		dataStoreClient.post(Entity.class, toEntity1);
		EntityLink link = new EntityLink();
		link.setFromEntity(fromEntity1.getUri());
		link.setToEntity(toEntity1.getUri());
		link.addEntityRelation(EntityRelation.same_as_temporal);
		dataStoreClient.post(EntityLink.class, link);
		List<String> links = Arrays.asList(link.getUri());
		
		Execution exec = new Execution();
		exec.setLinks(links);
		OntologyLinker ontoLinker = new OntologyLinker(
				dataStoreClient, dataStoreClient, fileResolver, fileResolver);
		ontoLinker.setExecution(exec);
		
		for (EntityLink newLink : dataStoreClient.get(EntityLink.class, ontoLinker.enhanceLinksUsingOntology(links))) {
			Entity fromEntity = dataStoreClient.get(Entity.class, newLink.getFromEntity());
			Entity toEntity = dataStoreClient.get(Entity.class, newLink.getToEntity());
			log.debug("fromEntity: " + fromEntity.getName());
			log.debug("toEntity: " + toEntity.getName());
			log.debug("toEntitySpatial: " + toEntity.getSpatial());
			log.debug("entityRelations: " + newLink.getEntityRelations());
			
			assertEquals("ontologyDataset1", toEntity.getName());
		}
	}
}