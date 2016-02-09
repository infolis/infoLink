package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.resolve.DaraHTMLQueryService;
import io.github.infolis.resolve.QueryService;

/**
 * 
 * @author kata
 *
 */
public class ReferenceResolverTest extends InfolisBaseTest {
	
	@Test
	public void testExecute() {
		// BestMatchRanker and MultiMatchesRanker should yield the same result in this case
		InfolisFile infolisFile = new InfolisFile();
		dataStoreClient.post(InfolisFile.class, infolisFile);
		TextualReference reference = new TextualReference("In this snippet, the reference", "Studierendensurvey", "2012/13 is to be linked", infolisFile.getUri(), "pattern", infolisFile.getUri());
		dataStoreClient.post(TextualReference.class, reference);
		Execution exec = new Execution();
		exec.setTextualReferences(Arrays.asList(reference.getUri()));
		exec.setAlgorithm(ReferenceResolver.class);
		QueryService queryService = new DaraHTMLQueryService();
		dataStoreClient.post(QueryService.class, queryService);
		exec.setQueryServices(Arrays.asList(queryService.getUri()));
		exec.setSearchResultRankerClass(BestMatchRanker.class);
		exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		List<String> linkUris = exec.getLinks();
	    assertEquals(1, linkUris.size());
	    EntityLink link = dataStoreClient.get(EntityLink.class, linkUris.get(0));
	    Entity toEntity = dataStoreClient.get(Entity.class, link.getToEntity());
	    assertEquals("Studiensituation und studentische Orientierungen 2012/13 (Studierenden-Survey)", toEntity.getName());
	    assertEquals("10.4232/1.5126", toEntity.getIdentifier());
	    
	    Execution exec2 = new Execution();
	    exec2.setTextualReferences(Arrays.asList(reference.getUri()));
		exec2.setAlgorithm(ReferenceResolver.class);
		exec2.setQueryServices(Arrays.asList(queryService.getUri()));
		exec2.setSearchResultRankerClass(MultiMatchesRanker.class);
		exec2.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		linkUris = exec2.getLinks();
	    assertEquals(1, linkUris.size());
	    link = dataStoreClient.get(EntityLink.class, linkUris.get(0));
	    toEntity = dataStoreClient.get(Entity.class, link.getToEntity());
	    assertEquals("Studiensituation und studentische Orientierungen 2012/13 (Studierenden-Survey)", toEntity.getName());
	    assertEquals("10.4232/1.5126", toEntity.getIdentifier());
	    
	    // BestMatchRanker and MultiMatchesRanker should not yield the same result in this case
	    Execution exec3 = new Execution();
	    TextualReference reference2 = new TextualReference("In this snippet, the reference", "Studierendensurvey", "of any year is to", infolisFile.getUri(), "pattern", infolisFile.getUri());
		dataStoreClient.post(TextualReference.class, reference2);
	    exec3.setTextualReferences(Arrays.asList(reference2.getUri()));
		exec3.setAlgorithm(ReferenceResolver.class);
		exec3.setQueryServices(Arrays.asList(queryService.getUri()));
		exec3.setSearchResultRankerClass(BestMatchRanker.class);
		exec3.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		linkUris = exec3.getLinks();
	    assertEquals(1, linkUris.size());
	    link = dataStoreClient.get(EntityLink.class, linkUris.get(0));
	    toEntity = dataStoreClient.get(Entity.class, link.getToEntity());
	    assertEquals("Studiensituation und studentische Orientierungen 1982/83 (Studierenden-Survey)", toEntity.getName());
	    assertEquals("10.4232/1.1884", toEntity.getIdentifier());
	    
	    Execution exec4 = new Execution();
	    exec4.setTextualReferences(Arrays.asList(reference2.getUri()));
		exec4.setAlgorithm(ReferenceResolver.class);
		exec4.setQueryServices(Arrays.asList(queryService.getUri()));
		exec4.setSearchResultRankerClass(MultiMatchesRanker.class);
		exec4.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		linkUris = exec4.getLinks();
	    assertEquals(12, linkUris.size());
	}
    
}