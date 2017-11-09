package io.github.infolis.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.StringJoiner;

import java.io.StringReader;
import java.util.Arrays;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import io.github.infolis.InfolisConfig;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.EntityType;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.EntityLink.EntityRelation;
import io.github.infolis.util.SerializationUtils;
import io.github.infolis.algorithm.DbIndexer;
import io.github.infolis.algorithm.DbIndexer.ElasticLink;

public class LinkIndexer extends BaseAlgorithm {

	private static final Logger log = LoggerFactory.getLogger(LinkIndexer.class);
	
	public LinkIndexer(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
			FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	//TODO taken from dataProcessing's Importer..refactor
	private Entity getUniqueEntity(Multimap<String, String> toSearch) {
		List<Entity> hits = getInputDataStoreClient().search(Entity.class, toSearch);
		if (hits.size() > 1) log.warn("warning: found more than one entity " + toSearch.toString());
		if (hits.size() < 1) return null;
		else {
			log.debug("Found entity " + toSearch.toString());
			return hits.get(0);
		}
	}
		
	private Entity getDataSearchEntity(String doi) {
		String doiPrefix = "http://dx.doi.org/";
		doi = doi.replace(doiPrefix, "");
		//search in local database: datasearch doi found? if so return, if not:
			//connect to datasearch es index
			//search doi, create entity with all needed metadata, push to datastore and return
		Entity dataset = getDatasetWithDoi(doi);
		if (null == dataset) dataset = getDatasearchDatasetWithDoi(doi);
		return dataset;
	}

	private Entity getDatasearchDatasetWithDoi(String doi) {
		String dataSearchIndex = "http://193.175.238.35:8089/dc/_search";
		String query = "{ \"query\": {" +
					"\"bool\": { " +
						"\"must\": [ " + 
							"{\"nested\": {" +
								"\"path\": \"dc.identifier\"," +
									"\"query\": {" +
										"\"bool\": {" +
											"\"must\": [" +
											"{" +
												"\"match\": {" +
													"\"dc.identifier.nn\": \"" + doi + "\"" +
												"}" +
											"}" +
											"]" +
										"}" +
									"}" +
								"}" +
							"}" +
						"]" +
					"}" +
				"}" +
			"}";
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(dataSearchIndex);
		String answer = null;
		try {
			answer = importer_post(httpclient, httppost, new StringEntity(query, ContentType.APPLICATION_JSON));
		} catch (IOException e) { log.error(e.toString()); }
		if (null == answer) return null;

		JsonObject json = Json.createReader(new StringReader(answer)).readObject();
		JsonObject hits1 = json.getJsonObject("hits");
		JsonArray hits = hits1.getJsonArray("hits");
		
		if (hits.size() > 1) log.warn("Dataset with doi seems to be registered in more than one repository; using first definition found: " + doi);
		if (hits.size() < 1) return null;//log.debug(hits.get(0).toString());
		JsonObject entry = (JsonObject) hits.get(0);
		JsonObject source = (JsonObject) entry.get("_source");
		JsonObject metadata = (JsonObject) source.get("dc");
		//-> dc -> creator -> all // title -> all // date -> all OR anydateYear
		//log.debug(metadata.toString());
		JsonObject creatorLists = (JsonObject) metadata.get("creator");
		JsonArray creatorArray = (JsonArray) creatorLists.get("all");
		List<String> creatorList = new ArrayList<>();
		for (int i = 0; i<creatorArray.size(); i++) if(!creatorList.contains(creatorArray.getString(i))) creatorList.add(creatorArray.getString(i));
		
		JsonObject titleLists = (JsonObject) metadata.get("title");
		JsonArray titles = (JsonArray) titleLists.get("all");
		String title = null;
		// TODO different languages...
		if (null != titles) {
			if (titles.size() >= 1) title = titles.getString(0);
		}

		JsonArray yearArray = (JsonArray) metadata.get("anydateYear");
		String year = "o.J.";
		//TODO array to list; use first entry or, if more than one: don't use any...
		
		if (null != yearArray) {
			if (yearArray.size() > 1) log.debug("Warning: more than one entry in field anydateYear. Ignoring: " + yearArray.toString());
			else if (yearArray.size() == 1) year = yearArray.getString(0);
			else log.debug("No entry in field anydateYear");
		}

		Entity entity = new Entity();
		entity.setEntityType(EntityType.dataset);
		entity.setEntityProvenance("dataSearch");
		entity.setDoi(doi);
		String dataSearchId = source.get("esid").toString().replace("\"", "");
		String gwsId = "datasearch-" + dataSearchId.replaceAll("[.:/]", "-");
		entity.setIdentifiers(Arrays.asList(doi, dataSearchId, gwsId));
		entity.setGwsId(gwsId);
		if (null != title) entity.setName(title);
		entity.setYear(year);
		if (!creatorList.isEmpty()) entity.setAuthors(creatorList);
		entity = setEntityView(entity);	

		getOutputDataStoreClient().post(Entity.class, entity);
		return entity;
	}

	protected Entity setEntityView(Entity entity) {
		String year = entity.getYear();
		if (null == year) year = "o.J.";
		entity.setEntityView(String.format("%s (%s): %s", getAuthorString(entity), entity.getYear(), entity.getName()));
		return entity;
	}


	public Entity getDatasetWithDoi(String doi) {
		Multimap<String, String> toSearch = HashMultimap.create();
		toSearch.put("identifiers", doi);
		//toSearch.put("doi", doi);
		toSearch.put("entityType", "dataset");
		toSearch.put("entityProvenance", "dbk");
		//toSearch.put("entityProvenance", "datorium");
		return getUniqueEntity(toSearch);
	}


	private String importer_post(HttpClient httpclient, HttpPost httppost, StringEntity data) throws ClientProtocolException, IOException {
                httppost.setEntity(data);
                httppost.setHeader("content-type", ContentType.APPLICATION_JSON.toString());
                httppost.setHeader("Accept", ContentType.APPLICATION_JSON.toString());

                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                        
                if (entity != null) {
                    InputStream instream = entity.getContent();
                    try {
                        //log.debug(IOUtils.toString(instream));
			return IOUtils.toString(instream);
                    } finally {
                        instream.close();
                    }
                }

		return null;
        }
	
	protected String getAuthorString(Entity entity) {
		StringJoiner author = new StringJoiner("; ");
		String authorString = null;
		int n = 0;
		// TODO shorten and add "et al." when more than 2 authors
		if (null != entity.getAuthors()) {
			for (String a : entity.getAuthors()) {
				if (n > 1) {
					authorString = author.toString() + " et al.";
					break;
				} else {
					author.add(a);
					n++;
				}
			}	
		}
		if (null == authorString) return author.toString();
		else return authorString;
	}

//end of Importer's methods
	// TODO citedData that could not be linked is ignored
	// desired behaviour might change in the future; if so, modify here
/*
	private List<EntityLink> getGwsLinksForEntity(
			Multimap<String, String> entityEntityMap, 
			Multimap<String, String> entitiesLinkMap,
			String startEntityUri, Multimap<String, String> toEntities, 
			List<EntityLink> processedLinks) {
		List<EntityLink> flattenedLinks = new ArrayList<>();
		for (Map.Entry<String, String> entry : toEntities.entries()) {
			Entity toEntity = getInputDataStoreClient().get(Entity.class, entry.getKey().replaceAll("http://.*//*entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
			EntityLink link = getInputDataStoreClient().get(EntityLink.class, entry.getValue().replaceAll("http://.*//*entityLink", "http://svkolodtest.gesis.intra/link-db/api/entityLink"));
			
			if (!toEntity.getEntityType().equals(EntityType.citedData)) {

				EntityLink directLink = new EntityLink();
				directLink.setFromEntity(startEntityUri);
				directLink.setToEntity(link.getToEntity());
				directLink.setEntityRelations(link.getEntityRelations());
				// set cited data as link view
				Entity fromEntity = getInputDataStoreClient().get(Entity.class, link.getFromEntity().replaceAll("http://.*//*entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
				StringJoiner linkView = new StringJoiner(" ");
				//TODO check
				if (fromEntity.getEntityType().equals(EntityType.citedData)) {
					linkView.add(fromEntity.getName());
					//for (String number : fromEntity.getNumericInfo()) linkView.add(number);
					if (null != fromEntity.getNumericInfo() && !fromEntity.getNumericInfo().isEmpty()) linkView.add(fromEntity.getNumericInfo().get(0));
				}
				directLink.setLinkView(linkView.toString());
				directLink.setTags(link.getTags());
				
				int intermediateLinks = processedLinks.size();
				String linkReason = null;
				double confidenceSum = 0;
				Set<String> provenance = new HashSet<>();
				for (EntityLink intermediateLink : processedLinks) {
					confidenceSum += intermediateLink.getConfidence();
					if (null != intermediateLink.getLinkReason()) {
						//TextualReference ref = getInputDataStoreClient().get(TextualReference.class, intermediateLink.getLinkReason().replaceAll("http://.*//*textualReference", "http://svkolodtest.gesis.intra/link-db/api/textualReference"));
						//TODO REPLACE TOKENS LIKE -RRB-
						//linkReason = ref.toPrettyString();
						linkReason = intermediateLink.getLinkReason().replaceAll("http://.*//*textualReference", "http://svkolodtest.gesis.intra/link-db/api/textualReference");
					}
					directLink.addAllTags(intermediateLink.getTags());
					for (EntityRelation relation : intermediateLink.getEntityRelations()) {
						if (!relation.equals(EntityRelation.same_as)) directLink.addEntityRelation(relation);
					}
					// provenance entries of intermediate links do not have to be equal - e.g. manually specified cited data may have been linked to datasets automatically
					if (null != intermediateLink.getProvenance() && !intermediateLink.getProvenance().isEmpty()) provenance.add(intermediateLink.getProvenance());
				}
				confidenceSum += link.getConfidence();
				intermediateLinks += 1;
				directLink.setConfidence(confidenceSum / intermediateLinks);
				directLink.setLinkReason(linkReason);
				log.debug("reference: " + linkReason);
						
				if (null != link.getProvenance() && !link.getProvenance().isEmpty()) provenance.add(link.getProvenance());
				if (null == provenance || provenance.isEmpty()) directLink.setProvenance("InfoLink");
				else directLink.setProvenance(String.join(" + ", provenance));
						
				directLink.addAllTags(getExecution().getTags());
				log.debug("flattenedLink: " + SerializationUtils.toJSON(directLink));
				flattenedLinks.add(directLink);

			} else {
				toEntities = ArrayListMultimap.create();
				// only use 1:1 links
				/*if (entityEntityMap.get(toEntity.getUri()).size() > 1) {
					log.debug("citedData was linked to more than one dataset - ignoring: " + toEntity.getName() + " -> " + entityEntityMap.get(toEntity.getUri()));
					processedLinks.add(link);
					flattenedLinks.addAll(getFlattenedLinksForEntity(entityEntityMap, entitiesLinkMap, startEntityUri, toEntities, processedLinks));
				}
				else {*//*
					for (String toEntityUri : entityEntityMap.get(toEntity.getUri())) {
						toEntities.putAll(toEntityUri, entitiesLinkMap.get(toEntity.getUri() + toEntityUri));
					}
					processedLinks.add(link);
					flattenedLinks.addAll(getGwsLinksForEntity(entityEntityMap,
							entitiesLinkMap, startEntityUri,
							toEntities,
							processedLinks));
				//}
			}
		}
		return flattenedLinks;
	}*/
	private List<EntityLink> getFlattenedLinksForEntity(
			Multimap<String, String> entityEntityMap, 
			Multimap<String, String> entitiesLinkMap,
			String startEntityUri, Multimap<String, String> toEntities, 
			List<EntityLink> processedLinks) {
		List<EntityLink> flattenedLinks = new ArrayList<>();
		for (Map.Entry<String, String> entry : toEntities.entries()) {
			Entity toEntity = getInputDataStoreClient().get(Entity.class, entry.getKey().replaceAll("http://.*/entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
			EntityLink link = getInputDataStoreClient().get(EntityLink.class, entry.getValue().replaceAll("http://.*/entityLink", "http://svkolodtest.gesis.intra/link-db/api/entityLink"));
			
			toEntities = ArrayListMultimap.create();
			for (String toEntityUri : entityEntityMap.get(toEntity.getUri())) {
				toEntities.putAll(toEntityUri, entitiesLinkMap.get(toEntity.getUri() + toEntityUri));
			}
			if ((!toEntity.getEntityType().equals(EntityType.citedData)) || toEntities.isEmpty()) {

				EntityLink directLink = new EntityLink();
				directLink.setFromEntity(startEntityUri);
				directLink.setToEntity(link.getToEntity());
				directLink.setEntityRelations(link.getEntityRelations());
				// set cited data as link view
				if (!toEntity.getEntityType().equals(EntityType.citedData)) {
					Entity fromEntity = getInputDataStoreClient().get(Entity.class, link.getFromEntity().replaceAll("http://.*/entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
					StringJoiner linkView = new StringJoiner(" ");
					//TODO check
					if (fromEntity.getEntityType().equals(EntityType.citedData)) {
						// hack for infolis links
						if (null == fromEntity.getEntityView() || fromEntity.getEntityView().isEmpty()) {
							linkView.add(fromEntity.getName());
							for (String number : fromEntity.getNumericInfo()) linkView.add(number);
							fromEntity.setEntityView(linkView.toString());
							getOutputDataStoreClient().put(Entity.class, fromEntity, fromEntity.getUri());
						}

					//	linkView.add(fromEntity.getEntityView());
						//if (null == directLink.getProvenance() || directLink.getProvenance().isEmpty() || directLink.getProvenance().equals("InfoLink")) {
						//if (null != fromEntity.getNumericInfo() && !fromEntity.getNumericInfo().isEmpty()) linkView.add(fromEntity.getNumericInfo().get(0));
					//	}
					}
					directLink.setLinkView(fromEntity.getEntityView());
				} else {
					StringJoiner linkView = new StringJoiner(" ");

					//TODO set link view for infolisLinks
					//if (null == directLink.getProvenance() || directLink.getProvenance().isEmpty() || directLink.getProvenance().equals("InfoLink")) {
					// hack for infolis links
					if (null == toEntity.getEntityView() || toEntity.getEntityView().isEmpty()) {
						linkView.add(toEntity.getName());
						if (null != toEntity.getNumericInfo() && !toEntity.getNumericInfo().isEmpty()) {
							for (String number : toEntity.getNumericInfo()) linkView.add(number);
						}
						toEntity.setEntityView(linkView.toString());
						getOutputDataStoreClient().put(Entity.class, toEntity, toEntity.getUri());
					} 
					//linkView.add(toEntity.getEntityView());
					directLink.setLinkView(toEntity.getEntityView());
				}
				directLink.setTags(link.getTags());
				
				int intermediateLinks = processedLinks.size();
				String linkReason = null;
				if (null != link.getLinkReason() && !link.getLinkReason().isEmpty()) linkReason = link.getLinkReason().replaceAll("http://.*/textualReference", "http://svkolodtest.gesis.intra/link-db/api/textualReference");
				double confidenceSum = 0;
				Set<String> provenance = new HashSet<>();
				for (EntityLink intermediateLink : processedLinks) {
					confidenceSum += intermediateLink.getConfidence();
					if (null != intermediateLink.getLinkReason()) {
						//TextualReference ref = getInputDataStoreClient().get(TextualReference.class, intermediateLink.getLinkReason().replaceAll("http://.*/textualReference", "http://svkolodtest.gesis.intra/link-db/api/textualReference"));
						//TODO REPLACE TOKENS LIKE -RRB-
						//linkReason = ref.toPrettyString();	
						linkReason = intermediateLink.getLinkReason().replaceAll("http://.*/textualReference", "http://svkolodtest.gesis.intra/link-db/api/textualReference");
					}
					directLink.addAllTags(intermediateLink.getTags());
					for (EntityRelation relation : intermediateLink.getEntityRelations()) {
						if (!relation.equals(EntityRelation.same_as)) directLink.addEntityRelation(relation);
					}
					// provenance entries of intermediate links do not have to be equal - e.g. manually specified cited data may have been linked to datasets automatically
					if (null != intermediateLink.getProvenance() && !intermediateLink.getProvenance().isEmpty()) provenance.add(intermediateLink.getProvenance());
				}
				confidenceSum += link.getConfidence();
				intermediateLinks += 1;
				directLink.setConfidence(confidenceSum / intermediateLinks);
				directLink.setLinkReason(linkReason);
				log.debug("reference: " + linkReason);
						
				if (null != link.getProvenance() && !link.getProvenance().isEmpty()) provenance.add(link.getProvenance());
				if (null == provenance || provenance.isEmpty()) directLink.setProvenance("InfoLink");
				else directLink.setProvenance(String.join(" + ", provenance));
						
				directLink.addAllTags(getExecution().getTags());
				log.debug("flattenedLink: " + SerializationUtils.toJSON(directLink));
				flattenedLinks.add(directLink);

			} else {
				processedLinks.add(link);
				flattenedLinks.addAll(getFlattenedLinksForEntity(entityEntityMap,
					entitiesLinkMap, startEntityUri,
					toEntities,
					processedLinks));
			}
		}
		return flattenedLinks;
	}
	
	private List<EntityLink> flattenLinks(List<EntityLink> links) {
		List<EntityLink> flattenedLinks = new ArrayList<>();
		Multimap<String, String> entityEntityMap = ArrayListMultimap.create();
		Multimap<String, String> entitiesLinkMap = ArrayListMultimap.create();
		for (EntityLink link : links) {
			//TODO refactor
			//treat only infolis links here
			//if (!(null == link.getProvenance() || link.getProvenance().isEmpty())) continue;
			//if (link.getProvenance().equals("dbk")) continue;
			//log.debug("found infolis link: " + link.toString());
			Entity fromEntity = getInputDataStoreClient().get(Entity.class, link.getFromEntity().replaceAll("http://.*/entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
			if (fromEntity.getTags().contains("infolis-ontology")) continue;
			entityEntityMap.put(fromEntity.getUri(), link.getToEntity());
			entitiesLinkMap.put(fromEntity.getUri()+link.getToEntity(), link.getUri());
		}
		
		for (String entityUri : entityEntityMap.keySet()) {
			Entity fromEntity = getInputDataStoreClient().get(Entity.class, entityUri.replaceAll("http://.*/entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
			Multimap<String, String> linkedEntities = ArrayListMultimap.create();
			if (fromEntity.getEntityType().equals(EntityType.citedData)) continue;
			for (String toEntityUri : entityEntityMap.get(entityUri)) {
				linkedEntities.putAll(toEntityUri, entitiesLinkMap.get(entityUri + toEntityUri));
			}
			
			flattenedLinks.addAll(getFlattenedLinksForEntity(entityEntityMap, entitiesLinkMap, entityUri, linkedEntities, new ArrayList<>()));
			//flattenedLinks.addAll(getGwsLinksForEntity(entityEntityMap, entitiesLinkMap, entityUri, linkedEntities, new ArrayList<>()));
		}
		return flattenedLinks;
	}
	
	private void put(HttpClient httpclient, HttpPut httpput, StringEntity data) throws ClientProtocolException, IOException {
		httpput.setEntity(data);
		//httpput.setHeader("content-type", "application/json;charset=UTF-8");
		httpput.setHeader("content-type", ContentType.APPLICATION_JSON.toString());
		//httpput.setHeader("Accept", "application/json");
		httpput.setHeader("Accept", ContentType.APPLICATION_JSON.toString());

		HttpResponse response = httpclient.execute(httpput);
		HttpEntity entity = response.getEntity();
			
		if (entity != null) {
		    InputStream instream = entity.getContent();
		    try {
		        log.debug(IOUtils.toString(instream));
		    } finally {
		        instream.close();
		    }
		}
	}
	
	private void post(HttpClient httpclient, HttpPost httppost, StringEntity data) throws ClientProtocolException, IOException {
		httppost.setEntity(data);
		//httppost.setHeader("content-type", "application/json;charset=UTF-8");
		httppost.setHeader("content-type", ContentType.APPLICATION_JSON.toString());
		//httppost.setHeader("Accept", "application/json");
		httppost.setHeader("Accept", ContentType.APPLICATION_JSON.toString());

		HttpResponse response = httpclient.execute(httppost);
		HttpEntity entity = response.getEntity();
			
		if (entity != null) {
		    InputStream instream = entity.getContent();
		    try {
		        log.debug(IOUtils.toString(instream));
		    } finally {
		        instream.close();
		    }
		}
	}
	
	private void pushToIndex(List<EntityLink> flattenedLinks, String index) throws ClientProtocolException, IOException {
		Set<Entity> entities = new HashSet<>();	
		String prefixRegex = "http://.*/entity/";
		Pattern prefixPattern = Pattern.compile(prefixRegex);
	 	// assume all entities have the same prefix
		String entityPrefix = "";	
		if (null == index || index.isEmpty()) index = InfolisConfig.getElasticSearchIndex();
		String newPrefix = index + "Entity/";
		HttpClient httpclient = HttpClients.createDefault();
		
		for (EntityLink link : flattenedLinks) {
			//Matcher m = prefixPattern.matcher(link.getFromEntity());
			//if (m.find()) entityPrefix = m.group();
			Entity fromEntity = getInputDataStoreClient().get(Entity.class, link.getFromEntity().replaceAll("http://.*/entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
			Entity toEntity = getInputDataStoreClient().get(Entity.class, link.getToEntity().replaceAll("http://.*/entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
			// link to za numbers / elastic search entities
			log.debug(toEntity.getName());
			log.debug(toEntity.getEntityView());
			//quick hack for current ssoar infolink data; remove later
			if (toEntity.getEntityType().equals(EntityType.dataset)) {
				log.debug(toEntity.getIdentifiers().get(0));
				toEntity = getDataSearchEntity(toEntity.getIdentifiers().get(0));
				//log.debug(toEntity.getIdentifiers().get(0));
				log.debug(SerializationUtils.toJSON(toEntity).toString());
				//System.exit(0);
				//toEntity.setGwsId("doi:" + toEntity.getIdentifiers().get(0).replace("/", "-"));
				// ignore link if dataset wasn't found in dbk or elasticsearch
				if (null == toEntity) continue;
			}

			for (String pubId : fromEntity.getIdentifiers())  {
				if (pubId.startsWith("urn:")) {
					fromEntity.setGwsId(pubId);
					break;
				}
			}
			if (null != fromEntity.getYear() && !fromEntity.getYear().isEmpty()) {;}
			else if (null!= fromEntity.getNumericInfo() && !fromEntity.getNumericInfo().isEmpty()) fromEntity.setYear(fromEntity.getNumericInfo().get(0));
			else fromEntity.setYear("o.J.");
			
			if (null!= toEntity.getNumericInfo() && !toEntity.getNumericInfo().isEmpty()) toEntity.setYear(toEntity.getNumericInfo().get(0));
			else fromEntity.setYear("o.J.");
			
			StringJoiner fromEntityAuthor = new StringJoiner("; ");
			if (null != fromEntity.getAuthors()) for (String author : fromEntity.getAuthors()) fromEntityAuthor.add(author);
			StringJoiner toEntityAuthor = new StringJoiner("; ");
			//if (null != toEntity.getAuthors()) for (String author : toEntity.getAuthors()) toEntityAuthor.add(author);
			fromEntity.setEntityView(String.format("%s (%s): %s", fromEntityAuthor.toString(), fromEntity.getYear(), fromEntity.getName()));
			//toEntity.setEntityView(String.format("%s", toEntity.getName()));
			//end of hack
			//post only links when ids of both entities are known
			//if ((null == toEntity.getGwsId()) || (null == fromEntity.getGwsId())) continue;
			if (null != fromEntity.getGwsId()) fromEntity.setUri(fromEntity.getGwsId());
			else fromEntity.setUri(fromEntity.getUri().replaceAll("http.*/entity/","literaturpool-"));
			if (null != toEntity.getGwsId()) toEntity.setUri(toEntity.getGwsId());
			else toEntity.setUri(toEntity.getUri().replaceAll("http.*/entity/","literaturpool-"));

			link.setFromEntity(fromEntity.getUri());
			link.setToEntity(toEntity.getUri());

			DbIndexer indexer = new DbIndexer(getInputDataStoreClient(), getOutputDataStoreClient(), getInputFileResolver(), getOutputFileResolver());
			ElasticLink elink = indexer.new ElasticLink(link);
			//elink.setGws_fromID(elink.getFromEntity());
			//elink.setGws_toID(elink.getToEntity());
			elink.setGws_fromView(fromEntity.getEntityView());
			elink.setGws_toView(toEntity.getEntityView());
			elink.setGws_fromType(fromEntity.getEntityType());
			elink.setGws_toType(toEntity.getEntityType());
			//TODO refactor
			//elink.setProvenance("InfoLink");
			// set URI in a way that duplicates are overriden
			//TODO do not overwrite links; instead:search if links with such a uri already exist. if so: merge linkReasons and confidence scores, pssibly other fields as well?
			elink.setUri(elink.getGws_fromID() + "---" + elink.getGws_toID());
			if (toEntity.getEntityType().equals(EntityType.citedData)) elink.setGws_link(elink.getGwsLink(toEntity.getName().replaceAll("\\d", "").trim()));
			if (null != elink.getUri()) {
				HttpPut httpput = new HttpPut(index + "EntityLink/" + elink.getUri().replaceAll("http://.*/entityLink/", ""));
				put(httpclient, httpput, new StringEntity(SerializationUtils.toJSON(elink), ContentType.APPLICATION_JSON));
				log.debug(String.format("put link \"%s\" to %s", elink, index));
			}
			// flattened links are not pushed to any datastore and thus have no uri
			else {
				HttpPost httppost = new HttpPost(index + "EntityLink/");
				post(httpclient, httppost, new StringEntity(SerializationUtils.toJSON(elink), ContentType.APPLICATION_JSON));
				log.debug(String.format("posted link \"%s\" to %s", elink, index));
			}
				entities.add(fromEntity);
				entities.add(toEntity);
		}

		for (Entity entity : entities) {
			HttpPut httpput = new HttpPut(index + "Entity/" + entity.getUri());
			//Entity e = getInputDataStoreClient().get(Entity.class, entity.replaceAll(newPrefix, entityPrefix));
			//workaround for links with incorrect prefix
			//Entity e = getInputDataStoreClient().get(Entity.class, entity.replaceAll(newPrefix, "http://svkolodtest.gesis.intra/link-db/api/entity/"));
			//e.setUri(e.getGwsId());
			put(httpclient, httpput, new StringEntity(SerializationUtils.toJSON(entity), ContentType.APPLICATION_JSON));
			log.debug(String.format("put entity \"%s\" to %s", entity.getUri(), index));
		}
		
	}
	
	private List<String> getAllLinksInDatabase() {
		List<String> links = new ArrayList<>();
		Multimap<String, String> query = ArrayListMultimap.create();
		//TODO refactor...
		//query.put("provenance", "");
		for (EntityLink link : getInputDataStoreClient().search(EntityLink.class, query)) {
			//hack for current infolislinks
			if (null == link.getProvenance() && !link.getTags().contains("infolis-ontology")) links.add(link.getUri());
			//if (!link.getTags().contains("infolis-ontology")) links.add(link.getUri());
		}
		return links;
	}
	

	@Override
	public void execute() throws IOException {
		//either use specified set of links or use on all links in the database
		if (null == getExecution().getLinks() || getExecution().getLinks().isEmpty()) {
			debug(log, "list of input links is empty, indexing all links in the database");
			getExecution().setLinks(getAllLinksInDatabase());
		}
		pushToIndex(flattenLinks(getInputDataStoreClient().get(EntityLink.class, getExecution().getLinks())), getExecution().getIndexDirectory());
		
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub
		
	}
	
}
