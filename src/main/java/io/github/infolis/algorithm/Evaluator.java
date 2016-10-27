package io.github.infolis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.util.EvaluationUtils;

/**
 * Class for evaluating infoLink against a gold standard.
 * 
 * @author kata
 *
 */
public class Evaluator extends BaseAlgorithm {
	
	public Evaluator(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
			FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private static final Logger log = LoggerFactory.getLogger(Evaluator.class);

	// TODO implement (see AnnotationHandler's compare method)
	/**
	 * Compares the references contained in foundReferences to references in goldReferences.
	 * Computes precision (exact and partial matches) and recall.
	 * 
	 * @param foundReferences
	 * @param goldReferences
	 */
	public void compareReferences(List<TextualReference> foundReferences, List<TextualReference> goldReferences) {
		// 1. iterate through references, generate maps for each different textFile
		// count: exact matches, partial matches; precision, recall; per individual references; per reference types per file
	}
	
	// TODO compute study group - wise precision and recall using the ontology
	/**
	 * Compares foundLinks to goldLinks and computes precision and recall (for individual links 
	 * and entity-wise).
	 * 
	 * @param foundLinks
	 * @param goldLinks
	 */
	public void compareLinks(List<EntityLink> foundLinks, List<EntityLink> goldLinks) {
		ListMultimap<String, String> foundLinkMap = ArrayListMultimap.create();
		ListMultimap<String, String> goldLinkMap = ArrayListMultimap.create();
		for (EntityLink foundLink : foundLinks) {
			Entity fromEntity = getInputDataStoreClient().get(Entity.class, foundLink.getFromEntity());
			Entity toEntity = getInputDataStoreClient().get(Entity.class, foundLink.getToEntity());
			foundLinkMap.put(fromEntity.getIdentifiers().get(0), toEntity.getIdentifiers().get(0));
		}
		for (EntityLink goldLink : goldLinks) {
			Entity fromEntity = getInputDataStoreClient().get(Entity.class, goldLink.getFromEntity());
			Entity toEntity = getInputDataStoreClient().get(Entity.class, goldLink.getToEntity());
			goldLinkMap.put(fromEntity.getIdentifiers().get(0), toEntity.getIdentifiers().get(0));
		}
		
		double precision = EvaluationUtils.getPrecision(flatten(goldLinkMap), flatten(foundLinkMap));
		double recall = EvaluationUtils.getRecall(flatten(goldLinkMap), flatten(foundLinkMap));
		double f1 = EvaluationUtils.getF1Measure(precision, recall);
		log.debug("Precision (counting individual links): " + precision);
		log.debug("Recall (counting individual links): " + recall);
		log.debug("F1 (counting individual links): " + f1); 
		
		precision = EvaluationUtils.getPrecision(new HashSet<>(flatten(goldLinkMap)), 
				new HashSet<>(flatten(foundLinkMap)));
		recall = EvaluationUtils.getRecall(new HashSet<>(flatten(goldLinkMap)), 
				new HashSet<>(flatten(foundLinkMap)));
		f1 = EvaluationUtils.getF1Measure(precision, recall);
		log.debug("Precision (counting link types): " + precision);
		log.debug("Recall (counting link types): " + recall);
		log.debug("F1 (counting link types): " + f1);	
	}
	
	private static List<String> flatten(ListMultimap<String, String> map) {
		List<String> entries = new ArrayList<>();
		for (String key : map.keySet()) {
			for (String value : map.get(key)) {
				entries.add(key + "->" + value);
			}
		}
		return entries;
	}

	@Override
	public void execute() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub
		
	}
}