package io.github.infolis.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisConfig;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
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
		// TODO flatten links
		//return flattenedLinks;
		return links;
	}
	
	private void put(HttpClient httpclient, HttpPut httput, StringEntity data) throws ClientProtocolException, IOException {
		httput.setEntity(data);
		httput.setHeader("content-type", "application/json");
		httput.setHeader("Accept", "application/json");

		HttpResponse response = httpclient.execute(httput);
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
			HttpPut httpput = new HttpPut(index + "enityLink/" + link.getUri());
			put(httpclient, httpput, new StringEntity(SerializationUtils.toJSON(link).toString()));
			log.debug(String.format("put link \"%s\" to %s", link, index));
			
			entities.add(link.getFromEntity());
			entities.add(link.getToEntity());
		}

		for (String entity : entities) {
			HttpPut httpput = new HttpPut(index + "enity/" + entity);
			put(httpclient, httpput, new StringEntity(SerializationUtils.toJSON(getInputDataStoreClient().get(Entity.class, entity)).toString()));
			log.debug(String.format("posted link \"%s\" to %s", entity, index));
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