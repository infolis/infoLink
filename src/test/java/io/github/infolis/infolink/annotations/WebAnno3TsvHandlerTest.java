package io.github.infolis.infolink.annotations;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import io.github.infolis.infolink.annotations.Annotation.Metadata;

/**
 * 
 * @author kata
 *
 */
public class WebAnno3TsvHandlerTest {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(WebAnno3TsvHandlerTest.class);
	
	String input = String.join("\n", 
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
			"9-9	445-457	implementing	_	_	_	_	_	_	_	",
			"\n");
	
	

	@Test
	public void testParse() {
		AnnotationHandler h = new WebAnno3TsvHandler();
		List<Annotation> annotations = h.parse(input);
		
		Annotation expectedAnno1 = new Annotation();
		expectedAnno1.setWord("(PIAAC)");
		expectedAnno1.setMetadata(Metadata.title);
		expectedAnno1.setPosition(56);
		expectedAnno1.setCharStart(389);
		expectedAnno1.setCharEnd(396);
		
		Annotation expectedAnno2 = new Annotation();
		expectedAnno2.setWord("Programme");
		expectedAnno2.setMetadata(Metadata.title_i);
		expectedAnno2.setPosition(48);
		expectedAnno2.setCharStart(324);
		expectedAnno2.setCharEnd(333);
		
		Annotation expectedAnno3 = new Annotation();
		expectedAnno3.setWord("Competencies");
		expectedAnno3.setMetadata(Metadata.title_i);
		expectedAnno3.setPosition(55);
		expectedAnno3.setCharStart(376);
		expectedAnno3.setCharEnd(388);
		expectedAnno3.setStartsNewSentence();
		
		int foundExpected = 0;
		for (Annotation anno : annotations) {
			log.debug(anno.toString());
			if (anno.toString().equals(expectedAnno1.toString()) ||
					anno.toString().equals(expectedAnno2.toString()) ||
					anno.toString().equals(expectedAnno3.toString()))
				foundExpected += 1;
		}
		
		assertEquals(3, foundExpected);
	}
}