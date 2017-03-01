package io.github.infolis.algorithm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.EntityType;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.EntityLink.EntityRelation;

public class LinkIndexerTest extends InfolisBaseTest {
	
	@Test
	public void test() {
		Execution exec = new Execution(LinkIndexer.class);
		EntityLink link1 = new EntityLink();
		EntityLink link2 = new EntityLink();
		Entity entity1 = new Entity();
		Entity entity2 = new Entity();
		Entity entity3 = new Entity();
		entity1.setIdentifiers(Arrays.asList("pub1"));
		entity2.setIdentifiers(Arrays.asList("cit1"));
		entity3.setIdentifiers(Arrays.asList("dat1"));
		entity1.setEntityType(EntityType.publication);
		entity2.setEntityType(EntityType.citedData);
		entity3.setEntityType(EntityType.dataset);
		dataStoreClient.post(Entity.class, entity1);
		dataStoreClient.post(Entity.class, entity2);
		dataStoreClient.post(Entity.class, entity3);
		link1.setFromEntity(entity1.getUri());
		link1.setToEntity(entity2.getUri());
		link2.setFromEntity(entity2.getUri());
		link2.setToEntity(entity3.getUri());
		link1.setConfidence(0.5);
		link2.setConfidence(0.9);
		link1.setEntityRelations(new HashSet<>(Arrays.asList(EntityRelation.references)));
		link2.setEntityRelations(new HashSet<>(Arrays.asList(EntityRelation.part_of_spatial, EntityRelation.part_of_temporal)));
		TextualReference ref = new TextualReference();
		ref.setLeftText("left text");
		ref.setRightText("right text");
		ref.setReference("reference");
		ref.setReferenceReliability(0.5);
		dataStoreClient.post(TextualReference.class, ref);
		link1.setLinkReason(ref.getUri());
		dataStoreClient.post(EntityLink.class, link1);
		dataStoreClient.post(EntityLink.class, link2);
		List<String> links = Arrays.asList(link1.getUri(), link2.getUri());
		exec.setLinks(links);
		exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		// TODO tests
	}
}