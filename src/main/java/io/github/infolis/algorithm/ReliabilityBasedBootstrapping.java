package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.luceneIndexing.PatternInducer;
import io.github.infolis.infolink.patternLearner.Reliability;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.entity.Instance;
import io.github.infolis.model.TextualReference;
import io.github.infolis.util.RegexUtils;

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

import org.apache.lucene.queryParser.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 */
public class ReliabilityBasedBootstrapping extends BaseAlgorithm {

    public ReliabilityBasedBootstrapping(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(ReliabilityBasedBootstrapping.class);
    private Reliability r = new Reliability();

    private List<TextualReference> bootstrap() throws IOException, ParseException {
        Set<Instance> reliableInstances = new HashSet<>();
        Set<InfolisPattern> reliablePatterns = new HashSet<>();
        PatternRanker patternRanker = new PatternRanker();
        int numIter = 1;
        Set<Instance> seeds = new HashSet<>();
        Set<String> seedTerms = new HashSet<>();
        seedTerms.addAll(getExecution().getSeeds());
        this.r.setSeedTerms(seedTerms); 
        Map<String, Double> lastTopK = new HashMap<>();
        //Set<TextualReference> contextsOfReliablePatterns = new HashSet<>();
        
        // initialize bootstrapping:
        // 1. search for all initial seeds and save contexts
        for (String seed : seedTerms) {
            log.info("Bootstrapping with seed \"" + seed + "\"");
            Instance newSeed = new Instance(seed);
            newSeed.setTextualReferences(getStudyContexts(getContextsForSeed(seed)));
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
                String newInstanceName = sC.getTerm();
                Collection<String> reliableInstanceTerms = new HashSet<>();
                for (Instance i : reliableInstances) { reliableInstanceTerms.add(i.getName()); }
                if (!reliableInstanceTerms.contains(newInstanceName)) {
                    newInstanceNames.add(newInstanceName);
                    log.debug("Found new instance: " + newInstanceName);
                }
            }
            for (String newInstanceName : newInstanceNames) {
                Instance newInstance = new Instance(newInstanceName);
                // counts of instances are required for computation of pmi
                newInstance.setTextualReferences(getStudyContexts(getContextsForSeed(newInstanceName)));
                log.debug("new Instance stored contexts: " + newInstance.getTextualReferences());
                // for computation of reliability, save time nad consider only patterns of this iteration: 
                // if instance had been found by patterns of earlier iterations, it would not be 
                // considered as new instance here
                if (newInstance.isReliable(reliablePatterns_iteration, getExecution().getInputFiles().size(), r, getExecution().getReliabilityThreshold())) {
                    seeds.add(newInstance);
                }
                log.debug("Reliability of instance \"" + newInstanceName + "\": " + newInstance.getReliability());
            }

            for (Instance i : r.getInstances()) {
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
        for (Instance i : reliableInstances) { log.debug(i.getName() + "=" + i.getReliability()); }
        log.debug("Final top patterns: " + patternRanker.topK);
        return removeUnreliableInstances(topContexts, reliableInstances);
    }

    private List<TextualReference> removeUnreliableInstances(Collection<TextualReference> contexts, Set<Instance> reliableInstances) {
    	Set<String> reliableInstanceTerms = new HashSet<String>();
    	for (Instance i : reliableInstances) { reliableInstanceTerms.add(i.getName()); }
        List<TextualReference> res = new ArrayList<>();
        for (TextualReference context : contexts) {
            if (reliableInstanceTerms.contains(context.getTerm())) {
                res.add(context);
            }
        }
        return res;
    }

    private List<String> getContextsForSeed(String seed) {
        // use lucene index to search for term in corpus
        Execution execution = new Execution();
        execution.setAlgorithm(SearchTermPosition.class);
        execution.setSearchTerm(seed);
        execution.setSearchQuery(RegexUtils.normalizeQuery(seed, true));
        execution.setInputFiles(getExecution().getInputFiles());
        execution.setReliabilityThreshold(getExecution().getReliabilityThreshold());
        Algorithm algo = execution.instantiateAlgorithm(getInputDataStoreClient(), getOutputDataStoreClient(), getInputFileResolver(), getOutputFileResolver());
        algo.run();
        return execution.getTextualReferences();
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
    private List<List<InfolisPattern>> constructCandidates(Collection<Instance> instances, Double[] thresholds) {
        List<List<InfolisPattern>> candidateList = new ArrayList<>();
        for (Instance i : instances) { 
        	for (TextualReference context : i.getTextualReferences()) {
        		candidateList.add(new PatternInducer(context, thresholds).candidates);
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
    private class PatternRanker {
    	//TODO custom comparator for entities..
    	private Map<String,InfolisPattern> knownPatterns = new HashMap<>();
        private Map<Double, Collection<String>> reliableMinimals = new HashMap<>();
        private Map<String, Double> topK = new HashMap<>();

        /**
         * Calls PatternApplier to extract all contexts of this pattern.
         *
         * @param pattern
         * @return
         */
        private List<String> extractContexts(InfolisPattern pattern) {
            Execution execution_pa = new Execution();
            execution_pa.getPatterns().add(pattern.getUri());
            execution_pa.setAlgorithm(PatternApplier.class);
            execution_pa.setUpperCaseConstraint(getExecution().isUpperCaseConstraint());
            execution_pa.getInputFiles().addAll(getExecution().getInputFiles());
            Algorithm algo = execution_pa.instantiateAlgorithm(getInputDataStoreClient(), getOutputDataStoreClient(), getInputFileResolver(), getOutputFileResolver());
            algo.run();
            return execution_pa.getTextualReferences();
        }

        /**
         * 
         * @param candidatesPerContext
         * @param relInstances
         * @return
         * @throws IOException
         * @throws ParseException
         */
        private Map<String, Double> getReliablePatterns(List<List<InfolisPattern>> candidatesPerContext, Set<Instance> relInstances) throws IOException, ParseException {
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
                        getOutputDataStoreClient().post(InfolisPattern.class, candidate);
                        candidate.setTextualReferences(getStudyContexts(extractContexts(candidate)));
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

    @Override
    public void execute() throws IOException {
        List<TextualReference> detectedContexts = new ArrayList<>();
        try {
            detectedContexts = bootstrap();
        } catch (IOException | ParseException ex) {
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

    @Override
    public void validate() {
        //TODO: warn when standard values are used (threshold, maxIterations not specified)
        //TODO: warn when superfluous parameters are specified
        //TODO: BaseAlgorithm: bootstrapStrategy wrong in case of r. based bootstrapping...
        if (null == this.getExecution().getSeeds()
                || this.getExecution().getSeeds().isEmpty()) {
            throw new IllegalArgumentException("Must set at least one term as seed!");
        }
        if (null == this.getExecution().getInputFiles()
                || this.getExecution().getInputFiles().isEmpty()) {
            throw new IllegalArgumentException("Must set at least one input file!");
        }
    }

}
