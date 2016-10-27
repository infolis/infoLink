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
 * 
 * @author kata
 *
 */
public class WebAnnoTsvHandler extends AnnotationHandler {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(WebAnnoTsvHandler.class);
	
	public List<Annotation> parse(String input) {
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
				String relPos = numMatcher.group(2);
				String word = numMatcher.group(3);
				String annoString = numMatcher.group(4);
				textMap.put(wordCount, word);
				annotationMap.put(wordCount, annoString);
				//log.debug(String.valueOf(wordCount));
				//log.debug(word);
				
				Annotation anno = new Annotation();
				anno.setPosition(wordCount);
				anno.setWord(word);
				anno.setMetadata(getMetadata(annoString.split("\\s+")[0]));
				if ("1".equals(relPos))	anno.setStartsNewSentence();
				//TODO
				//anno.addRelation(targetPosition, getRelation(annoString.split("\\s+")[relAnnoPos]));
				annotations.add(anno);
				
			}
			//log.debug(annotation);
			//annotations.add(annotationItem);
		}
		return annotations;
	}
	

	
	
	public static String exportAsWebAnnoTsv(List<Annotation> annotations) {
		//String tsv = " # webanno.custom.Dataset | Information | LeftContext | RightContext  # webanno.custom.Metadata | MetadataforDataset | Relation | sameas | AttachTo=webanno.custom.Dataset\n";
		String tsv = " # webanno.custom.Dataset | Information | LeftContext | RightContext\n";
		// webanno 3 tsv starts counting at 1
		int sentenceNum = 0;
		int wordNum = 1;
		String sentenceText = "";
		String sentenceAnno = "";
		// TODO add header
		for (Annotation annotation : annotations) {
			if (annotation.getStartsNewSentence()) {
				if (sentenceNum != 0) {
					tsv += "#id=" + sentenceNum + "\n" + sentenceText.trim() + "\n";
					tsv += sentenceAnno.trim() + "\n\n";
				}
				sentenceText = "\n#text=";
				sentenceAnno = "";
				sentenceNum++;
			}
			sentenceText += annotation.getWord() + " ";
			String datasetAnno = getWebAnnoTsvMetadataClass(annotation.getMetadata());
			String datasetAnnoLeft = "O";
			if (!datasetAnno.equals("O")) {
				if (datasetAnno.startsWith("B-")) datasetAnnoLeft = "B-webanno.custom.Dataset_";
				else datasetAnnoLeft = "I-webanno.custom.Dataset_";
			}
			String datasetAnnoRight = datasetAnnoLeft;
			
			/*sentenceAnno += String.format("%s-%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", 
					sentenceNum, wordNum,
					annotation.getWord(),
					datasetAnno,
					datasetAnnoLeft,
					datasetAnnoRight,
					"_", "_", "_", "_");*/
			sentenceAnno += String.format("%s-%s\t%s\t%s\t%s\t%s\n", 
					sentenceNum, wordNum,
					annotation.getWord(),
					datasetAnno,
					datasetAnnoLeft,
					datasetAnnoRight);
			wordNum++;
		}
		tsv += "#id=" + sentenceNum + "\n" + sentenceText.trim() + "\n";
		tsv += sentenceAnno.trim() + "\n\n";
		return tsv;
	}
	
	protected Metadata getMetadata(String annotatedItem) {
		switch (annotatedItem) {
		case ("B-Title"):
			return Metadata.title_b;
		case ("I-Title"):
			return Metadata.title_i;
		case ("B-Scale"):
			return Metadata.scale_b;
		case ("I-Scale"):
			return Metadata.scale_i;
		case ("B-Project Title"):
			return Metadata.project_title_b;
		case ("I-Project Title"):
			return Metadata.project_title_i;
		case ("B-Creator"):
			return Metadata.creator_b;
		case ("I-Creator"):
			return Metadata.creator_i;
		case ("B-Publisher"):
			return Metadata.publisher_b;
		case ("I-Publisher"):
			return Metadata.publisher_i;
		case ("B-Geographical Coverage"):
			return Metadata.geographical_coverage_b;
		case ("I-Geographical Coverage"):
			return Metadata.geographical_coverage_i;
		case ("B-ID"):
			return Metadata.id_b;
		case ("I-ID"):
			return Metadata.id_i;
		case ("B-Vague Title"):
			return Metadata.vague_title_b;
		case ("I-Vague Title"):
			return Metadata.vague_title_i;
		case ("B-Number"):
			return Metadata.number_b;
		case ("I-Number"):
			return Metadata.number_i;
		case ("B-Project Funder"):
			return Metadata.project_funder_b;
		case ("I-Project Funder"):
			return Metadata.project_funder_i;
		case ("B-Sample"):
			return Metadata.sample_b;
		case ("I-Sample"):
			return Metadata.sample_i;
		case ("B-Topic"):
			return Metadata.topic_b;
		case ("I-Topic"):
			return Metadata.topic_i;
		case ("B-URL"):
			return Metadata.url_b;
		case ("I-URL"):
			return Metadata.url_i;
		case ("B-Version"):
			return Metadata.version_b;
		case ("I-Version"):
			return Metadata.version_i;
		case ("B-Year"):
			return Metadata.year_b;
		case ("I-Year"):
			return Metadata.year_i;
		default:
			return Metadata.none;
		}
	}
	
	private static String getWebAnnoTsvMetadataClass(Metadata metadata) {
		switch(metadata) {
			case title_b:
				return "B-Title";
			case title_i:
				return "I-Title";
			case scale_b:
				return "B-Scale";
			case scale_i:
				return "I-Scale";
			case project_title_b:
				return "B-Project Title";
			case project_title_i:
				return "I-Project Title";
			case creator_b:
				return "B-Creator";
			case creator_i:
				return "I-Creator";
			case publisher_b:
				return "B-Publisher";
			case publisher_i:
				return "I-Publisher";
			case geographical_coverage_b:
				return "B-Geographical Coverage";
			case geographical_coverage_i:
				return "I-Geographical Coverage";
			case id_b:
				return "B-ID";
			case id_i:
				return "I-ID";
			case vague_title_b:
				return "B-Vague Title";
			case vague_title_i:
				return "I-Vague Title";
			case number_b:
				return "B-Number";
			case number_i:
				return "I-Number";
			case project_funder_b:
				return "B-Project Funder";
			case project_funder_i:
				return "I-Project Funder";
			case sample_b:
				return "B-Sample";
			case sample_i:
				return "I-Sample";
			case topic_b:
				return "B-Topic";
			case topic_i:
				return "I-Topic";
			case url_b:
				return "B-URL";
			case url_i:
				return "I-URL";
			case version_b:
				return "B-Version";
			case version_i:
				return "I-Version";
			case year_b:
				return "B-Year";
			case year_i:
				return "I-Year";
			default:
				return "O";
		}
	}

}