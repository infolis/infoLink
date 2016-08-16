package io.github.infolis.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class BaseModel {

	@JsonIgnore
	private String uri;
	
	/**
	 * Free-form tags to assign to the execution and all generated entities. 
	 * This makes all uploaded and generated data identifiable and searchable 
	 * e.g. using infolisFileTags and infolisPatternTags.
	 */
	private Set<String> tags = new HashSet<>();
	
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public Set<String> getTags() {
		return this.tags;
	}
	
	public void setTags(Set<String> tags) {
		this.tags = tags;
	}
	
    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new HashSet<>();
        }
        this.tags.add(tag);
    }
    
    public void addAllTags(Collection<String> tags) {
    	if (this.tags == null) this.tags = new HashSet<>();
    	this.tags.addAll(tags);
    }

}
