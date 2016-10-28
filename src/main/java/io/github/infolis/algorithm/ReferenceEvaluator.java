package io.github.infolis.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import io.github.infolis.algorithm.BaseAlgorithm;
import io.github.infolis.algorithm.IllegalAlgorithmArgumentException;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.annotations.Annotation;
import io.github.infolis.infolink.annotations.Annotation.Metadata;
import io.github.infolis.infolink.annotations.AnnotationHandler;
import io.github.infolis.infolink.annotations.WebAnno3TsvHandler;
import io.github.infolis.infolink.evaluation.Agreement;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisFile;

/**
 * Class for evaluating reference extraction against gold standard annotations.
 * 
 * @author kata
 *
 */
public class ReferenceEvaluator extends BaseAlgorithm {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ReferenceEvaluator.class);
			
	Set<Metadata> relevantFields = new HashSet<>();
	
	private InputStream goldstandard;
	AnnotationHandler annotationHandler;
	List<Annotation> goldAnnotations;
	
	public ReferenceEvaluator(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
			FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
		
		relevantFields.addAll(Arrays.asList(
				Metadata.title_b, Metadata.vague_title_b, Metadata.scale_b,
				Metadata.year_b, Metadata.number_b, Metadata.version_b
				));
	}
	
	public void setRelevantFields(Set<Metadata> relevantFields) {
		this.relevantFields = relevantFields;
	}
	
	private void loadAnnotations(InfolisFile goldFile) throws IOException {
		this.goldstandard = this.getInputFileResolver().openInputStream(goldFile);
		this.annotationHandler = new WebAnno3TsvHandler();
		this.goldAnnotations = readAnnotations(getExecution().isTokenize());
	}
	
	private List<Annotation> readAnnotations(boolean tokenize) throws IOException {
		String annotationTsv = IOUtils.toString(this.goldstandard);
		List<Annotation> annotations = this.annotationHandler.parse(annotationTsv);
		if (tokenize) { 
			annotations = this.annotationHandler.tokenizeAnnotations(annotations);
		}
		return AnnotationHandler.mergeNgrams(annotations);
	}
	
	/**
	 * Compares the references contained in foundReferences to references in the gold annotations.
	 * Computes precision (exact and partial matches) and recall.
	 * 
	 * @param foundReferences
	 * @throws IOException 
	 */
	protected void evaluate(List<TextualReference> foundReferences, List<InfolisFile> goldFiles) throws IOException {
		// 1. iterate through references, sort by text file names
		ListMultimap<String, TextualReference> refFileMap = ArrayListMultimap.create();
		for (TextualReference ref : foundReferences) {
			String textFileName = getInputDataStoreClient().get(InfolisFile.class, ref.getTextFile()).getOriginalName();
			refFileMap.put(Paths.get(textFileName).getFileName().toString().replace(".pdf", "").replace(".txt", ""), ref);
		}
		// 2. iterate through gold files, sort by gold file names
		Map<String, InfolisFile> goldFileMap = new HashMap<>();
		for (InfolisFile goldFile : goldFiles) {
			goldFileMap.put(Paths.get(goldFile.getOriginalName()).getFileName().toString().replace(".tsv", ""), goldFile);
		}
		// 3. evaluate references in every text file
		for (String goldFilename : goldFileMap.keySet()) {
			loadAnnotations(goldFileMap.get(goldFilename));
			Agreement agreement = compareToGoldstandard(foundReferences);
			log.debug("agreement for {}", goldFilename);
			agreement.logStats();
			// TODO count: exact matches, partial matches; precision, recall; per individual references; per reference types per file
		}
		//TODO 4. aggregate scores for all files

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
	
	
	private Agreement compareToGoldstandard(List<TextualReference> foundReferences) throws IOException {
		return compare(foundReferences, this.goldAnnotations, this.relevantFields);
	}
 	
	// TODO count near misses? (algo identified context of reference as reference?) -> would need positions
	// TODO compare contexts, not only reference terms?
	public static Agreement compare(List<TextualReference> textualReferences, 
			List<Annotation> annotations, Set<Metadata> relevantFields) {
		
		List<String> annotatedReferences = new ArrayList<String>();
		for (Annotation anno : annotations) {
			if (relevantFields.contains(anno.getMetadata())) {
				annotatedReferences.add(anno.getWord());
			}
		}
		
		Agreement agreement = new Agreement(textualReferences.size(), annotatedReferences.size());

		for (TextualReference textRef : textualReferences) {
			boolean referenceFoundInAnnotations = false;
			for (Annotation anno : annotations) {
				if (anno.getWord().equals(textRef.getReference()) && 
						relevantFields.contains(anno.getMetadata())) {
					agreement.addExactMatch(textRef.getReference());
					referenceFoundInAnnotations = true;
					// break: do not search for further occurrences of the 
					// reference in other annotations; 1 found references
					// should only count as success once
					break;
				}
				if (isSubstring(anno.getWord(), textRef.getReference()) && 
						relevantFields.contains(anno.getMetadata())) {
					agreement.addInflatedReference(anno.getWord(), textRef.getReference());
					referenceFoundInAnnotations = true;
					break;
				}
				if (isSubstring(textRef.getReference(), anno.getWord()) && 
						relevantFields.contains(anno.getMetadata())) {
					agreement.addIncompleteReference(anno.getWord(), textRef.getReference());
					referenceFoundInAnnotations = true;
					break;
				}
				if (overlap(anno.getWord(), textRef.getReference()) && 
						relevantFields.contains(anno.getMetadata())) {
					agreement.addOverlap(anno.getWord(), textRef.getReference());
					referenceFoundInAnnotations = true;
					break;
				}
			}
			if (!referenceFoundInAnnotations) {
				agreement.addNoMatch(textRef.getReference());
			}
		}
		return agreement;
	}

	@Override
	public void execute() throws IOException {
		evaluate(getInputDataStoreClient().get(TextualReference.class, getExecution().getTextualReferences()),
				getInputDataStoreClient().get(InfolisFile.class, getExecution().getInputFiles()));
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub

	}
}