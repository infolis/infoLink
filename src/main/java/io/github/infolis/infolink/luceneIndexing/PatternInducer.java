
package io.github.infolis.infolink.luceneIndexing;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.github.infolis.infolink.patternLearner.Reliability;
import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.RegexUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.lucene.queryParser.ParseException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 * @author domi
 */
public class PatternInducer {
    
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(PatternInducer.class);
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

        for (StudyContext context : contexts) {
        	log.debug("Processing next context");
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
            		leftWords.get(windowSize - 2), leftWords.get(windowSize - 1), rightWords.get(0))), threshold - 0.02);
            InfolisPattern type3 = new InfolisPattern(regex3_normalizedAndQuoted, luceneQuery3, regex3_quoted, new ArrayList<String>(Arrays.asList(
            		leftWords.get(windowSize - 3), leftWords.get(windowSize - 2), leftWords.get(windowSize - 1), rightWords.get(0))), threshold - 0.04);
            InfolisPattern type4 = new InfolisPattern(regex4_normalizedAndQuoted, luceneQuery4, regex4_quoted, new ArrayList<String>(Arrays.asList(
            		leftWords.get(windowSize - 4), leftWords.get(windowSize - 3), leftWords.get(windowSize - 2), leftWords.get(windowSize - 1), 
            		rightWords.get(0))), threshold - 0.06);
            InfolisPattern type5 = new InfolisPattern(regex5_normalizedAndQuoted, luceneQuery5, regex5_quoted, new ArrayList<String>(Arrays.asList(
            		leftWords.get(windowSize - 5), leftWords.get(windowSize - 4), leftWords.get(windowSize - 3), leftWords.get(windowSize - 2), 
            		leftWords.get(windowSize - 1), rightWords.get(0))), threshold - 0.08);
            InfolisPattern typeA = new InfolisPattern(regexA_normalizedAndQuoted, luceneQueryA, regexA_quoted, new ArrayList<String>(Arrays.asList(
            		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1))), threshold - 0.02);
            InfolisPattern typeB = new InfolisPattern(regexB_normalizedAndQuoted, luceneQueryB, regexB_quoted, new ArrayList<String>(Arrays.asList(
            		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1), rightWords.get(2))), threshold - 0.04);
            InfolisPattern typeC = new InfolisPattern(regexC_normalizedAndQuoted, luceneQueryC, regexC_quoted, new ArrayList<String>(Arrays.asList(
            		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1), rightWords.get(2), rightWords.get(3))), threshold - 0.06);
            InfolisPattern typeD = new InfolisPattern(regexD_normalizedAndQuoted, luceneQueryD, regexD_quoted, new ArrayList<String>(Arrays.asList(
            		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1), rightWords.get(2), rightWords.get(3), rightWords.get(4))), 
            		threshold - 0.08);
            candidates.addAll(Arrays.asList(type1, type2, type3, type4, type5, typeA, typeB, typeC, typeD));
            
            for (InfolisPattern candidate : candidates) {
            	log.debug("Checking if pattern is relevant: " + candidate.getMinimal());
            	if (processedMinimals.contains(candidate.getMinimal()) | processedMinimals_iteration.contains(candidate.getMinimal())) {
            		// no need to induce less general patterns, continue with next context
            		//TODO: separate number and character patterns: omit only less general patterns of the same type, do not limit generation of other type
            		//TODO: also store unsuccessful patterns to avoid multiple computations of their score?
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
            		//TODO: separate number and character patterns: omit only less general patterns of the same type, do not limit generation of other type
            		break;
            	}
            } 
        }
        return patterns;
    }
    
        /**
     * Generates extraction patterns, computes their reliability and saves
     * contexts extracted by reliable patterns
     *
     * @param filenames_arff	training files containing dataset references, basis
     * for pattern generation
     * @param threshold	threshold for pattern reliability
     * @return	...
     */
    public static Set<InfolisPattern> saveReliablePatternData(Set<StudyContext> contexts, double threshold, Set<String> processedPattern, int size, Set<String> relInstances, Reliability r) throws IOException, ParseException {
        int n = 0;
        Set<InfolisPattern> reliablePatterns_iteration = new HashSet<>();
        for (StudyContext context : contexts) {
            n++;
            System.out.println("Inducing relevant patterns for instance " + n + " of " + contexts.size() + " for " + " \"" + context.getTerm() + "\"");

            String attVal0 = context.getLeftWords().get(0); //l5
            String attVal1 = context.getLeftWords().get(1); //l4
            String attVal2 = context.getLeftWords().get(2); //l3
            String attVal3 = context.getLeftWords().get(3); //l2
            String attVal4 = context.getLeftWords().get(4); //l1
            String attVal5 = context.getRightWords().get(0); //r1
            String attVal6 = context.getRightWords().get(1); //r2
            String attVal7 = context.getRightWords().get(2); //r3
            String attVal8 = context.getRightWords().get(3); //r4
            String attVal9 = context.getRightWords().get(4); //r5

            //TODO: CONSTRUCT LUCENE QUERIES ONLY WHEN NEEDED (BELOW) 
            String attVal0_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal0);
            String attVal1_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal1);
            String attVal2_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal2);
            String attVal3_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal3);
            String attVal4_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal4);
            String attVal5_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal5);
            String attVal6_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal6);
            String attVal7_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal7);
            String attVal8_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal8);
            String attVal9_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal9);

            String attVal0_quoted = Pattern.quote(attVal0);
            String attVal1_quoted = Pattern.quote(attVal1);
            String attVal2_quoted = Pattern.quote(attVal2);
            String attVal3_quoted = Pattern.quote(attVal3);
            String attVal4_quoted = Pattern.quote(attVal4);
            String attVal5_quoted = Pattern.quote(attVal5);
            String attVal6_quoted = Pattern.quote(attVal6);
            String attVal7_quoted = Pattern.quote(attVal7);
            String attVal8_quoted = Pattern.quote(attVal8);
            String attVal9_quoted = Pattern.quote(attVal9);

            String attVal4_regex = RegexUtils.normalizeAndEscapeRegex(attVal4);
            String attVal5_regex = RegexUtils.normalizeAndEscapeRegex(attVal5);

            //...
            if (attVal4.matches(".*\\P{Punct}")) {
                attVal4_quoted += "\\s";
                attVal4_regex += "\\s";
            }
            if (attVal5.matches("\\P{Punct}.*")) {
                attVal5_quoted = "\\s" + attVal5_quoted;
                attVal5_regex = "\\s" + attVal5_regex;
            }

            // two words enclosing study name
            String luceneQuery1 = "\"" + attVal4_lucene + " * " + attVal5_lucene + "\"";
            String regex_ngram1_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
            String regex_ngram1_minimal = attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex;

            // phrase consisting of 2 words behind study title + fixed word before
            String luceneQueryA = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + "\"";
            String regex_ngramA_normalizedAndQuoted = RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
            String regex_ngramA_minimal = attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6);

            // phrase consisting of 2 words behind study title + (any) word found in data before
            // (any word cause this pattern is induced each time for each different instance having this phrase...)
            // TODO needed?
//			String luceneQueryA_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + "\""; 
//			String regex_ngramA_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted; 
            //String regex_ngramA_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
            // phrase consisting of 3 words behind study title + fixed word before
            String luceneQueryB = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + "\"";
            String regex_ngramB_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
            String regex_ngramB_minimal = attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7);
			// TODO needed?
//			String luceneQueryB_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + "\""; 
//			String regex_ngramB_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted;
            //String regex_ngramB_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;

            //phrase consisting of 4 words behind study title + fixed word before
            String luceneQueryC = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + "\"";
            String regex_ngramC_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal8) + "\\s" + RegexUtils.lastWordRegex;
            String regex_ngramC_minimal = attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal8);

            // TODO needed?
//			String luceneQueryC_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + "\""; 
//			String regex_ngramC_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted + "\\s" + attVal8_quoted;
            //String regex_ngramC_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal8) + "\\s" + RegexUtils.lastWordRegex;
            //phrase consisting of 5 words behind study title + fixed word before
            String luceneQueryD = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + " " + attVal9_lucene + "\"";
            String regex_ngramD_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal8) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal9);
            String regex_ngramD_minimal = attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal8) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal9);

            // now the pattern can emerge at other positions, too, and is counted here as relevant...
            // TODO needed?
//			String luceneQueryD_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + " " + attVal9_lucene + "\""; 
//			String regex_ngramD_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted + "\\s" + attVal8_quoted + "\\s" + attVal9_quoted;
            //String regex_ngramD_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal8) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal9);
            // phrase consisting of 2 words before study title + fixed word behind
            String luceneQuery2 = "\"" + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
            // TODO needed?
//			String regex_ngram2_quoted = attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_quoted;
            String regex_ngram2_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
            String regex_ngram2_minimal = RegexUtils.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex;

            // TODO needed?
//			String luceneQuery2_flex = "\"" + attVal3_lucene + " " + attVal4_lucene + "\""; 
//			String regex_ngram2_flex_quoted = attVal3_quoted + "\\s" + attVal4_quoted;
            //String regex_ngram2_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize-2) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
            // phrase consisting of 3 words before study title + fixed word behind
            String luceneQuery3 = "\"" + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
            // TODO needed?
//			String regex_ngram3_quoted = attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_quoted;
            String regex_ngram3_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal2) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
            String regex_ngram3_minimal = RegexUtils.normalizeAndEscapeRegex(attVal2) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex;

            // TODO needed?
//			String luceneQuery3_flex = "\"" + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + "\""; 
//			String regex_ngram3_flex_quoted = attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted;
            //String regex_ngram3_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize-3) + "\\s" + leftWords_regex.get(windowSize-2) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
            //phrase consisting of 4 words before study title + fixed word behind
            String luceneQuery4 = "\"" + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
            // TODO needed?
//			String regex_ngram4_quoted = attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_quoted;
            String regex_ngram4_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal1) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal2) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
            String regex_ngram4_minimal = RegexUtils.normalizeAndEscapeRegex(attVal1) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal2) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex;
			// TODO needed?
//			String luceneQuery4_flex = "\"" + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + "\"";
//			String regex_ngram4_flex_quoted = attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted;
            //String regex_ngram4_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize-4) + "\\s" + leftWords_regex.get(windowSize-3) + "\\s" + leftWords_regex.get(windowSize-2) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;

            // phrase consisting of 5 words before study title + fixed word behind
            String luceneQuery5 = "\"" + attVal0_lucene + " " + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
            // TODO needed?
//			String regex_ngram5_quoted = attVal0_quoted + "\\s" + attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted+ "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_quoted;
            String regex_ngram5_normalizedAndQuoted = RegexUtils.normalizeAndEscapeRegex(attVal0) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal1) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal2) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
            String regex_ngram5_minimal = RegexUtils.normalizeAndEscapeRegex(attVal0) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal1) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal2) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex;

            // TODO needed?
//			String luceneQuery5_flex = "\"" + attVal0_lucene + " " + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + "\"";
//			String regex_ngram5_flex_quoted = attVal0_quoted + "\\s" + attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted;
            //String regex_ngram5_flex_normalizedAndQuoted = leftWords_regex.get(windowsize-5) + "\\s" + leftWords_regex.get(windowSize-4) + "\\s" + leftWords_regex.get(windowSize-3) + "\\s" + leftWords_regex.get(windowSize-2) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
            // constraint for ngrams: at least one component not be a stopword
            //TODO: CHECK DOCSTRINGS: ORDER CORRECT?
            // first entry: luceneQuery; second entry: normalized and quoted version; third entry: minimal version (for reliability checks...)
            InfolisPattern newPat = new InfolisPattern();
            // prevent induction of patterns less general than already known patterns:
            // check whether pattern is known before continuing
            // also improves performance
            // use pmi scores that are already stored... only compute reliability again, max may have changed
            if (processedPattern.contains(regex_ngram1_normalizedAndQuoted)) {
                continue;
            }
            newPat.setLuceneQuery(luceneQuery1);
            newPat.setPatternRegex(regex_ngram1_normalizedAndQuoted);
            newPat.setMinimal(regex_ngram1_minimal);
            if (!(RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5))) {
                if (!newPat.isReliable(threshold, size, relInstances, contexts, r).isEmpty()) {
                    reliablePatterns_iteration.add(newPat);
                    continue;
                }
            }
            //TODO: do not return here, instead process Type phrase behind study title terms also!
            if (processedPattern.contains(regex_ngram2_normalizedAndQuoted)) {
                continue;
            }
            newPat.setLuceneQuery(luceneQuery2);
            newPat.setPatternRegex(regex_ngram2_normalizedAndQuoted);
            newPat.setMinimal(regex_ngram2_minimal);
            if (!((RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5)) | (RegexUtils.isStopword(attVal3) & RegexUtils.isStopword(attVal5)) | (RegexUtils.isStopword(attVal3) & RegexUtils.isStopword(attVal4)))) {
                if (!newPat.isReliable(threshold, size, relInstances, contexts, r).isEmpty()) {
                    reliablePatterns_iteration.add(newPat);
                    continue;
                }
            }

            if (processedPattern.contains(regex_ngram3_normalizedAndQuoted)) {
                continue;
            }
            newPat.setLuceneQuery(luceneQuery3);
            newPat.setPatternRegex(regex_ngram3_normalizedAndQuoted);
            newPat.setMinimal(regex_ngram3_minimal);
            if (!(RegexUtils.isStopword(attVal2) & RegexUtils.isStopword(attVal3) & RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5))) {
                if (!newPat.isReliable(threshold, size, relInstances, contexts, r).isEmpty()) {
                    reliablePatterns_iteration.add(newPat);
                    continue;
                }
            }

            if (processedPattern.contains(regex_ngram4_normalizedAndQuoted)) {
                continue;
            }
            newPat.setLuceneQuery(luceneQuery4);
            newPat.setPatternRegex(regex_ngram4_normalizedAndQuoted);
            newPat.setMinimal(regex_ngram4_minimal);
            if (!(RegexUtils.isStopword(attVal1) & RegexUtils.isStopword(attVal2) & RegexUtils.isStopword(attVal3) & RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5))) {
                if (!newPat.isReliable(threshold, size, relInstances, contexts, r).isEmpty()) {
                    reliablePatterns_iteration.add(newPat);
                    continue;
                }
            }
            if (processedPattern.contains(regex_ngram5_normalizedAndQuoted)) {
                continue;
            }
            newPat.setLuceneQuery(luceneQuery5);
            newPat.setPatternRegex(regex_ngram5_normalizedAndQuoted);
            newPat.setMinimal(regex_ngram5_minimal);
            if (!(RegexUtils.isStopword(attVal0) & RegexUtils.isStopword(attVal1) & RegexUtils.isStopword(attVal2) & RegexUtils.isStopword(attVal3) & RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5))) {
                if (!newPat.isReliable(threshold, size, relInstances, contexts, r).isEmpty()) {
                    reliablePatterns_iteration.add(newPat);
                    continue;
                }
            }

            //...
            if (processedPattern.contains(regex_ngramA_normalizedAndQuoted)) {
                continue;
            }
            newPat.setLuceneQuery(luceneQueryA);
            newPat.setPatternRegex(regex_ngramA_normalizedAndQuoted);
            newPat.setMinimal(regex_ngramA_minimal);
            if (!((RegexUtils.isStopword(attVal5) & RegexUtils.isStopword(attVal6)) | (RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal6)) | (RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5)))) {
                if (!newPat.isReliable(threshold, size, relInstances, contexts, r).isEmpty()) {
                    reliablePatterns_iteration.add(newPat);
                    continue;
                }
            }

            if (processedPattern.contains(regex_ngramB_normalizedAndQuoted)) {
                continue;
            }
            newPat.setLuceneQuery(luceneQueryB);
            newPat.setPatternRegex(regex_ngramB_normalizedAndQuoted);
            newPat.setMinimal(regex_ngramB_minimal);
            if (!(RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5) & RegexUtils.isStopword(attVal6) & RegexUtils.isStopword(attVal7))) {
                if (!newPat.isReliable(threshold, size, relInstances, contexts, r).isEmpty()) {
                    reliablePatterns_iteration.add(newPat);
                    continue;
                }
            }

            if (processedPattern.contains(regex_ngramC_normalizedAndQuoted)) {
                continue;
            }
            newPat.setLuceneQuery(luceneQueryC);
            newPat.setPatternRegex(regex_ngramC_normalizedAndQuoted);
            newPat.setMinimal(regex_ngramC_minimal);
            if (!(RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5) & RegexUtils.isStopword(attVal6) & RegexUtils.isStopword(attVal7) & RegexUtils.isStopword(attVal8))) {
                if (!newPat.isReliable(threshold, size, relInstances, contexts, r).isEmpty()) {
                    reliablePatterns_iteration.add(newPat);
                    continue;
                }
            }

            if (processedPattern.contains(regex_ngramD_normalizedAndQuoted)) {
                continue;
            }
            newPat.setLuceneQuery(luceneQueryD);
            newPat.setPatternRegex(regex_ngramD_normalizedAndQuoted);
            newPat.setMinimal(regex_ngramD_minimal);
            if (!(RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5) & RegexUtils.isStopword(attVal6) & RegexUtils.isStopword(attVal7) & RegexUtils.isStopword(attVal8) & RegexUtils.isStopword(attVal9))) {
                if (!newPat.isReliable(threshold, size, relInstances, contexts, r).isEmpty()) {
                    reliablePatterns_iteration.add(newPat);
                    continue;
                }
            }
        }
        return reliablePatterns_iteration;
    }    
}
