package io.github.infolis.algorithm;

import java.util.Arrays;

import org.junit.Test;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;

/**
 * 
 * @author kata
 *
 */
public class EvaluatorTest extends InfolisBaseTest {
	
	@Test
	public void testCompareLinks() {
		Execution exec = new Execution();
		Evaluator evaluator = new Evaluator(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
		evaluator.setExecution(exec);
		
		Entity fromEntity1 = new Entity();
		fromEntity1.setIdentifiers(Arrays.asList("1"));
		Entity toEntity1a = new Entity();
		toEntity1a.setIdentifiers(Arrays.asList("a"));
		Entity toEntity1b = new Entity();
		toEntity1b.setIdentifiers(Arrays.asList("b"));
		dataStoreClient.post(Entity.class, Arrays.asList(fromEntity1, toEntity1a, toEntity1b));
		
		EntityLink goldLink1 = new EntityLink(fromEntity1.getUri(), toEntity1a.getUri(), 0.0, "");
		EntityLink goldLink2 = new EntityLink(fromEntity1.getUri(), toEntity1a.getUri(), 0.0, "");
		EntityLink goldLink3 = new EntityLink(fromEntity1.getUri(), toEntity1b.getUri(), 0.0, "");
		
		
		Entity fromEntity2 = new Entity();
		fromEntity2.setIdentifiers(Arrays.asList("1"));
		Entity toEntity2 = new Entity();
		toEntity2.setIdentifiers(Arrays.asList("a"));
		dataStoreClient.post(Entity.class, Arrays.asList(fromEntity2, toEntity2));
		
		EntityLink foundLink = new EntityLink(fromEntity2.getUri(), toEntity2.getUri(), 0.0, "");
		
		evaluator.compareLinks(Arrays.asList(foundLink), Arrays.asList(goldLink1, goldLink2, goldLink3));
	}
	
	
	@Test
	public void testCompareReferences() {
		Execution exec = new Execution();
		Evaluator evaluator = new Evaluator(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
		evaluator.setExecution(exec);
		
		TextualReference goldRef1 = new TextualReference("Using the ", "ALLBUS", "dataset , we", "textFile1", "", "");
		TextualReference goldRef2 = new TextualReference("Using the ", "SOEP", "dataset , we", "textFile1", "", "");
		TextualReference goldRef3 = new TextualReference("Using the ", "SOEP", "dataset , we", "textFile1", "", "");
		TextualReference goldRef4 = new TextualReference("Using the ", "SOEP", "dataset , we", "textFile2", "", "");
		
		TextualReference foundRef1 = new TextualReference("Using the ", "ALLBUS", "dataset , we", "textFile1", "", "");
		TextualReference foundRef2 = new TextualReference("Using the ", "ALLBUS dataset", ", we", "textFile1", "", "");
		
		evaluator.compareReferences(Arrays.asList(goldRef1, goldRef2, goldRef3, goldRef4), Arrays.asList(foundRef1, foundRef2));
	}
}