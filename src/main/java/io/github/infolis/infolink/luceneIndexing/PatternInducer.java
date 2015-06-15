
package io.github.infolis.infolink.luceneIndexing;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.RegexUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 * @author domi
 */
public class PatternInducer {
    
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(PatternInducer.class);
	public List<InfolisPattern> candidates = new ArrayList<>();
	
	public PatternInducer(StudyContext context, Double[] thresholds) {
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
//		String luceneQueryA_flex = "\"" + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + "\""; 
//		String regexA_flex_quoted = rightWords_quoted.get(0) + "\\s" + rightWords_quoted.get(1); 
        //String regex_ngramA_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
        // phrase consisting of 3 words behind study title + fixed word before
        String luceneQueryB = "\"" + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + " " + rightWords_lucene.get(2) + "\"";
        String regexB_quoted = leftWords_quoted.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_quoted.get(0) + "\\s" + rightWords_quoted.get(1) + "\\s" + rightWords_quoted.get(2);
        String regexB_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + rightWords_regex.get(2) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;

        // TODO needed?
//		String luceneQueryB_flex = "\"" + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + " " + rightWords_lucene.get(2) + "\""; 
//		String regexB_flex_quoted = rightWords_quoted.get(0) + "\\s" + rightWords_quoted.get(1) + "\\s" + rightWords_quoted.get(2);
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
        
        InfolisPattern type1 = new InfolisPattern(regex1_normalizedAndQuoted, luceneQuery1, regex1_quoted, new ArrayList<String>(Arrays.asList(
        		leftWords.get(windowSize - 1), rightWords.get(0))), thresholds[0]);
        InfolisPattern type2 = new InfolisPattern(regex2_normalizedAndQuoted, luceneQuery2, regex2_quoted, new ArrayList<String>(Arrays.asList(
        		leftWords.get(windowSize - 2), leftWords.get(windowSize - 1), rightWords.get(0))), thresholds[1]);
        InfolisPattern type3 = new InfolisPattern(regex3_normalizedAndQuoted, luceneQuery3, regex3_quoted, new ArrayList<String>(Arrays.asList(
        		leftWords.get(windowSize - 3), leftWords.get(windowSize - 2), leftWords.get(windowSize - 1), rightWords.get(0))), thresholds[2]);
        InfolisPattern type4 = new InfolisPattern(regex4_normalizedAndQuoted, luceneQuery4, regex4_quoted, new ArrayList<String>(Arrays.asList(
        		leftWords.get(windowSize - 4), leftWords.get(windowSize - 3), leftWords.get(windowSize - 2), leftWords.get(windowSize - 1), 
        		rightWords.get(0))), thresholds[3]);
        InfolisPattern type5 = new InfolisPattern(regex5_normalizedAndQuoted, luceneQuery5, regex5_quoted, new ArrayList<String>(Arrays.asList(
        		leftWords.get(windowSize - 5), leftWords.get(windowSize - 4), leftWords.get(windowSize - 3), leftWords.get(windowSize - 2), 
        		leftWords.get(windowSize - 1), rightWords.get(0))), thresholds[4]);
        InfolisPattern typeA = new InfolisPattern(regexA_normalizedAndQuoted, luceneQueryA, regexA_quoted, new ArrayList<String>(Arrays.asList(
        		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1))), thresholds[5]);
        InfolisPattern typeB = new InfolisPattern(regexB_normalizedAndQuoted, luceneQueryB, regexB_quoted, new ArrayList<String>(Arrays.asList(
        		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1), rightWords.get(2))), thresholds[6]);
        InfolisPattern typeC = new InfolisPattern(regexC_normalizedAndQuoted, luceneQueryC, regexC_quoted, new ArrayList<String>(Arrays.asList(
        		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1), rightWords.get(2), rightWords.get(3))), thresholds[7]);
        InfolisPattern typeD = new InfolisPattern(regexD_normalizedAndQuoted, luceneQueryD, regexD_quoted, new ArrayList<String>(Arrays.asList(
        		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1), rightWords.get(2), rightWords.get(3), rightWords.get(4))), 
        		thresholds[8]);
        this.candidates.addAll(Arrays.asList(type1, type2, type3, type4, type5, typeA, typeB, typeC, typeD));
	}
	
	
	
    /**
     * Analyse contexts and induce relevant patterns given the specified
     * threshold.
     *
     * @param contexts
     * @param threshold
     * @param processedPattern
     * @return Set of created Infolis Patterns
     */
    public static Set<InfolisPattern> inducePatterns(List<StudyContext> contexts, double threshold, List<String> processedMinimals) {
        Set<InfolisPattern> patterns = new HashSet<>();
        Set<String> processedMinimals_iteration = new HashSet<>();
        List<String> allContextStrings_iteration = StudyContext.getContextStrings(contexts);
        int n = 0;
        for (StudyContext context : contexts) {
        	n++;
        	log.debug("Inducing relevant patterns for context " + n + " of " + contexts.size());
        	Double[] thresholds = {threshold, threshold - 0.02, threshold - 0.04, threshold - 0.06, threshold - 0.08, threshold - 0.02, threshold - 0.04, threshold - 0.06, threshold - 0.08};
        	PatternInducer inducer = new PatternInducer(context, thresholds);
            
            for (InfolisPattern candidate : inducer.candidates) {
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
    
}

