package io.github.infolis.model;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.util.SerializationUtils;

public class TagMapTest
{

	Logger log = LoggerFactory.getLogger(TagMapTest.class);

	@Test
	public void testTagMap() throws Exception
	{
		TagMap tm = new TagMap();
		log.debug("{}", SerializationUtils.toJSON(tm));

	}

}
