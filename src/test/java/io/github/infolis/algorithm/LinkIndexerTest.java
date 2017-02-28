package io.github.infolis.algorithm;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;

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
		dataStoreClient.post(Entity.class, entity1);
		dataStoreClient.post(Entity.class, entity2);
		dataStoreClient.post(Entity.class, entity3);
		link1.setFromEntity(entity1.getUri());
		link1.setToEntity(entity2.getUri());
		link2.setFromEntity(entity2.getUri());
		link2.setToEntity(entity3.getUri());
		dataStoreClient.post(EntityLink.class, link1);
		dataStoreClient.post(EntityLink.class, link2);
		List<String> links = Arrays.asList(link1.getUri(), link2.getUri());
		exec.setLinks(links);
		exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		// TODO tests
	}
}