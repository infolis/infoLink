
package io.github.infolis.infolink.luceneIndexing;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.RegexUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author domi
 */
public class PatternInducer {
    
    /**
     * Analyse contexts and induce relevant patterns given the specified
     * threshold.
     *
     * @param contexts
     * @param threshold
     */
    @SuppressWarnings("unused")
    public static Set<InfolisPattern> inducePatterns(List<StudyContext> contexts, double threshold, List<InfolisPattern> processedPattern) {
        Set<InfolisPattern> patterns = new HashSet<>();
        Set<InfolisPattern> processedPatterns_iteration = new HashSet<>();
        List<String> allContextStrings_iteration = StudyContext.getContextStrings(contexts);

        for (StudyContext context : contexts) {

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
            List<String> leftWords_lucene = new ArrayList<String>(Lists.transform(leftWords, normalizeAndEscape_lucene));
            List<String> rightWords_lucene = new ArrayList<String>(Lists.transform(rightWords, normalizeAndEscape_lucene));
            List<String> leftWords_quoted = new ArrayList<String>(Lists.transform(leftWords, pattern_quote));
            List<String> rightWords_quoted = new ArrayList<String>(Lists.transform(rightWords, pattern_quote));
            List<String> leftWords_regex = new ArrayList<String>(Lists.transform(leftWords, regex_escape));
            List<String> rightWords_regex = new ArrayList<String>(Lists.transform(rightWords, regex_escape));

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
            // also improves performance
            //TODO: check for all patterns whether already found in current iteration
            if (processedPattern.contains(regex1_normalizedAndQuoted) | processedPatterns_iteration.contains(regex1_normalizedAndQuoted)) {
                continue;
            }
            if (!(RegexUtils.isStopword(leftWords.get(windowSize - 1)) & RegexUtils.isStopword(rightWords.get(0))) & InfolisPattern.isRelevant(regex1_quoted, allContextStrings_iteration, threshold))//0.2
            {
                // substitute normalized numbers etc. with regex
                InfolisPattern newPat = new InfolisPattern();
                newPat.setLuceneQuery(luceneQuery1);
                newPat.setPatternRegex(regex1_normalizedAndQuoted);
                patterns.add(newPat);
                processedPatterns_iteration.add(newPat);
                System.out.println("found relevant type 1 pattern (most general): " + regex1_normalizedAndQuoted);
                continue;
            }
            //TODO: do not return here, instead process Type phrase behind study title terms also"
            if (processedPattern.contains(regex2_normalizedAndQuoted)) {
                continue;
            }
            if (!(RegexUtils.isStopword(leftWords.get(windowSize - 1)) & RegexUtils.isStopword(rightWords.get(0)) | RegexUtils.isStopword(leftWords.get(windowSize - 2)) & RegexUtils.isStopword(rightWords.get(0)) | RegexUtils.isStopword(leftWords.get(windowSize - 2)) & RegexUtils.isStopword(leftWords.get(windowSize - 1))) & InfolisPattern.isRelevant(regex2_quoted, allContextStrings_iteration, threshold - 0.02))//0.18
            {
                System.out.println("found relevant type 2 pattern: " + regex2_normalizedAndQuoted);
                InfolisPattern newPat = new InfolisPattern();
                newPat.setLuceneQuery(luceneQuery2);
                newPat.setPatternRegex(regex2_normalizedAndQuoted);
                patterns.add(newPat);
                continue;
            }
            if (processedPattern.contains(regex3_normalizedAndQuoted)) {
                continue;
            }
            if (!(RegexUtils.isStopword(leftWords.get(windowSize - 3)) & RegexUtils.isStopword(leftWords.get(windowSize - 2)) & RegexUtils.isStopword(leftWords.get(windowSize - 1)) & RegexUtils.isStopword(rightWords.get(0))) & InfolisPattern.isRelevant(regex3_quoted, allContextStrings_iteration, threshold - 0.04))//0.16
            {
                System.out.println("found relevant type 3 pattern: " + regex3_normalizedAndQuoted);
                InfolisPattern newPat = new InfolisPattern();
                newPat.setLuceneQuery(luceneQuery3);
                newPat.setPatternRegex(regex3_normalizedAndQuoted);
                patterns.add(newPat);
                continue;
            }
            if (processedPattern.contains(regex4_normalizedAndQuoted)) {
                continue;
            }
            if (!(RegexUtils.isStopword(leftWords.get(windowSize - 4)) & RegexUtils.isStopword(leftWords.get(windowSize - 3)) & RegexUtils.isStopword(leftWords.get(windowSize - 2)) & RegexUtils.isStopword(leftWords.get(windowSize - 1)) & RegexUtils.isStopword(rightWords.get(0))) & InfolisPattern.isRelevant(regex4_quoted, allContextStrings_iteration, threshold - 0.06))//0.14
            {
                System.out.println("found relevant type 4 pattern: " + regex4_normalizedAndQuoted);
                InfolisPattern newPat = new InfolisPattern();
                newPat.setLuceneQuery(luceneQuery4);
                newPat.setPatternRegex(regex4_normalizedAndQuoted);
                patterns.add(newPat);
                continue;
            }
            if (processedPattern.contains(regex5_normalizedAndQuoted)) {
                continue;
            }
            if (!(RegexUtils.isStopword(leftWords.get(windowSize - 5)) & RegexUtils.isStopword(leftWords.get(windowSize - 4)) & RegexUtils.isStopword(leftWords.get(windowSize - 3)) & RegexUtils.isStopword(leftWords.get(windowSize - 2)) & RegexUtils.isStopword(leftWords.get(windowSize - 1)) & RegexUtils.isStopword(rightWords.get(0))) & InfolisPattern.isRelevant(regex5_quoted, allContextStrings_iteration, threshold - 0.08))//0.12
            {
                System.out.println("found relevant type 5 pattern: " + regex5_normalizedAndQuoted);
                InfolisPattern newPat = new InfolisPattern();
                newPat.setLuceneQuery(luceneQuery5);
                newPat.setPatternRegex(regex5_normalizedAndQuoted);
                patterns.add(newPat);
                continue;
            }

            if (processedPattern.contains(regexA_normalizedAndQuoted)) {
                continue;
            }
            if (!(RegexUtils.isStopword(rightWords.get(0)) & RegexUtils.isStopword(rightWords.get(1)) | RegexUtils.isStopword(leftWords.get(windowSize - 1)) & RegexUtils.isStopword(rightWords.get(1)) | RegexUtils.isStopword(leftWords.get(windowSize - 1)) & RegexUtils.isStopword(rightWords.get(0))) & InfolisPattern.isRelevant(regexA_quoted, allContextStrings_iteration, threshold - 0 - 02))//0.18
            {
                System.out.println("found relevant type A pattern: " + regexA_normalizedAndQuoted);
                InfolisPattern newPat = new InfolisPattern();
                newPat.setLuceneQuery(luceneQueryA);
                newPat.setPatternRegex(regexA_normalizedAndQuoted);
                patterns.add(newPat);
                continue;
            }
            if (processedPattern.contains(regexB_normalizedAndQuoted)) {
                continue;
            }
            if (!(RegexUtils.isStopword(leftWords.get(windowSize - 1)) & RegexUtils.isStopword(rightWords.get(0)) & RegexUtils.isStopword(rightWords.get(1)) & RegexUtils.isStopword(rightWords.get(2))) & InfolisPattern.isRelevant(regexB_quoted, allContextStrings_iteration, threshold - 0.04))//0.16
            {
                System.out.println("found relevant type B pattern: " + regexB_normalizedAndQuoted);
                InfolisPattern newPat = new InfolisPattern();
                newPat.setLuceneQuery(luceneQueryB);
                newPat.setPatternRegex(regexB_normalizedAndQuoted);
                patterns.add(newPat);
                continue;
            }
            if (processedPattern.contains(regexC_normalizedAndQuoted)) {
                continue;
            }
            if (!(RegexUtils.isStopword(leftWords.get(windowSize - 1)) & RegexUtils.isStopword(rightWords.get(0)) & RegexUtils.isStopword(rightWords.get(1)) & RegexUtils.isStopword(rightWords.get(2)) & RegexUtils.isStopword(rightWords.get(3))) & InfolisPattern.isRelevant(regexC_quoted, allContextStrings_iteration, threshold - 0.06))//0.14
            {
                System.out.println("found relevant type C pattern: " + regexC_normalizedAndQuoted);
                InfolisPattern newPat = new InfolisPattern();
                newPat.setLuceneQuery(luceneQueryC);
                newPat.setPatternRegex(regexC_normalizedAndQuoted);
                patterns.add(newPat);
                continue;
            }
            if (processedPattern.contains(regexD_normalizedAndQuoted)) {
                continue;
            }
            if (!(RegexUtils.isStopword(leftWords.get(windowSize - 1)) & RegexUtils.isStopword(rightWords.get(0)) & RegexUtils.isStopword(rightWords.get(1)) & RegexUtils.isStopword(rightWords.get(2)) & RegexUtils.isStopword(rightWords.get(3)) & RegexUtils.isStopword(rightWords.get(4))) & InfolisPattern.isRelevant(regexD_quoted, allContextStrings_iteration, threshold - 0.08))//0.12
            {
                System.out.println("found relevant type D pattern: " + regexD_normalizedAndQuoted);
                InfolisPattern newPat = new InfolisPattern();
                newPat.setLuceneQuery(luceneQueryD);
                newPat.setPatternRegex(regexD_normalizedAndQuoted);
                patterns.add(newPat);
                processedPatterns_iteration.add(newPat);
                continue;
            }
        }
        return patterns;
    }
    
}
