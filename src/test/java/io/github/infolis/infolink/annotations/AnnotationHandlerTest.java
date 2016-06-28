package io.github.infolis.infolink.annotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import io.github.infolis.infolink.annotations.Annotation.Metadata;
import io.github.infolis.model.TextualReference;

/**
 * 
 * @author kata
 *
 */
public class AnnotationHandlerTest {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(AnnotationHandlerTest.class);
	
	String inputWebAnno2 = String.join("\n", "#id=49",
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
	
	String inputWebAnno3 = String.join("\n", 
			"#Text=Abstract",
			"7-1	277-285	Abstract	_	_	_	_	_	_	_	",
			"\n",
			"#Text=This article gives an overview of the Programme for the International Assessment of Adult",
			"8-1	286-290	This	_	_	_	_	_	_	_	",
			"8-2	291-298	article	_	_	_	_	_	_	_	",
			"8-3	299-304	gives	_	_	_	_	_	_	_	",
			"8-4	305-307	an	_	_	_	_	_	_	_	",
			"8-5	308-316	overview	_	_	_	_	_	_	_",	
			"8-6	317-319	of	_	_	_	_	_	_	_	",
			"8-7	320-323	the	_	_	_	_	_	_	_	",
			"8-8	324-333	Programme	Title[2]	*[2]	*[2]	*	same-as	false	9-2[0_2]	",
			"8-9	334-337	for	Title[2]	*[2]	*[2]	_	_	_	_	",
			"8-10	338-341	the	Title[2]	*[2]	*[2]	_	_	_	_	",
			"8-11	342-355	International	Title[2]	*[2]	*[2]	_	_	_	_	",
			"8-12	356-366	Assessment	Title[2]	*[2]	*[2]	_	_	_	_	",
			"8-13	367-369	of	Title[2]	*[2]	*[2]	_	_	_	_	",
			"8-14	370-375	Adult	Title[2]	*[2]	*[2]	_	_	_	_	",
			"\n",
			"#Text=Competencies (PIAAC) and introduces the methodological challenges in implementing",
			"9-1	376-388	Competencies	Title[2]	*[2]	*[2]	_	_	_	_	",
			"9-2	389-396	(PIAAC)	Title	*	*	_	_	_	_	",
			"9-3	397-400	and	_	_	_	_	_	_	_	",
			"9-4	401-411	introduces	_	_	_	_	_	_	_	",
			"9-5	412-415	the	_	_	_	_	_	_	_	",
			"9-6	416-430	methodological	_	_	_	_	_	_	_	",
			"9-7	431-441	challenges	_	_	_	_	_	_	_	",
			"9-8	442-444	in	_	_	_	_	_	_	_	",
			"9-9	445-457	implementing	_	_	_	_	_	_	_	",
			"\n");
	
	@Test
	public void testCompareWebAnno2() {
		AnnotationHandler h = new WebAnnoTsvHandler();
		List<Annotation> annotations = h.parse(inputWebAnno2);
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
		AnnotationHandler.compare(textualReferences, annotations, relevantFields, false, false);
	}
	
	@Test
	public void testCompareWebAnno3() {
		AnnotationHandler h = new WebAnno3TsvHandler();
		List<Annotation> annotations = h.parse(inputWebAnno3);
		for (Annotation anno : annotations) log.debug(anno.toString());
		List<TextualReference> textualReferences = new ArrayList<>();
		TextualReference textRef = new TextualReference();
		textRef.setReference("PIAAC");
		textualReferences.add(textRef);
		TextualReference textRef2 = new TextualReference();
		textRef2.setReference("Programme for the International Assessment of Adult Competencies");
		textualReferences.add(textRef2);
		Set<Metadata> relevantFields = new HashSet<>();
		relevantFields.addAll(Arrays.asList(
				Metadata.title_b, Metadata.title_i));
		AnnotationHandler.compare(textualReferences, annotations, relevantFields, true, false);
	}
}