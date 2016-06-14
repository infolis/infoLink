package io.github.infolis.infolink.annotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import io.github.infolis.infolink.annotations.Annotation.Metadata;
import io.github.infolis.model.TextualReference;

/**
 * 
 * @author kata
 *
 */
public abstract class AnnotationHandler {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(AnnotationHandler.class);
	
	protected String read(String filename) throws IOException{
		return FileUtils.readFileToString(new File(filename));
	}
	
	//TODO implement
	// for testing of link creation: 
	// 1) create links from these manually created textual references
	// 2) compare links to manually created list of links
	protected List<TextualReference> toTextualReferenceList(List<Annotation> annotations) {
		List<TextualReference> references = new ArrayList<>();
		return references;
	}
	
	
	protected List<Annotation> importData(String filename) throws IOException {
		String input = read(filename);
		log.debug("read annotation file " + filename);
		return parse(input);
	}
	
	// title_b, title_i and title have equal classes...
	// this is used by a work-around to deal with webAnno breaking annotations
	// that cross sentence boundaries. 
	private static boolean metadataClassesEqual(Metadata metadata1, Metadata metadata2) {
		if (metadata1.equals(metadata2)) return true;
		else if (metadata1.toString().replaceAll("_\\w", "")
				.equals(metadata2.toString().replaceAll("_\\w", "")))
			return true;
		else return false;
	}
	
	private static boolean metadataClassesFollow(Metadata metadata1, Metadata metadata2) {
		if (metadata1.toString().endsWith("_b") &&
				metadata2.toString().endsWith("_i") &&
				metadata1.toString().replaceAll("_\\w", "")
				.equals(metadata2.toString().replaceAll("_\\w", "")))
			return true;
		else return false;
	}
	
	/**
	 * Words are annotated using the BIO system; this method merges 
	 * words that are annotated as being one entity.
	 * 
	 * @param annotations
	 */
	// this includes a work-around to deal with webAnno breaking annotations
	// that cross sentence boundaries. 
	// This work-around may introduce errors.
	// If annotations were correct, neighbouring entities with _b annotations
	// should not be merged
	private static List<Annotation> mergeNgrams(List<Annotation> annotations) {
		List<Annotation> mergedAnnotations = new ArrayList<>();
		for (int i = 0; i < annotations.size(); i++) {
			Annotation anno = annotations.get(i);
			// work-around...
			// TODO log these corrections
			if (anno.getStartsNewSentence() && !anno.getMetadata().equals(Metadata.none)) {
				Metadata meta = anno.getMetadata();
				StringJoiner ngram = new StringJoiner(" ", "", "");
				ngram.add(anno.getWord());
				for (int j = i+1; j < annotations.size(); j++) {
					Annotation nextAnno = annotations.get(j);
					if (metadataClassesEqual(nextAnno.getMetadata(), meta)) {
						ngram.add(nextAnno.getWord());
					}
					else {
						Annotation mergedAnnotation = new Annotation(anno);
						mergedAnnotation.setWord(ngram.toString());
						mergedAnnotations.add(mergedAnnotation);
						i = j;
						break;
					}
				}	
			}
			
			else if (anno.getMetadata().toString().endsWith("_b")) {
				Metadata meta = anno.getMetadata();
				StringJoiner ngram = new StringJoiner(" ", "", "");
				ngram.add(anno.getWord());
				for (int j = i+1; j < annotations.size(); j++) {
					Annotation nextAnno = annotations.get(j);
					if (metadataClassesFollow(meta, nextAnno.getMetadata())) {
						ngram.add(nextAnno.getWord());
					}
					else {
						Annotation mergedAnnotation = new Annotation(anno);
						mergedAnnotation.setWord(ngram.toString());
						mergedAnnotations.add(mergedAnnotation);
						i = j;
						break;
					}
				}	
			}
			else mergedAnnotations.add(anno);
		}
		return mergedAnnotations;
	}
	
	// TODO count overlaps
	// TODO compare contexts, not only reference terms
	protected static void compare(List<TextualReference> textualReferences, 
			List<Annotation> annotations, Set<Metadata> relevantFields) {
		List<String> exactMatchesRefToAnno = new ArrayList<>();
		List<String> noMatchesRefToAnno = new ArrayList<>();
		
		annotations = mergeNgrams(annotations);

		for (TextualReference textRef : textualReferences) {
			boolean referenceFoundInAnnotations = false;
			for (Annotation anno : annotations) {
				if (anno.getWord().equals(textRef.getReference()) && 
						relevantFields.contains(anno.getMetadata())) {
					exactMatchesRefToAnno.add(textRef.getReference());
					referenceFoundInAnnotations = true;
					// break: do not search for further occurrences of the 
					// reference in other annotations; 1 found references
					// should only count as success once
					break;
				}
			}
			if (!referenceFoundInAnnotations) {
				noMatchesRefToAnno.add(textRef.getReference());
			}
		}
		
		log.debug(String.format("%s (%s) references have an exact match in the gold standard", 
				exactMatchesRefToAnno.size(), exactMatchesRefToAnno));
		log.debug(String.format("%s (%s) references do not have an exact match in the gold standard", 
				noMatchesRefToAnno.size(), noMatchesRefToAnno));
		
		List<String> exactMatchesAnnoToRef = new ArrayList<>();
		List<String> noMatchesAnnoToRef = new ArrayList<>();
		for (Annotation anno : annotations) {
			boolean annoFoundInReferences = false;
			for (TextualReference textRef : textualReferences) {
				if (anno.getWord().equals(textRef.getReference()) && 
						relevantFields.contains(anno.getMetadata())) {
					exactMatchesAnnoToRef.add(anno.getWord());
					annoFoundInReferences = true;
					// break: do not search for further occurrences of the 
					// term in other textual references; 1 found references
					// should only count as success once
					break;
				}
			}
			if (!annoFoundInReferences) {
				if (relevantFields.contains(anno.getMetadata())) {
					noMatchesAnnoToRef.add(anno.getWord());
				}
			}
		}
		
		log.debug(String.format("%s (%s) annotated entites were found by the algorithm", 
				exactMatchesAnnoToRef.size(), exactMatchesAnnoToRef));
		log.debug(String.format("%s (%s) annotated entities were not found by the algorithm", 
				noMatchesAnnoToRef.size(), noMatchesAnnoToRef));
		return;
	}
	
	//TODO implement: needs position of textual references though...
	/*
	protected List<Annotation> exportData(List<TextualReference> annotatedData) {
		List<Annotation> annotations = new ArrayList<>();
		return annotations;
	}*/
	
	//TODO implement
	// agreement scores: matches and overlaps of annotated items; classifications...
	/*
	protected void calculateAgreementScores(List<Annotation> annotations1, List<Annotation> annotations2) {
		//for (Annotation anno : annotations1)
		return;
	}*/
	
	/*
	protected void exportAndCompare(List<TextualReference> textualReferences, List<Annotation> annotations) {
		List<Annotation> annotations1 = exportData(textualReferences);
		compare(annotations1, annotations);
	}*/
	
	
	protected abstract List<Annotation> parse(String input);
	protected abstract Metadata getMetadata(String annotatedItem);

}