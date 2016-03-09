package io.github.infolis.infolink.annotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import io.github.infolis.infolink.annotations.Annotation.Metadata;

/**
 * 
 * @author kata
 *
 */
public class WebAnnoTsvHandler extends AnnotationHandler {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(WebAnnoTsvHandler.class);
	
	protected List<Annotation> parse(String input) {
		List<Annotation> annotations = new ArrayList<>();
		Map<Integer, String> textMap = new HashMap<>();
		Map<Integer, String> annotationMap = new HashMap<>();
		int wordCount = 0;

		String idRegex = "(#id=(\\d+)\\s*)";
		String textRegex = "(#text=(.*?)\\s*\n)";
		String sentencesRegex = "(((.*?)(\n+))+)";
		String segmentRegex = "(" + idRegex + textRegex + sentencesRegex + ")";
		
		Pattern p = Pattern.compile(segmentRegex);
		Matcher m = p.matcher(input);
		while (m.find()) {
			String id = m.group(3);
			String text = m.group(5);
			String annotation = m.group(6);
			String numRegex = "(\\d+)-(\\d+)\\s+(.*?)\\s+(.*?)\n";
			Pattern numPat = Pattern.compile(numRegex);
			Matcher numMatcher = numPat.matcher(annotation);
			while (numMatcher.find()) {
				wordCount += 1;
				String word = numMatcher.group(3);
				String annoString = numMatcher.group(4);
				textMap.put(wordCount, word);
				annotationMap.put(wordCount, annoString);
				log.debug(String.valueOf(wordCount));
				log.debug(word);
				
				Annotation anno = new Annotation();
				anno.setPosition(wordCount);
				anno.setWord(word);
				anno.setMetadata(getMetadata(annoString.split("\\s+")[0]));
				//TODO
				//anno.addRelation();
				annotations.add(anno);
			}
			//log.debug(annotation);
			//annotations.add(annotationItem);
		}
		return annotations;
	}
	
	//TODO add all cases
	protected Metadata getMetadata(String annotatedItem) {
		switch (annotatedItem) {
		case ("B-Title"):
			return Metadata.title;
		case ("I-Title"):
			return Metadata.title;
		case ("B-Creator"):
			return Metadata.creator;
		case ("I-Creator"):
			return Metadata.creator;
		default:
			return Metadata.none;
		}
	}

}