package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.patternLearner.StandardPatternInducer;
import io.github.infolis.model.BootstrapStrategy;
import io.github.infolis.model.EntityType;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.util.RegexUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 */
public class FrequencyBasedBootstrapping extends Bootstrapping {

    public FrequencyBasedBootstrapping(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) throws IOException {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(FrequencyBasedBootstrapping.class);

	public PatternInducer getPatternInducer() {
	   	return new StandardPatternInducer(getExecution().getWindowsize());
	}

	public PatternRanker getPatternRanker() {
    	return new FrequencyPatternRanker();
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
    public List<TextualReference> bootstrap() throws ParseException, IOException, InstantiationException, IllegalAccessException {
        int numIter = 1;
        List<TextualReference> extractedContextsFromSeeds = new ArrayList<>();
        List<TextualReference> extractedContextsFromPatterns = new ArrayList<>();
        Map<String, Entity> processedSeeds = new HashMap<>();
        List<String> processedPatterns = new ArrayList<>();
        Set<Entity> seeds = new HashSet<>();
        Set<Entity> newSeedsIteration = new HashSet<>();
        Set<String> newSeedTermsIteration = new HashSet<>();
        FrequencyPatternRanker ranker = new FrequencyPatternRanker();
        //TODO define and use generic PatternRanker
        //PatternRanker ranker = getPatternRanker();
        for (String term : getExecution().getSeeds()) {
        	Entity entity = new Entity(term);
        	entity.setTags(getExecution().getTags());
        	entity.setEntityType(EntityType.citedData);
        	entity.setIsSeed();
        	newSeedsIteration.add(entity);
        }

        while (numIter < getExecution().getMaxIterations()) {
        	updateProgress(numIter, getExecution().getMaxIterations());
        	seeds = newSeedsIteration;
        	newSeedsIteration = new HashSet<>();
        	newSeedTermsIteration = new HashSet<>();
        	HashSet<String> addedSeeds = new HashSet<>();
        	//info(log, "Bootstrapping... Iteration: " + numIter);
        	debug(log, "Bootstrapping... Iteration: " + numIter);
            Set<InfolisPattern> newPatterns = new HashSet<>();
            List<TextualReference> contexts_currentIteration = new ArrayList<>();
            for (Entity seed : seeds) {
            	//info(log, "Bootstrapping with seed \"" + seed.getName() + "\"");
            	debug(log, "Bootstrapping with seed \"" + seed.getName() + "\"");
                if (processedSeeds.keySet().contains(seed.getName())) {
                	if (getExecution().getBootstrapStrategy() == BootstrapStrategy.mergeCurrent) {
                		// add context of each seed only once even if seed was found multiple times
                		if (!addedSeeds.contains(seed.getName())) {
                			contexts_currentIteration.addAll(processedSeeds.get(seed.getName()).getTextualReferences());
                			addedSeeds.add(seed.getName());
                		}
                	}
                	//log.trace("seed " + seed.getName() + " already known, continuing.");
                    continue;
                }
                // 1. use lucene index to search for term in corpus
                List<TextualReference> detectedContexts = this.getContextsForSeed(seed.getName());

                contexts_currentIteration.addAll(detectedContexts);
				extractedContextsFromSeeds.addAll(detectedContexts);

                seed.setTextualReferences(detectedContexts);
                processedSeeds.put(seed.getName(), seed);
                addedSeeds.add(seed.getName());

                //info(log, "Extracted contexts of seed.");
                debug(log, "Extracted contexts of seed.");
                // 2. generate patterns
                if (getExecution().getBootstrapStrategy() == BootstrapStrategy.separate) {
                	debug(log, "--- Entering Pattern Induction phase ---");//info
                	List<InfolisPattern> candidates = inducePatterns(detectedContexts);
                	debug(log, "Pattern Induction completed.");//info
                    debug(log, "--- Entering Pattern Selection phase ---");//info
                    //newPatterns.addAll(ranker.getBestPatterns(candidates, detectedContexts, processedPatterns, new HashSet<Entity>()));
                    newPatterns.addAll(ranker.getBestPatterns(candidates, processedPatterns, new HashSet<Entity>()));
                }
            }
            // mergeNew and mergeCurrent have different contexts_currentIteration at this point, with previously processed seeds filtered for mergeNew but not for mergeCurrent
            if (getExecution().getBootstrapStrategy() == BootstrapStrategy.mergeCurrent
            		|| getExecution().getBootstrapStrategy() == BootstrapStrategy.mergeNew) {
            	debug(log, "--- Entering Pattern Induction phase ---");//info
            	List<InfolisPattern> candidates = inducePatterns(contexts_currentIteration);
            	debug(log, "Pattern Induction completed.");//info
            	debug(log, "--- Entering Pattern Selection phase ---");//info
                //newPatterns.addAll(ranker.getBestPatterns(candidates, contexts_currentIteration, processedPatterns, new HashSet<Entity>()));
                newPatterns.addAll(ranker.getBestPatterns(candidates, processedPatterns, new HashSet<Entity>()));
            }

            if (getExecution().getBootstrapStrategy() == BootstrapStrategy.mergeAll) {
            	debug(log, "--- Entering Pattern Induction phase ---");//info
            	List<InfolisPattern> candidates = inducePatterns(extractedContextsFromSeeds);
            	debug(log, "Pattern Induction completed.");//info
            	debug(log, "--- Entering Pattern Selection phase ---");//info
                //newPatterns.addAll(ranker.getBestPatterns(candidates, extractedContextsFromSeeds, processedPatterns, new HashSet<Entity>()));
                newPatterns.addAll(ranker.getBestPatterns(candidates, processedPatterns, new HashSet<Entity>()));
            }

            // POST the patterns
            getOutputDataStoreClient().post(InfolisPattern.class, newPatterns);
            for (InfolisPattern pattern : newPatterns) {
            	processedPatterns.add(pattern.getPatternRegex());
            }

            debug(log, "Pattern Selection completed.");//info
            debug(log, "Selected " + newPatterns.size() + " new patterns");
            debug(log, "--- Entering Instance Extraction phase ---");//info

            // 3. search for patterns in corpus
            if (!newPatterns.isEmpty()) {
	            //List<String> res = this.getContextsForPatterns(newPatterns);
            	List<String> res = this.getContextsForPatterns(newPatterns, this.getOutputDataStoreClient());
	            for (TextualReference studyContext : getInputDataStoreClient().get(TextualReference.class, res)) {
	            	extractedContextsFromPatterns.add(studyContext);
	            	Entity entity = new Entity(studyContext.getReference());
	            	entity.setTags(studyContext.getTags());
	            	entity.setEntityType(EntityType.citedData);

	            	// an entity is just as reliable as the textual reference it was extracted from
	            	try {
	            		entity.setReliability(studyContext.getReliability());
	            	} catch (NullPointerException npe) {
	            		log.debug("Cannot set reliability of entity: textual reference's reliability score is null");
	            	}
	            	
	            	newSeedsIteration.add(entity);
	            	newSeedTermsIteration.add(studyContext.getReference());
	            }
            }
            debug(log, String.format("Found %s seeds in current iteration (%s occurrences): %s)", newSeedTermsIteration.size(), newSeedsIteration.size(), newSeedTermsIteration));
            numIter++;

            if (newSeedTermsIteration.isEmpty() || processedSeeds.keySet().containsAll(newSeedTermsIteration)) {
            	debug(log, "No new seeds found in iteration, returning.");
            	// extractedContexts contains all contexts resulting from searching a seed term
            	// extractedContexts_patterns contains all contexts resulting from searching for the induced patterns
            	// thus, return the latter here
            	debug(log, "Final iteration: " + numIter);//info
                log.debug("Final list of instances:  ");
                for (Entity i : processedSeeds.values()) { log.debug(i.getName() + "=" + i.getReliability()); }
            	return extractedContextsFromPatterns;
            }
        }
        debug(log, "Maximum number of iterations reached, returning.");

        debug(log, "Final iteration: " + numIter);//info
        log.debug("Final list of instances:  ");
        for (Entity i : processedSeeds.values()) { log.debug(i.getName() + "=" + i.getReliability()); }
        return extractedContextsFromPatterns;
    }

    private List<InfolisPattern> inducePatterns(Collection<TextualReference> contexts) {
    	List<InfolisPattern> patterns = new ArrayList<>();
    	int n = 0;
    	double threshold = getExecution().getReliabilityThreshold();
    	for (TextualReference context : contexts) {
    		n++;
    		log.debug("Inducing patterns for context " + n + " of " + contexts.size());
    		Double[] thresholds = {threshold, threshold - 0.02, threshold - 0.04, threshold - 0.06, threshold - 0.08, threshold - 0.02, threshold - 0.04, threshold - 0.06, threshold - 0.08};
    		patterns.addAll(getPatternInducer().induce(context, thresholds));
    	}
    	return patterns;
    }

    class FrequencyPatternRanker extends Bootstrapping.PatternRanker {

    	protected Set<InfolisPattern> getBestPatterns(List<InfolisPattern> candidates, List<String> processedRegex, Set<Entity> reliableSeeds) {
	        Set<InfolisPattern> patterns = new HashSet<>();
	        Set<String> processedRegex_iteration = new HashSet<>();
	        // constraint for patterns: at least one component not be a stopword
	        // prevent induction of patterns less general than already known patterns:
	        // check whether pattern is known before continuing
	        int acceptedPatterns = 0;
		    for (int candidateNo = 0; candidateNo < candidates.size(); candidateNo++) {
		    	InfolisPattern candidate = candidates.get(candidateNo);
		    	int candidateNoInCurrentContext = candidateNo % getPatternInducer().getPatternsPerContext();
		    	int remainingCandidatesForContext = getPatternInducer().getPatternsPerContext() - candidateNoInCurrentContext;
		    	log.debug(String.format("Processing candidate no. %s (context no. %s)", String.valueOf(candidateNo),
		    			String.valueOf( ((candidateNo - remainingCandidatesForContext) / getPatternInducer().getPatternsPerContext()))));
		      	log.debug("Checking if pattern is relevant: " + candidate.getPatternRegex());
		      	if (processedRegex.contains(candidate.getPatternRegex()) || processedRegex_iteration.contains(candidate.getPatternRegex())) {
		            log.debug("Pattern already known, continuing with candidates for next context.");
		            // skip less general pattern candidates of the same context
		            candidateNo += remainingCandidatesForContext -1;
		            continue;
		        }
		      	
		        boolean nonStopwordPresent = false;
		        double relevance = 0;
		        try {
		        	relevance = computeRelevance(candidate, candidates);
		        } catch (NullPointerException npe) {}
		        
		        for (String word : candidate.getWords()) {
		         	if (!RegexUtils.isStopword(word)) {
		          		nonStopwordPresent = true;
		           		continue;
		           	}
		        }
 
		        if (!nonStopwordPresent) {
		        	log.debug("Pattern rejected - stopwords only");
		        	if (acceptedPatterns == 1) {
		        		candidateNo += remainingCandidatesForContext -1;
		           		acceptedPatterns = 0;
		           		continue;
		        	}
		        }
		        
		        else if (nonStopwordPresent && isRelevant(candidate, relevance)) {
		        	candidate.setTags(getExecution().getTags());
		        	candidate.setReliability(relevance);
		          	patterns.add(candidate);
		           	processedRegex_iteration.add(candidate.getPatternRegex());
		           	log.debug("Pattern accepted");
		           	
		           	if (acceptedPatterns == 1) {
		           		candidateNo += remainingCandidatesForContext -1;
		           		acceptedPatterns = 0;
		           		continue;
		           	}
		           	// omit only less general patterns, do not limit generation of patterns on same level
		           	else {
		           		acceptedPatterns ++;
		           	}
		           	
		        }
		        else {
		        	log.debug("Pattern rejected - not relevant");
		        	if (acceptedPatterns == 1) {
		        		candidateNo += remainingCandidatesForContext -1;
		           		acceptedPatterns = 0;
		           		continue;
		           	}
		        }
    		}
	        return patterns;
	    }

    	private double computeRelevance(int count, int size, int minCount) {
    		double score = 0.0;
    		
    		if (count >= minCount) {
    			//TODO make configurable
    			int norm = 1;
	    		score = ((double) count / size) * norm;
    		}
    		
    		log.debug("Relevance score: " + score);
    		log.debug("Occurrences: " + count);
    		log.debug("Size: " + size);
    		return score;
    	}
    	
    	private double computeRelevance(InfolisPattern pattern, List<InfolisPattern> candidateList) {
    		//TODO make configurable
    		int minCount = 0;
    		int count = 0;
    		for (InfolisPattern candidateP : candidateList) {
    			if (pattern.getPatternRegex().equals(candidateP.getPatternRegex())) count++;
    		}
    		return computeRelevance(count, candidateList.size() / getPatternInducer().getPatternsPerContext(), minCount);
    	}

    	private boolean isRelevant(InfolisPattern pattern, double score) {
    		return (score >= pattern.getThreshold());
    	}
    }

}
