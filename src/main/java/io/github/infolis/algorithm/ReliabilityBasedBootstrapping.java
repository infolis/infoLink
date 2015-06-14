package io.github.infolis.algorithm;

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
import java.util.regex.Pattern;

import org.apache.lucene.queryParser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 *
 * @author kata
 * @author domi
 */
public class ReliabilityBasedBootstrapping extends BaseAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(ReliabilityBasedBootstrapping.class);

    private List<StudyContext> bootstrapReliabilityBased() throws IOException, ParseException {
        Set<String> reliableInstances = new HashSet<>();
        Set<StudyContext> contexts_seeds_all = new HashSet<>();
        List<StudyContext> contextsOfReliablePatterns = new ArrayList<>();
        PatternInducer inducer = new PatternInducer();
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
                execution.instantiateAlgorithm(getDataStoreClient(), getFileResolver()).run();
                  
                for (String sC : execution.getStudyContexts()) {
                    StudyContext context = this.getDataStoreClient().get(StudyContext.class, sC);
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
                		contextsOfReliablePatterns_iteration.add(getDataStoreClient().get(StudyContext.class, contextURI));
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
    
    class PatternInducer {
    	
    	Map<String,List<String>> minimal_context_map = new HashMap<>();
    	List<String> reliableMinimals = new ArrayList<>();
    	//Map<String, Double> patternScoreMap = new HashMap<>();
    	
    	PatternInducer() { }
    	
    	/**
         * Calls PatternApplier to extract all contexts of this pattern.
         * 
         * @param parentExecution
         * @param client
         * @param resolver
         * @return
         */
        List<String> extractContexts(InfolisPattern pattern) {
        	Execution execution_pa = new Execution();   
        	execution_pa.getPattern().add(pattern.getUri());
        	execution_pa.setAlgorithm(PatternApplier.class);
        	execution_pa.getInputFiles().addAll(getExecution().getInputFiles());
        	Algorithm algo = new PatternApplier();
        	algo.setDataStoreClient(getDataStoreClient());
        	algo.setFileResolver(getFileResolver());
        	algo.setExecution(execution_pa);
        	algo.run();
        	return execution_pa.getStudyContexts();
        }
    	
        List<String> getReliablePatternData(Set<StudyContext> contexts, Set<String> relInstances, Reliability r) throws IOException, ParseException {
	    	double threshold = getExecution().getThreshold();
	    	int size = getExecution().getInputFiles().size();
	    	List<String> processedMinimals_iteration = new ArrayList<>();
	    	List<String> new_reliableMinimals = new ArrayList<>();
	        // new list of reliable contexts and patterns after this iteration (patterns may be removed from list of most reliable patterns)
	        //Map<String, Double> new_reliableMinimals = new HashMap<>();
	        int n = 0;
	        for (StudyContext context : contexts) {
	        	n++;
	        	log.debug("Inducing relevant patterns for context " + n + " of " + contexts.size());
	            List<String> leftWords = context.getLeftWords();
	            List<String> rightWords = context.getRightWords();
	
	            Function<String, String> normalizeAndEscape_lucene
	                    = new Function<String, String>() {
	                        public String apply(String s) {
	                            return RegexUtils.normalizeAndEscapeRegex_lucene(s);
	                        }
	                    };
	
	            Function<String, String> pattern_quote
	                    = new Function<String, String>() {
	                        public String apply(String s) {
	                            return Pattern.quote(s);
	                        }
	                    };
	
	            Function<String, String> regex_escape
	                    = new Function<String, String>() {
	                        public String apply(String s) {
	                            return RegexUtils.normalizeAndEscapeRegex(s);
	                        }
	                    };
	            //apply normalizeAndEscape_lucene method on all words of the context
	            List<String> leftWords_lucene = new ArrayList<>(Lists.transform(leftWords, normalizeAndEscape_lucene));
	            List<String> rightWords_lucene = new ArrayList<>(Lists.transform(rightWords, normalizeAndEscape_lucene));
	            List<String> leftWords_quoted = new ArrayList<>(Lists.transform(leftWords, pattern_quote));
	            List<String> rightWords_quoted = new ArrayList<>(Lists.transform(rightWords, pattern_quote));
	            List<String> leftWords_regex = new ArrayList<>(Lists.transform(leftWords, regex_escape));
	            List<String> rightWords_regex = new ArrayList<>(Lists.transform(rightWords, regex_escape));
	
	            int windowSize = leftWords.size();
	            String directNeighbourLeft = leftWords.get(windowSize - 1);
	            String directNeighbourRight = rightWords.get(0);
	
	            //directly adjacent words may appear without being separated by whitespace iff those words consist of punctuation marks
	            if (directNeighbourLeft.matches(".*\\P{Punct}")) {
	                leftWords_quoted.set(windowSize - 1, leftWords_quoted.get(windowSize - 1) + "\\s");
	                leftWords_regex.set(windowSize - 1, leftWords_regex.get(windowSize - 1) + "\\s");
	            }
	            if (directNeighbourRight.matches("\\P{Punct}.*")) {
	                rightWords_quoted.set(0, "\\s" + rightWords_quoted.get(0));
	                rightWords_regex.set(0, "\\s" + rightWords_regex.get(0));
	            }
	
	            // construct all allowed patterns
	            //TODO: atomic regex...?
	            // most general pattern: two words enclosing study name
	            String luceneQuery1 = "\"" + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + "\"";
	            String regex1_quoted = leftWords_quoted.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_quoted.get(0);
	            String regex1_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	            // phrase consisting of 2 words behind study title + fixed word before
	            String luceneQueryA = "\"" + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + "\"";
	            String regexA_quoted = regex1_quoted + "\\s" + rightWords_quoted.get(1);
	            String regexA_normalizedAndQuoted = RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	            // phrase consisting of 2 words behind study title + (any) word found in data before!
	            // (any word cause this pattern is induced each time for each different instance having this phrase...)
	            // TODO needed?
	//			String luceneQueryA_flex = "\"" + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + "\""; 
	//			String regexA_flex_quoted = rightWords_quoted.get(0) + "\\s" + rightWords_quoted.get(1); 
	            //String regex_ngramA_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	            // phrase consisting of 3 words behind study title + fixed word before
	            String luceneQueryB = "\"" + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + " " + rightWords_lucene.get(2) + "\"";
	            String regexB_quoted = leftWords_quoted.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_quoted.get(0) + "\\s" + rightWords_quoted.get(1) + "\\s" + rightWords_quoted.get(2);
	            String regexB_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + rightWords_regex.get(2) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	            // TODO needed?
	//			String luceneQueryB_flex = "\"" + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + " " + rightWords_lucene.get(2) + "\""; 
	//			String regexB_flex_quoted = rightWords_quoted.get(0) + "\\s" + rightWords_quoted.get(1) + "\\s" + rightWords_quoted.get(2);
	            //String regex_ngramB_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + rightWords_regex.get(2) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	            // phrase consisting of 4 words behind study title + fixed word before
	            String luceneQueryC = "\"" + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + " " + rightWords_lucene.get(2) + " " + rightWords_lucene.get(3) + "\"";
	            String regexC_quoted = leftWords_quoted.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_quoted.get(0) + "\\s" + rightWords_quoted.get(1) + "\\s" + rightWords_quoted.get(2) + "\\s" + rightWords_quoted.get(3);
	            String regexC_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + rightWords_regex.get(2) + "\\s" + rightWords_regex.get(3) + "\\s" + RegexUtils.lastWordRegex;
	
	            String luceneQueryC_flex = "\"" + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + " " + rightWords_lucene.get(2) + " " + rightWords_lucene.get(3) + "\"";
	            String regexC_flex_quoted = rightWords_quoted.get(0) + "\\s" + rightWords_quoted.get(1) + "\\s" + rightWords_quoted.get(2) + "\\s" + rightWords_quoted.get(3);
				//String regex_ngramC_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + rightWords_regex.get(2) + "\\s" + rightWords_regex.get(3) + "\\s" + RegexUtils.lastWordRegex;
	
	            // phrase consisting of 5 words behind study title + fixed word before
	            String luceneQueryD = "\"" + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + " " + rightWords_lucene.get(2) + " " + rightWords_lucene.get(3) + " " + rightWords_lucene.get(4) + "\"";
	            String regexD_quoted = leftWords_quoted.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_quoted.get(0) + "\\s" + rightWords_quoted.get(1) + "\\s" + rightWords_quoted.get(2) + "\\s" + rightWords_quoted.get(3) + "\\s" + rightWords_quoted.get(4);
	            String regexD_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + rightWords_regex.get(2) + "\\s" + rightWords_regex.get(3) + "\\s" + rightWords_regex.get(4);
	
	            // now the pattern can emerge at other positions, too, and is counted here as relevant...
	            String luceneQueryD_flex = "\"" + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + " " + rightWords_lucene.get(2) + " " + rightWords_lucene.get(3) + " " + rightWords_lucene.get(4) + "\"";
	            String regexD_flex_quoted = rightWords_quoted.get(0) + "\\s" + rightWords_quoted.get(1) + "\\s" + rightWords_quoted.get(2) + "\\s" + rightWords_quoted.get(3) + "\\s" + rightWords_quoted.get(4);
				//String regex_ngramD_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + rightWords_regex.get(2) + "\\s" + rightWords_regex.get(3) + "\\s" + rightWords_regex.get(4);
	
	            // phrase consisting of 2 words before study title + fixed word behind
	            String luceneQuery2 = "\"" + leftWords_lucene.get(windowSize - 2) + " " + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + "\"";
	            String regex2_quoted = leftWords_quoted.get(windowSize - 2) + "\\s" + leftWords_quoted.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_quoted.get(0);
	            String regex2_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 2) + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	            String luceneQuery2_flex = "\"" + leftWords_lucene.get(windowSize - 2) + " " + leftWords_lucene.get(windowSize - 1) + "\"";
	            String regex2_flex_quoted = leftWords_quoted.get(windowSize - 2) + "\\s" + leftWords_quoted.get(windowSize - 1);
				//String regex_ngram2_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize-2) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	            // phrase consisting of 3 words before study title + fixed word behind
	            String luceneQuery3 = "\"" + leftWords_lucene.get(windowSize - 3) + " " + leftWords_lucene.get(windowSize - 2) + " " + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + "\"";
	            String regex3_quoted = leftWords_quoted.get(windowSize - 3) + "\\s" + leftWords_quoted.get(windowSize - 2) + "\\s" + leftWords_quoted.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_quoted.get(0);
	            String regex3_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 3) + "\\s" + leftWords_regex.get(windowSize - 2) + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	            String luceneQuery3_flex = "\"" + leftWords_lucene.get(windowSize - 3) + " " + leftWords_lucene.get(windowSize - 2) + " " + leftWords_lucene.get(windowSize - 1) + "\"";
	            String regex3_flex_quoted = leftWords_quoted.get(windowSize - 3) + "\\s" + leftWords_quoted.get(windowSize - 2) + "\\s" + leftWords_quoted.get(windowSize - 1);
				//String regex_ngram3_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize-3) + "\\s" + leftWords_regex.get(windowSize-2) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	            // phrase consisting of 4 words before study title + fixed word behind
	            String luceneQuery4 = "\"" + leftWords_lucene.get(windowSize - 4) + " " + leftWords_lucene.get(windowSize - 3) + " " + leftWords_lucene.get(windowSize - 2) + " " + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + "\"";
	            String regex4_quoted = leftWords_quoted.get(windowSize - 4) + "\\s" + leftWords_quoted.get(windowSize - 3) + "\\s" + leftWords_quoted.get(windowSize - 2) + "\\s" + leftWords_quoted.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_quoted.get(0);
	            String regex4_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 4) + "\\s" + leftWords_regex.get(windowSize - 3) + "\\s" + leftWords_regex.get(windowSize - 2) + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	            String luceneQuery4_flex = "\"" + leftWords_lucene.get(windowSize - 4) + " " + leftWords_lucene.get(windowSize - 3) + " " + leftWords_lucene.get(windowSize - 2) + " " + leftWords_lucene.get(windowSize - 1) + "\"";
	            String regex4_flex_quoted = leftWords_quoted.get(windowSize - 4) + "\\s" + leftWords_quoted.get(windowSize - 3) + "\\s" + leftWords_quoted.get(windowSize - 2) + "\\s" + leftWords_quoted.get(windowSize - 1);
				//String regex_ngram4_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize-4) + "\\s" + leftWords_regex.get(windowSize-3) + "\\s" + leftWords_regex.get(windowSize-2) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	            // phrase consisting of 5 words before study title + fixed word behind
	            String luceneQuery5 = "\"" + leftWords_lucene.get(windowSize - 5) + " " + leftWords_lucene.get(windowSize - 4) + " " + leftWords_lucene.get(windowSize - 3) + " " + leftWords_lucene.get(windowSize - 2) + " " + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + "\"";
	            String regex5_quoted = leftWords_quoted.get(windowSize - 5) + "\\s" + leftWords_quoted.get(windowSize - 4) + "\\s" + leftWords_quoted.get(windowSize - 3) + "\\s" + leftWords_quoted.get(windowSize - 2) + "\\s" + leftWords_quoted.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_quoted.get(0);
	            String regex5_normalizedAndQuoted = leftWords_regex.get(windowSize - 5) + "\\s" + leftWords_regex.get(windowSize - 4) + "\\s" + leftWords_regex.get(windowSize - 3) + "\\s" + leftWords_regex.get(windowSize - 2) + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	            String luceneQuery5_flex = "\"" + leftWords_lucene.get(windowSize - 5) + " " + leftWords_lucene.get(windowSize - 4) + " " + leftWords_lucene.get(windowSize - 3) + " " + leftWords_lucene.get(windowSize - 2) + " " + leftWords_lucene.get(windowSize - 1) + "\"";
	            String regex5_flex_quoted = leftWords_quoted.get(windowSize - 5) + "\\s" + leftWords_quoted.get(windowSize - 4) + "\\s" + leftWords_quoted.get(windowSize - 3) + "\\s" + leftWords_quoted.get(windowSize - 2) + "\\s" + leftWords_quoted.get(windowSize - 1);
				//String regex_ngram5_flex_normalizedAndQuoted = leftWords_regex.get(windowSize-5) + "\\s" + leftWords_regex.get(windowSize-4) + "\\s" + leftWords_regex.get(windowSize-3) + "\\s" + leftWords_regex.get(windowSize-2) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	            // constraint for patterns: at least one component not be a stopword
	            // prevent induction of patterns less general than already known patterns:
	            // check whether pattern is known before continuing
	            
	            List<InfolisPattern> candidates = new ArrayList<>();
	            InfolisPattern type1 = new InfolisPattern(regex1_normalizedAndQuoted, luceneQuery1, regex1_quoted, new ArrayList<String>(Arrays.asList(
	            		leftWords.get(windowSize - 1), rightWords.get(0))), threshold);
	            InfolisPattern type2 = new InfolisPattern(regex2_normalizedAndQuoted, luceneQuery2, regex2_quoted, new ArrayList<String>(Arrays.asList(
	            		leftWords.get(windowSize - 2), leftWords.get(windowSize - 1), rightWords.get(0))), threshold);
	            InfolisPattern type3 = new InfolisPattern(regex3_normalizedAndQuoted, luceneQuery3, regex3_quoted, new ArrayList<String>(Arrays.asList(
	            		leftWords.get(windowSize - 3), leftWords.get(windowSize - 2), leftWords.get(windowSize - 1), rightWords.get(0))), threshold);
	            InfolisPattern type4 = new InfolisPattern(regex4_normalizedAndQuoted, luceneQuery4, regex4_quoted, new ArrayList<String>(Arrays.asList(
	            		leftWords.get(windowSize - 4), leftWords.get(windowSize - 3), leftWords.get(windowSize - 2), leftWords.get(windowSize - 1), 
	            		rightWords.get(0))), threshold);
	            InfolisPattern type5 = new InfolisPattern(regex5_normalizedAndQuoted, luceneQuery5, regex5_quoted, new ArrayList<String>(Arrays.asList(
	            		leftWords.get(windowSize - 5), leftWords.get(windowSize - 4), leftWords.get(windowSize - 3), leftWords.get(windowSize - 2), 
	            		leftWords.get(windowSize - 1), rightWords.get(0))), threshold);
	            InfolisPattern typeA = new InfolisPattern(regexA_normalizedAndQuoted, luceneQueryA, regexA_quoted, new ArrayList<String>(Arrays.asList(
	            		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1))), threshold);
	            InfolisPattern typeB = new InfolisPattern(regexB_normalizedAndQuoted, luceneQueryB, regexB_quoted, new ArrayList<String>(Arrays.asList(
	            		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1), rightWords.get(2))), threshold);
	            InfolisPattern typeC = new InfolisPattern(regexC_normalizedAndQuoted, luceneQueryC, regexC_quoted, new ArrayList<String>(Arrays.asList(
	            		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1), rightWords.get(2), rightWords.get(3))), threshold);
	            InfolisPattern typeD = new InfolisPattern(regexD_normalizedAndQuoted, luceneQueryD, regexD_quoted, new ArrayList<String>(Arrays.asList(
	            		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1), rightWords.get(2), rightWords.get(3), rightWords.get(4))), 
	            		threshold);
	            candidates.addAll(Arrays.asList(type1, type2, type3, type4, type5, typeA, typeB, typeC, typeD));
	            
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
	            		getDataStoreClient().post(InfolisPattern.class, candidate);
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
            getDataStoreClient().post(StudyContext.class, sC);
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
