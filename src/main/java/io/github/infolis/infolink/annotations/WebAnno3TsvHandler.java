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
	Pattern annoDigitPat = Pattern.compile("([^\\|]+)\\[(\\d+)\\]");
	private Map<String,Integer> recentGroups = new HashMap<>();
	
	protected List<Annotation> parse(String input) {
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
				
			anno.setMetadata(getMetadata(annoString.split("\\s+")[0]));
			anno.setCharStart(Integer.valueOf(charStart));
			anno.setCharEnd(Integer.valueOf(charEnd));
			if ("1".equals(relPos))	anno.setStartsNewSentence();
			//TODO
			//anno.addRelation(targetPosition, getRelation(annoString.split("\\s+")[relAnnoPos]));
			annotations.add(anno);
			
			Matcher annoDigitMatcher = annoDigitPat.matcher(annoString.split("\\s+")[0]);
			while (annoDigitMatcher.find()) {
				String annoClass = annoDigitMatcher.group(1);
				int recentGroup = Integer.valueOf(annoDigitMatcher.group(2));
				recentGroups.put(annoClass, recentGroup); 
			}	
		}
		//log.debug(annotation);
		//annotations.add(annotationItem);
		return annotations;
	}
	
	//TODO add all cases
	protected Metadata getMetadata(String annotatedItem) {
		switch (annotatedItem) {
		case ("Title"):
			return Metadata.title_b;
		/*case ("B-Scale"):
			return Metadata.scale_b;
		case ("I-Scale"):
			return Metadata.scale_i;
		// TODO correct name of the tag in the annotation files?
		case ("B-ProjectTitle"):
			return Metadata.project_title_b;
		case ("I-ProjectTitle"):
			return Metadata.project_title_i;
		case ("B-Creator"):
			return Metadata.creator_b;
		case ("I-Creator"):
			return Metadata.creator_i;*/
		default:
			if (annotatedItem.matches("\\S*Title\\[\\d+\\]\\S*")) {
				int group = -1;
				Matcher digitMatcher = digitPat.matcher(annotatedItem);
				while (digitMatcher.find()) group = Integer.valueOf(digitMatcher.group()); 
				if (group > recentGroups.getOrDefault("Title", 0)) return Metadata.title_b;
				else if (group == recentGroups.get("Title")) return Metadata.title_i;
				else {
					log.warn("annotated item: " + annotatedItem);
					log.warn("group: " + group);
					log.warn("recentGroup: " + recentGroups.get("Title"));
					throw new IllegalArgumentException("cannot handle non-continuous multi-word annotations");
				}
			}	
			else return Metadata.none;
		}
	}

}