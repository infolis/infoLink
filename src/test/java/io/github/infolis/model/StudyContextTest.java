package io.github.infolis.model;

import static org.junit.Assert.assertEquals;
import io.github.infolis.model.util.XmlSerializer;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;


public class StudyContextTest {
	
	@Test
	public void testXML() throws JsonProcessingException {
		
		StudyContext ctx = new StudyContext("a b c d e f", "FOOBAR!", "v w x y z", "doc1", "par1");
		assertEquals(XmlSerializer.toXML(ctx).replaceAll("\\s", ""), ctx.toXML().replaceAll("\\s", ""));
	}

}
