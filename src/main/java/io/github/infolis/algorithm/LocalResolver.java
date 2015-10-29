package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Algorithm to locally find all the entities to which a link exists.
 * For example, given an URN, find all the entities like studies 
 * that are linked to this URN.
 * 
 * @author domi
 */
public class LocalResolver extends BaseAlgorithm {

    public LocalResolver(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }
    

    @Override
    public void execute() throws IOException {
        String textRefURI = getExecution().getTextualReferences().get(0);
        TextualReference textRef = getInputDataStoreClient().get(TextualReference.class, textRefURI);
        
        //query the internal data to find the entity with this identifier
        Map<String, String> query = new HashMap<>();
        String identifier = textRef.getTerm();
        //TODO: correct propertyURI
        String propertyURI = "identifier";
        query.put(propertyURI, identifier);
                
        //TODO: not implemented yet
        //List<Entity> results =getInputDataStoreClient().search(Entity.class, query);
        List<Entity> results = new ArrayList<>();
         //TODO: correct URI
        String fromEntity = "fromEntity";
        query = new HashMap<>();   
        //try to find the entities in the links (as fromEntities)
        for(Entity e : results) {
            query.put(fromEntity, e.getUri());            
        }
        //TODO: not implemented yet
        //List<EntityLink> links = getInputDataStoreClient().search(EntityLink.class, query);
        List<EntityLink> links= new ArrayList<>();;
        
        //dereference the toEntities
        List<String> toEntities = new ArrayList<>();
        for(EntityLink link : links) {
            toEntities.add(link.getToEntity().getIdentifier());
        }
        //set the detected entities in the execution
        getExecution().setLinkedEntities(toEntities);
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        if (null == getExecution().getTextualReferences() || getExecution().getTextualReferences().isEmpty()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "textualReference", "Required parameter 'textual reference' is missing!");
        }
    }
    
    
    
}
