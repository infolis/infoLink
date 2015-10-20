package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.luceneIndexing.PatternInducer;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.BootstrapStrategy;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.RegexUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UnknownFormatConversionException;
import org.apache.commons.io.IOUtils;

import org.apache.lucene.queryParser.ParseException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 * @author domi
 */
public class FrequencyBasedBootstrapping extends BaseAlgorithm {

    public FrequencyBasedBootstrapping(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(FrequencyBasedBootstrapping.class);

    @Override
    public void execute() throws IOException {

        List<TextualReference> detectedContexts = new ArrayList<>();
        try {
            detectedContexts.addAll(bootstrap());
            // POST all the StudyContexts
            this.getOutputDataStoreClient().post(TextualReference.class, detectedContexts);
        } catch (ParseException | IOException | InstantiationException | IllegalAccessException ex) {
            fatal(log, "Could not apply frequency bootstrapping: " + ex);
            getExecution().setStatus(ExecutionStatus.FAILED);
        }

        for (TextualReference sC : detectedContexts) {
            this.getExecution().getTextualReferences().add(sC.getUri());
            //TODO: post instance uris? 
            this.getExecution().getPatterns().add(sC.getPattern());
        }

        getExecution().setStatus(ExecutionStatus.FINISHED);

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
    private List<TextualReference> bootstrap() throws ParseException, IOException, InstantiationException, IllegalAccessException {
        int numIter = 1;
        List<TextualReference> extractedContextsFromSeeds = new ArrayList<>();
        List<TextualReference> extractedContextsFromPatterns = new ArrayList<>();
        Map<String, Entity> processedSeeds = new HashMap<>();
        List<String> processedPatterns = new ArrayList<>();
        Set<Entity> seeds = new HashSet<>();
        Set<Entity> newSeedsIteration = new HashSet<>();
        Set<String> newSeedTermsIteration = new HashSet<>();
        PatternRanker ranker = new PatternRanker();
        for (String term : getExecution().getSeeds()) newSeedsIteration.add(new Entity(term)); 

        while (numIter < getExecution().getMaxIterations()) {
        	seeds = newSeedsIteration;
        	newSeedsIteration = new HashSet<>();
        	log.info("Bootstrapping... Iteration: " + numIter);
            Set<InfolisPattern> newPatterns = new HashSet<>();
            List<TextualReference> contexts_currentIteration = new ArrayList<>();
            for (Entity seed : seeds) {
            	log.info("Bootstrapping with seed \"" + seed.getName() + "\"");
                if (processedSeeds.keySet().contains(seed.getName())) {
                	if (getExecution().getBootstrapStrategy() == BootstrapStrategy.mergeCurrent) {
                		contexts_currentIteration.addAll(processedSeeds.get(seed.getName()).getTextualReferences());
                	}
                	debug(log, "seed " + seed.getName() + " already known, continuing.");
                    continue;
                }
                // 1. use lucene index to search for term in corpus
                Execution execution = new Execution();
                execution.setAlgorithm(SearchTermPosition.class);
                execution.setSearchTerm(seed.getName());
                execution.setSearchQuery(RegexUtils.normalizeQuery(seed.getName(), true));
                execution.setInputFiles(getExecution().getInputFiles());
                execution.setReliabilityThreshold(getExecution().getReliabilityThreshold());
                getOutputDataStoreClient().post(Execution.class, execution);
                execution.instantiateAlgorithm(this).run();
                getExecution().getLog().addAll(execution.getLog());

                List<TextualReference> detectedContexts = new ArrayList<>();
                for (TextualReference studyContext : getInputDataStoreClient().get(TextualReference.class, execution.getTextualReferences())) {
					detectedContexts.add(studyContext);
					contexts_currentIteration.add(studyContext);
					extractedContextsFromSeeds.add(studyContext);
//                    log.warn("{}", studyContext.getPattern());
                }
                seed.setTextualReferences(detectedContexts);
                processedSeeds.put(seed.getName(), seed);

                log.info("Extracted contexts of seed.");
                // 2. generate patterns
                if (getExecution().getBootstrapStrategy() == BootstrapStrategy.separate) {
                	log.info("--- Entering Pattern Induction phase ---");
                	List<List<InfolisPattern>> candidates = inducePatterns(detectedContexts);
                	log.info("Pattern Induction completed.");
                    log.info("--- Entering Pattern Selection phase ---");
                    newPatterns.addAll(ranker.getRelevantPatterns(candidates, detectedContexts, processedPatterns));
                }
            }
            // mergeNew and mergeCurrent have different contexts_currentIteration at this point, with previously processed seeds filtered for mergeNew but not for mergeCurrent
            if (getExecution().getBootstrapStrategy() == BootstrapStrategy.mergeCurrent 
            		|| getExecution().getBootstrapStrategy() == BootstrapStrategy.mergeNew) {
            	log.info("--- Entering Pattern Induction phase ---");
            	List<List<InfolisPattern>> candidates = inducePatterns(contexts_currentIteration);
            	log.info("Pattern Induction completed.");
                log.info("--- Entering Pattern Selection phase ---");
                newPatterns.addAll(ranker.getRelevantPatterns(candidates, contexts_currentIteration, processedPatterns));        
            }
            
            if (getExecution().getBootstrapStrategy() == BootstrapStrategy.mergeAll) {
            	log.info("--- Entering Pattern Induction phase ---");
            	List<List<InfolisPattern>> candidates = inducePatterns(extractedContextsFromSeeds);
            	log.info("Pattern Induction completed.");
                log.info("--- Entering Pattern Selection phase ---");
                newPatterns.addAll(ranker.getRelevantPatterns(candidates, extractedContextsFromSeeds, processedPatterns));
            }
            
            // POST the patterns
            getOutputDataStoreClient().post(InfolisPattern.class, newPatterns);
            for (InfolisPattern pattern : newPatterns) {
            	processedPatterns.add(pattern.getMinimal());
            }
            
            log.info("Pattern Selection completed.");
            log.info("--- Entering Instance Extraction phase ---");
            
            // 3. search for patterns in corpus
            List<TextualReference> res = findNewContextsForPatterns(newPatterns);
            extractedContextsFromPatterns.addAll(res);
            
            for (TextualReference entry : res) {
            	newSeedsIteration.add(new Entity(entry.getTerm()));
            	newSeedTermsIteration.add(entry.getTerm());
            }
            
            debug(log, "Found %s seeds in current iteration: %s", newSeedsIteration.size(), newSeedsIteration);
            numIter++;
            
            persistExecution();
            if (newSeedTermsIteration.isEmpty() | processedSeeds.keySet().containsAll(newSeedTermsIteration)) {
            	debug(log, "No new seeds found in iteration, returning.");
            	// extractedContexts contains all contexts resulting from searching a seed term
            	// extractedContexts_patterns contains all contexts resulting from searching for the induced patterns
            	// thus, return the latter here
            	log.info("Final iteration: " + numIter);
                log.debug("Final list of instances:  ");
                for (Entity i : processedSeeds.values()) { log.debug(i.getName() + "=" + i.getReliability()); }
                log.debug("Final list of patterns: " + processedPatterns);
            	return extractedContextsFromPatterns;
            }
        }
        debug(log, "Maximum number of iterations reached, returning.");
        // TODO now delete all the contexts that were only temporary
        
        log.info("Final iteration: " + numIter);
        log.debug("Final list of instances:  ");
        for (Entity i : processedSeeds.values()) { log.debug(i.getName() + "=" + i.getReliability()); }
        log.debug("Final list of patterns: " + processedPatterns);
        return extractedContextsFromPatterns;
    }
    
    private List<List<InfolisPattern>> inducePatterns(Collection<TextualReference> contexts) {
    	List<List<InfolisPattern>> patterns = new ArrayList<>();
    	int n = 0;
    	double threshold = getExecution().getReliabilityThreshold();
    	for (TextualReference context : contexts) {
    		n++;
    		log.debug("Inducing relevant patterns for context " + n + " of " + contexts.size());
    		Double[] thresholds = {threshold, threshold - 0.02, threshold - 0.04, threshold - 0.06, threshold - 0.08, threshold - 0.02, threshold - 0.04, threshold - 0.06, threshold - 0.08};
    		PatternInducer inducer = new PatternInducer(context, thresholds);
    		patterns.add(inducer.candidates);
    	}
    	return patterns;
    }

    private List<TextualReference> findNewContextsForPatterns(Set<InfolisPattern> patterns) throws IOException, ParseException, InstantiationException, IllegalAccessException {
        List<TextualReference> contexts = new ArrayList<>();

        for (InfolisPattern curPat : patterns) {
        	if (curPat.getUri() == null)
        		throw new RuntimeException("Pattern does not have a URI!");

    		debug(log, "Lucene pattern: " + curPat.getLuceneQuery());
			try { debug(log, "Regex: " + curPat.getPatternRegex()); }
			catch (UnknownFormatConversionException e) { debug(log, e.getMessage()); }

        	Execution stpExecution = new Execution();
            stpExecution.setAlgorithm(SearchTermPosition.class);
            stpExecution.setSearchTerm("");
    		stpExecution.setSearchQuery(curPat.getLuceneQuery());
    		stpExecution.setInputFiles(getExecution().getInputFiles());
    		stpExecution.instantiateAlgorithm(this).run();

            for (String filenameIn : stpExecution.getMatchingFiles()) {

                Execution applierExecution = new Execution();
                applierExecution.setPatternUris(Arrays.asList(curPat.getUri()));
                applierExecution.setAlgorithm(PatternApplier.class);                
                applierExecution.getInputFiles().add(filenameIn);
                applierExecution.setUpperCaseConstraint(getExecution().isUpperCaseConstraint());
                applierExecution.instantiateAlgorithm(this).run();

                for (TextualReference studyContext : getInputDataStoreClient().get(TextualReference.class, applierExecution.getTextualReferences())) {
                	// TODO: really? overwrite the pattern for every context?
                    studyContext.setPattern(curPat.getUri());
                    contexts.add(studyContext);
                }
            }
        }
        return contexts;
    }
    
    class PatternRanker {
    	
    	private Set<InfolisPattern> getRelevantPatterns(List<List<InfolisPattern>> candidates, List<TextualReference> contexts, List<String> processedMinimals) {
	        Set<InfolisPattern> patterns = new HashSet<>();
	        Set<String> processedMinimals_iteration = new HashSet<>();
	        List<String> allContextStrings_iteration = TextualReference.getContextStrings(contexts);
	        // constraint for patterns: at least one component not be a stopword
	        // prevent induction of patterns less general than already known patterns:
	        // check whether pattern is known before continuing
	        for (List<InfolisPattern> candidatesForContext : candidates) {
		        for (InfolisPattern candidate : candidatesForContext) {
		           	log.debug("Checking if pattern is relevant: " + candidate.getMinimal());
		           	if (processedMinimals.contains(candidate.getMinimal()) | processedMinimals_iteration.contains(candidate.getMinimal())) {
		            	// no need to induce less general patterns, continue with next context
		            	//TODO (enhancement): separate number and character patterns: omit only less general patterns of the same type, do not limit generation of other type
		            	//TODO (enhancement): also store unsuccessful patterns to avoid multiple computations of their score?
		            	log.debug("Pattern already known, returning.");
		                   break;
		            }
		            boolean nonStopwordPresent = false;
		            for (String word : candidate.getWords()) {
		            	if (!RegexUtils.isStopword(word)) { 
		            		nonStopwordPresent = true;
		            		continue;
		            	}
		            }
		            if (!nonStopwordPresent) log.debug("Pattern rejected - stopwords only");
		            if (nonStopwordPresent & candidate.isRelevant(allContextStrings_iteration)) {
		            	patterns.add(candidate);
		            	processedMinimals_iteration.add(candidate.getMinimal());
		            	log.debug("Pattern accepted");
		            	//TODO (enhancement): separate number and character patterns: omit only less general patterns of the same type, do not limit generation of other type
		            	break;
		            } 
		        }
    		}
	        return patterns;
	    }
    }//end of class
    
}
