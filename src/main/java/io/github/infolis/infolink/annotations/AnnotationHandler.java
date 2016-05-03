package io.github.infolis.infolink.annotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import io.github.infolis.infolink.annotations.Annotation.Metadata;
import io.github.infolis.model.TextualReference;

//TODO: split sentences with Stanford NLP, then convert to tsv!
//TODO: ImExPorter as algo, translationFunction in package
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
	protected List<TextualReference> toTextualReferenceList(List<Annotation> annotations) {
		List<TextualReference> references = new ArrayList<>();
		return references;
	}
	
	
	protected List<Annotation> importData(String filename) throws IOException {
		String input = read(filename);
		return parse(input);
	}
	
	//TODO implement
	protected List<Annotation> exportData(List<TextualReference> annotatedData) {
		List<Annotation> annotations = new ArrayList<>();
		return annotations;
	}
	
	//TODO implement
	protected void compare(List<Annotation> annotations1, List<Annotation> annotations2) {
		//for (Annotation anno : annotations1)
		return;
	}
	
	protected void exportAndCompare(List<TextualReference> textualReferences, List<Annotation> annotations) {
		List<Annotation> annotations1 = exportData(textualReferences);
		compare(annotations1, annotations);
	}
	
	
	protected abstract List<Annotation> parse(String input);
	protected abstract Metadata getMetadata(String annotatedItem);

}