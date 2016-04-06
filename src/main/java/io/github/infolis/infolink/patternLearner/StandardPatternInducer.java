
package io.github.infolis.infolink.patternLearner;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import io.github.infolis.algorithm.Bootstrapping;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.TextualReference;
import io.github.infolis.util.RegexUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

/**
 * Class for inducing patterns based on given textual references. 
 * Pattern thresholds are set according values specified in thresholds parameter. 
 * 
 * @author kata
 */
public class StandardPatternInducer extends Bootstrapping.PatternInducer {
    
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(StandardPatternInducer.class);
	// TODO derive from windowsize
	public final static int patternsPerContext = 9;
	
	public StandardPatternInducer() {}
	
	public final int getPatternsPerContext() {
		return patternsPerContext;
	}
	
	private InfolisPattern createPattern(Set<String> words, List<String> lucene_left, List<String> lucene_right, List<String> regex_left, List<String> regex_right, String delimiter_left, String delimiter_right, double threshold) {
		// left words are in reverse order, need to reverse again here
		// make a deep copy of the list to not alter the original one
		List<String> lucene_left_copy = new ArrayList<>();
		for (String word : lucene_left) lucene_left_copy.add(word);
		Collections.reverse(lucene_left_copy);
		
		List<String> regex_left_copy = new ArrayList<>();
		for (String word : regex_left) regex_left_copy.add(word);
		Collections.reverse(regex_left_copy);
		
		String luceneQuery = "\"" + String.join(" ", lucene_left_copy) + delimiter_left + "*" + delimiter_right + String.join(" ", lucene_right) + "\"";
		// TODO optimize: wildcards are necessary between words but not at the beginning or end of the query
				
		if (delimiter_left.matches("\\s")) delimiter_left = "\\s";
		else if (delimiter_left.matches("")) delimiter_left = "\\s?";
		if (delimiter_right.matches("\\s")) delimiter_right = "\\s";
		else if (delimiter_right.matches("")) delimiter_right = "\\s?";
		
	    String minimal = String.join("\\s", regex_left_copy) + delimiter_left + RegexUtils.studyRegex_ngram + delimiter_right + String.join("\\s", regex_right);
	    String regex = RegexUtils.leftContextRegex + minimal + RegexUtils.rightContextRegex;
		InfolisPattern pattern = new InfolisPattern(regex, luceneQuery, minimal, words, threshold);
		return pattern;
	}
	
	protected List<InfolisPattern> induce(TextualReference context, Double[] thresholds) {
		
		log.trace("context: " + context.toString());

		List<String> leftWords = new ArrayList<>();
		leftWords.addAll(context.getLeftWords());
        List<String> rightWords = context.getRightWords();
        
        // reverse order so that leftWords.get(0) is the direct neighbour of the search term
        Collections.reverse(leftWords);

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
        
        // delimiter between search term and context terms
        String delimiter_left = leftWords.get(0);
        String delimiter_right = rightWords.get(0);
        
        // TODO make configurable
        int windowsize = 5;
        // set default values in case the context of a term contains less elements than the given windowsize
        List<InfolisPattern> inducedPatternsLeft = Stream.generate(InfolisPattern::new)
        												.limit(windowsize)
        												.collect(Collectors.toList());
        List<InfolisPattern> inducedPatternsRight = Stream.generate(InfolisPattern::new)
														.limit(windowsize)
														.collect(Collectors.toList());

        InfolisPattern typeGeneral;
        
        try {
        	// most general pattern: two words enclosing study name
            Set<String> words = new HashSet<>();
            words.addAll(leftWords.subList(1, 2));
            words.addAll(rightWords.subList(1, 2));
        	typeGeneral = createPattern(words, leftWords_lucene.subList(1, 2), rightWords_lucene.subList(1, 2), leftWords_regex.subList(1, 2), rightWords_regex.subList(1, 2), delimiter_left, delimiter_right, thresholds[0]);
        	log.trace("induced pattern: " + typeGeneral.getLuceneQuery());
        } catch (IndexOutOfBoundsException e) {
        	log.debug("Not enough words in context to induce pattern of type general: " + context);
        	return new ArrayList<>();
        }
        
        // induce patterns with one word as left context and a phrase of windowsize * 1 right context words
        // i starts at 2 because index 0 is the delimiter, first word is at subList(1,2)
        for (int i = 3; i < Math.min(windowsize + 2, rightWords.size()); i++) {    
        	Set<String> words = new HashSet<>();
            words.addAll(leftWords.subList(1, 2));
            words.addAll(rightWords.subList(1, i));
        	InfolisPattern pattern = createPattern(words, leftWords_lucene.subList(1, 2), rightWords_lucene.subList(1, i), leftWords_regex.subList(1, 2), rightWords_regex.subList(1, i), delimiter_left, delimiter_right, thresholds[i+2]);
        	inducedPatternsRight.add(i-3, pattern);
        	log.trace("induced pattern: " + pattern.getLuceneQuery());
        }

        // induce patterns with one word as right context and a phrase of windowsize * 1 left context words
        for (int i = 3; i < Math.min(windowsize + 2, leftWords.size()); i++) {
        	Set<String> words = new HashSet<>();
            words.addAll(leftWords.subList(1, i));
            words.addAll(rightWords.subList(1, 2));
        	InfolisPattern pattern = createPattern(words, leftWords_lucene.subList(1, i), rightWords_lucene.subList(1, 2), leftWords_regex.subList(1, i), rightWords_regex.subList(1, 2), delimiter_left, delimiter_right, thresholds[i-2]);
        	inducedPatternsLeft.add(i-3, pattern);
        	log.trace("induced pattern: " + pattern.getLuceneQuery());
        }
	    // order is important here: patterns are listed in ascending order with regard to their generality
	    // type2left and type2right etc. have equal generality
	    return (Arrays.asList(typeGeneral, inducedPatternsLeft.get(0), inducedPatternsRight.get(0), inducedPatternsLeft.get(1), inducedPatternsRight.get(1), inducedPatternsLeft.get(2), inducedPatternsRight.get(2), inducedPatternsLeft.get(3), inducedPatternsRight.get(3)));
	}
	    
}

