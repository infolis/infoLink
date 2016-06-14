package io.github.infolis.infolink.annotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import io.github.infolis.infolink.annotations.Annotation.Metadata;
import io.github.infolis.model.TextualReference;

/**
 * 
 * @author kata
 *
 */
public class AnnotationHandlerTest {
	
	String input = String.join("\n", "#id=49",
			"#text=der regelmäßige telefonische Gesundheitssurvey",
			"49-1	der	O	O	O	_	_	_	_	",
			"49-2	regelmäßige	O	O	O	_	_	_	_	",
			"49-3	telefonische	O	O	O	_	_	_	_	",
			"49-4	Gesundheitssurvey	B-Title	B-webanno.custom.Dataset_	B-webanno.custom.Dataset_	webanno.custom.Metadata_	webanno.custom.Metadata_	false	50-2",	
			"\n",
			"#id=50",
			"#text=des Robert Koch Institutes, der",
			"50-1	des	O	O	O	_	_	_	_	",
			"50-2	Robert	B-Creator	B-webanno.custom.Dataset_	B-webanno.custom.Dataset_	_	_	_	_",	
			"50-3	Koch	I-Creator	I-webanno.custom.Dataset_	I-webanno.custom.Dataset_	_	_	_	_	",
			"50-4	Institutes,	I-Creator	I-webanno.custom.Dataset_	I-webanno.custom.Dataset_	_	_	_	_	",
			"50-5	der	O	O	O	_	_	_	_	\n");
	
	@Test
	public void testCompare() {
		AnnotationHandler h = new WebAnnoTsvHandler();
		List<Annotation> annotations = h.parse(input);
		List<TextualReference> textualReferences = new ArrayList<>();
		TextualReference textRef = new TextualReference();
		textRef.setReference("Gesundheitssurvey");
		textualReferences.add(textRef);
		TextualReference textRef2 = new TextualReference();
		textRef2.setReference("Koch Institutes");
		textualReferences.add(textRef2);
		Set<Metadata> relevantFields = new HashSet<>();
		relevantFields.addAll(Arrays.asList(
				Metadata.title_b, 
				Metadata.creator, Metadata.creator_b, Metadata.creator_i));
		AnnotationHandler.compare(textualReferences, annotations, relevantFields);
	}
}