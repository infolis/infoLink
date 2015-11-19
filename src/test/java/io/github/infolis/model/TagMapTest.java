package io.github.infolis.model;

import org.junit.Assert;
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
		tm.getInfolisFileTags().add("test");
		String tmSer = SerializationUtils.toJSON(tm);
		Assert.assertTrue(tmSer.contains("infolisPattern"));
		Assert.assertTrue(tmSer.contains("[\"test\"]"));

	}

}
