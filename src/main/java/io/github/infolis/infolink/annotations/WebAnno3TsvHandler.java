package io.github.infolis.infolink.annotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import io.github.infolis.infolink.annotations.Annotation.Metadata;

/**
 * Class for reading the new WebAnno 3 tsv format
 * 
 * @author kata
 *
 */
public class WebAnno3TsvHandler extends AnnotationHandler {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(WebAnno3TsvHandler.class);
	
	Pattern digitPat = Pattern.compile("\\d+");
	Pattern annoClassPat = Pattern.compile("([^|*]*)\\[(\\d+)\\]");
	private Map<String,Integer> recentGroups = new HashMap<>();
	
	public List<Annotation> parse(String input) {
		List<Annotation> annotations = new ArrayList<>();
		Map<Integer, String> textMap = new HashMap<>();
		Map<Integer, String> annotationMap = new HashMap<>();
		int wordCount = 0;

		String numRegex = "((\\d+)-(\\d+))[ \t\\x0B\f\r]+((\\d+)-(\\d+))[ \t\\x0B\f\r]+(.*?)[ \t\\x0B\f\r]+(.*?)\n";
		Pattern numPat = Pattern.compile(numRegex);
		
		Matcher numMatcher = numPat.matcher(input);
		while (numMatcher.find()) {
			wordCount += 1;
			//String sentenceNum = numMatcher.group(2);
			String relPos = numMatcher.group(3);
			String charStart = numMatcher.group(5);
			String charEnd = numMatcher.group(6);
			String word = numMatcher.group(7);
			String annoString = numMatcher.group(8);
			textMap.put(wordCount, word);
			annotationMap.put(wordCount, annoString);
			//log.debug(String.valueOf(wordCount));
			//log.debug(word);
			
			Annotation anno = new Annotation();
			anno.setPosition(wordCount);
			anno.setWord(word);
				
			anno.setMetadata(getMetadata(annoString.split("\\t")[0]));
			anno.setCharStart(Integer.valueOf(charStart));
			anno.setCharEnd(Integer.valueOf(charEnd));
			if ("1".equals(relPos))	anno.setStartsNewSentence();
			//TODO
			//anno.addRelation(targetPosition, getRelation(annoString.split("\\t+")[relAnnoPos]));
			annotations.add(anno);
			
			Matcher annoDigitMatcher = annoClassPat.matcher(annoString.split("\\t")[0]);
			while (annoDigitMatcher.find()) {
				String annoClass = annoDigitMatcher.group(1).trim();
				if (annoClass.isEmpty()) continue;
				int recentGroup = Integer.valueOf(annoDigitMatcher.group(2));
				recentGroups.put(annoClass, recentGroup); 
			}	
		}
		//log.debug(annotation);
		//annotations.add(annotationItem);
		return annotations;
	}

	protected Metadata getMetadata(String annotatedItem) {
		Matcher m = annoClassPat.matcher(annotatedItem);
		while (m.find()) {
			String annotatedClass = m.group(1).trim();
			if (annotatedClass.isEmpty()) continue;
			int group = -1;
			Matcher digitMatcher = digitPat.matcher(annotatedItem);
			while (digitMatcher.find()) group = Integer.valueOf(digitMatcher.group()); 
			if (group > recentGroups.getOrDefault(annotatedClass, 0)) return Enum.valueOf(Metadata.class, annotatedClass.toLowerCase().replace(" ", "_") + "_b");
			else if (group == recentGroups.get(annotatedClass)) return Enum.valueOf(Metadata.class, annotatedClass.toLowerCase().replace(" ", "_") + "_i");
			else {
				log.warn("annotated item: " + annotatedItem);
				log.warn("group: " + group);
				log.warn("recentGroup: " + recentGroups.get(annotatedClass));
				throw new IllegalArgumentException("cannot handle non-continuous multi-word annotations");
			}
		}
		if (annotatedItem.equals("_") || annotatedItem.startsWith("*")) return Metadata.none;
		return Enum.valueOf(Metadata.class, annotatedItem.toLowerCase().replace(" ", "_") + "_b");
	}

}