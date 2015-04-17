package io.github.infolis.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class BaseModel {

	@JsonIgnore
	private String uri;
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}

}
