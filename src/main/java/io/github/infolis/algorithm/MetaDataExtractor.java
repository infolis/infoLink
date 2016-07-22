package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.util.InformationExtractor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author kata
 *
 */
public class MetaDataExtractor extends BaseAlgorithm {

    public MetaDataExtractor(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }
    private static final Logger log = LoggerFactory.getLogger(MetaDataExtractor.class);

    @Override
    public void execute() throws IOException {

        String tr = getExecution().getTextualReferences().get(0);
        TextualReference ref = getInputDataStoreClient().get(TextualReference.class, tr);

        debug(log, "Extracting metadata from textual reference {}", ref);
        Entity entity = extractMetadata(ref);
        
        if ((null == entity.getName() || entity.getName().isEmpty()) &&
        	entity.getNumericInfo().isEmpty() &&
        	(null == entity.getIdentifier() || entity.getIdentifier().isEmpty()) &&
        	(null == entity.getURL() || entity.getURL().isEmpty())) {
        	error(log, "Could not extract metadata for reference {} ", ref);
        	getExecution().setStatus(ExecutionStatus.FAILED);
            return;
        }
        getOutputDataStoreClient().post(Entity.class, entity);
        getExecution().setLinkedEntities(Arrays.asList(entity.getUri()));
        getExecution().setStatus(ExecutionStatus.FINISHED);
    }
    
    /**
     * Extracts metadata from a TextualReference object and returns a corresponding Entity.
     *  
     * @param ref the textual reference
     * @return	an entity representing the extracted information
     */
    public Entity extractMetadata(TextualReference ref) {

    	Entity entity = new Entity();
    	//TODO hacky, other special characters may still cause problems
        String name = ref.getReference()
        		.replaceAll("\\d", "")
        		.replaceAll("\\p{Punct}+", " ")
        		.replace("ü", "ue")
        		.replace("ä", "ae")
        		.replace("ö", "oe")
        		.replace("Ü", "Ue")
        		.replace("Ä", "Ae")
        		.replace("Ö", "Oe")
        		.trim();
        entity.setName(name);
       
        List<String> numericInfo = InformationExtractor.extractNumericInfo(ref);
        //TODO make priorities configurable...
        numericInfo = InformationExtractor.sortNumericInfo(numericInfo);
        entity.setNumericInfo(InformationExtractor.sortNumericInfo(numericInfo)); 
        
        entity.setIdentifier(InformationExtractor.extractDOI(ref));
        entity.setURL(InformationExtractor.extractURL(ref));
        //TODO entity.setCreator(InformationExtractor.extractCreator(ref));
        return entity;	
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        if (null == getExecution().getTextualReferences() || getExecution().getTextualReferences().isEmpty()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "textualReference", "Required parameter 'textual reference' is missing!");
        }
    }
}
