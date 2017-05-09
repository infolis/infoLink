package io.github.infolis.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

		public ElasticLink(EntityLink copyFrom) {
			this.setFromEntity(copyFrom.getFromEntity());
			this.setToEntity(copyFrom.getToEntity());
			this.setGws_fromID(copyFrom.getFromEntity());
			this.setGws_toID(copyFrom.getToEntity());
			this.setConfidence(copyFrom.getConfidence());
			this.setLinkReason(copyFrom.getLinkReason());
			this.setEntityRelations(copyFrom.getEntityRelations());
			this.setProvenance(copyFrom.getProvenance());
			this.setLinkView(copyFrom.getLinkView());
			this.setFromEntity(copyFrom.getFromEntity());
			this.setToEntity(copyFrom.getToEntity());
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
	
	}	
	
	private void pushToIndex() throws ClientProtocolException, IOException {
		String index = InfolisConfig.getElasticSearchIndex();
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
			
			link.setFromEntity(fromEntity.getUri());
			link.setToEntity(toEntity.getUri());

			ElasticLink elink = new ElasticLink(link);
			elink.setGws_fromType(fromEntity.getEntityType());
			elink.setGws_toType(toEntity.getEntityType());
			elink.setGws_fromView(fromEntity.getEntityView());
			elink.setGws_toView(toEntity.getEntityView());
			elink.setUri(fromEntity.getUri() + "---" + toEntity.getUri());
			HttpPut httpput = new HttpPut(index + "EntityLink/" + elink.getUri());
			//log.debug(SerializationUtils.toJSON(elink).toString());
			put(httpclient, httpput, new StringEntity(SerializationUtils.toJSON(elink), ContentType.APPLICATION_JSON));
			//post(httpclient, httpost, new StringEntity(elink.toJson(), ContentType.APPLICATION_JSON));
			//log.debug(String.format("posted link \"%s\" to %s", link, index));
			entities.add(fromEntity);
			entities.add(toEntity);
		}

		//for (String entity : getExecution().getLinkedEntities()) {
		//	Entity e = getInputDataStoreClient().get(Entity.class, entity);
		for (Entity e : entities) {
			//e.setUri(e.getGwsId());
			HttpPut httpput = new HttpPut(index + "Entity/" + e.getUri());
			put(httpclient, httpput, new StringEntity(SerializationUtils.toJSON(e), ContentType.APPLICATION_JSON));
			if (null == e.getUri()) log.debug(String.format("put entity \"%s\" to %s", SerializationUtils.toJSON(e), index));
		}
		
	}
	
	private void getData() {
		Multimap<String, String> query = ArrayListMultimap.create();
		for (EntityLink link : getInputDataStoreClient().search(EntityLink.class, query)) {
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
		getData();		
		pushToIndex();
		
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub
		
	}
	
}
