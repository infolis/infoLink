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
	
	private Map<String, List<String>> values = new HashMap<>();

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
		if (null == this.values.get(name)) {
			this.values.put(name, new ArrayList<String>());
		}
        this.values.get(name).add(value);
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
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		for (String name : keySet()) {
			ret.append(name);
			ret.append("\n");
			for (String value : get(name)) {
				ret.append("\t");
				ret.append(value);
				ret.append("\n");
			}
		}
		return ret.toString();
	}

}