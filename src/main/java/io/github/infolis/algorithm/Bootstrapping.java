package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author kata
 *
 */
public abstract class Bootstrapping extends BaseAlgorithm {

    public Bootstrapping(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) throws IOException {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Bootstrapping.class);
    Execution indexerExecution = new Execution();
    
    abstract List<TextualReference> bootstrap() throws ParseException, IOException, InstantiationException, IllegalAccessException;
    
    Execution createIndex() throws IOException {
		Execution execution = new Execution();
		execution.setAlgorithm(Indexer.class);
		execution.setInputFiles(getExecution().getInputFiles());
		execution.setAllowLeadingWildcards(getExecution().isAllowLeadingWildcards());
		// 0 requires exact match, 5 means that up to 5 edit operations may be carried out...
		execution.setPhraseSlop(getExecution().getPhraseSlop());
		BooleanQuery.setMaxClauseCount(getExecution().getMaxClauseCount());
        getOutputDataStoreClient().post(Execution.class, execution);
        execution.instantiateAlgorithm(this).run();
		return execution;
	}
    
    @Override
    public void validate() {
        if (null == this.getExecution().getSeeds()
                || this.getExecution().getSeeds().isEmpty()) {
            throw new IllegalArgumentException("Must set at least one term as seed!");
        }
        if (null == this.getExecution().getInputFiles()
                || this.getExecution().getInputFiles().isEmpty()) {
            throw new IllegalArgumentException("Must set at least one input file!");
        }
        if (null == this.getExecution().getBootstrapStrategy()) {
            throw new IllegalArgumentException("Must set the bootstrap strategy");
        }
    }
    
    @Override
    public void execute() throws IOException {
    	this.indexerExecution = createIndex();
    	List<TextualReference> detectedContexts = new ArrayList<>();
        try {
        	detectedContexts = bootstrap();
        } catch (ParseException | IOException | InstantiationException | IllegalAccessException ex) {
            log.error("Could not apply reliability bootstrapping: " + ex);
            getExecution().setStatus(ExecutionStatus.FAILED);
        }
        
        for (TextualReference sC : detectedContexts) {
            getOutputDataStoreClient().post(TextualReference.class, sC);
            this.getExecution().getTextualReferences().add(sC.getUri());
            this.getExecution().getPatterns().add(sC.getPattern());
        }
        getExecution().setStatus(ExecutionStatus.FINISHED);
    }

}
