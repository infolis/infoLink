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
	    assertEquals(2, linkUris.size());
	    // link1: link from publication entity to referenced entity
	    // link2: link from referenced entity to dataset entity
	    EntityLink link1 = dataStoreClient.get(EntityLink.class, linkUris.get(0));
	    Entity toEntity1 = dataStoreClient.get(Entity.class, link1.getToEntity());
	    EntityLink link2 = dataStoreClient.get(EntityLink.class, linkUris.get(1));
	    Entity toEntity2 = dataStoreClient.get(Entity.class, link2.getToEntity());
	    Entity fromEntity2 = dataStoreClient.get(Entity.class, link2.getFromEntity());
	    assertEquals("Studiensituation und studentische Orientierungen 2012/13 (Studierenden-Survey)", toEntity2.getName());
	    assertEquals("10.4232/1.5126", toEntity2.getIdentifier());
	    assertEquals("Studierendensurvey", toEntity1.getName());
	    assertEquals(Arrays.asList("2012/13"), toEntity1.getNumericInfo());
	    assertEquals(toEntity1.getUri(), fromEntity2.getUri());
	    
	    Execution exec2 = new Execution();
	    exec2.setTextualReferences(Arrays.asList(reference.getUri()));
		exec2.setAlgorithm(ReferenceLinker.class);
		exec2.setQueryServices(Arrays.asList(queryService.getUri()));
		exec2.setSearchResultLinkerClass(MultiMatchesLinker.class);
		exec2.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		linkUris = exec2.getLinks();
	    assertEquals(4, linkUris.size());
	    List<EntityLink> links = dataStoreClient.get(EntityLink.class, linkUris);
	    toEntity1 = dataStoreClient.get(Entity.class, links.get(1).getToEntity());
	    toEntity2 = dataStoreClient.get(Entity.class, links.get(2).getToEntity());
	    Entity toEntity3 = dataStoreClient.get(Entity.class, links.get(3).getToEntity());
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
	    assertEquals(2, linkUris.size());
	    EntityLink link = dataStoreClient.get(EntityLink.class, linkUris.get(1));
	    Entity toEntity = dataStoreClient.get(Entity.class, link.getToEntity());
	    
	    Execution exec4 = new Execution();
	    exec4.setTextualReferences(Arrays.asList(reference2.getUri()));
		exec4.setAlgorithm(ReferenceLinker.class);
		exec4.setQueryServices(Arrays.asList(queryService.getUri()));
		exec4.setSearchResultLinkerClass(MultiMatchesLinker.class);
		exec4.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		linkUris = exec4.getLinks();
	    assertEquals(26, linkUris.size());

	    // test for query cache
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
	    assertEquals(3, linkUris.size());
	    EntityLink link5 = dataStoreClient.get(EntityLink.class, linkUris.get(1));
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
	    assertEquals(2, linkUris.size());
	    
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
	    assertEquals(3, linkUris.size());
	    EntityLink link7b = dataStoreClient.get(EntityLink.class, linkUris.get(1));
	    Entity toEntity7b = dataStoreClient.get(Entity.class, link7b.getToEntity());
	    assertEquals("Sozialwissenschaftliche Telefonumfragen in der Allgemeinbevölkerung über das Mobilfunknetz (CELLA 1)", toEntity7b.getName());
	    assertEquals("10.4232/1.4875", toEntity7b.getIdentifier());
	}
    
}