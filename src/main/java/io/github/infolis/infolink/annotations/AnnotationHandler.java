package io.github.infolis.infolink.annotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
		else if (metadata1.toString().endsWith("_i") &&
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
	// that cross sentence boundaries (mergeEntitiesCrossingSentenceBoundaries). 
	// This work-around may introduce errors.
	// If annotations were correct, neighbouring entities with _b annotations
	// should not be merged
	private static List<Annotation> mergeNgrams(List<Annotation> annotations, 
			boolean mergeEntitiesCrossingSentenceBoundaries) {
		List<Annotation> mergedAnnotations = new ArrayList<>();
		for (int i = 0; i < annotations.size(); i++) {
			Annotation anno = annotations.get(i);
			// work-around...
			// TODO log these corrections
			if (mergeEntitiesCrossingSentenceBoundaries) {
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
							i = j-1;
							break;
						}
					}	
				}
			}

			if (anno.getMetadata().toString().endsWith("_b")) {
				Metadata meta = anno.getMetadata();
				StringJoiner ngram = new StringJoiner(" ", "", "");
				ngram.add(anno.getWord());
				int charEnd = anno.getCharEnd();
				for (int j = i+1; j < annotations.size(); j++) {
					Annotation nextAnno = annotations.get(j);
					if (metadataClassesFollow(meta, nextAnno.getMetadata())) {
						ngram.add(nextAnno.getWord());
						charEnd = nextAnno.getCharEnd();
					}
					else {
						Annotation mergedAnnotation = new Annotation(anno);
						mergedAnnotation.setCharEnd(charEnd);
						mergedAnnotation.setWord(ngram.toString());
						mergedAnnotations.add(mergedAnnotation);
						i = j-1;
						break;
					}
				}	
			}
			else mergedAnnotations.add(anno);
		}
		return mergedAnnotations;
	}
	
	/**
	 * Returns true iff string1 is a substring of string2 
	 * 
	 * @param string1
	 * @param string2
	 * @return
	 */
	private static boolean isSubstring(String string1, String string2) {
		if (string1.length() >= 
				string2.length()) 
			return false;

		for (int i = 0; i < string2.length(); i++) {
			if (string2.regionMatches(i, string1, 0, string1.length()))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns true iff the words of string1 and string2 overlap
	 * @param string1
	 * @param string2
	 * @return
	 */
	private static boolean overlap(String string1, String string2) {
		String[] words1 = string1.split("\\s+");
		String[] words2 = string2.split("\\s+");
		String[] longerString = words1;
		String[] shorterString = words2;
		if (words1.length < words2.length) {
			longerString = words2;
			shorterString = words1;
		}
		for (int i = 0; i < longerString.length; i++) {
			for (int j = 0; j < shorterString.length; j++) {
				if (longerString[i].equals(shorterString[j])) return true;
			}
		}
		return false;
	}
	
 	// TODO annotations must be tokenized in same way as textual references...
	// TODO count near misses? (algo identified context of reference as reference?)
	// TODO compare contexts, not only reference terms
	protected static void compare(List<TextualReference> textualReferences, 
			List<Annotation> annotations, Set<Metadata> relevantFields,
			boolean mergeEntitiesCrossingSentenceBoundaries) {
		List<String> exactMatchesRefToAnno = new ArrayList<>();
		List<String> noMatchesRefToAnno = new ArrayList<>();
		// algorithm found incomplete reference
		List<List<String>> refPartOfAnno = new ArrayList<>();
		// algorithm found reference but included unrelated surrounding words
		List<List<String>> annoPartOfRef = new ArrayList<>();
		List<List<String>> refAndAnnoOverlap = new ArrayList<>();
		
		annotations = mergeNgrams(annotations, mergeEntitiesCrossingSentenceBoundaries);
		for (Annotation anno : annotations) log.debug(anno.toString());//System.exit(0);

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
				if (isSubstring(anno.getWord(), textRef.getReference()) && 
						relevantFields.contains(anno.getMetadata())) {
					annoPartOfRef.add(Arrays.asList(anno.getWord(), textRef.getReference()));
					referenceFoundInAnnotations = true;
					break;
				}
				if (isSubstring(textRef.getReference(), anno.getWord()) && 
						relevantFields.contains(anno.getMetadata())) {
					refPartOfAnno.add(Arrays.asList(anno.getWord(), textRef.getReference()));
					referenceFoundInAnnotations = true;
					break;
				}
				if (overlap(anno.getWord(), textRef.getReference()) && 
						relevantFields.contains(anno.getMetadata())) {
					//String[] refs = new String[] { anno.getWord(), textRef.getReference() };
					refAndAnnoOverlap.add(Arrays.asList(anno.getWord(), textRef.getReference()));
					referenceFoundInAnnotations = true;
					break;
				}
			}
			if (!referenceFoundInAnnotations) {
				noMatchesRefToAnno.add(textRef.getReference());
			}
		}
		
		log.debug(String.format("%s of %s (%s%%) (%s) references have an exact match in the gold standard", 
				exactMatchesRefToAnno.size(), textualReferences.size(), 
				(exactMatchesRefToAnno.size() / (double)textualReferences.size()) * 100,
				exactMatchesRefToAnno));
		log.debug(String.format("%s of %s (%s%%) (%s) references do not have any exact or inexact match in the gold standard", 
				noMatchesRefToAnno.size(), textualReferences.size(),
				(noMatchesRefToAnno.size() / (double)textualReferences.size()) * 100,
				noMatchesRefToAnno));
		
		log.debug(String.format("%s of %s (%s%%) (%s) references include annotation but also additional surrounding words", 
				annoPartOfRef.size(), textualReferences.size(), 
				(annoPartOfRef.size() / (double)textualReferences.size()) * 100,
				annoPartOfRef));
		log.debug(String.format("%s of %s (%s%%) (%s) references are a substring of the annotated reference", 
				refPartOfAnno.size(), textualReferences.size(),
				(refPartOfAnno.size() / (double)textualReferences.size()) * 100,
				refPartOfAnno));
		log.debug(String.format("%s of %s (%s%%) (%s) references and annotations are not substrings but overlap", 
				refAndAnnoOverlap.size(), textualReferences.size(),
				(refAndAnnoOverlap.size() / (double)textualReferences.size()) * 100,
				refAndAnnoOverlap));
		
		List<String> annotatedReferences = new ArrayList<String>();
		for (Annotation anno : annotations) {
			if (relevantFields.contains(anno.getMetadata())) {
				annotatedReferences.add(anno.getWord());
			}
		}
		
		List foundReferences = new ArrayList();
		foundReferences.addAll(exactMatchesRefToAnno);
		foundReferences.addAll(annoPartOfRef);
		foundReferences.addAll(refPartOfAnno);
		foundReferences.addAll(refAndAnnoOverlap);
		
		log.debug(String.format("%s of %s (%s%%) (%s of %s) annotated entites were found by the algorithm", 
				foundReferences.size(),
				annotatedReferences.size(),
				(foundReferences.size() / (double)annotatedReferences.size()) * 100,
				foundReferences,
				annotatedReferences));
	}
	
	//TODO implement
	// agreement scores: matches and overlaps of annotated items; classifications...
	/*
	protected void calculateAgreementScores(List<Annotation> annotations1, List<Annotation> annotations2) {
		//for (Annotation anno : annotations1)
		return;
	}*/
	
	//TODO implement: needs position of textual references though...
	/*
	protected List<Annotation> exportData(List<TextualReference> annotatedData) {
		List<Annotation> annotations = new ArrayList<>();
		return annotations;
	}*/
	
	/*
	protected void exportAndCompare(List<TextualReference> textualReferences, List<Annotation> annotations) {
		List<Annotation> annotations1 = exportData(textualReferences);
		compare(annotations1, annotations);
	}*/
	
	
	protected abstract List<Annotation> parse(String input);
	protected abstract Metadata getMetadata(String annotatedItem);

}