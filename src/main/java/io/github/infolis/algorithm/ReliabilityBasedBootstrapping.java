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
        Set<String> reliableInstances = new HashSet<>();
        Set<String> reliablePatterns = new HashSet<>();
        //key: seed, value: set of contexts
        Map<String, Set<TextualReference>> contexts_seeds_all = new HashMap<>();
        PatternRanker patternRanker = new PatternRanker();
        int numIter = 1;
        Set<String> seeds = new HashSet<>();
        seeds.addAll(getExecution().getTerms());
        this.r.setSeedInstances(seeds);
        Map<String, Double> lastTopK = new HashMap<>();
        Set<TextualReference> contextsOfReliablePatterns = new HashSet<>();

        // start bootstrapping
        while (numIter < getExecution().getMaxIterations()) {

            log.info("Bootstrapping... Iteration: " + numIter);
            log.debug("Current reliable instances:  " + reliableInstances);
            log.debug("Current top patterns: " + lastTopK);

            // reset list of reliable patterns and contexts found in this iteration
            Map<String, Double> reliableMinimals_iteration = new HashMap<>();
            List<TextualReference> contextsOfReliablePatterns_iteration = new ArrayList<>();
            Set<TextualReference> contexts_seeds_iteration = new HashSet<>();

            // add seeds selected in last iteration to list of reliable instances
            reliableInstances.addAll(seeds);

            // start iteration:
            // 1. search for all seeds and save contexts
            for (String seed : seeds) {

                log.info("Bootstrapping with seed \"" + seed + "\"");
                // after searching for initial seeds, contexts are already stored because 
                // they have to be extracted for computing seed reliability
                if (contexts_seeds_all.containsKey(seed)) {
                    contexts_seeds_iteration.addAll(contexts_seeds_all.get(seed));
                } else {
                    Set<TextualReference> contexts_seed = new HashSet<>();
                    for (String sC : getContextsForSeed(seed)) {
                        TextualReference context = this.getOutputDataStoreClient().get(TextualReference.class, sC);
                        contexts_seed.add(context);
                        contexts_seeds_iteration.add(context);
                    }
                    contexts_seeds_all.put(seed, contexts_seed);
                }
            }

            log.info("Extracted contexts of all seeds.");
            log.info("--- Entering Pattern Induction phase ---");

            // Pattern Induction 
            double threshold = getExecution().getThreshold();
            Double[] thresholds = new Double[9];
            // use equal threshold for all candidates
            Arrays.fill(thresholds, threshold);
            List<List<InfolisPattern>> candidatePatterns = constructCandidates(contexts_seeds_iteration, thresholds);

            log.info("Pattern Induction completed.");
            log.info("--- Entering Pattern Selection phase ---");

            // Pattern Ranking/Selection
            // 2. get reliable patterns, save their data to inducer.minimal_context_map and 
            // new seeds to seeds
            reliableMinimals_iteration = patternRanker.getReliablePatternData(candidatePatterns, reliableInstances, contexts_seeds_all);
            reliablePatterns.addAll(reliableMinimals_iteration.keySet());

            for (String minimal : reliableMinimals_iteration.keySet()) {
                List<String> reliableContexts = patternRanker.minimal_context_map.get(minimal);
                // resolve URIs and save all contexts of current iteration, needed to get new seeds
                for (String contextURI : reliableContexts) {
                    contextsOfReliablePatterns_iteration.add(getOutputDataStoreClient().get(TextualReference.class, contextURI));
                }
            }
            contextsOfReliablePatterns.addAll(contextsOfReliablePatterns_iteration);

            log.info("Pattern Selection completed.");
            log.info("--- Entering Instance Extraction phase ---");

            // Instance Extraction: filter seeds, select only reliable ones
            seeds = new HashSet<>();

            // get list of new instances from contextsOfReliablePatterns_iteration
            // compute reliability of all instances 
            Set<String> newInstanceNames = new HashSet<>();
            for (TextualReference sC : contextsOfReliablePatterns_iteration) {
                String newInstanceName = sC.getTerm();
                if (!reliableInstances.contains(newInstanceName)) {
                    newInstanceNames.add(newInstanceName);
                    log.debug("Found new instance: " + newInstanceName);
                }
            }
            for (String newInstanceName : newInstanceNames) {
                Instance newInstance = new Instance(newInstanceName);
                // counts of instances are required for computation of pmi
                Set<TextualReference> contexts_seed = new HashSet<>();
                for (String sC : getContextsForSeed(newInstanceName)) {
                    TextualReference context = this.getOutputDataStoreClient().get(TextualReference.class, sC);
                    contexts_seed.add(context);
                }
                contexts_seeds_all.put(newInstanceName, contexts_seed);

                if (newInstance.isReliable(contextsOfReliablePatterns, getExecution().getInputFiles().size(), reliablePatterns, contexts_seeds_all, r, getExecution().getThreshold())) {
                    seeds.add(newInstanceName);
                }
            }

            for (Instance i : r.getInstances()) {
                log.debug("stored instance: " + i.getName());
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

        List<String> topKContextURIs = new ArrayList<>();
        for (String minimal : patternRanker.topK.keySet()) {
            topKContextURIs.addAll(patternRanker.minimal_context_map.get(minimal));
        }
        log.info("Final iteration: " + numIter);
        log.debug("Final reliable instances:  " + reliableInstances);
        log.debug("Final top patterns: " + patternRanker.topK);
        return removeUnreliableInstances(getStudyContexts(topKContextURIs), reliableInstances);
    }

    private List<TextualReference> removeUnreliableInstances(List<TextualReference> contexts, Set<String> reliableInstances) {
        List<TextualReference> res = new ArrayList<>();
        for (TextualReference context : contexts) {
            if (reliableInstances.contains(context.getTerm())) {
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
        execution.setThreshold(getExecution().getThreshold());
        execution.instantiateAlgorithm(getOutputDataStoreClient(), getOutputFileResolver()).run();
        return execution.getStudyContexts();
    }

    /**
     * Resolves list of study context URIs and returns list of corresponding
     * studyContexts.
     *
     * @param URIs	list of study context URIs
     * @return	list of corresponding study contexts
     */
    private List<TextualReference> getStudyContexts(List<String> URIs) {
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
    private List<List<InfolisPattern>> constructCandidates(Set<TextualReference> contexts, Double[] thresholds) {
        List<List<InfolisPattern>> candidateList = new ArrayList<>();
        for (TextualReference context : contexts) {
            candidateList.add(new PatternInducer(context, thresholds).candidates);
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

        private Map<String, List<String>> minimal_context_map = new HashMap<>();
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
            execution_pa.getPattern().add(pattern.getUri());
            execution_pa.setAlgorithm(PatternApplier.class);
            execution_pa.getInputFiles().addAll(getExecution().getInputFiles());
            Algorithm algo = execution_pa.instantiateAlgorithm(getInputDataStoreClient(), getOutputDataStoreClient(), getInputFileResolver(), getOutputFileResolver());
            algo.run();
            return execution_pa.getStudyContexts();
        }

        //TODO correct docstring
        /**
         * Ranks patterns and saves their contexts.
         *
         * @param contexts_seeds all	contexts extracted through term search of
         * seeds
         * @param relInstances	set of reliable instances
         * @param r	Reliability instance storing known associations between
         * patterns and instances
         * @return	List of top k patterns
         * @throws IOException
         * @throws ParseException
         */
        private Map<String, Double> getReliablePatternData(List<List<InfolisPattern>> candidatesPerContext, Set<String> relInstances, Map<String, Set<TextualReference>> contexts_seeds_all) throws IOException, ParseException {
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

                    List<String> contexts_pattern;
                    // compute reliability again for patterns known from previous iterations - scores may change
                    if (this.minimal_context_map.containsKey(candidate.getMinimal())) {
                        contexts_pattern = this.minimal_context_map.get(candidate.getMinimal());
                    } // even potentially unreliable candidates need a URI for extraction of contexts
                    else {
                        getOutputDataStoreClient().post(InfolisPattern.class, candidate);
                        contexts_pattern = extractContexts(candidate);
                        this.minimal_context_map.put(candidate.getMinimal(), contexts_pattern);
                    }

                    // Pattern Ranking / Selection
                    if (candidate.isReliable(contexts_pattern, size, relInstances, contexts_seeds_all, r)) {
                        //TODO: k as param...
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
                        this.topK = getTopK(removeBelowThreshold(this.reliableMinimals, getExecution().getThreshold()), 100);
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
            this.getExecution().getStudyContexts().add(sC.getUri());
            this.getExecution().getPattern().add(sC.getPattern());
        }

        getExecution().setStatus(ExecutionStatus.FINISHED);
    }

    @Override
    public void validate() {
        //TODO: warn when standard values are used (threshold, maxIterations not specified)
        //TODO: warn when superfluous parameters are specified
        //TODO: BaseAlgorithm: bootstrapStrategy wrong in case of r. based bootstrapping...
        if (null == this.getExecution().getTerms()
                || this.getExecution().getTerms().isEmpty()) {
            throw new IllegalArgumentException("Must set at least one term as seed!");
        }
        if (null == this.getExecution().getInputFiles()
                || this.getExecution().getInputFiles().isEmpty()) {
            throw new IllegalArgumentException("Must set at least one input file!");
        }
    }

}
