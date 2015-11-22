package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
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
import java.util.MissingFormatArgumentException;
import java.util.UnknownFormatConversionException;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

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
    Execution indexerExecution;
    
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
		execution.setAllowLeadingWildcards(getExecution().isAllowLeadingWildcards());
		// 0 requires exact match, 5 means that up to 5 edit operations may be carried out...
		execution.setPhraseSlop(getExecution().getPhraseSlop());
		BooleanQuery.setMaxClauseCount(getExecution().getMaxClauseCount());
        getOutputDataStoreClient().post(Execution.class, execution);
        execution.instantiateAlgorithm(this).run();
		return execution;
	}
    
    List<TextualReference> getContextsForSeed(String seed) {
        // use lucene index to search for term in corpus
        Execution execution = getExecution().createSubExecution(SearchTermPosition.class);
        execution.setIndexDirectory(this.indexerExecution.getOutputDirectory());
        execution.setPhraseSlop(this.indexerExecution.getPhraseSlop());
        execution.setAllowLeadingWildcards(this.indexerExecution.isAllowLeadingWildcards());
        execution.setMaxClauseCount(this.indexerExecution.getMaxClauseCount());
        execution.setSearchTerm(seed);
        execution.setSearchQuery(RegexUtils.normalizeQuery(seed, true));
        execution.setInputFiles(getExecution().getInputFiles());
        execution.setReliabilityThreshold(getExecution().getReliabilityThreshold());
        Algorithm algo = execution.instantiateAlgorithm(this);
        getOutputDataStoreClient().post(Execution.class, execution);
        algo.run();
        getExecution().getLog().addAll(execution.getLog());
        List<TextualReference> textualReferences = new ArrayList<>();
            for (String uri : execution.getTextualReferences()) {
            	textualReferences.add(getOutputDataStoreClient().get(TextualReference.class, uri));
            }
        return textualReferences;
    } 
    
    private Multimap<String, InfolisPattern> getFilenamesForPatterns(Collection<InfolisPattern> patterns) {
        HashMultimap<String, InfolisPattern> patternToFilename = HashMultimap.create();
        for (InfolisPattern curPat : patterns) {
    		debug(log, "Lucene pattern: " + curPat.getLuceneQuery());
			try { debug(log, "Regex: " + curPat.getPatternRegex()); }
			catch (UnknownFormatConversionException e) { debug(log, e.getMessage()); }
			catch (MissingFormatArgumentException e) { debug(log, e.getMessage()); }

        	Execution stpExecution = getExecution().createSubExecution(SearchTermPosition.class);
            stpExecution.setIndexDirectory(this.indexerExecution.getOutputDirectory());
            stpExecution.setPhraseSlop(this.indexerExecution.getPhraseSlop());
            stpExecution.setAllowLeadingWildcards(this.indexerExecution.isAllowLeadingWildcards());
            stpExecution.setMaxClauseCount(this.indexerExecution.getMaxClauseCount());
    		stpExecution.setSearchQuery(curPat.getLuceneQuery());
    		stpExecution.setInputFiles(getExecution().getInputFiles());
    		// with empty searchTerm, SearchTermPosition does not post any textual references
    		// thus, no need to create temporary file resolver / data store client here
    		stpExecution.instantiateAlgorithm(this).run();
    		for (String fileUri : stpExecution.getMatchingFiles()) {
    		    patternToFilename.put(fileUri, curPat);
    		}
        }
        return patternToFilename;
    }
    
    private List<String> getPatternUris(Collection<InfolisPattern> patternList) {
    	List<String> patternUris = new ArrayList<String>();
    	for (InfolisPattern curPat : patternList) {
	    	if (curPat.getUri() == null)
	    		throw new RuntimeException("Pattern does not have a URI!");
	    	patternUris.add(curPat.getUri());
    	}
    	return patternUris;
    }
    		
    List<String> getContextsForPatterns(Collection<InfolisPattern> patterns) {
    	// for all patterns, retrieve documents in which they occur (using lucene)
    	Multimap<String, InfolisPattern> filenamesForPatterns = getFilenamesForPatterns(patterns);
    	List<String> textualReferences = new ArrayList<>();
    	// open each file once and search for all regex for which a corresponding (but more general)
    	// lucene pattern has been found in it
    	for (String fileUri : filenamesForPatterns.keySet()) {
    	    Collection<InfolisPattern> patternList = filenamesForPatterns.get(fileUri);
    		List<String> patternURIs = getPatternUris(patternList);
    		Execution applierExecution = getExecution().createSubExecution(RegexSearcher.class);
            applierExecution.setPatterns(patternURIs);
            applierExecution.getInputFiles().add(fileUri);
            applierExecution.setUpperCaseConstraint(getExecution().isUpperCaseConstraint());
            applierExecution.instantiateAlgorithm(this).run();
            textualReferences.addAll(applierExecution.getTextualReferences());
    	}
        return textualReferences;
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
    	
    	getExecution().getPatterns().addAll(tagExec.getPatterns());
    	getExecution().getInputFiles().addAll(tagExec.getInputFiles());
    	
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
