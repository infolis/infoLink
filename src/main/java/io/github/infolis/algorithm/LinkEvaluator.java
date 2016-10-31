package io.github.infolis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.EntityType;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.EvaluationUtils;

/**
 * Class for evaluating infoLink against a gold standard.
 * 
 * @author kata
 *
 */
public class LinkEvaluator extends BaseAlgorithm {
	
	public LinkEvaluator(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
			FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private static final Logger log = LoggerFactory.getLogger(LinkEvaluator.class);
	private Mode mode = Mode.INFOLINK;
	
	public enum Mode {
		INFOLINK, LINKING;
	}
	
	public void setMode (Mode mode) {
		this.mode = mode;
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
		
		List<String> goldLinkStrings = flatten(goldLinkMap);
		List<String> foundLinkStrings = flatten(foundLinkMap);
		
		double precision = EvaluationUtils.getPrecision(goldLinkStrings, foundLinkStrings);
		double recall = EvaluationUtils.getRecall(goldLinkStrings, foundLinkStrings);
		double f1 = EvaluationUtils.getF1Measure(precision, recall);
		log.debug("Precision (counting individual links): " + precision);
		log.debug("Recall (counting individual links): " + recall);
		log.debug("F1 (counting individual links): " + f1); 
		
		precision = EvaluationUtils.getPrecision(new HashSet<>(goldLinkStrings), 
				new HashSet<>(foundLinkStrings));
		recall = EvaluationUtils.getRecall(new HashSet<>(goldLinkStrings), 
				new HashSet<>(foundLinkStrings));
		f1 = EvaluationUtils.getF1Measure(precision, recall);
		log.debug("Precision (counting link types): " + precision);
		log.debug("Recall (counting link types): " + recall);
		log.debug("F1 (counting link types): " + f1);	
	}
	
	// TODO compute study group - wise precision and recall using the ontology
	/**
	 * Compares foundLinks to goldLinks and computes precision and recall (for individual links 
	 * and entity-wise).
	 * 
	 * @param foundLinks
	 * @param goldLinks
	 */
	private void compareTransformedLinks(List<EntityLink> foundLinks, List<EntityLink> goldLinks) {
		ListMultimap<String, String> foundLinkMap = ArrayListMultimap.create();
		ListMultimap<String, String> goldLinkMap = ArrayListMultimap.create();
		for (EntityLink foundLink : foundLinks) {
			foundLinkMap.put(foundLink.getFromEntity(), foundLink.getToEntity());
		}
		for (EntityLink goldLink : goldLinks) {
			goldLinkMap.put(goldLink.getFromEntity(), goldLink.getToEntity());
		}
		
		List<String> goldLinkStrings = flatten(goldLinkMap);
		List<String> foundLinkStrings = flatten(foundLinkMap);
		
		double precision = EvaluationUtils.getPrecision(goldLinkStrings, foundLinkStrings);
		double recall = EvaluationUtils.getRecall(goldLinkStrings, foundLinkStrings);
		double f1 = EvaluationUtils.getF1Measure(precision, recall);
		log.debug("Precision (counting individual links): " + precision);
		log.debug("Recall (counting individual links): " + recall);
		log.debug("F1 (counting individual links): " + f1); 
		
		precision = EvaluationUtils.getPrecision(new HashSet<>(goldLinkStrings), 
				new HashSet<>(foundLinkStrings));
		recall = EvaluationUtils.getRecall(new HashSet<>(goldLinkStrings), 
				new HashSet<>(foundLinkStrings));
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
	
	private String normalizeFilename(String filename) {
		return filename.replaceAll(".*/", "").replaceAll("(\\.bibless)?(\\.tokenized)?\\.(tsv)?(txt)?(pdf)?", "");
	}
	
	/**
	 * For evaluation of <ol>
	 * <li>the linking algorithm (check whether given references are linked 
	 * correctly): mode.LINKING</li>
	 * <li>the whole InfoLink workflow 
	 * (extracting references + creating links): mode.INFOLINK</li>
	 * </ol>
	 * 
	 * @param mode	whether to evaluate linking or infolink
	 * @return
	 * @throws IOException
	 */
	private List<EntityLink> getGoldLinks() throws IOException {
		List<EntityLink> goldLinks = new ArrayList<>();
		
		for (InfolisFile goldFile : getInputDataStoreClient().get(InfolisFile.class, getExecution().getInputFiles())) {
			String content = IOUtils.toString(getInputFileResolver().openInputStream(goldFile));
			for (String line : content.split("\n+")) {
				String[] annotation = line.split("\t+");
				String reference = annotation[0].trim();
				String numericInfo = annotation[1].trim();
				String sourceDocs = annotation[2].trim();
				String unsure = annotation[3].trim();
				String dois = annotation[4].trim();
				
				for (String doi : dois.split("\\s*;\\s*")) {
					if (doi.trim().equals("-")) break;
					EntityLink link = new EntityLink();
					link.setToEntity(doi);
					
					switch(this.mode) {
					case LINKING: {
						link.setFromEntity(reference + numericInfo);
						goldLinks.add(link);
						break;
					}
					case INFOLINK: {
						for (String doc : sourceDocs.split("\\s*;\\s*")) {
							link.setFromEntity(normalizeFilename(doc));
							goldLinks.add(link);
							link = new EntityLink();
							link.setToEntity(doi);
						}
						break;
					}
					}
				}	
			}
		}
		return goldLinks;
	}
	
	private List<EntityLink> sortByToEntityType(List<EntityLink> links) {
		List<EntityLink> referenceLinks = new ArrayList<>();
		List<EntityLink> datasetLinks = new ArrayList<>();
		
		for (EntityLink link : links) {
			Entity toEntity = getInputDataStoreClient().get(Entity.class, link.getToEntity());
			if (toEntity.getEntityType().equals(EntityType.citedData)) referenceLinks.add(link);
			else datasetLinks.add(link);
		}
		
		List<EntityLink> sortedLinks = new ArrayList<>();
		sortedLinks.addAll(referenceLinks);
		sortedLinks.addAll(datasetLinks);
		
		return sortedLinks;
	}
	
	/**
	 * Replace entity identifiers with string representations of their content 
	 * for comparison with gold links.
	 * 
	 * @return
	 */
	private List<EntityLink> transformLinks(List<EntityLink> links) {
		List<EntityLink> transformedLinks = new ArrayList<>();
		Map<String, EntityLink> citedDataLinks = new HashMap<>();
		
		if(this.mode.equals(Mode.INFOLINK)) links = sortByToEntityType(links);
		
		for (EntityLink link : links) {
			EntityLink transformedLink = new EntityLink();
			Entity toEntity = getInputDataStoreClient().get(Entity.class, link.getToEntity());
			
			switch(this.mode) {
			case LINKING: {
				if (toEntity.getEntityType().equals(EntityType.citedData)) break;
				Entity fromEntity = getInputDataStoreClient().get(Entity.class, link.getFromEntity());
				transformedLink.setFromEntity(fromEntity.getName() + fromEntity.getNumericInfo());
				transformedLink.setToEntity(toEntity.getIdentifiers().get(0));
				transformedLinks.add(transformedLink);
				break;
			}
			case INFOLINK: {
				if (toEntity.getEntityType().equals(EntityType.citedData)) {
					TextualReference ref = getInputDataStoreClient().get(TextualReference.class, link.getLinkReason());
					InfolisFile file = getInputDataStoreClient().get(InfolisFile.class, ref.getTextFile());
					transformedLink.setFromEntity(normalizeFilename(file.getFileName()));
					citedDataLinks.put(toEntity.getUri(), transformedLink);
					break;
				} else {
					transformedLink = citedDataLinks.get(link.getFromEntity());
					transformedLink.setToEntity(toEntity.getIdentifiers().get(0));
					transformedLinks.add(transformedLink);
				}
			}
			}
		}
		return transformedLinks;
	}


	@Override
	public void execute() throws IOException {
		List<EntityLink> goldLinks = getGoldLinks();
		log.debug("imported {} gold links", goldLinks.size());
		List<EntityLink> foundLinks = transformLinks(
				getInputDataStoreClient().get(EntityLink.class, 
						getExecution().getLinks()));
		log.debug("transformed {} found links", foundLinks.size());
		compareTransformedLinks(foundLinks, goldLinks);
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub
		
	}
}