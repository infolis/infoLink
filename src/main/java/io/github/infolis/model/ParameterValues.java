package io.github.infolis.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author kba
 */
@JsonSerialize(using=ParameterValuesSerializer.class)
@JsonDeserialize(using=ParameterValuesDeserializer.class)
public class ParameterValues {

	private Map<String, List<String>> values;

	public Map<String, List<String>> getValues() {
		return values;
	}

	public void setValues(Map<String, List<String>> values) {
		this.values = values;
	}

}
