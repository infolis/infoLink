package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.luceneIndexing.PatternInducer;
import io.github.infolis.infolink.patternLearner.Reliability;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.RegexUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryParser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author kata
 * @author domi
 */
public class ReliabilityBasedBootstrapping extends BaseAlgorithm {

    public ReliabilityBasedBootstrapping(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}

	private static final Logger log = LoggerFactory.getLogger(ReliabilityBasedBootstrapping.class);

    private List<StudyContext> bootstrapReliabilityBased() throws IOException, ParseException {
        Set<String> reliableInstances = new HashSet<>();
        Set<StudyContext> contexts_seeds_all = new HashSet<>();
        List<StudyContext> contextsOfReliablePatterns = new ArrayList<>();
        PatternInducer_reliability inducer = new PatternInducer_reliability();
        int numIter = 1;
        Reliability r = new Reliability();
        Set<String> seeds = new HashSet<>();
        seeds.addAll(getExecution().getTerms());
        //TODO: different stop criterion here!
        while (numIter < getExecution().getMaxIterations()) {
            log.debug("Bootstrapping... Iteration: " + numIter);
            List<String> reliableMinimals_iteration = new ArrayList<String>();
            List<StudyContext> contextsOfReliablePatterns_iteration = new ArrayList<>();
            // 0. filter seeds, select only reliable ones
            // alternatively: use all seeds extracted by reliable patterns
            reliableInstances.addAll(seeds);
            // 1. search for all seeds and save contexts
            for (String seed : seeds) {
            	//TODO: if seed was already searched once, use stored contexts
                log.debug("Bootstrapping with seed " + seed);
                Set<StudyContext> contexts_seed = new HashSet<>();
                // 1. use lucene index to search for term in corpus
                Execution execution = new Execution();
                execution.setAlgorithm(SearchTermPosition.class);
                execution.setSearchTerm(seed);
                execution.setSearchQuery(RegexUtils.normalizeQuery(seed, true));
                execution.setInputFiles(getExecution().getInputFiles());
                execution.setThreshold(getExecution().getThreshold());
                execution.instantiateAlgorithm(this).run();
                  
                for (String sC : execution.getStudyContexts()) {
                    StudyContext context = this.getOutputDataStoreClient().get(StudyContext.class, sC);
                    contexts_seed.add(context);
                    contexts_seeds_all.add(context);
                }
                // PatternInducer.getReliablePatternData must be called for the context of each seed separately
                // 2. get reliable patterns, save their data to this.reliablePatternsAndContexts and 
                // new seeds to this.foundSeeds_iteration
                //TODO: check: contexts having same words must have different URI if not the same position in text
                reliableMinimals_iteration = inducer.getReliablePatternData(contexts_seed, reliableInstances, r);
                for (String minimal : reliableMinimals_iteration) {
                	List<String> reliableContexts = inducer.minimal_context_map.get(minimal);
                	for (String contextURI : reliableContexts) {
                		contextsOfReliablePatterns_iteration.add(getInputDataStoreClient().get(StudyContext.class, contextURI));
                	}
                }
                // TODO: contexts previously deemed as reliable should be removable by getReliablePatternData if pattern is not 
                // deemed reliable anymore
                contextsOfReliablePatterns.addAll(contextsOfReliablePatterns_iteration);
            }
            
            seeds = new HashSet<>();
            for (StudyContext sC : contextsOfReliablePatterns_iteration) seeds.add(sC.getTerm());
            numIter++;
        }
        //TODO: after implementing usage of top-k patterns, use inducer.reliableMinimals instead
        return contextsOfReliablePatterns;
    }
    
    private class PatternInducer_reliability {
    	
    	private Map<String,List<String>> minimal_context_map = new HashMap<>();
    	private List<String> reliableMinimals = new ArrayList<>();
    	//private Map<String, Double> patternScoreMap = new HashMap<>();
    	
    	private List<InfolisPattern> constructCandidates(StudyContext context, Double[] thresholds) {
    		return new PatternInducer(context, thresholds).candidates;
    	}
    	
    	/**
         * Calls PatternApplier to extract all contexts of this pattern.
         * 
         * @param parentExecution
         * @param client
         * @param resolver
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
    	
        private List<String> getReliablePatternData(Set<StudyContext> contexts, Set<String> relInstances, Reliability r) throws IOException, ParseException {
	    	double threshold = getExecution().getThreshold();
	    	Double[] thresholds = new Double[9];
	    	// use equal threshold for all candidates
	    	Arrays.fill(thresholds, threshold);
	    	int size = getExecution().getInputFiles().size();
	    	List<String> processedMinimals_iteration = new ArrayList<>();
	    	List<String> new_reliableMinimals = new ArrayList<>();
	        // new list of reliable contexts and patterns after this iteration (patterns may be removed from list of most reliable patterns)
	        //Map<String, Double> new_reliableMinimals = new HashMap<>();
	        int n = 0;
	        for (StudyContext context : contexts) {
	        	n++;
	        	log.debug("Inducing reliable patterns for context " + n + " of " + contexts.size());
	        	List<InfolisPattern> candidates = constructCandidates(context, thresholds);
	            for (InfolisPattern candidate : candidates) {
	            	log.debug("Checking if pattern is relevant: " + candidate.getMinimal());
	            	if (processedMinimals_iteration.contains(candidate.getMinimal())) {
	            		// no need to induce less general patterns, continue with next context
	            		//TODO (enhancement): separate number and character patterns: omit only less general patterns of the same type, do not limit generation of other type
	            		//TODO (enhancement): also store unsuccessful patterns to avoid multiple computations of their score?
	            		log.debug("Pattern already known, returning.");
	                    break;
	            	}
	
	            	List<String> contexts_pattern;
	            	// compute reliability again for known patterns - scores may change
	            	if (this.minimal_context_map.containsKey(candidate.getMinimal())) {
	            		contexts_pattern = this.minimal_context_map.get(candidate.getMinimal());
	            	}
	            	// candidates need a URI for extraction of contexts even if not deemed reliable in the end
	            	else { 
	            		getOutputDataStoreClient().post(InfolisPattern.class, candidate);
	            		contexts_pattern = extractContexts(candidate); 
	            		this.minimal_context_map.put(candidate.getMinimal(), contexts_pattern);
	            		processedMinimals_iteration.add(candidate.getMinimal());
	            	}
	            	if (candidate.isReliable(contexts_pattern, size, relInstances, contexts, r)) {
	            		//TODO: if not reliable anymore (for known patterns), remove!
		            	//TODO: or rather: implement using top-k patterns (store scores along with patterns...)
		            	//TODO: make sure patterns and contexts are posted etc	
	            		this.reliableMinimals.add(candidate.getMinimal());
	            		new_reliableMinimals.add(candidate.getMinimal());
	            		log.debug("Pattern accepted");
	            		//TODO (enhancement): separate number and character patterns: omit only less general patterns of the same type, do not limit generation of other type
	            		break;
	            	}
	            	
	            } 
	        }
	        return new_reliableMinimals;
        }
	}
    

    @Override
    public void execute() throws IOException {
        List<StudyContext> detectedContexts = new ArrayList<>();
        try {
            detectedContexts = bootstrapReliabilityBased();
        } catch (IOException | ParseException ex) {
            log.error("Could not apply reliability bootstrapping: " + ex);
            getExecution().setStatus(ExecutionStatus.FAILED);
        }

        for (StudyContext sC : detectedContexts) {
            getOutputDataStoreClient().post(StudyContext.class, sC);
            this.getExecution().getStudyContexts().add(sC.getUri());
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
    }

}
