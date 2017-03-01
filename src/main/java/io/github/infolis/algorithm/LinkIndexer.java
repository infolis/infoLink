package io.github.infolis.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import io.github.infolis.InfolisConfig;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.EntityType;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.util.SerializationUtils;

public class LinkIndexer extends BaseAlgorithm {

	private static final Logger log = LoggerFactory.getLogger(LinkIndexer.class);
	
	public LinkIndexer(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
			FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	
	private List<EntityLink> flattenLinks(List<EntityLink> links) {
		List<EntityLink> flattenedLinks = new ArrayList<>();
		Multimap<String, String> entityEntityMap = ArrayListMultimap.create();
		Multimap<String, String> entitiesLinkMap = ArrayListMultimap.create();
		for (EntityLink link : links) {
			Entity fromEntity = getInputDataStoreClient().get(Entity.class, link.getFromEntity());
			if (fromEntity.getTags().contains("infolis-ontology")) continue;
			entityEntityMap.put(fromEntity.getUri(), link.getToEntity());
			entitiesLinkMap.put(fromEntity.getUri()+link.getToEntity(), link.getUri());
		}
		
		for (String entityUri : entityEntityMap.keySet()) {
			Entity fromEntity = getInputDataStoreClient().get(Entity.class, entityUri);
			Multimap<String, String> linkedEntities = ArrayListMultimap.create();
			if (fromEntity.getEntityType().equals(EntityType.citedData)) continue;
			for (String toEntityUri : entityEntityMap.get(entityUri)) {
				linkedEntities.putAll(toEntityUri, entitiesLinkMap.get(entityUri + toEntityUri));
			}
			
			// TODO make recursive, stop at the first non-citedData link
			// keep confidence of previous links and number of links
			// this would automatically treat same-as links properly
			for (Map.Entry<String, String> entry : linkedEntities.entries()) {
				Entity toEntity1 = getInputDataStoreClient().get(Entity.class, entry.getKey());
				EntityLink link1 = getInputDataStoreClient().get(EntityLink.class, entry.getValue());
				
				log.debug("fromEntity1: " + fromEntity.getIdentifiers());
				log.debug("toEntity1: " + toEntity1.getIdentifiers());
				log.debug("link1: " + link1.getUri());
				//Multimap<String, String> linkedEntities2 = ArrayListMultimap.create();
				
				if (!toEntity1.getEntityType().equals(EntityType.citedData)) {
					log.warn("direct link found: " + fromEntity + " -> " + toEntity1);
				}
				
				for (String toEntity2uri : entityEntityMap.get(link1.getToEntity())) {
					log.debug("key: " + toEntity1.getUri() + toEntity2uri);
					log.debug("keys: " + entitiesLinkMap.keySet());
					for (String link2uri : entitiesLinkMap.get(link1.getToEntity() + toEntity2uri)) {
						EntityLink link2 = getInputDataStoreClient().get(EntityLink.class, link2uri);
						log.debug(link2uri);
						EntityLink directLink = new EntityLink();
						directLink.setFromEntity(entityUri);
						directLink.setToEntity(link2.getToEntity());
						directLink.setEntityRelations(link1.getEntityRelations());
						directLink.getEntityRelations().addAll(link2.getEntityRelations());
						directLink.setConfidence((link1.getConfidence() + link2.getConfidence()) / 2);
						String linkReason = null;
						for (String reason : Arrays.asList(link1.getLinkReason(), link2.getLinkReason())) {
							if (null != reason) linkReason = reason;
						}
						directLink.setLinkReason(linkReason);
						if (link1.getProvenance() != link2.getProvenance()) log.warn("link1 and link2 have different provenance info!");
						directLink.setProvenance(link1.getProvenance());
						// TODO view?
						directLink.setTags(link1.getTags());
						directLink.addAllTags(link2.getTags());
						directLink.addAllTags(getExecution().getTags());
						flattenedLinks.add(directLink);
					}
				}
			}
		}
		return flattenedLinks;
	}
	
	private void put(HttpClient httpclient, HttpPut httpput, StringEntity data) throws ClientProtocolException, IOException {
		httpput.setEntity(data);
		httpput.setHeader("content-type", "application/json");
		httpput.setHeader("Accept", "application/json");

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
		httppost.setHeader("content-type", "application/json");
		httppost.setHeader("Accept", "application/json");

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
	
	private void pushToIndex(List<EntityLink> flattenedLinks) throws ClientProtocolException, IOException {
		Set<String> entities = new HashSet<>();
		
		String index = InfolisConfig.getElasticSearchIndex();
		HttpClient httpclient = HttpClients.createDefault();
		
		for (EntityLink link : flattenedLinks) {
			if (null != link.getUri()) {
				HttpPut httpput = new HttpPut(index + "enityLink/" + link.getUri());
				put(httpclient, httpput, new StringEntity(SerializationUtils.toJSON(link).toString()));
				log.debug(String.format("put link \"%s\" to %s", link, index));
			}
			// flattened links are not pushed to any datastore and thus have no uri
			else {
				HttpPost httppost = new HttpPost(index + "enityLink/");
				post(httpclient, httppost, new StringEntity(SerializationUtils.toJSON(link).toString()));
				log.debug(String.format("posted link \"%s\" to %s", link, index));
			}
				entities.add(link.getFromEntity());
				entities.add(link.getToEntity());
		}

		for (String entity : entities) {
			HttpPut httpput = new HttpPut(index + "enity/" + entity);
			put(httpclient, httpput, new StringEntity(SerializationUtils.toJSON(getInputDataStoreClient().get(Entity.class, entity)).toString()));
			log.debug(String.format("put entity \"%s\" to %s", entity, index));
		}
	}
	

	@Override
	public void execute() throws IOException {
		pushToIndex(flattenLinks(getInputDataStoreClient().get(EntityLink.class, getExecution().getLinks())));
		
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub
		
	}
	
}