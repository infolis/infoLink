package io.github.infolis.infolink.annotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.io.FileUtils;
//import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.CoreLabel;
import io.github.infolis.algorithm.TokenizerStanford;
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
	
	// one annotated sentence may contain multiple references -> return list
	private static List<TextualReference> createTextualReferencesFromAnnotations(List<Annotation> sentence, 
			Set<Metadata> relevantFields) {
		List<String> text = new ArrayList<>();
		List<TextualReference> textualRefs = new ArrayList<>();
		sentence = mergeNgrams(sentence);
		
		for (int i = 0; i < sentence.size(); i++) {
			Annotation annotation = sentence.get(i); 
			text.add(annotation.getWord());
		}
		
		for (int i = 0; i < sentence.size(); i++) {
			Annotation annotation = sentence.get(i); 
			if (relevantFields.contains(annotation.getMetadata())) {
				TextualReference textRef = new TextualReference();
				// assumes that annotations are locked to token boundaries 
				// -> annotated entities are separated from surrounding words by whitespace
				textRef.setLeftText(String.join(" ", text.subList(0, i)) + " ");
				textRef.setReference(annotation.getWord());
				textRef.setRightText(" " + String.join(" ", text.subList(i + 1, text.size())));
				textualRefs.add(textRef);
			} 
		}
		
		return textualRefs;
	}
	
	private static List<List<Annotation>> getSentences(List<Annotation> annotations) {
		List<List<Annotation>> sentences = new ArrayList<>();
		
		List<Annotation> sentence = new ArrayList<>();
		
		for (Annotation annotation : annotations) {
			if (annotation.getStartsNewSentence()) {
				// first sentence
				if (!sentence.isEmpty()) sentences.add(sentence);
				sentence = new ArrayList<>();
				sentence.add(annotation);
			} else sentence.add(annotation); 
		}
		if (!sentence.isEmpty()) sentences.add(sentence);
		log.debug("sentences: ");
		for (List<Annotation> sent : sentences) log.debug("" + sent);;
		return sentences;
	}
	
	// for testing of link creation: 
	// 1) create links from these manually created textual references
	// 2) compare links to manually created list of links
	protected static List<TextualReference> toTextualReferenceList(List<Annotation> annotations, 
			Set<Metadata> relevantFields) {
		List<TextualReference> references = new ArrayList<>();
		
		List<List<Annotation>> sentences = getSentences(annotations);
		for (List<Annotation> sentence : sentences) {
				List<TextualReference> textRefs = createTextualReferencesFromAnnotations(sentence, relevantFields);
				if (!textRefs.isEmpty()) references.addAll(textRefs);
		}
		return references;
	}
	
	private Metadata[] buildMetadataCharArray(List<Annotation> annotations) {
		Metadata[] charAnnotations = new Metadata[
		                               annotations.get(annotations.size()-1).getCharEnd() + 1];
		for (Annotation annotation : annotations) {
			// Make sure annotations contain character offsets for this method to work
			if (annotation.getCharStart() == Integer.MIN_VALUE 
					|| annotation.getCharEnd() == Integer.MIN_VALUE)
				throw new IllegalArgumentException("Annotation missing character offsets, aborting.");
			
			for (int i = annotation.getCharStart(); i <= annotation.getCharEnd(); i++) {
				charAnnotations[i] = annotation.getMetadata();
			}
		}
		return charAnnotations;
	}
	
	private String reconstructText(List<Annotation> annotations) {
		String text = "";
		for (Annotation annotation : annotations) {
			text += " " + annotation.getWord();
		}
		return text;
	}
	
	private List<List<CoreLabel>> applyTokenizer(String text) {
		return TokenizerStanford.getInvertibleSentences(text, true, true);
	}
	
	private List<Annotation> transferAnnotationsToTokenizedText(Metadata[] charAnnotations, 
			List<List<CoreLabel>> sentences, String originalText) {
		List<Annotation> transformedAnnotations = new ArrayList<>();
		
		int position = -1;
		for (List<CoreLabel> sentence : sentences) {
			int wordInSentence = -1;
			for (CoreLabel token : sentence) {
				position ++;
				wordInSentence++;
				/*log.debug("originaltext: " + token.originalText());
				log.debug("tokenized: " + token.word());
				log.debug("token beginposition: " + token.beginPosition());
				log.debug("token endposition: " + token.endPosition());
				
				log.debug("annotation at beginpos: " + charAnnotations[token.beginPosition()]);
				log.debug("annotation at endpos: " + charAnnotations[token.endPosition() - 1]);
				*/
				
				// if this token was not separated from the previous token by whitespace 
				// in the original text, it means that the tokenizer split this word 
				boolean entitySplit = false;
				char prevChar = originalText.charAt(token.beginPosition() - 1);

				if (prevChar != ' ') {
					//original text was split here
					entitySplit = true;
				}
				
				String multiword = TokenizerStanford.splitCompounds(token.word());
					int w = -1;
					int curChar = token.beginPosition();
					for (String word : multiword.split(" ")) {
						w ++;
						Annotation anno = new Annotation();
						anno.setWord(word);
						if (!entitySplit && w == 0) anno.setMetadata(charAnnotations[token.beginPosition()]);
						// if this token used to be part of a larger word in the annotation file,
						// change the BIO annotation to _i
						else anno.setMetadata(getFollowingClass(charAnnotations[token.beginPosition()]));
						if (wordInSentence == 0 && w == 0) anno.setStartsNewSentence();
						anno.setPosition(position + w);
						
						// charStart and charEnd positions correspond to positions in 
						// the original text!
						anno.setCharStart(curChar);
						int wordLength = word.length();
						// special characters that may be inserted by tokenizer
						if (word.equals("-LRB-") 
								|| word.equals("-RRB-") 
								|| word.equals("*NL*")) 
							wordLength = 1;
						anno.setCharEnd(curChar + wordLength);
						transformedAnnotations.add(anno);
						curChar = curChar + word.length();
					}
			}
		}
		return transformedAnnotations;
	}
	
	// ignore annotations for punctuation
	// TODO test if this yields the same number of annotations...
	private List<Annotation> transferAnnotationsToTokenizedText2(Metadata[] charAnnotations, 
			List<List<CoreLabel>> sentences, String originalText) {
		List<Annotation> transformedAnnotations = new ArrayList<>();
		
		int position = -1;
		for (List<CoreLabel> sentence : sentences) {
			int wordInSentence = -1;
			boolean moveAnnotation = false;
			for (CoreLabel token : sentence) {
				position ++;
				wordInSentence++;
				
				// if this token was not separated from the previous token by whitespace 
				// in the original text, it means that the tokenizer split this word 
				boolean entitySplit = false;
				char prevChar = originalText.charAt(token.beginPosition() - 1);

				if (prevChar != ' ') {
					//original text was split here
					entitySplit = true;
				}
				
				String multiword = TokenizerStanford.splitCompounds(token.word());
				int w = -1;
				int curChar = token.beginPosition();
				
				for (String word : multiword.split(" ")) {
					w ++;
					Annotation anno = new Annotation();
					anno.setWord(word);
					
					// word is punctuation, ignore annotated label
					// TODO may punctuation occur inside of an annotated entity?
					if (word.equals("-LRB-") || word.equals("-RRB-") || 
							word.equals("``") || word.equals("''") |
							word.matches("[\\p{Punct}\\p{P}]+")) {
						
						anno.setMetadata(Metadata.none);
						if (charAnnotations[token.beginPosition()].toString().endsWith("_b")) {
							moveAnnotation = true;
						}
					} else {
					
						if (!entitySplit && w == 0) {
							if (moveAnnotation) {
								anno.setMetadata(getStartingClass(charAnnotations[token.beginPosition()]));
								moveAnnotation = false;
							} else anno.setMetadata(charAnnotations[token.beginPosition()]);
						}
						else if (entitySplit && (w == 0) ) {
							if (moveAnnotation) {
								anno.setMetadata(getStartingClass(charAnnotations[token.beginPosition()]));
								moveAnnotation = false;
							} else	anno.setMetadata(charAnnotations[token.beginPosition()]);	
						}
						// if this token used to be part of a larger word in the annotation file,
						// change the BIO annotation to _i
						else {
							if (moveAnnotation) {
								anno.setMetadata(getStartingClass(charAnnotations[token.beginPosition()]));
								moveAnnotation = false;
							}
							else anno.setMetadata(getFollowingClass(charAnnotations[token.beginPosition()]));
						}
					}
					if (wordInSentence == 0 && w == 0) anno.setStartsNewSentence();
					anno.setPosition(position + w);
						
					// charStart and charEnd positions correspond to positions in 
					// the original text!
					anno.setCharStart(curChar);
					int wordLength = word.length();
					// special characters that may be inserted by tokenizer
					if (word.equals("-LRB-") 
							|| word.equals("-RRB-") 
							|| word.equals("*NL*")) 
						wordLength = 1;
					anno.setCharEnd(curChar + wordLength);
					transformedAnnotations.add(anno);
					curChar = curChar + word.length();
				}
			}
		}
		return transformedAnnotations;
	}
	
	private Metadata getFollowingClass(Metadata metadata) {
		return Enum.valueOf(Annotation.Metadata.class, metadata.toString().replace("_b", "_i"));
	}
	
	private Metadata getStartingClass(Metadata metadata) {
		return Enum.valueOf(Annotation.Metadata.class, metadata.toString().replace("_i", "_b"));
	}
	
	/**
	 * Transform annotations: apply tokenizer to text but keep annotations. 
	 * 
	 * @param annotations
	 */
	protected List<Annotation> tokenizeAnnotations(List<Annotation> annotations) throws IllegalArgumentException {
		Metadata[] charAnnotations = buildMetadataCharArray(annotations);
		log.debug(String.format("charAnnotation array contains annotations for %s chars", charAnnotations.length));
		String originalText = reconstructText(annotations);
		List<List<CoreLabel>> sentences = applyTokenizer(originalText);
		log.debug(String.format("split annotated text into %s sentences", sentences.size()));
		List<Annotation> transformedAnnotations = transferAnnotationsToTokenizedText2(charAnnotations, 
				sentences, originalText);
		return transformedAnnotations;
	}
	
	
	protected List<Annotation> importData(String filename) throws IOException {
		String input = read(filename);
		log.debug("read annotation file " + filename);
		return parse(input);
	}
	
	// title_b, title_i and title have equal classes...
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
	private static List<Annotation> mergeNgrams(List<Annotation> annotations) {
		List<Annotation> mergedAnnotations = new ArrayList<>();
		for (int i = 0; i < annotations.size(); i++) {
			Annotation anno = annotations.get(i);

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
			List<Annotation> annotations, Set<Metadata> relevantFields) {
		List<String> exactMatchesRefToAnno = new ArrayList<>();
		List<String> noMatchesRefToAnno = new ArrayList<>();
		// algorithm found incomplete reference
		List<List<String>> refPartOfAnno = new ArrayList<>();
		// algorithm found reference but included unrelated surrounding words
		List<List<String>> annoPartOfRef = new ArrayList<>();
		List<List<String>> refAndAnnoOverlap = new ArrayList<>();
		
		annotations = mergeNgrams(annotations);
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