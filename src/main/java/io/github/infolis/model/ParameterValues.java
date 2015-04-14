package io.github.infolis.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author kba
 */
@JsonSerialize(using=ParameterValuesSerializer.class)
@JsonDeserialize(using=ParameterValuesDeserializer.class)
public class ParameterValues {
	
	public ParameterValues() {
		this.values = new HashMap<>();
	}

	private Map<String, List<String>> values;

	public Map<String, List<String>> getValues() {
		return values;
	}

	public void setValues(Map<String, List<String>> values) {
		this.values = values;
	}

	public void put(String name, List<String> value) {
		this.values.put(name, value);
	}
	
	public void put(String name, String value) {
		List<String> list = this.values.get(name);
		if (null == list) {
			list = new ArrayList<String>();
			this.values.put(name, list);
		}
		if (list.isEmpty()) {
			list.add(value);
		} else {
			throw new IllegalArgumentException("Will not replace existing list value with single string");
		}
	}
	
	public List<String> get(String name) {
		return this.values.get(name);
	}
	
	public boolean containsKey(String name) {
		return this.values.containsKey(name);
	}

	public String getFirst(String name) {
		List<String> list = this.values.get(name);
		if (list == null || list.isEmpty()) {
			return null;
		} else {
			return list.get(0);
		}
	}
	
	public Set<String> keySet() {
		return this.values.keySet();
	}

	public void putEmpty(String name) {
		this.values.put(name, new ArrayList<String>());
	}

}