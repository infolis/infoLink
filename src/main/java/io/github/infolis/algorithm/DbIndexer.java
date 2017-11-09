package io.github.infolis.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
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
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.EntityType;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.TextualReference;
import io.github.infolis.util.SerializationUtils;

public class DbIndexer extends BaseAlgorithm {

	private static final Logger log = LoggerFactory.getLogger(DbIndexer.class);
	
	public DbIndexer(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
			FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private void put(HttpClient httpclient, HttpPut httpput, StringEntity data) throws ClientProtocolException, IOException {
		httpput.setEntity(data);
		httpput.setHeader("content-type", ContentType.APPLICATION_JSON.toString());
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
		httppost.setHeader("content-type", ContentType.APPLICATION_JSON.toString());
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

	public class ElasticLink extends EntityLink {
		private EntityType gws_fromType;
		private EntityType gws_toType;
		private String gws_fromView;
		private String gws_toView;
		private String gws_fromID;
		private String gws_toID;
		private String gws_link;

		private Map<String, String> getDataUrls() {
			Map<String, String> data_urls = new HashMap<>();
			try {
				String content = FileUtils.readFileToString(new File("/infolis-files/data-urls.csv"));
				for (String line : content.trim().split("\n")) {
					String[] keyValue = line.split(";");
					data_urls.put(keyValue[0].trim(), keyValue[1].trim());
				}
			} catch (IOException e) {
				log.error(e.toString());
			}
			return data_urls;
		}

		protected String getGwsLink(String citedData) {
			Map<String, String> data_urls = getDataUrls();
			String dataLink = data_urls.get(citedData);
			if (null != dataLink) {
				log.debug("FOUND DATA LINK FOR " + citedData + ": " + dataLink);
				return dataLink;
			} else {
				log.debug("NO DATA LINK FOR " + citedData);
				return null;
			}
		}

		public ElasticLink() {}

		public ElasticLink(EntityLink copyFrom) {
			this.setFromEntity(copyFrom.getFromEntity());
			this.setToEntity(copyFrom.getToEntity());
			this.setGws_fromID(copyFrom.getFromEntity());
			this.setGws_toID(copyFrom.getToEntity());
			this.setConfidence(copyFrom.getConfidence());
			String linkReason = copyFrom.getLinkReason();
			if (null != linkReason && !linkReason.isEmpty()) this.setLinkReason(removePtb3escaping(resolveLinkReason(linkReason)));
			this.setEntityRelations(copyFrom.getEntityRelations());
			this.setProvenance(copyFrom.getProvenance());
			this.setLinkView(copyFrom.getLinkView());
			this.setGws_link(null);
		}

		private String removePtb3escaping(String string) {
			return string.replaceAll("-LRB-", "(").replaceAll("-RRB-", ")");
		}

		private String resolveLinkReason(String uri) {
			return getInputDataStoreClient().get(TextualReference.class, uri).toPrettyString();
		}

		public void setGws_fromID(String gws_fromID) {
			this.gws_fromID = gws_fromID;
		}

		public String getGws_fromID() {
			return this.gws_fromID;
		}

		public String getGws_toID() {
			return this.gws_toID;
		}

		public void setGws_toID(String gws_toID) {
			this.gws_toID = gws_toID;
		}

		public void setGws_fromType(EntityType gws_fromType) {
			this.gws_fromType = gws_fromType;
		}

		public EntityType getGws_fromType() {
			return this.gws_fromType;
		}

		public void setGws_toType(EntityType gws_toType) {
			this.gws_toType = gws_toType;
		}

		public EntityType getGws_toType() {
			return this.gws_toType;
		}

		public void setGws_fromView(String gws_fromView) {
			this.gws_fromView = gws_fromView;
		}

		public String getGws_fromView() {
			return this.gws_fromView;
		}
		
		public void setGws_toView(String gws_toView) {
			this.gws_toView = gws_toView;
		}

		public String getGws_toView() {
			return this.gws_toView;
		}

		public void setGws_link(String link) {
			this.gws_link = link;
		}

		public String getGws_link() {
			return this.gws_link;
		}
	
	}	

	private boolean showInGws(Entity fromEntity, Entity toEntity) {
		if (basicPublicationMetadataExists(fromEntity) && basicPublicationMetadataExists(toEntity)) return true;
		return false;
	}

	private boolean basicPublicationMetadataExists(Entity entity) {
		// apply filter on publications only
		if (entity.getEntityType() != EntityType.publication) return true;
		// ignore all entities where not even the basic metadata (title, author, year) is known
		if ((null == entity.getGwsId() || entity.getGwsId().isEmpty()) && (null == entity.getName() || entity.getName().isEmpty() || null == entity.getAuthors() || entity.getAuthors().isEmpty() || null == entity.getYear() || entity.getYear().isEmpty())) return false;
		return true;
	}

	
	private void pushToIndex(String index) throws ClientProtocolException, IOException {
		if (null == index || index.isEmpty()) index = InfolisConfig.getElasticSearchIndex();
		HttpClient httpclient = HttpClients.createDefault();
		List<Entity> entities = new ArrayList<>();
		
		for (EntityLink link : getInputDataStoreClient().get(EntityLink.class, getExecution().getLinks())) {
			Entity fromEntity = getInputDataStoreClient().get(Entity.class, link.getFromEntity().replaceAll("http.*/entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
			Entity toEntity = getInputDataStoreClient().get(Entity.class, link.getToEntity().replaceAll("http.*/entity", "http://svkolodtest.gesis.intra/link-db/api/entity"));
			// do not post entities or their links having no gwsId
			//if ((null == fromEntity.getGwsId()) || (null == toEntity.getGwsId())) continue;
			// TODO check if entityType == entityType.citedData and don't add prefix "literaturpool-" in this case if that prefix should be applied to publications only
			if (null != fromEntity.getGwsId()) fromEntity.setUri(fromEntity.getGwsId());
			else fromEntity.setUri(fromEntity.getUri().replaceAll("http.*/entity/", "literaturpool-"));
			if (null != toEntity.getGwsId()) toEntity.setUri(toEntity.getGwsId());
			else toEntity.setUri(toEntity.getUri().replaceAll("http.*/entity/", "literaturpool-"));
		
			// always post entities; links only when to be shown in gws	
			if (showInGws(fromEntity, toEntity)) {
				link.setFromEntity(fromEntity.getUri());
				link.setToEntity(toEntity.getUri());

				ElasticLink elink = new ElasticLink(link);
				elink.setGws_fromType(fromEntity.getEntityType());
				elink.setGws_toType(toEntity.getEntityType());
				if (EntityType.citedData.equals(toEntity.getEntityType())) {
					log.debug("searching for gwsLink for " + toEntity.getName());
					elink.setGws_link(elink.getGwsLink(toEntity.getName().replaceAll("\\d", "").trim()));
					//elink.setGws_link(elink.getGwsLink(toEntity.getName().trim()));
					//log.debug(elink.getGws_link());
				} else if (EntityType.citedData.equals(fromEntity.getEntityType())) {
					log.debug("Searching for gwsLink for " + fromEntity.getName());
					elink.setGws_link(elink.getGwsLink(fromEntity.getName().replaceAll("\\d", "").trim()));
					//elink.setGws_link(elink.getGwsLink(fromEntity.getName().trim()));
				}
				elink.setGws_fromView(fromEntity.getEntityView());
				elink.setGws_toView(toEntity.getEntityView());
				elink.setUri(fromEntity.getUri() + "---" + toEntity.getUri());
				HttpPut httpput = new HttpPut(index + "EntityLink/" + elink.getUri());
				log.debug(SerializationUtils.toJSON(elink).toString());
				put(httpclient, httpput, new StringEntity(SerializationUtils.toJSON(elink), ContentType.APPLICATION_JSON));
				//post(httpclient, httpost, new StringEntity(elink.toJson(), ContentType.APPLICATION_JSON));
				//log.debug(String.format("posted link \"%s\" to %s", link, index));
				//if (elink.getGws_link() != null) throw new RuntimeException();
			}

			entities.add(fromEntity);
			entities.add(toEntity);
		}

		//for (String entity : getExecution().getLinkedEntities()) {
		//	Entity e = getInputDataStoreClient().get(Entity.class, entity);
		for (Entity e : entities) {
			if (null == e.getGwsId() || e.getGwsId().isEmpty()) e.setGwsId(e.getUri());
			//log.debug("putting: " + SerializationUtils.toJSON(e));
			HttpPut httpput = new HttpPut(index + "Entity/" + e.getUri());
			put(httpclient, httpput, new StringEntity(SerializationUtils.toJSON(e), ContentType.APPLICATION_JSON));
			if (null == e.getUri()) log.warn(String.format("uri is null: cannot put entity \"%s\" to %s", SerializationUtils.toJSON(e), index));
		}
		
	}
	
	private void getData(List<String> ignoreLinksWithProvenance) {
		Multimap<String, String> query = ArrayListMultimap.create();
		for (EntityLink link : getInputDataStoreClient().search(EntityLink.class, query)) {
			if (null != ignoreLinksWithProvenance && !ignoreLinksWithProvenance.isEmpty()) {
				for (String provenance : ignoreLinksWithProvenance) {
					if (link.getProvenance().equals(provenance)) continue;
				}
			}
			getExecution().getLinks().add(link.getUri());
		}
		/*for (Entity entity : getInputDataStoreClient().search(Entity.class, query)) {
			getExecution().getLinkedEntities().add(entity.getUri());
		}*/
	}
	

	@Override
	public void execute() throws IOException {
		getExecution().setLinks(new ArrayList<>());
		getExecution().setLinkedEntities(new ArrayList<>());
		getData(getExecution().getSeeds());		
		pushToIndex(getExecution().getIndexDirectory());
		
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub
		
	}
	
}
