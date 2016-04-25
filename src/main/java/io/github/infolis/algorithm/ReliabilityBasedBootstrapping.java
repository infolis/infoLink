package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.patternLearner.Reliability;
import io.github.infolis.infolink.patternLearner.StandardPatternInducer;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 */
public class ReliabilityBasedBootstrapping extends Bootstrapping {

    public ReliabilityBasedBootstrapping(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) throws IOException {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(ReliabilityBasedBootstrapping.class);
    private Reliability r = new Reliability();
    
    public PatternInducer getPatternInducer() {
    	return new StandardPatternInducer();
    }

    public PatternRanker getPatternRanker() {
    	return new ReliabilityPatternRanker();
    }
    
    public List<TextualReference> bootstrap() throws IOException, ParseException {
        Set<Entity> reliableInstances = new HashSet<>();
        Set<InfolisPattern> reliablePatterns = new HashSet<>();
        ReliabilityPatternRanker patternRanker = new ReliabilityPatternRanker();
        //TODO define and use generic PatternRanker
        //PatternRanker patternRanker = getPatternRanker();
        int numIter = 1;
        Set<Entity> seeds = new HashSet<>();
        Set<String> seedTerms = new HashSet<>();
        seedTerms.addAll(getExecution().getSeeds());
        this.r.setSeedTerms(seedTerms); 
        Map<String, Double> lastTopK = new HashMap<>();
        //Set<TextualReference> contextsOfReliablePatterns = new HashSet<>();
        
        // initialize bootstrapping:
        // 1. search for all initial seeds and save contexts
        for (String seed : seedTerms) {
            log.info("Bootstrapping with seed \"" + seed + "\"");
            Entity newSeed = new Entity(seed);
            newSeed.setTextualReferences(this.getContextsForSeed(seed));
            newSeed.setIsSeed();
            seeds.add(newSeed);
        }
        log.info("Extracted contexts of all seeds.");
        log.info("--- Entering Pattern Induction phase ---");

        // start bootstrapping
        while (numIter < getExecution().getMaxIterations()) {

            log.info("Bootstrapping... Iteration: " + numIter);
            log.debug("Current reliable instances:  " + reliableInstances);
            log.debug("Current top patterns: " + lastTopK);

            // add seeds selected in last iteration to list of reliable instances
            reliableInstances.addAll(seeds);
            
            // delete cache of reliability scores as they may change with new evidence of new iterations
            r.deleteScoreCache();

            // Pattern Induction 
            double threshold = getExecution().getReliabilityThreshold();
            Double[] thresholds = new Double[9];
            // use equal threshold for all candidates
            Arrays.fill(thresholds, threshold);
            List<List<InfolisPattern>> candidatePatterns = constructCandidates(seeds, thresholds);

            log.info("Pattern Induction completed.");
            log.info("--- Entering Pattern Selection phase ---");

            // Pattern Ranking/Selection
            // 2. get reliable patterns along with their textual references
            
            // reset list of reliable patterns found in this iteration
            Collection<InfolisPattern> reliablePatterns_iteration = new HashSet<>();
            Map<String, Double> reliableMinimals_iteration = patternRanker.getReliablePatterns(candidatePatterns, reliableInstances);
            for (String relMinimal : reliableMinimals_iteration.keySet()) {
            	reliablePatterns.add(patternRanker.knownPatterns.get(relMinimal));
            	reliablePatterns_iteration.add(patternRanker.knownPatterns.get(relMinimal));
            }

            log.info("Pattern Selection completed.");
            log.info("--- Entering Instance Extraction phase ---");

            // Instance Extraction: filter seeds, select only reliable ones
            seeds = new HashSet<>();

            // get list of new instances from textual references of reliablePatterns_iteration
            // compute reliability of all instances 
            Collection<TextualReference> reliableContexts_iteration = new ArrayList<>();
            for (InfolisPattern reliablePattern : reliablePatterns_iteration) reliableContexts_iteration.addAll(reliablePattern.getTextualReferences());
            Set<String> newInstanceNames = new HashSet<>();
            for (TextualReference sC : reliableContexts_iteration) {
                String newInstanceName = sC.getReference();
                Collection<String> reliableInstanceTerms = new HashSet<>();
                for (Entity i : reliableInstances) { reliableInstanceTerms.add(i.getName()); }
                if (!reliableInstanceTerms.contains(newInstanceName)) {
                    newInstanceNames.add(newInstanceName);
                    log.debug("Found new instance: " + newInstanceName);
                }
            }
            for (String newInstanceName : newInstanceNames) {
                Entity newInstance = new Entity(newInstanceName);
                // counts of instances are required for computation of pmi
                newInstance.setTextualReferences(this.getContextsForSeed(newInstanceName));
                log.debug("new Instance stored contexts: " + newInstance.getTextualReferences());
                // for computation of reliability, save time nad consider only patterns of this iteration: 
                // if instance had been found by patterns of earlier iterations, it would not be 
                // considered as new instance here
                if (newInstance.isReliable(reliablePatterns_iteration, getExecution().getInputFiles().size(), r, getExecution().getReliabilityThreshold())) {
                    seeds.add(newInstance);
                }
                log.debug("Reliability of instance \"" + newInstanceName + "\": " + newInstance.getReliability());
            }

            for (Entity i : r.getInstances()) {
                log.debug("stored instance: \"" + i.getName() + "\"=" + i.getReliability());
                log.debug("stored associations: " + i.getAssociations().size());
            }
            for (InfolisPattern p : r.getPatterns()) {
                log.debug("stored pattern: " + p.getPatternRegex());
                log.debug("stored associations: " + p.getAssociations().size());
            }

            // return if pattern set is stable or seed set is empty
            if (patternRanker.topK.equals(lastTopK)) {
                log.debug("pattern set is stable, nothing more to do. Returning.");
                break;
            } else if (seeds.isEmpty()) {
                log.debug("no new seeds, nothing more to do. Returning.");
                break;
            } else {
                lastTopK = patternRanker.topK;
                numIter++;
            }
        }

        Collection<TextualReference> topContexts = new ArrayList<>();
        for (String minimal : patternRanker.topK.keySet()) {
        	InfolisPattern topPattern = patternRanker.knownPatterns.get(minimal);
        	topContexts.addAll(topPattern.getTextualReferences());
        }
        log.info("Final iteration: " + numIter);
        log.debug("Final reliable instances:  ");
        for (Entity i : reliableInstances) { log.debug(i.getName() + "=" + i.getReliability()); }
        log.debug("Final top patterns: " + patternRanker.topK);
        return removeUnreliableInstances(topContexts, reliableInstances);
    }

    private List<TextualReference> removeUnreliableInstances(Collection<TextualReference> contexts, Set<Entity> reliableInstances) {
    	Set<String> reliableInstanceTerms = new HashSet<String>();
    	for (Entity i : reliableInstances) { reliableInstanceTerms.add(i.getName()); }
        List<TextualReference> res = new ArrayList<>();
        for (TextualReference context : contexts) {
            if (reliableInstanceTerms.contains(context.getReference())) {
                res.add(context);
            }
        }
        return res;
    }

    /**
     * Resolves list of study context URIs and returns list of corresponding
     * studyContexts.
     *
     * @param URIs	list of study context URIs
     * @return	list of corresponding study contexts
     */
    private List<TextualReference> getStudyContexts(Collection<String> URIs) {
        List<TextualReference> contexts = new ArrayList<>();
        for (String uri : URIs) {
            contexts.add(getOutputDataStoreClient().get(TextualReference.class, uri));
        }
        return contexts;
    }

    /**
     * Constructs all pattern candidates from context using the specified
     * thresholds for the different kinds of patterns (different generality
     * levels).
     *
     * @param context	context retrieved through term search for seed, basis for
     * pattern induction
     * @param thresholds	thresholds for different generality levels of patterns
     * @return
     */
    private List<List<InfolisPattern>> constructCandidates(Collection<Entity> instances, Double[] thresholds) {
        List<List<InfolisPattern>> candidateList = new ArrayList<>();
        for (Entity i : instances) { 
        	for (TextualReference context : i.getTextualReferences()) {
        		candidateList.add(getPatternInducer().induce(context, thresholds));
        	}
        }
        return candidateList;
    }

    /**
     * Class for pattern ranking and selection.
     *
     * @author kata
     *
     */
    private class ReliabilityPatternRanker extends Bootstrapping.PatternRanker {
    	//TODO custom comparator for entities..
    	private Map<String,InfolisPattern> knownPatterns = new HashMap<>();
        private Map<Double, Collection<String>> reliableMinimals = new HashMap<>();
        private Map<String, Double> topK = new HashMap<>();

        /**
         * 
         * @param candidatesPerContext
         * @param relInstances
         * @return
         * @throws IOException
         * @throws ParseException
         */
        private Map<String, Double> getReliablePatterns(List<List<InfolisPattern>> candidatesPerContext, Set<Entity> relInstances) throws IOException, ParseException {
            int size = getExecution().getInputFiles().size();
            List<String> processedMinimals_iteration = new ArrayList<>();
            for (List<InfolisPattern> candidatesForContext : candidatesPerContext) {

                for (InfolisPattern candidate : candidatesForContext) {
                    log.debug("Checking if pattern is reliable: " + candidate.getMinimal());
	            	// Do not process patterns more than once in one iteration, scores do not change.
                    // Scores may change from iteration to iteration though, thus do not exclude 
                    // patterns already checked in another iteration
                    if (processedMinimals_iteration.contains(candidate.getMinimal())) {
                        log.debug("Pattern already known, continuing.");
                        break; // this prohibits induction of less general patterns
                        //continue; // this prohibits induction of duplicate patterns but allows less general ones
                    }

                    // compute reliability again for patterns known from previous iterations - scores may change
                    if (this.knownPatterns.containsKey(candidate.getMinimal())) {
                    	candidate = this.knownPatterns.get(candidate.getMinimal());
                        //contexts_pattern = candidatePattern.getTextualReferences();
                    } // even potentially unreliable candidates need a URI for extraction of contexts
                    else {
                    	// TODO post temp patterns to temp datastore
                        getOutputDataStoreClient().post(InfolisPattern.class, candidate);
                        // TODO: use on set of candidates instead of on single candidate
                        candidate.setTextualReferences(getStudyContexts(getContextsForPatterns(Arrays.asList(candidate))));
                        this.knownPatterns.put(candidate.getMinimal(), candidate);
                    }

                    // Pattern Ranking / Selection
                    if (candidate.isReliable(size, relInstances, r)) {
                        double candidateReliability = candidate.getReliability();
                        log.debug("Pattern reliable, score: " + candidateReliability);
                        Collection<String> minimalsWithSameScore = new ArrayList<>();
                        if (this.reliableMinimals.containsKey(candidateReliability)) {
                            minimalsWithSameScore = this.reliableMinimals.get(candidateReliability);
                        }
                        minimalsWithSameScore.add(candidate.getMinimal());
                        this.reliableMinimals.put(candidateReliability, minimalsWithSameScore);
	            		// this returns the top k patterns regardless if their score is above the threshold
                        //topK = getTopK(this.reliableMinimals, 5);
                        // this returns all top k patterns above the threshold 
                        //TODO: start with small k and increase with each iteration
                        //TODO: at the same time, decrease thresholds slightly
                        this.topK = getTopK(removeBelowThreshold(this.reliableMinimals, getExecution().getReliabilityThreshold()), 100);
                        processedMinimals_iteration.add(candidate.getMinimal());
                        break; // this prohibits induction of less general patterns 
                        // and equally general pattern of the other type (e.g. candidate2 vs. candidateB)
                        //continue; // this prohibits induction of duplicate patterns but allows less general ones
                    } else {
                        processedMinimals_iteration.add(candidate.getMinimal());
                        log.debug("Pattern unreliable, score: " + candidate.getReliability());
                    }
                }
            }
	        // this returns only the most reliable patterns, not all reliable ones
            // thus, new seeds are generated based on the most reliable patterns only
            return this.topK;
        }
    }

    /**
     * Checks whether score of map entry is below the given threshold.
     *
     * @param item	map entry having score as key
     * @param threshold	threshold for score
     * @return
     */
    static boolean isBelowThreshold(Map.Entry<Double, Collection<String>> item, double threshold) {
        return (item.getKey() < threshold);
    }

    /**
     * Removes all entries from map whose score is below the given threshold.
     *
     * @param patternScoreMap	map with scores as keys
     * @param threshold	threshold for acceptance of entries
     * @return	map containing only entries with scores higher than or equal to
     * threshold
     */
    static Map<Double, Collection<String>> removeBelowThreshold(Map<Double, Collection<String>> patternScoreMap, double threshold) {
        Iterator<Map.Entry<Double, Collection<String>>> iter = patternScoreMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Double, Collection<String>> entry = iter.next();
            if (isBelowThreshold(entry, threshold)) {
                iter.remove();
            }
        }
        return patternScoreMap;
    }

    /**
     * Filters given map and returns only the top k entries.
     *
     * @param patternScoreMap	map with scores as keys and a collection of
     * patterns with this score as values
     * @param k	maximal number of entries to return
     * @return	map of k-best entries having pattern strings as keys and scores
     * as values
     */
    static Map<String, Double> getTopK(Map<Double, Collection<String>> patternScoreMap, int k) {
        Map<String, Double> topK = new HashMap<>();
        List<Double> scores = new ArrayList<>(patternScoreMap.keySet());
        Collections.sort(scores, Collections.reverseOrder());
        int n = 0;
        for (double score : scores.subList(0, Math.min(k, scores.size()))) {
            for (String value : patternScoreMap.get(score)) {
                if (n >= k) {
                    break;
                }
                topK.put(value, score);
                n++;
            }
        }
        return topK;
    }

}
