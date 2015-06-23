package io.github.infolis.infolink.datasetMatcher;

import io.github.infolis.model.Study;

import javax.json.JsonArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Filter {
	
	Logger log = LoggerFactory.getLogger(Filter.class);

	//TODO: implement
	public static JsonArray filter(JsonArray candidates, Study study) {
		return candidates;
	}
}