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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 *
 */
public abstract class Bootstrapping extends ComplexAlgorithm implements BootstrapLearner {

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
		execution.setOutputDirectory(getExecution().getIndexDirectory());
        execution.instantiateAlgorithm(this).run();
        getOutputDataStoreClient().post(Execution.class, execution);
		return execution;
	}

    List<TextualReference> getContextsForSeed(String seed) {
        // use lucene index to search for term in corpus
        Execution execution = getExecution().createSubExecution(LuceneSearcher.class);
        execution.setIndexDirectory(indexerExecution.getOutputDirectory());
        execution.setPhraseSlop(0);
        execution.setAllowLeadingWildcards(true);
        execution.setMaxClauseCount(getExecution().getMaxClauseCount());
        execution.setSearchTerm(seed);
        InfolisPattern termPattern = new InfolisPattern(RegexUtils.normalizeQuery(seed, true));
        DataStoreClient tempClient = getTempDataStoreClient();
        tempClient.post(InfolisPattern.class, termPattern);
        execution.setPatterns(Arrays.asList(termPattern.getUri()));
        execution.setInputFiles(getExecution().getInputFiles());
        execution.setReliabilityThreshold(getExecution().getReliabilityThreshold());
        Algorithm algo = execution.instantiateAlgorithm(
        		getInputDataStoreClient(), tempClient, getInputFileResolver(), getOutputFileResolver());
        algo.run();
        getOutputDataStoreClient().post(Execution.class, execution);
        getExecution().getLog().addAll(execution.getLog());
        List<TextualReference> textualReferences = new ArrayList<>();
        for (String uri : execution.getTextualReferences()) {
           	textualReferences.add(tempClient.get(TextualReference.class, uri));
        }
        tempClient.clear();
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

    List<String> getContextsForPatterns(Collection<InfolisPattern> patterns, 
    		DataStoreClient outputDataStoreClient) {
    	Execution searcherExec = getExecution().createSubExecution(InfolisPatternSearcher.class);
    	searcherExec.setInputFiles(getExecution().getInputFiles());
    	searcherExec.setIndexDirectory(indexerExecution.getOutputDirectory());
    	searcherExec.setPatterns(getPatternUris(patterns));
    	searcherExec.setUpperCaseConstraint(getExecution().isUpperCaseConstraint());
    	searcherExec.setPhraseSlop(getExecution().getPhraseSlop());
    	searcherExec.setAllowLeadingWildcards(true);
    	searcherExec.setMaxClauseCount(getExecution().getMaxClauseCount());
    	searcherExec.instantiateAlgorithm(getInputDataStoreClient(), outputDataStoreClient, 
    			getInputFileResolver(), getOutputFileResolver()).run();
    	return searcherExec.getTextualReferences();
    }

    @Override
    public void validate() {
        Execution exec = this.getExecution();
		if (null == exec.getSeeds() || exec.getSeeds().isEmpty()) {
            throw new IllegalArgumentException("Must set at least one term as seed!");
        }
        if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) &&
    		(null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())) {
            throw new IllegalArgumentException("Must set at least one input file!");
        }
        if (null == exec.getBootstrapStrategy()) {
            throw new IllegalArgumentException("Must set the bootstrap strategy!");
        }
        if (null == exec.isTokenize()) {
			warn(log, "tokenize parameter unspecified. Setting to true for Bootstrapping"); 
			exec.setTokenize(true);
		}
    }

    @Override
    public void execute() throws IOException {
    	Execution tagExec = getExecution().createSubExecution(TagSearcher.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.getInfolisPatternTags().addAll(getExecution().getInfolisPatternTags());
    	tagExec.instantiateAlgorithm(this).run();

    	getExecution().getPatterns().addAll(tagExec.getPatterns());
    	getExecution().getInputFiles().addAll(tagExec.getInputFiles());
    	
    	preprocessInputFiles();
    	
    	this.indexerExecution = createIndex();
    	List<TextualReference> detectedReferences = new ArrayList<>();
        try {
        	detectedReferences = bootstrap();
        } catch (ParseException | IOException | InstantiationException | IllegalAccessException ex) {
            log.error("Could not apply reliability bootstrapping: " + ex);
            getExecution().setStatus(ExecutionStatus.FAILED);
        }

        Set<String> detectedPatterns = new HashSet<>();
        for (TextualReference ref : detectedReferences) {
            this.getExecution().getTextualReferences().add(ref.getUri());
            detectedPatterns.add(ref.getPattern());
        }
        
        this.getExecution().getPatterns().addAll(detectedPatterns);
        
        debug(log, "Final list of patterns: ");
        for (InfolisPattern p : getOutputDataStoreClient().get(InfolisPattern.class, this.getExecution().getPatterns())) {
        	debug(log, p.getPatternRegex() + "=" + p.getPatternReliability());
        }

        getExecution().setStatus(ExecutionStatus.FINISHED);
    }

}
