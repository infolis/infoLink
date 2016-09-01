package io.github.infolis.algorithm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.entity.InfolisFile;

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
		InputStream in = getInputFileResolver().openInputStream(linkFile);
		BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8")); 
		JsonObject jsonObject = Json.createReader(streamReader).readObject();
		// first pass: create all entities
		for (Entry<String, JsonValue> values : jsonObject.entrySet()) {
			if (values.getKey().equals("entity")) {
				//create entity
				//post entity
				//create link from entity to corresponding entities in the ontology
			}
		}
		// second pass: create all links
		for (Entry<String, JsonValue> values : jsonObject.entrySet()) {
			if (values.getKey().equals("entityLink")) {
				//create links
				//post links
			}
			
		}
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