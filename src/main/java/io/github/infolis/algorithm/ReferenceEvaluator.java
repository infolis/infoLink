package io.github.infolis.algorithm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import io.github.infolis.algorithm.BaseAlgorithm;
import io.github.infolis.algorithm.IllegalAlgorithmArgumentException;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.annotations.Annotation;
import io.github.infolis.infolink.annotations.Annotation.Metadata;
import io.github.infolis.infolink.annotations.AnnotationHandler;
import io.github.infolis.infolink.annotations.WebAnno3TsvHandler;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisFile;

/**
 * Class for evaluating reference extraction against gold standard annotations.
 * 
 * @author kata
 *
 */
public class ReferenceEvaluator extends BaseAlgorithm {
	
	private InputStream goldstandard;
	AnnotationHandler annotationHandler;
	Set<Metadata> relevantFields;
	List<Annotation> goldAnnotations;
	
	public ReferenceEvaluator(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
			FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	public void init(File goldFile, AnnotationHandler annotationHandler, Set<Metadata> relevantFields, boolean tokenize) throws IOException {
		this.goldstandard = new FileInputStream(goldFile);
		this.annotationHandler = annotationHandler;
		this.relevantFields = relevantFields;
		this.goldAnnotations = readAnnotations(tokenize);
	}
	
	private void init() throws IOException {
		InfolisFile goldFile = this.getInputDataStoreClient().get(InfolisFile.class, getExecution().getFirstInputFile());
		this.goldstandard = this.getInputFileResolver().openInputStream(goldFile);
		this.annotationHandler = new WebAnno3TsvHandler();
		relevantFields = new HashSet<>();
		relevantFields.addAll(Arrays.asList(
				Metadata.title_b, Metadata.vague_title_b, Metadata.scale_b,
				Metadata.year_b, Metadata.number_b, Metadata.version_b
				));
		this.goldAnnotations = readAnnotations(getExecution().isTokenize());
	}
	
	private List<Annotation> readAnnotations(boolean tokenize) throws IOException {
		String annotationTsv = IOUtils.toString(this.goldstandard);
		List<Annotation> annotations = this.annotationHandler.parse(annotationTsv);
		if (tokenize) { 
			annotations = this.annotationHandler.tokenizeAnnotations(annotations);
		}
		return annotations;
	}
	
	protected void compareToGoldstandard(List<TextualReference> foundReferences) throws IOException {
		AnnotationHandler.compare(foundReferences, this.goldAnnotations, this.relevantFields);
	}
	
	// TODO implement
	/**
	 * Compares the references contained in foundReferences to references in the gold annotations.
	 * Computes precision (exact and partial matches) and recall.
	 * 
	 * @param foundReferences
	 * @throws IOException 
	 */
	public void evaluate(List<TextualReference> foundReferences) throws IOException {
		// 1. iterate through references, generate maps for each different textFile
		// count: exact matches, partial matches; precision, recall; per individual references; per reference types per file
		compareToGoldstandard(foundReferences);
	}

	@Override
	public void execute() throws IOException {
		init();
		evaluate(getInputDataStoreClient().get(TextualReference.class, getExecution().getTextualReferences()));
		
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub

	}
}