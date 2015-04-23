package io.github.infolis.model;

import static org.junit.Assert.assertEquals;
import io.github.infolis.util.SerializationUtils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;


public class StudyContextTest {
	
	Logger log = LoggerFactory.getLogger(StudyContextTest.class);
	
	@Test
	public void testXML() throws JsonProcessingException {
		
                InfolisPattern p = new InfolisPattern();
                p.setPatternRegex("par1");
		StudyContext ctx = new StudyContext("a b c d e f", "FOOBAR!", "v w x y z", "doc1", p);
		assertEquals(SerializationUtils.toXML(ctx).replaceAll("\\s", ""), ctx.toXML().replaceAll("\\s", ""));
		String x = SerializationUtils.jacksonMapper.writeValueAsString(ctx);
		log.debug(x);
	}
}
