package io.github.infolis.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ParameterValuesTest {
	
	@Test
	public void testDeserialize() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        assertTrue(objectMapper.canSerialize(ParameterValues.class));

        {
        	String testInput = "{'foo':'bar'}".replace('\'', '"');
        	ParameterValues readValue = objectMapper.readValue(testInput, ParameterValues.class);
        	assertThat(readValue, not(nullValue()));
        	assertThat(readValue.getValues().keySet().size(), equalTo(1));
        	assertThat(readValue.getValues().get("foo"), not(nullValue()));
        	assertThat(readValue.getValues().get("foo").size(), equalTo(1));
        	assertThat(readValue.getValues().get("foo").get(0), is("bar"));
        	assertThat(objectMapper.writeValueAsString(readValue), equalTo("{'foo':['bar']}".replace('\'', '"')));
        }
        {
        	String testInput = "{'foo':['bar']}".replace('\'', '"');
        	ParameterValues readValue = objectMapper.readValue(testInput, ParameterValues.class);
        	assertThat(readValue, not(nullValue()));
        	assertThat(readValue.getValues().keySet().size(), equalTo(1));
        	assertThat(readValue.getValues().get("foo"), not(nullValue()));
        	assertThat(readValue.getValues().get("foo").size(), equalTo(1));
        	assertThat(readValue.getValues().get("foo").get(0), is("bar"));
        	assertThat(objectMapper.writeValueAsString(readValue), equalTo(testInput));
        }
		
	}
}
