package io.github.infolis.algorithm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisConfig;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.LocalClient;
import io.github.infolis.model.EntityType;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.EntityLink.EntityRelation;

/**
 * Class for importing entities and entityLinks. Entities are connected to entities 
 * in the ontology at import.
 * 
 * @author kata
 *
 */
public class LinkImporter extends BaseAlgorithm {

	private static final Logger log = LoggerFactory.getLogger(LinkImporter.class);
			
	public LinkImporter(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
			FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	/*
	 * Create entities and entityLinks, post them to the datastore and link entities 
	 * to corresponding entities in the ontology.
	 */
	private void importLinkFile(InfolisFile linkFile) throws IOException {
		List<String> importedEntities = new ArrayList<>();
		List<String> importedLinks = new ArrayList<>();
		
		InputStream in = getInputFileResolver().openInputStream(linkFile);
		BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8")); 
		JsonObject jsonObject = Json.createReader(streamReader).readObject();

		for (Entry<String, JsonValue> values : jsonObject.entrySet()) {
			if (values.getKey().equals("entity")) {
				JsonObject entities = (JsonObject)(values.getValue());
				for (Entry<String, JsonValue> entityEntry : entities.entrySet()) {
					JsonObject entityValues = (JsonObject)(entityEntry.getValue());
					Entity entity = new Entity();
					try {
						entity.setIdentifiers(toList(entityValues.get("identifiers")));
					} catch (NullPointerException npe) {};
					try {
						entity.setAbstractText(entityValues.getString("abstractText"));
					} catch (NullPointerException npe) {};
					try {
						entity.setAlternativeNames(toList(entityValues.get("alternativeNames")));
					} catch (NullPointerException npe) {};
					try {
						entity.setAuthors(toList(entityValues.get("authors")));
					} catch (NullPointerException npe) {};
					try {
						entity.setEntityType(EntityType.valueOf(EntityType.class, entityValues.getString("entityType")));
					} catch (NullPointerException npe) {};
					try {
						entity.setLanguage(entityValues.getString("language"));
					} catch (NullPointerException npe) {};
					try {
						entity.setName(entityValues.getString("name"));
					} catch (NullPointerException npe) {};
					try {
						entity.setNumericInfo(toList(entityValues.get("numericInfo")));
					} catch (NullPointerException npe) {};
					try {
						entity.setSpatial(new HashSet<>(toList(entityValues.get("spatial"))));
					} catch (NullPointerException npe) {};
					try {
						entity.setSubjects(toList(entityValues.get("subjects")));
					} catch (NullPointerException npe) {};
					try {
						entity.setVersionInfo(entityValues.getString("versionInfo"));
					} catch (NullPointerException npe) {};
					try {
						entity.setTags(new HashSet<>(toList(entityValues.get("tags"))));
					} catch (NullPointerException npe) {};
					entity.addAllTags(getExecution().getTags());
					try {
						entity.setURL(entityValues.getString("url"));
					} catch (NullPointerException npe) {};

					getOutputDataStoreClient().post(Entity.class, entity);
					importedEntities.add(entity.getUri());
					debug(log, "imported entity {}", entity.getUri());
					
					//create link from entity to corresponding entities in the ontology
					if (null != entity.getIdentifiers() && !entity.getIdentifiers().isEmpty()) {
						String ontologyEntity = getOntologyEntity(entity);
						if (null != ontologyEntity)  {
							EntityLink ontoLink = createLink(entity.getUri(), ontologyEntity, new HashSet<EntityRelation>(Arrays.asList(EntityRelation.same_as)));
							ontoLink.setTags(getExecution().getTags());
							getOutputDataStoreClient().post(EntityLink.class, ontoLink);
							importedLinks.add(ontoLink.getUri());
						}
					}
					
				};
			}
			else if (values.getKey().equals("entityLink")) {
				JsonObject links = (JsonObject)(values.getValue());
				for (Entry<String, JsonValue> linkEntry : links.entrySet()) {
					JsonObject linkValues = (JsonObject)(linkEntry.getValue());
					EntityLink link = new EntityLink();
					try {
						link.setConfidence(Double.valueOf(linkValues.get("confidence").toString()));
					} catch (NullPointerException npe) {};
					try {
						link.setEntityRelations(new HashSet<>(toEntityRelationList(linkValues.get("entityRelations"))));
					} catch (NullPointerException npe) {};
					try {
						link.setFromEntity(linkValues.getString("fromEntity"));
					} catch (NullPointerException npe) {
						warn(log, "entityLink missing fromEntity. Ignoring link");
						continue;
					};
					try {
						link.setLinkReason(linkValues.getString("linkReason"));
					} catch (NullPointerException npe) {};
					try {
						link.setTags(new HashSet<>(toList(linkValues.get("tags"))));
					} catch (NullPointerException npe) {};
					link.addAllTags(getExecution().getTags());
					try {
						link.setToEntity(linkValues.getString("toEntity"));
					} catch (NullPointerException npe) {
						warn(log, "entityLink missing toEntity. Ignoring link");
						continue;
					};
					
					getOutputDataStoreClient().post(EntityLink.class, link);
					importedLinks.add(link.getUri());
					debug(log, "imported entityLink {}", link.getUri());
				}
			}
		}
		getExecution().setLinkedEntities(importedEntities);
		getExecution().setLinks(importedLinks);
	}
	
	private List<EntityRelation> toEntityRelationList(JsonValue jsonValue) {
		List<EntityRelation> list = new ArrayList<>();
		JsonArray array = (JsonArray) jsonValue;
		for (JsonValue val : array) {
			list.add(EntityRelation.valueOf(EntityRelation.class, val.toString().replaceAll("\"", "")));
		}
		return list;
	}
	
	private List<String> toList(JsonValue jsonValue) {
		List<String> list = new ArrayList<>();
		JsonArray array = (JsonArray) jsonValue;
		for (JsonValue val : array) {
			// remove '"' from string
			list.add(val.toString().substring(1, val.toString().length() - 1));
		}
		return list;
	}
	
	private EntityLink createLink(String fromEntityUri, String toEntityUri, Set<EntityRelation> entityRelations) {
		EntityLink link = new EntityLink();
		link.setFromEntity(fromEntityUri);
		link.setToEntity(toEntityUri);
		link.setEntityRelations(entityRelations);
		return link;
	}
	
	private String getOntologyEntity(Entity entity) {
		String ontologyUri = "dataset_" + entity.getIdentifiers().get(0)
				.replace("/", "")
				.replace(".", "");
		// TODO add "getUriPrefix"-method to AbstractClient and implementing classes; use this here
		if (!getOutputDataStoreClient().getClass().isAssignableFrom(LocalClient.class)) {
			ontologyUri = InfolisConfig.getFrontendURI() + "/entity/" + ontologyUri;
		}
		Entity ontologyEntity = getOutputDataStoreClient().get(Entity.class, ontologyUri);
		if (null != ontologyEntity) {
			return ontologyUri;
		} else return null;
	}

	@Override
	public void execute() throws IOException {
		int i = 0;
		for (InfolisFile linkFile : getInputDataStoreClient().get(InfolisFile.class, getExecution().getInputFiles())) {
			log.debug("Importing " + linkFile);
			importLinkFile(linkFile);
			i ++;
			updateProgress(i, getExecution().getInputFiles().size());
		}
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		if (null == getExecution().getInputFiles() || getExecution().getInputFiles().isEmpty()) {
			throw new IllegalArgumentException("Must set at least one inputFile!");
		}
	}
	
}