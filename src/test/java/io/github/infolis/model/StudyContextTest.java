package io.github.infolis.model;

import static org.junit.Assert.assertEquals;
import io.github.infolis.util.SerializationUtils;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;


public class StudyContextTest {
	
	@Test
	public void testXML() throws JsonProcessingException {
		
                InfolisPattern p = new InfolisPattern();
                p.setPatternRegex("par1");
		StudyContext ctx = new StudyContext("a b c d e f", "FOOBAR!", "v w x y z", "doc1", p);
		assertEquals(SerializationUtils.toXML(ctx).replaceAll("\\s", ""), ctx.toXML().replaceAll("\\s", ""));
	}

}
