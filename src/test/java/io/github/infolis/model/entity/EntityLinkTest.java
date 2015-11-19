package io.github.infolis.model.entity;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.util.SerializationUtils;

public class EntityLinkTest
{
	
	Logger log = LoggerFactory.getLogger(EntityLinkTest.class);
	
	@Test
	public void testName() throws Exception
	{
		
		EntityLink link = new EntityLink();
		Entity e1 = new Entity();
		e1.setName("foo");
		Entity e2 = new Entity();
		e1.setName("bar");
		link.setFromEntity(e1);
		link.setToEntity(e2);
		
		log.debug(SerializationUtils.toJSON(link));


	}

}
