package io.github.infolis.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ParameterValuesSerializer extends JsonSerializer<ParameterValues> {

	@Override
	public void serialize(ParameterValues value, JsonGenerator jgen,
			SerializerProvider provider) throws IOException,
			JsonProcessingException {
		jgen.writeStartObject();
		for (String fieldName : value.getValues().keySet()) {
            jgen.writeArrayFieldStart(fieldName);
            for (String fieldValue : value.getValues().get(fieldName)) {
            	jgen.writeString(fieldValue);
            }
            jgen.writeEndArray();
		}
		jgen.writeEndObject();
	}

}
