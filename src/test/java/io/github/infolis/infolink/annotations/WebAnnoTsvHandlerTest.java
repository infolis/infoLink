package io.github.infolis.infolink.annotations;

import java.util.List;

import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author kata
 *
 */
public class WebAnnoTsvHandlerTest {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(WebAnnoTsvHandlerTest.class);
	
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
	public void testParse() {
		AnnotationHandler h = new WebAnnoTsvHandler();
		List<Annotation> annotations = h.parse(input);
		for (Annotation anno : annotations) {
			log.debug(anno.toString());
		}
		//TODO assertEquals
	}
}