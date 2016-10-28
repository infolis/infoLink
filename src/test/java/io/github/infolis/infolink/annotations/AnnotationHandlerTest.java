package io.github.infolis.infolink.annotations;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.Assert;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisConfig;
import io.github.infolis.infolink.annotations.Annotation.Metadata;
import io.github.infolis.model.TextualReference;

/**
 * 
 * @author kata
 *
 */
public class AnnotationHandlerTest {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(AnnotationHandlerTest.class);
	
	static String inputWebAnno2 = String.join("\n", "#id=49",
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
	
	static String inputWebAnno3 = String.join("\n", 
			"#FORMAT=WebAnno TSV 3",
			"#T_SP=webanno.custom.Dataset|Information|LeftContext|RightContext",
			"#T_RL=webanno.custom.Metadata|MetadataforDataset|Relation|sameas|BT_webanno.custom.Dataset",
			"\n",
			"\n",
			"#Text=DOI: 10.12758/mda.2014.005methods, data, analyses | Vol. 8(2), 2014, pp. 125-136",
			"1-1	0-4	DOI:	_	_	_	_	_	_	_"	,
			"1-2	5-34	10.12758/mda.2014.005methods,	_	_	_	_	_	_	_	",	
			"1-3	35-40	data,	_	_	_	_	_	_	_	",	
			"1-4	41-49	analyses	_	_	_	_	_	_	_	",	
			"1-5	50-51	|	_	_	_	_	_	_	_	",	
			"1-6	52-56	Vol.	_	_	_	_	_	_	_	",	
			"1-7	57-62	8(2),	_	_	_	_	_	_	_	",	
			"1-8	63-68	2014,	_	_	_	_	_	_	_	",	
			"1-9	69-72	pp.	_	_	_	_	_	_	_	",	
			"1-10	73-80	125-136	_	_	_	_	_	_	_	",	
			"\n",
			"#Text=PIAAC and its Methodological",
			"2-1	81-86	PIAAC	Title	*	*	_	_	_	_	",	
			"2-2	87-90	and	_	_	_	_	_	_	_	",	
			"2-3	91-94	its	_	_	_	_	_	_	_	",	
			"2-4	95-109	Methodological	_	_	_	_	_	_	_	",	
			"\n",
			"#Text=Challenges",
			"3-1	110-120	Challenges	_	_	_	_	_	_	_	",	
			"\n",
			"#Text=Beatrice Rammstedt 1 & Débora B. Maehler 1,2",
			"4-1	121-129	Beatrice	_	_	_	_	_	_	_	",	
			"4-2	130-139	Rammstedt	_	_	_	_	_	_	_	",
			"4-3	140-141	1	_	_	_	_	_	_	_	",
			"4-4	142-143	&	_	_	_	_	_	_	_	",
			"4-5	144-150	Débora	_	_	_	_	_	_	_	",	
			"4-6	151-153	B.	_	_	_	_	_	_	_	",
			"4-7	154-161	Maehler	_	_	_	_	_	_	_	",	
			"4-8	162-165	1,2	_	_	_	_	_	_	_	",
			"\n",
			"#Text=1 GESIS – Leibniz Institute for the Social Sciences",
			"5-1	166-167	1	_	_	_	_	_	_	_	",
			"5-2	168-173	GESIS	_	_	_	_	_	_	_	",
			"5-3	174-175	–	_	_	_	_	_	_	_	",
			"5-4	176-183	Leibniz	_	_	_	_	_	_	_	",
			"5-5	184-193	Institute	_	_	_	_	_	_	_	",
			"5-6	194-197	for	_	_	_	_	_	_	_	",	
			"5-7	198-201	the	_	_	_	_	_	_	_	",
			"5-8	202-208	Social	_	_	_	_	_	_	_	",
			"5-9	209-217	Sciences	_	_	_	_	_	_	_	",
			"\n",
			"#Text=2 College for Interdisciplinary Education Research (CIDER)",
			"6-1	218-219	2	_	_	_	_	_	_	_	",
			"6-2	220-227	College	_	_	_	_	_	_	_	",
			"6-3	228-231	for	_	_	_	_	_	_	_	",
			"6-4	232-249	Interdisciplinary	_	_	_	_	_	_	_	",
			"6-5	250-259	Education	_	_	_	_	_	_	_	",
			"6-6	260-268	Research	_	_	_	_	_	_	_	",
			"6-7	269-276	(CIDER)	_	_	_	_	_	_	_	",
			"\n",
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
			"9-9	445-470	implementing/implementing	_	_	_	_	_	_	_	",
			"\n");
	
	public static String getTestAnnotationString() {
		return inputWebAnno3;
	}
	
	public static String getTestAnnotationStringOldFormat() {
		return inputWebAnno2;
	}
	
	
	@Test
	public void testTokenizeAnnotations() throws IOException {
		AnnotationHandler h = new WebAnno3TsvHandler();
		/*String annotationTsv = FileUtils.readFileToString(new File("/tmp/44275 (7).tsv"), "utf8");
		List<Annotation> annotations = h.parse(annotationTsv);
		List<Annotation> tokenizedAnnotations = h.tokenizeAnnotations(annotations);*/
		List<Annotation> annotations = h.parse(inputWebAnno3);
		List<Annotation> tokenizedAnnotations = h.tokenizeAnnotations(annotations);
		for (Annotation anno: tokenizedAnnotations) {
			log.debug(anno.toString());
		}
		
		//testExportAsWebAnnoTsv(tokenizedAnnotations);
		
		Set<Metadata> relevantFields = new HashSet<>();
		relevantFields.addAll(Arrays.asList(
				Metadata.title_b, Metadata.scale_b,
				Metadata.year_b, Metadata.number_b, Metadata.version_b
				));
		testToTextualReferenceList(tokenizedAnnotations, relevantFields);
		
	}
	
	public void testExportAsWebAnnoTsv(List<Annotation> annotations) throws IOException {
		log.debug("output of WebAnnoTsvHandler:\n");
		log.debug(WebAnnoTsvHandler.exportAsWebAnnoTsv(annotations));
		/*File testOut = new File("/tmp/44275._tsv");
		FileUtils.write(testOut, WebAnnoTsvHandler.exportAsWebAnnoTsv(annotations), false);
		*/
	}
	
	public void testToTextualReferenceList(List<Annotation> annotations, 
			Set<Metadata> relevantFields) throws IOException {
		List<String> allAnnotatedTextRefs = new ArrayList<>();
		for (Metadata field : relevantFields) {
			//File testOut = new File("/tmp/44275_textrefs_" + field.toString() + ".txt");
			/*File testOut = Files.createTempFile(
					InfolisConfig.getTmpFilePath(), 
					"test_textrefs_" + field.toString(),
					".txt")
					.toFile();*/
			Set<Metadata> fields = new HashSet<>();
			fields.add(field);
			List<TextualReference> references = AnnotationHandler
					.toTextualReferenceList(annotations, fields);
			//AnnotationHandler.writeToAnnotatedTextRefFile(testOut, references, field.toString());
			List<String> annotatedTextRefs = AnnotationHandler.toAnnotatedTextRefs(references, field.toString());
			log.debug("number of references: " + references.size());
			allAnnotatedTextRefs.addAll(annotatedTextRefs);
			Assert.assertEquals(references.size(), annotatedTextRefs.size());
		}
		log.debug("\nunmerged annotated text refs:");
		for (String ref : allAnnotatedTextRefs) log.debug(ref);
		Collection<String> mergedAnnotatedTextRefs = AnnotationHandler.mergeAnnotatedTextualReferences(allAnnotatedTextRefs);
		log.debug("\nmerged annotated text refs:");
		for (String ref : mergedAnnotatedTextRefs) log.debug(ref);
	}
}