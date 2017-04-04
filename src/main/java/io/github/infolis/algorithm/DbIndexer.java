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
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import io.github.infolis.InfolisConfig;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.entity.Entity;
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
	
	private void pushToIndex() throws ClientProtocolException, IOException {
		String index = InfolisConfig.getElasticSearchIndex();
		HttpClient httpclient = HttpClients.createDefault();
		List<Entity> entities = new ArrayList<>();

		for (EntityLink link : getInputDataStoreClient().get(EntityLink.class, getExecution().getLinks())) {
			Entity fromEntity = getInputDataStoreClient().get(Entity.class, link.getFromEntity());
			Entity toEntity = getInputDataStoreClient().get(Entity.class, link.getToEntity());
			link.setFromEntity(fromEntity.getGwsId());
			link.setToEntity(toEntity.getGwsId());
			HttpPut httpput = new HttpPut(index + "EntityLink/" + link.getUri().replaceAll("http://.*/entityLink/", ""));
			put(httpclient, httpput, new StringEntity(SerializationUtils.toJSON(link).toString()));
			log.debug(String.format("put link \"%s\" to %s", link, index));
			entities.add(fromEntity);
			entities.add(toEntity);
		}

		//for (String entity : getExecution().getLinkedEntities()) {
		//	Entity e = getInputDataStoreClient().get(Entity.class, entity);
		for (Entity e : entities) {
			e.setUri(e.getGwsId());
			HttpPut httpput = new HttpPut(index + "Entity/" + e.getUri());
			put(httpclient, httpput, new StringEntity(SerializationUtils.toJSON(e).toString()));
			log.debug(String.format("put entity \"%s\" to %s", e, index));
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
