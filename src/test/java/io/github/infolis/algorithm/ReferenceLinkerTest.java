package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.infolink.querying.DaraHTMLQueryService;
import io.github.infolis.infolink.querying.QueryService;

/**
 * 
 * @author kata
 *
 */
public class ReferenceLinkerTest extends InfolisBaseTest {
	
	@Test
	public void testExecute() throws IOException {
		InfolisFile infolisFile = new InfolisFile();
		dataStoreClient.post(InfolisFile.class, infolisFile);
		TextualReference reference = new TextualReference("In this snippet, the reference", "Studierendensurvey", "2012/13 is to be linked", infolisFile.getUri(), "pattern", infolisFile.getUri());
		dataStoreClient.post(TextualReference.class, reference);
		Execution exec = new Execution();
		exec.setTextualReferences(Arrays.asList(reference.getUri()));
		exec.setAlgorithm(ReferenceLinker.class);
		QueryService queryService = new DaraHTMLQueryService();
		dataStoreClient.post(QueryService.class, queryService);
		exec.setQueryServices(Arrays.asList(queryService.getUri()));
		exec.setSearchResultLinkerClass(BestMatchLinker.class);
		exec.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		List<String> linkUris = exec.getLinks();
	    assertEquals(1, linkUris.size());
	    EntityLink link = dataStoreClient.get(EntityLink.class, linkUris.get(0));
	    Entity toEntity = dataStoreClient.get(Entity.class, link.getToEntity());
	    assertEquals("Studiensituation und studentische Orientierungen 2012/13 (Studierenden-Survey)", toEntity.getName());
	    assertEquals("10.4232/1.5126", toEntity.getIdentifier());
	    
	    Execution exec2 = new Execution();
	    exec2.setTextualReferences(Arrays.asList(reference.getUri()));
		exec2.setAlgorithm(ReferenceLinker.class);
		exec2.setQueryServices(Arrays.asList(queryService.getUri()));
		exec2.setSearchResultLinkerClass(MultiMatchesLinker.class);
		exec2.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		linkUris = exec2.getLinks();
	    assertEquals(3, linkUris.size());
	    List<EntityLink> links = dataStoreClient.get(EntityLink.class, linkUris);
	    Entity toEntity1 = dataStoreClient.get(Entity.class, links.get(0).getToEntity());
	    Entity toEntity2 = dataStoreClient.get(Entity.class, links.get(1).getToEntity());
	    Entity toEntity3 = dataStoreClient.get(Entity.class, links.get(2).getToEntity());
	    assertTrue(Arrays.asList(toEntity1.getName(), toEntity2.getName(), toEntity3.getName()).contains("Studiensituation und studentische Orientierungen 2012/13 (Studierenden-Survey)"));
	    assertTrue(Arrays.asList(toEntity1.getName(), toEntity2.getName(), toEntity3.getName()).contains("Studiensituation und studentische Orientierungen (Studierenden-Survey) Kumulation 1983 - 2013"));
	    assertTrue(Arrays.asList(toEntity1.getIdentifier(), toEntity2.getIdentifier(), toEntity3.getIdentifier()).contains("10.4232/1.5126"));
	    assertTrue(Arrays.asList(toEntity1.getIdentifier(), toEntity2.getIdentifier(), toEntity3.getIdentifier()).contains("10.4232/1.12510"));
	    assertTrue(Arrays.asList(toEntity1.getIdentifier(), toEntity2.getIdentifier(), toEntity3.getIdentifier()).contains("10.4232/1.12494"));
	    
	    Execution exec3 = new Execution();
	    TextualReference reference2 = new TextualReference("In this snippet, the reference", "Studierendensurvey", "of any year is to", infolisFile.getUri(), "pattern", infolisFile.getUri());
		dataStoreClient.post(TextualReference.class, reference2);
	    exec3.setTextualReferences(Arrays.asList(reference2.getUri()));
		exec3.setAlgorithm(ReferenceLinker.class);
		exec3.setQueryServices(Arrays.asList(queryService.getUri()));
		exec3.setSearchResultLinkerClass(BestMatchLinker.class);
		exec3.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		linkUris = exec3.getLinks();
	    assertEquals(1, linkUris.size());
	    link = dataStoreClient.get(EntityLink.class, linkUris.get(0));
	    toEntity = dataStoreClient.get(Entity.class, link.getToEntity());
	    
	    Execution exec4 = new Execution();
	    exec4.setTextualReferences(Arrays.asList(reference2.getUri()));
		exec4.setAlgorithm(ReferenceLinker.class);
		exec4.setQueryServices(Arrays.asList(queryService.getUri()));
		exec4.setSearchResultLinkerClass(MultiMatchesLinker.class);
		exec4.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		linkUris = exec4.getLinks();
	    assertEquals(25, linkUris.size());

	    // testa for query cache
        Execution exec5 = new Execution();
        TextualReference reference3 = new TextualReference("In this snippet, the reference", "Studierendensurvey", "2012/13 is to be linked", infolisFile.getUri(), "pattern", infolisFile.getUri());
        TextualReference reference4 = new TextualReference("In this snippet, the reference", "Studierendensurvey", "2012/13 is to be linked", infolisFile.getUri(), "pattern", infolisFile.getUri());
		dataStoreClient.post(TextualReference.class, reference3);
		dataStoreClient.post(TextualReference.class, reference4);
	    exec5.setTextualReferences(Arrays.asList(reference3.getUri(), reference4.getUri()));
		exec5.setAlgorithm(ReferenceLinker.class);
		exec5.setQueryServices(Arrays.asList(queryService.getUri()));
		exec5.setSearchResultLinkerClass(BestMatchLinker.class);
		exec5.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		linkUris = exec5.getLinks();
	    assertEquals(2, linkUris.size());
	    EntityLink link5 = dataStoreClient.get(EntityLink.class, linkUris.get(0));
	    Entity toEntity5 = dataStoreClient.get(Entity.class, link5.getToEntity());
	    assertEquals("Studiensituation und studentische Orientierungen 2012/13 (Studierenden-Survey)", toEntity5.getName());
	    assertEquals("10.4232/1.5126", toEntity5.getIdentifier());
	    
	    EntityLink link5b = dataStoreClient.get(EntityLink.class, linkUris.get(1));
	    Entity toEntity5b = dataStoreClient.get(Entity.class, link5b.getToEntity());
	    assertEquals("Studiensituation und studentische Orientierungen 2012/13 (Studierenden-Survey)", toEntity5b.getName());
	    assertEquals("10.4232/1.5126", toEntity5b.getIdentifier());
	    
	    // no matching entries in dara
	    Execution exec6 = new Execution();
        TextualReference reference6 = new TextualReference("In this snippet, the reference", "hübbeldiebübb", "2012/13 is to be linked", infolisFile.getUri(), "pattern", infolisFile.getUri());
        TextualReference reference6b = new TextualReference("In this snippet, the reference", "hübbeldiebübb", "2012/13 is to be linked", infolisFile.getUri(), "pattern", infolisFile.getUri());
        dataStoreClient.post(TextualReference.class, reference6);
		dataStoreClient.post(TextualReference.class, reference6b);
	    exec6.setTextualReferences(Arrays.asList(reference6.getUri(), reference6b.getUri()));
		exec6.setAlgorithm(ReferenceLinker.class);
		exec6.setQueryServices(Arrays.asList(queryService.getUri()));
		exec6.setSearchResultLinkerClass(BestMatchLinker.class);
		exec6.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		linkUris = exec6.getLinks();
	    assertEquals(0, linkUris.size());
	    
	    // exactly one matching entry in dara (to date)
	    Execution exec7 = new Execution();
        TextualReference reference7 = new TextualReference("In this snippet, the reference", "CELLA1", " is to be linked", infolisFile.getUri(), "pattern", infolisFile.getUri());
        TextualReference reference7b = new TextualReference("In this snippet, the reference", "CELLA1", " is to be linked", infolisFile.getUri(), "pattern", infolisFile.getUri());
        dataStoreClient.post(TextualReference.class, reference7);
		dataStoreClient.post(TextualReference.class, reference7b);
	    exec7.setTextualReferences(Arrays.asList(reference7.getUri(), reference7b.getUri()));
		exec7.setAlgorithm(ReferenceLinker.class);
		exec7.setQueryServices(Arrays.asList(queryService.getUri()));
		exec7.setSearchResultLinkerClass(MultiMatchesLinker.class);
		exec7.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		linkUris = exec7.getLinks();
	    assertEquals(2, linkUris.size());
	    EntityLink link7b = dataStoreClient.get(EntityLink.class, linkUris.get(1));
	    Entity toEntity7b = dataStoreClient.get(Entity.class, link7b.getToEntity());
	    assertEquals("Sozialwissenschaftliche Telefonumfragen in der Allgemeinbevölkerung über das Mobilfunknetz (CELLA 1)", toEntity7b.getName());
	    assertEquals("10.4232/1.4875", toEntity7b.getIdentifier());
	}
    
}