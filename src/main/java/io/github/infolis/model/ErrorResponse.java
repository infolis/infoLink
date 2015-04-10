package io.github.infolis.model;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class ErrorResponse {
	
	private String stack;
	private String message;
	private Map<String,Object> cause;
	public String getStack() {
		return stack;
	}
	public void setStack(String stack) {
		this.stack = stack;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Map<String, Object> getCause() {
		return cause;
	}
	public void setCause(Map<String, Object> cause) {
		this.cause = cause;
	}

}
