package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.patternLearner.BootstrapLearner;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.util.RegexUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.queryParser.ParseException;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author kata
 *
 */
public abstract class Bootstrapping extends BaseAlgorithm implements BootstrapLearner {

    public Bootstrapping(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) throws IOException {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Bootstrapping.class);
    public Execution indexerExecution;
    
    public abstract List<TextualReference> bootstrap() throws ParseException, IOException, InstantiationException, IllegalAccessException;
    
    public abstract static class PatternInducer {
    	protected abstract List<InfolisPattern> induce(TextualReference context, Double[] thresholds);
    	public abstract int getPatternsPerContext();
    };
    // TODO define getBestPatterns - method
    public abstract class PatternRanker {};
    
    Execution createIndex() throws IOException {
		Execution execution = getExecution().createSubExecution(Indexer.class);
		execution.setInputFiles(getExecution().getInputFiles());
        getOutputDataStoreClient().post(Execution.class, execution);
        execution.instantiateAlgorithm(this).run();
		return execution;
	}
    
    List<TextualReference> getContextsForSeed(String seed) {
        // use lucene index to search for term in corpus
        Execution execution = getExecution().createSubExecution(SearchTermPosition.class);
        execution.setIndexDirectory(indexerExecution.getOutputDirectory());
        execution.setPhraseSlop(getExecution().getPhraseSlop());
        execution.setAllowLeadingWildcards(getExecution().isAllowLeadingWildcards());
        execution.setMaxClauseCount(getExecution().getMaxClauseCount());
        execution.setSearchTerm(seed);
        execution.setSearchQuery(RegexUtils.normalizeQuery(seed, true));
        execution.setInputFiles(getExecution().getInputFiles());
        execution.setReliabilityThreshold(getExecution().getReliabilityThreshold());
        Algorithm algo = execution.instantiateAlgorithm(this);
        getOutputDataStoreClient().post(Execution.class, execution);
        algo.run();
        if(execution.getStatus()==ExecutionStatus.FAILED) {
            getExecution().setSubExecutionFailed(true);
        }
        getExecution().getLog().addAll(execution.getLog());
        List<TextualReference> textualReferences = new ArrayList<>();
            for (String uri : execution.getTextualReferences()) {
            	textualReferences.add(getOutputDataStoreClient().get(TextualReference.class, uri));
            }
        return textualReferences;
    } 
    
    private List<String> getPatternUris(Collection<InfolisPattern> patternList) {
    	List<String> patternUris = new ArrayList<String>();
    	for (InfolisPattern curPat : patternList) {
	    	if (curPat.getUri() == null)
	    		throw new RuntimeException("Pattern does not have a URI!");
	    	patternUris.add(curPat.getUri());
	    	log.debug("pattern " + curPat + " has uri " + curPat.getUri());
    	}
    	return patternUris;
    }

    List<String> getContextsForPatterns(Collection<InfolisPattern> patterns) {
    	Execution applierExec = new Execution();
    	applierExec.setAlgorithm(PatternApplier.class);
    	applierExec.setInputFiles(getExecution().getInputFiles());
    	applierExec.setIndexDirectory(indexerExecution.getOutputDirectory());
    	applierExec.setPatterns(getPatternUris(patterns));
    	applierExec.setUpperCaseConstraint(getExecution().isUpperCaseConstraint());
    	applierExec.setPhraseSlop(getExecution().getPhraseSlop());
    	// TODO this need not be a parameter for execution
    	applierExec.setAllowLeadingWildcards(true);	
    	// TODO this need not be a parameter for execution
    	applierExec.setMaxClauseCount(getExecution().getMaxClauseCount());
    	applierExec.setTags(getExecution().getTags());
    	applierExec.instantiateAlgorithm(this).run();
        if(applierExec.getStatus()==ExecutionStatus.FAILED) {
            getExecution().setSubExecutionFailed(true);
        }
    	return applierExec.getTextualReferences();
    }

    @Override
    public void validate() {
        Execution exec = this.getExecution();
		if (null == exec.getSeeds() || exec.getSeeds().isEmpty()) {
            throw new IllegalArgumentException("Must set at least one term as seed!");
        }
        if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) && 
    		(null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())) {
            throw new IllegalArgumentException("Must set at least one inputFile!");
        }
        if (null == exec.getBootstrapStrategy()) {
            throw new IllegalArgumentException("Must set the bootstrap strategy");
        }
    }
    
    @Override
    public void execute() throws IOException {
    	Execution tagExec = getExecution().createSubExecution(TagResolver.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.getInfolisPatternTags().addAll(getExecution().getInfolisPatternTags());
    	tagExec.instantiateAlgorithm(this).run();        
        if(tagExec.getStatus()==ExecutionStatus.FAILED) {
            getExecution().setSubExecutionFailed(true);
        }
    	
    	getExecution().getPatterns().addAll(tagExec.getPatterns());
    	getExecution().getInputFiles().addAll(tagExec.getInputFiles());

    	this.indexerExecution = createIndex();
    	getExecution().setIndexDirectory(this.indexerExecution.getIndexDirectory());
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
        if(getExecution().isSubExecutionFailed()) {
            getExecution().setStatus(ExecutionStatus.FAILED);
        }
        else {
            getExecution().setStatus(ExecutionStatus.FINISHED);
        }
    }

}
