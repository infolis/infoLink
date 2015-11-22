/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisPattern;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.MissingFormatArgumentException;
import java.util.UnknownFormatConversionException;

import org.apache.lucene.search.BooleanQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 *
 * @author kata
 *
 */
public class PatternApplier extends BaseAlgorithm {

    public PatternApplier(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(PatternApplier.class);
    
    private List<InfolisPattern> getInfolisPatterns(Collection<String> patternUris) {
    	List<InfolisPattern> patterns = new ArrayList<>();
    	for (String uri : patternUris) {
    		patterns.add(getInputDataStoreClient().get(InfolisPattern.class, uri));
    	}
    	return patterns;
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
    
    private Multimap<String, InfolisPattern> getFilenamesForPatterns(Collection<InfolisPattern> patterns) {
        HashMultimap<String, InfolisPattern> patternToFilename = HashMultimap.create();
        for (InfolisPattern curPat : patterns) {
    		debug(log, "Lucene pattern: " + curPat.getLuceneQuery());
			try { debug(log, "Regex: " + curPat.getPatternRegex()); }
			catch (UnknownFormatConversionException e) { debug(log, e.getMessage()); }
			catch (MissingFormatArgumentException e) { debug(log, e.getMessage()); }

        	Execution stpExecution = getExecution().createSubExecution(SearchTermPosition.class);
            stpExecution.setIndexDirectory(getExecution().getIndexDirectory());
            stpExecution.setPhraseSlop(getExecution().getPhraseSlop());
            stpExecution.setAllowLeadingWildcards(getExecution().isAllowLeadingWildcards());
            stpExecution.setMaxClauseCount(getExecution().getMaxClauseCount());
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
    
    List<String> getContextsForPatterns(Collection<InfolisPattern> patterns) {
    	// for all patterns, retrieve documents in which they occur (using lucene)
    	Multimap<String, InfolisPattern> filenamesForPatterns = getFilenamesForPatterns(patterns);
    	List<String> textualReferences = new ArrayList<>();
    	// open each file once and search for all regex for which a corresponding (but more general)
    	// lucene pattern has been found in it
    	for (String fileUri : filenamesForPatterns.keySet()) {
    	    Collection<InfolisPattern> patternList = filenamesForPatterns.get(fileUri);
    		List<String> patternURIs = getPatternUris(patternList);
    		
    		Execution regexExec = new Execution();
        	regexExec.getInputFiles().add(fileUri);
        	regexExec.setPatterns(patternURIs);
        	regexExec.setTags(getExecution().getTags());
        	regexExec.setUpperCaseConstraint(getExecution().isUpperCaseConstraint());
        	regexExec.setAlgorithm(RegexSearcher.class);
        	regexExec.instantiateAlgorithm(this).run();
        	getExecution().setTextualReferences(regexExec.getTextualReferences());
            textualReferences.addAll(regexExec.getTextualReferences());
    	}
        return textualReferences;
    }

    Execution createIndex() throws IOException {
		Execution execution = getExecution().createSubExecution(Indexer.class);
		execution.setInputFiles(getExecution().getInputFiles());
        getOutputDataStoreClient().post(Execution.class, execution);
        execution.instantiateAlgorithm(this).run();
		return execution;
	}
    
    @Override
    public void execute() throws IOException {
    	Execution tagExec = new Execution();
    	tagExec.setAlgorithm(TagResolver.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.getInfolisPatternTags().addAll(getExecution().getInfolisPatternTags());
    	tagExec.instantiateAlgorithm(this).run();
    	getExecution().getPatterns().addAll(tagExec.getPatterns());
    	getExecution().getInputFiles().addAll(tagExec.getInputFiles());
    	
    	if (null == getExecution().getIndexDirectory() || getExecution().getIndexDirectory().isEmpty()) {
    		debug(log, "No index directory specified, indexing on demand");
    		Execution indexerExecution = createIndex();
    		getExecution().setIndexDirectory(indexerExecution.getIndexDirectory());
    	}
        //int counter = 0, size = getExecution().getInputFiles().size();
        //log.debug("number of input files: " + size);
        //updateProgress(counter, size);
    	log.debug("started");
        getExecution().setTextualReferences(getContextsForPatterns(getInfolisPatterns(getExecution().getPatterns())));
        log.debug("No. contexts found: {}", getExecution().getTextualReferences().size());
        getExecution().setStatus(ExecutionStatus.FINISHED);
    }

    @Override
    public void validate() {
    	Execution exec = this.getExecution();
		if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) && 
    		(null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())) {
            throw new IllegalArgumentException("Must set at least one inputFile!");
        }
        if ((null == exec.getPatterns() || exec.getPatterns().isEmpty()) && 
        		(null == exec.getInfolisPatternTags() || exec.getInfolisPatternTags().isEmpty())) {
            throw new IllegalArgumentException("No patterns given.");
        }
    }
}
