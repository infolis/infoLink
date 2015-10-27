package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.util.RegexUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UnknownFormatConversionException;

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
    
    List<String> getContextsForSeed(String seed) {
        // use lucene index to search for term in corpus
        Execution execution = new Execution();
        execution.setAlgorithm(SearchTermPosition.class);
        execution.setInputDirectory(this.indexerExecution.getOutputDirectory());
        execution.setPhraseSlop(this.indexerExecution.getPhraseSlop());
        execution.setAllowLeadingWildcards(this.indexerExecution.isAllowLeadingWildcards());
        execution.setMaxClauseCount(this.indexerExecution.getMaxClauseCount());
        execution.setSearchTerm(seed);
        execution.setSearchQuery(RegexUtils.normalizeQuery(seed, true));
        execution.setInputFiles(getExecution().getInputFiles());
        execution.setReliabilityThreshold(getExecution().getReliabilityThreshold());
        Algorithm algo = execution.instantiateAlgorithm(getInputDataStoreClient(), getOutputDataStoreClient(), getInputFileResolver(), getOutputFileResolver());
        getOutputDataStoreClient().post(Execution.class, execution);
        algo.run();
        getExecution().getLog().addAll(execution.getLog());
        return execution.getTextualReferences();
    } 

    List<String> getContextsForPattern(InfolisPattern pattern) {
        Execution execution_pa = new Execution();
        execution_pa.getPatterns().add(pattern.getUri());
        execution_pa.setAlgorithm(PatternApplier.class);
        execution_pa.setUpperCaseConstraint(getExecution().isUpperCaseConstraint());
        execution_pa.getInputFiles().addAll(getExecution().getInputFiles());
        Algorithm algo = execution_pa.instantiateAlgorithm(getInputDataStoreClient(), getOutputDataStoreClient(), getInputFileResolver(), getOutputFileResolver());
        algo.run();
        return execution_pa.getTextualReferences();
    }
    
    //TODO: use getContextsForPattern method above for 2nd part
    List<String> getContextsForPatterns(Set<InfolisPattern> patterns) {
        List<String> contexts = new ArrayList<>();
        // for each pattern, retrieve documents in which it occurs (using lucene)
        for (InfolisPattern curPat : patterns) {
        	if (curPat.getUri() == null)
        		throw new RuntimeException("Pattern does not have a URI!");

    		debug(log, "Lucene pattern: " + curPat.getLuceneQuery());
			try { debug(log, "Regex: " + curPat.getPatternRegex()); }
			catch (UnknownFormatConversionException e) { debug(log, e.getMessage()); }

        	Execution stpExecution = new Execution();
            stpExecution.setAlgorithm(SearchTermPosition.class);
            stpExecution.setInputDirectory(this.indexerExecution.getOutputDirectory());
            stpExecution.setPhraseSlop(this.indexerExecution.getPhraseSlop());
            stpExecution.setAllowLeadingWildcards(this.indexerExecution.isAllowLeadingWildcards());
            stpExecution.setMaxClauseCount(this.indexerExecution.getMaxClauseCount());
            stpExecution.setSearchTerm("");
    		stpExecution.setSearchQuery(curPat.getLuceneQuery());
    		stpExecution.setInputFiles(getExecution().getInputFiles());
    		stpExecution.instantiateAlgorithm(this).run();
    		
    		//matchingFilenames: documents in which lucene pattern has been found
            for (String filenameIn : stpExecution.getMatchingFiles()) {

                Execution applierExecution = new Execution();
                applierExecution.setPatternUris(Arrays.asList(curPat.getUri()));
                applierExecution.setAlgorithm(PatternApplier.class);    
                applierExecution.getInputFiles().add(filenameIn);
                applierExecution.setUpperCaseConstraint(getExecution().isUpperCaseConstraint());
                applierExecution.instantiateAlgorithm(this).run();
                contexts.addAll(applierExecution.getTextualReferences());
            }
        }
        return contexts;
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
