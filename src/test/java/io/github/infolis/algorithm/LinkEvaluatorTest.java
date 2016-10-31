package io.github.infolis.algorithm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.infolink.querying.DaraSolrQueryService;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;

/**
 * 
 * @author kata
 *
 */
public class LinkEvaluatorTest extends InfolisBaseTest {
	
	@Test
	public void testCompareLinks() {
		Execution exec = new Execution();
		LinkEvaluator evaluator = new LinkEvaluator(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
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
	
	//@Test
	public void example() throws IOException {
		File goldDir = new File(getClass().getResource("/linkEvaluator/gold").getFile());
		File textDir = new File(getClass().getResource("/linkEvaluator/text").getFile());
		DataStoreClient client = DataStoreClientFactory.local();
		FileResolver resolver = FileResolverFactory.local();
		
		Execution link = new Execution(LearnPatternsAndCreateLinks.class);
		link.setInputFiles(ReferenceEvaluatorTest.uploadFiles(textDir, client));
		link.setUpperCaseConstraint(true);
		link.setTokenize(true);
		link.setTokenizeNLs(false);
		link.setPtb3Escaping(true);
		link.setRemoveBib(true);
		link.setSeeds(Arrays.asList("ALLBUS", "SOEP"));
		link.setReliabilityThreshold(0.05);
		link.setQueryServiceClasses(Arrays.asList(DaraSolrQueryService.class));
		link.setSearchResultLinkerClass(MultiMatchesLinker.class);
		link.instantiateAlgorithm(client, resolver).run();
		
		Execution eval = new Execution(LinkEvaluator.class);
		eval.setInputFiles(ReferenceEvaluatorTest.uploadFiles(goldDir, client));
		eval.setLinks(link.getLinks());
		eval.instantiateAlgorithm(client, resolver).run();
	}

}