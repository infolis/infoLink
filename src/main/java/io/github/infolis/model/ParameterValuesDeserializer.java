package io.github.infolis.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ValueNode;

/*
 * Make sure that even string values are parsed as List<String>, so
 * interface is uniform for single and multi-value fields
 */
public class ParameterValuesDeserializer extends JsonDeserializer<ParameterValues> {

	@Override
	public ParameterValues deserialize(JsonParser jp,
			DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		TreeNode node = jp.getCodec().readTree(jp);
		Map<String, List<String>> ret = new HashMap<>();
		Iterator<String> paramNames = node.fieldNames();
		while (paramNames.hasNext()) {
			String next = paramNames.next();
			TreeNode paramNode = node.get(next);
			ArrayList<String> wrapperList = new ArrayList<>();
			if (paramNode.isValueNode()) {
				wrapperList.add(((ValueNode) paramNode).asText());
			} else if (paramNode.isArray()) {
				ArrayNode paramArray = (ArrayNode) paramNode;
				Iterator<JsonNode> valuesIter = paramArray.elements();
				while (valuesIter.hasNext()) {
					JsonNode valuesListValue = valuesIter.next();
					if (valuesListValue.isValueNode()) {
                        wrapperList.add(((ValueNode) valuesListValue).asText());
					} else {
						throw new IllegalArgumentException();
					}
				}
			} else {
				throw new IllegalArgumentException();
			}
			ret.put(next, wrapperList);
		}
		ParameterValues newPV = new ParameterValues();
		newPV.setValues(ret);
		return newPV;
	}

}