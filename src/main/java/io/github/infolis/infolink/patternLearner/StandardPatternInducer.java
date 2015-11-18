
package io.github.infolis.infolink.patternLearner;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import io.github.infolis.algorithm.Bootstrapping;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.TextualReference;
import io.github.infolis.util.RegexUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * Class for inducing patterns based on given textual references. 
 * Pattern thresholds are set according values specified in thresholds parameter. 
 * 
 * @author kata
 */
public class StandardPatternInducer extends Bootstrapping.PatternInducer {
    
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(StandardPatternInducer.class);
	public final static int patternsPerContext = 9;
	
	public StandardPatternInducer() {}
	
	public final int getPatternsPerContext() {
		return patternsPerContext;
	}
	
	protected List<InfolisPattern> induce(TextualReference context, Double[] thresholds) {

		List<String> leftWords = context.getLeftWords();
        List<String> rightWords = context.getRightWords();

        Function<String, String> normalizeAndEscape_lucene
                = new Function<String, String>() {
                    public String apply(String s) {
                        return RegexUtils.normalizeAndEscapeRegex_lucene(s);
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
        List<String> leftWords_regex = new ArrayList<>(Lists.transform(leftWords, regex_escape));
        List<String> rightWords_regex = new ArrayList<>(Lists.transform(rightWords, regex_escape));

        int windowSize = leftWords.size();
        String directNeighbourLeft = leftWords.get(windowSize - 1);
        String directNeighbourRight = rightWords.get(0);

        try {
	        //directly adjacent words may appear without being separated by whitespace iff those words consist of punctuation marks
	        if (directNeighbourLeft.matches(".*\\P{Punct}")) {
	            leftWords_regex.set(windowSize - 1, leftWords_regex.get(windowSize - 1) + "\\s");
	        }
	        if (directNeighbourRight.matches("\\P{Punct}.*")) {
	            rightWords_regex.set(0, "\\s" + rightWords_regex.get(0));
	        }
	
	        // construct all allowed patterns
	        //TODO: atomic regex...?
	        // most general pattern: two words enclosing study name
	        String luceneQuery1 = "\"" + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + "\"";
	        String minimal1 = leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0);
	        String regex1 = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	        // phrase consisting of 2 words behind study title + fixed word before
	        String luceneQueryB = "\"" + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + "\"";
	        String minimalB = minimal1 + "\\s" + rightWords_regex.get(1);
	        String regexB = RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	        // phrase consisting of 3 words behind study title + fixed word before
	        String luceneQueryC = "\"" + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + " " + rightWords_lucene.get(2) + "\"";
	        String minimalC = leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + rightWords_regex.get(2);
	        String regexC = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + rightWords_regex.get(2) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	        String luceneQueryD = "\"" + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + " " + rightWords_lucene.get(2) + " " + rightWords_lucene.get(3) + "\"";
	        String minimalD = leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + rightWords_regex.get(2) + "\\s" + rightWords_regex.get(3);
	        String regexD = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + rightWords_regex.get(2) + "\\s" + rightWords_regex.get(3) + "\\s" + RegexUtils.lastWordRegex;
	
	        // phrase consisting of 5 words behind study title + fixed word before
	        String luceneQueryE = "\"" + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + " " + rightWords_lucene.get(1) + " " + rightWords_lucene.get(2) + " " + rightWords_lucene.get(3) + " " + rightWords_lucene.get(4) + "\"";
	        String minimalE = leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + rightWords_regex.get(2) + "\\s" + rightWords_regex.get(3) + "\\s" + rightWords_regex.get(4);
	        String regexE = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + rightWords_regex.get(1) + "\\s" + rightWords_regex.get(2) + "\\s" + rightWords_regex.get(3) + "\\s" + rightWords_regex.get(4);
	
	        // phrase consisting of 2 words before study title + fixed word behind
	        String luceneQuery2 = "\"" + leftWords_lucene.get(windowSize - 2) + " " + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + "\"";
	        String minimal2 = leftWords_regex.get(windowSize - 2) + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0);
	        String regex2 = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 2) + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	        // phrase consisting of 3 words before study title + fixed word behind
	        String luceneQuery3 = "\"" + leftWords_lucene.get(windowSize - 3) + " " + leftWords_lucene.get(windowSize - 2) + " " + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + "\"";
	        String minimal3 = leftWords_regex.get(windowSize - 3) + "\\s" + leftWords_regex.get(windowSize - 2) + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0);
	        String regex3 = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 3) + "\\s" + leftWords_regex.get(windowSize - 2) + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	        // phrase consisting of 4 words before study title + fixed word behind
	        String luceneQuery4 = "\"" + leftWords_lucene.get(windowSize - 4) + " " + leftWords_lucene.get(windowSize - 3) + " " + leftWords_lucene.get(windowSize - 2) + " " + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + "\"";
	        String minimal4 = leftWords_regex.get(windowSize - 4) + "\\s" + leftWords_regex.get(windowSize - 3) + "\\s" + leftWords_regex.get(windowSize - 2) + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0);
	        String regex4 = RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize - 4) + "\\s" + leftWords_regex.get(windowSize - 3) + "\\s" + leftWords_regex.get(windowSize - 2) + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	        // phrase consisting of 5 words before study title + fixed word behind
	        String luceneQuery5 = "\"" + leftWords_lucene.get(windowSize - 5) + " " + leftWords_lucene.get(windowSize - 4) + " " + leftWords_lucene.get(windowSize - 3) + " " + leftWords_lucene.get(windowSize - 2) + " " + leftWords_lucene.get(windowSize - 1) + " * " + rightWords_lucene.get(0) + "\"";
	        String minimal5 = leftWords_regex.get(windowSize - 5) + "\\s" + leftWords_regex.get(windowSize - 4) + "\\s" + leftWords_regex.get(windowSize - 3) + "\\s" + leftWords_regex.get(windowSize - 2) + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0);
	        String regex5 = leftWords_regex.get(windowSize - 5) + "\\s" + leftWords_regex.get(windowSize - 4) + "\\s" + leftWords_regex.get(windowSize - 3) + "\\s" + leftWords_regex.get(windowSize - 2) + "\\s" + leftWords_regex.get(windowSize - 1) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + rightWords_regex.get(0) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
	
	        InfolisPattern type_general = new InfolisPattern(regex1, luceneQuery1, minimal1, new ArrayList<String>(Arrays.asList(
	        		leftWords.get(windowSize - 1), rightWords.get(0))), thresholds[0]);
	        InfolisPattern type2 = new InfolisPattern(regex2, luceneQuery2, minimal2, new ArrayList<String>(Arrays.asList(
	        		leftWords.get(windowSize - 2), leftWords.get(windowSize - 1), rightWords.get(0))), thresholds[1]);
	        InfolisPattern type3 = new InfolisPattern(regex3, luceneQuery3, minimal3, new ArrayList<String>(Arrays.asList(
	        		leftWords.get(windowSize - 3), leftWords.get(windowSize - 2), leftWords.get(windowSize - 1), rightWords.get(0))), thresholds[2]);
	        InfolisPattern type4 = new InfolisPattern(regex4, luceneQuery4, minimal4, new ArrayList<String>(Arrays.asList(
	        		leftWords.get(windowSize - 4), leftWords.get(windowSize - 3), leftWords.get(windowSize - 2), leftWords.get(windowSize - 1), 
	        		rightWords.get(0))), thresholds[3]);
	        InfolisPattern type5 = new InfolisPattern(regex5, luceneQuery5, minimal5, new ArrayList<String>(Arrays.asList(
	        		leftWords.get(windowSize - 5), leftWords.get(windowSize - 4), leftWords.get(windowSize - 3), leftWords.get(windowSize - 2), 
	        		leftWords.get(windowSize - 1), rightWords.get(0))), thresholds[4]);
	        InfolisPattern typeB = new InfolisPattern(regexB, luceneQueryB, minimalB, new ArrayList<String>(Arrays.asList(
	        		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1))), thresholds[5]);
	        InfolisPattern typeC = new InfolisPattern(regexC, luceneQueryC, minimalC, new ArrayList<String>(Arrays.asList(
	        		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1), rightWords.get(2))), thresholds[6]);
	        InfolisPattern typeD = new InfolisPattern(regexD, luceneQueryD, minimalD, new ArrayList<String>(Arrays.asList(
	        		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1), rightWords.get(2), rightWords.get(3))), thresholds[7]);
	        InfolisPattern typeE = new InfolisPattern(regexE, luceneQueryE, minimalE, new ArrayList<String>(Arrays.asList(
	        		leftWords.get(windowSize - 1), rightWords.get(0), rightWords.get(1), rightWords.get(2), rightWords.get(3), rightWords.get(4))), 
	        		thresholds[8]);
	        // order is important here: patterns are listed in ascending order with regard to their generality
	        // type2 and typeB, type3 and typeC etc. have equal generality
	        return (Arrays.asList(type_general, type2, typeB, type3, typeC, type4, typeD, type5, typeE));
        } catch (IndexOutOfBoundsException e) {
        	log.error("Error: missing words in context: " + context);
        	log.error("trace: " + e); 
        	return new ArrayList();
        }
	}
	    
}

