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
	
	private List<EntityLink> getFlattenedLinksForEntity(
			Multimap<String, String> entityEntityMap, 
			Multimap<String, String> entitiesLinkMap,
			String startEntityUri, Multimap<String, String> toEntities, 
			List<EntityLink> processedLinks) {
		List<EntityLink> flattenedLinks = new ArrayList<>();
		for (Map.Entry<String, String> entry : toEntities.entries()) {
			Entity toEntity = getInputDataStoreClient().get(Entity.class, entry.getKey().replaceAll("http://.*/entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
			EntityLink link = getInputDataStoreClient().get(EntityLink.class, entry.getValue().replaceAll("http://.*/entityLink", "http://svkolodtest.gesis.intra/link-db/api/entityLink"));
			
			if (!toEntity.getEntityType().equals(EntityType.citedData)) {

				EntityLink directLink = new EntityLink();
				directLink.setFromEntity(startEntityUri);
				directLink.setToEntity(link.getToEntity());
				directLink.setEntityRelations(link.getEntityRelations());
				// set cited data as link view
				Entity fromEntity = getInputDataStoreClient().get(Entity.class, link.getFromEntity().replaceAll("http://.*/entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
				StringJoiner linkView = new StringJoiner(" ");
				linkView.add(fromEntity.getName());
				for (String number : fromEntity.getNumericInfo()) linkView.add(number);
				directLink.setLinkView(linkView.toString());
				directLink.setTags(link.getTags());
				
				int intermediateLinks = processedLinks.size();
				String linkReason = null;
				double confidenceSum = 0;
				StringJoiner provenance = new StringJoiner(" + ");
				for (EntityLink intermediateLink : processedLinks) {
					confidenceSum += intermediateLink.getConfidence();
					if (null != intermediateLink.getLinkReason()) {
						TextualReference ref = getInputDataStoreClient().get(TextualReference.class, intermediateLink.getLinkReason().replaceAll("http://.*/textualReference", "http://svkolodtest.gesis.intra/link-db/api/textualReference"));
						linkReason = ref.toPrettyString();
					}
					directLink.addAllTags(intermediateLink.getTags());
					for (EntityRelation relation : intermediateLink.getEntityRelations()) {
						if (!relation.equals(EntityRelation.same_as)) directLink.addEntityRelation(relation);
					}
					// provenance entries of intermediate links do not have to be equal - e.g. manually specified cited data may have been linked to datasets automatically
					if (null != intermediateLink.getProvenance()) provenance.add(intermediateLink.getProvenance());
				}
				confidenceSum += link.getConfidence();
				intermediateLinks += 1;
				directLink.setConfidence(confidenceSum / intermediateLinks);
				directLink.setLinkReason(linkReason);
				log.debug("reference: " + linkReason);
						
				provenance.add(link.getProvenance());
				directLink.setProvenance(provenance.toString());
						
				directLink.addAllTags(getExecution().getTags());
				log.debug("flattenedLink: " + SerializationUtils.toJSON(directLink));
				flattenedLinks.add(directLink);

			} else {
				toEntities = ArrayListMultimap.create();
				for (String toEntityUri : entityEntityMap.get(toEntity.getUri())) {
					toEntities.putAll(toEntityUri, entitiesLinkMap.get(toEntity.getUri() + toEntityUri));
				}
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
	
	private void pushToIndex(List<EntityLink> flattenedLinks) throws ClientProtocolException, IOException {
		Set<Entity> entities = new HashSet<>();	
		String prefixRegex = "http://.*/entity/";
		Pattern prefixPattern = Pattern.compile(prefixRegex);
	 	// assume all entities have the same prefix
		String entityPrefix = "";	
		String index = InfolisConfig.getElasticSearchIndex();
		String newPrefix = index + "Entity/";
		HttpClient httpclient = HttpClients.createDefault();
		
		for (EntityLink link : flattenedLinks) {
			//Matcher m = prefixPattern.matcher(link.getFromEntity());
			//if (m.find()) entityPrefix = m.group();
			Entity fromEntity = getInputDataStoreClient().get(Entity.class, link.getFromEntity().replaceAll("http://.*/entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
			Entity toEntity = getInputDataStoreClient().get(Entity.class, link.getToEntity().replaceAll("http://.*/entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
			//quick hack for current ssoar infolink data; remove later
			
			/*toEntity.setGwsId("doi:" + toEntity.getIdentifiers().get(0).replace("/", "-"));
			for (String pubId : fromEntity.getIdentifiers())  {
				if (pubId.startsWith("urn:")) {
					fromEntity.setGwsId(pubId);
					break;
				}
			}
			if (null!= fromEntity.getNumericInfo() && !fromEntity.getNumericInfo().isEmpty()) fromEntity.setYear(fromEntity.getNumericInfo().get(0));
			else fromEntity.setYear("o.J.");
			
			if (null!= toEntity.getNumericInfo() && !toEntity.getNumericInfo().isEmpty()) toEntity.setYear(toEntity.getNumericInfo().get(0));
			else fromEntity.setYear("o.J.");
			
			StringJoiner fromEntityAuthor = new StringJoiner("; ");
			if (null != fromEntity.getAuthors()) for (String author : fromEntity.getAuthors()) fromEntityAuthor.add(author);
			StringJoiner toEntityAuthor = new StringJoiner("; ");
			//if (null != toEntity.getAuthors()) for (String author : toEntity.getAuthors()) toEntityAuthor.add(author);
			fromEntity.setEntityView(String.format("%s (%s): %s", fromEntityAuthor.toString(), fromEntity.getYear(), fromEntity.getName()));
			toEntity.setEntityView(String.format("%s", toEntity.getName()));
			*/
			//post only links when ids of both entities are known
			//if ((null == toEntity.getGwsId()) || (null == fromEntity.getGwsId())) continue;
			if (null != fromEntity.getGwsId()) fromEntity.setUri(fromEntity.getGwsId());
			else fromEntity.setUri(fromEntity.getUri().replaceAll("http.*/entity/",""));
			if (null != toEntity.getGwsId()) toEntity.setUri(toEntity.getGwsId());
			else toEntity.setUri(toEntity.getUri().replaceAll("http.*/entity/",""));

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
			if (null != link.getUri()) {
				HttpPut httpput = new HttpPut(index + "EntityLink/" + link.getUri().replaceAll("http://.*/entityLink/", ""));
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
		for (EntityLink link : getInputDataStoreClient().search(EntityLink.class, query)) {
			links.add(link.getUri());
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
		pushToIndex(flattenLinks(getInputDataStoreClient().get(EntityLink.class, getExecution().getLinks())));
		
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub
		
	}
	
}
