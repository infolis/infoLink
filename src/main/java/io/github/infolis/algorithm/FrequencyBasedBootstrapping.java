package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.luceneIndexing.PatternInducer;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.TextualReference;
import io.github.infolis.util.RegexUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.queryParser.ParseException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 * @author domi
 */
public class FrequencyBasedBootstrapping extends BaseAlgorithm {

	public static final TextualReference EMPTY_CONTEXT = new TextualReference();

    public FrequencyBasedBootstrapping(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(FrequencyBasedBootstrapping.class);

    @Override
    public void execute() throws IOException {

        List<TextualReference> detectedContexts = new ArrayList<>();
        try {
            detectedContexts.addAll(bootstrapFrequencyBased());
            // POST all the StudyContexts
            this.getOutputDataStoreClient().post(TextualReference.class, detectedContexts);
        } catch (ParseException | IOException | InstantiationException | IllegalAccessException ex) {
            fatal(log, "Could not apply frequency bootstrapping: " + ex);
            getExecution().setStatus(ExecutionStatus.FAILED);
        }

        for (TextualReference sC : detectedContexts) {
            this.getExecution().getStudyContexts().add(sC.getUri());
            //TODO: use URI instead of the term string?
            this.getExecution().getStudies().add(sC.getTerm());
            this.getExecution().getPattern().add(sC.getPattern());
        }

        getExecution().setStatus(ExecutionStatus.FINISHED);

    }

    @Override
    public void validate() {
        //TODO: what about the index path? need to be given!
        if (null == this.getExecution().getTerms()
                || this.getExecution().getTerms().isEmpty()) {
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
   
    private TextualReference getContextForTerm(List<TextualReference> contextList, String term) {
    	
    	for (TextualReference context : contextList) {
    		if (context.getTerm().equals(term)) return context;
    	}
    	// TODO Won't this create empty (non-sensical) contexts? Why can this happen?
		return EMPTY_CONTEXT;
    }

    /**
     * Generates extraction patterns using an iterative bootstrapping approach.
     *
     * <ol>
     * <li>searches for seeds in the specified corpus and extracts the
     * surrounding words as contexts</li>
     * <li>analyzes contexts and generates extraction patterns</li>
     * <li>applies extraction patterns on corpus to extract new seeds</li>
     * <li>continues with 1) until maximum number of iterations is reached</li>
     * <li>outputs found seeds, contexts and extraction patterns</li>
     * </ol>
     *
     * Method for assessing pattern validity is frequency-based.
     *
     * @param seed	the term to be searched as starting point in the current
     * iteration
     * @param threshold		threshold for accepting patterns
     * @param maxIterations		maximum number of iterations for algorithm
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     *
     */
    private List<TextualReference> bootstrapFrequencyBased() throws ParseException, IOException, InstantiationException, IllegalAccessException {
        int numIter = 0;
        List<TextualReference> extractedContextsFromSeed = new ArrayList<>();
        List<TextualReference> extractedContextsFromPatterns = new ArrayList<>();
        List<String> processedSeeds = new ArrayList<>();
        List<String> processedPatterns = new ArrayList<>();
        Set<String> seeds = new HashSet<>();
        Set<String> newSeedsIteration = new HashSet<>(getExecution().getTerms());
        // TODO Log start and end of eahch iteration
        while (numIter < getExecution().getMaxIterations()) {
        	seeds = newSeedsIteration;
        	newSeedsIteration = new HashSet<>();
        	debug(log, "Start iteration #%s Looking for seeds: %s", numIter, seeds);
            Set<InfolisPattern> newPatterns = new HashSet<>();
            List<TextualReference> contexts_currentIteration = new ArrayList<>();
            for (String seed : seeds) {
            	
            	List<TextualReference> detectedContexts = new ArrayList<>();
            	
                if (processedSeeds.contains(seed)) {
                	if (getExecution().getBootstrapStrategy() == Execution.Strategy.mergeCurrent) {
                		contexts_currentIteration.add(getContextForTerm(extractedContextsFromSeed, seed));
                	}
                	debug(log, "seed " + seed + " already known, continuing.");
                    continue;
                }
                debug(log, "Processing seed \"" + seed + "\"");
                debug(log, "Strategy: %s", getExecution().getBootstrapStrategy());
                // 1. use lucene index to search for term in corpus
                Execution execution = new Execution();
                execution.setAlgorithm(SearchTermPosition.class);
                execution.setSearchTerm(seed);
                execution.setSearchQuery(RegexUtils.normalizeQuery(seed, true));
                execution.setInputFiles(getExecution().getInputFiles());
                execution.setThreshold(getExecution().getThreshold());
                execution.instantiateAlgorithm(this).run();
                getExecution().getLog().addAll(execution.getLog());

                for (TextualReference studyContext : getInputDataStoreClient().get(TextualReference.class, execution.getStudyContexts())) {
					detectedContexts.add(studyContext);
//                    log.warn("{}", studyContext.getPattern());
                }
                contexts_currentIteration.addAll(detectedContexts);
                extractedContextsFromSeed.addAll(detectedContexts);
                // 2. generate patterns
                if (getExecution().getBootstrapStrategy() == Execution.Strategy.separate) {
                    Set<InfolisPattern> patterns = PatternInducer.inducePatterns(detectedContexts, getExecution().getThreshold(), processedPatterns);
                    newPatterns.addAll(patterns);
                }
            }
            
            // mergeNew and mergeCurrent have different contexts_currentIteration at this point, with previously processed seeds filtered for mergeNew but not for mergeCurrent
            if (getExecution().getBootstrapStrategy() == Execution.Strategy.mergeCurrent 
            		|| getExecution().getBootstrapStrategy() == Execution.Strategy.mergeNew) {
                Set<InfolisPattern> patterns = PatternInducer.inducePatterns(contexts_currentIteration, getExecution().getThreshold(), processedPatterns);
                newPatterns.addAll(patterns);         
            }
            
            // TODO ensure this is not an 'else if'
            if (getExecution().getBootstrapStrategy() == Execution.Strategy.mergeAll) {
                Set<InfolisPattern> patterns = PatternInducer.inducePatterns(extractedContextsFromSeed, getExecution().getThreshold(), processedPatterns);
                newPatterns.addAll(patterns);
            }
            
            // POST the patterns
            getOutputDataStoreClient().post(InfolisPattern.class, newPatterns);
            for (InfolisPattern pattern : newPatterns) {
            	processedPatterns.add(pattern.getMinimal());
            }
            
            // 3. search for patterns in corpus
            List<TextualReference> res = findNewContextsForPatterns(newPatterns);
            // res contains all contexts extracted by searching for patterns
            extractedContextsFromPatterns.addAll(res);
            processedSeeds.addAll(seeds);
            
            for (TextualReference entry : res) {
            	newSeedsIteration.add(entry.getTerm());
            }
            
            debug(log, "Found %s seeds in current iteration: %s", newSeedsIteration.size(), newSeedsIteration);
            numIter++;
            
            persistExecution();
            if (processedSeeds.containsAll(newSeedsIteration)) {
            	debug(log, "No new seeds found in iteration, returning.");
            	// extractedContexts contains all contexts resulting from searching a seed term
            	// extractedContexts_patterns contains all contexts resulting from searching for the induced patterns
            	// thus, return the latter here
            	return extractedContextsFromPatterns;
            }
        }
        debug(log, "Maximum number of iterations reached, returning.");
        // TODO now delete all the contexts that were only temporary
        return extractedContextsFromPatterns;
    }

    private List<TextualReference> findNewContextsForPatterns(Set<InfolisPattern> patterns) throws IOException, ParseException, InstantiationException, IllegalAccessException {
        List<TextualReference> contexts = new ArrayList<>();

        for (InfolisPattern curPat : patterns) {
        	if (curPat.getUri() == null)
        		throw new RuntimeException("Pattern does not have a URI!");

    		debug(log, "Lucene pattern: " + curPat.getLuceneQuery());
			debug(log, "Regex: " + curPat.getPatternRegex());

        	Execution stpExecution = new Execution();
            stpExecution.setAlgorithm(SearchTermPosition.class);
            stpExecution.setSearchTerm("");
    		stpExecution.setSearchQuery(curPat.getLuceneQuery());
    		stpExecution.setInputFiles(getExecution().getInputFiles());
    		stpExecution.instantiateAlgorithm(this).run();

            for (String filenameIn : stpExecution.getMatchingFilenames()) {

                Execution applierExecution = new Execution();
                applierExecution.setPattern(Arrays.asList(curPat.getUri()));
                applierExecution.setAlgorithm(PatternApplier.class);                
                applierExecution.getInputFiles().add(filenameIn);
                applierExecution.instantiateAlgorithm(this).run();

                for (TextualReference studyContext : getInputDataStoreClient().get(TextualReference.class, applierExecution.getStudyContexts())) {
                	// TODO: really? overwrite the pattern for every context?
                    studyContext.setPattern(curPat.getUri());
                    contexts.add(studyContext);
                }
            }
        }
        return contexts;
    }
}
