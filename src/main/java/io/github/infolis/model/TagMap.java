package io.github.infolis.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * 
 * @author kata
 *
 */
public class TagMap {
	
	private List<SelectionTags> classnamesAndTags;
	
	public TagMap() {
		classnamesAndTags = new ArrayList<>();
	}
	
	public void addClassnameAndTags(String className, Set<String> tags) {
		SelectionTags selectionTags = new SelectionTags(className, tags);
		classnamesAndTags.add(selectionTags);
	}
	
	public Set<String> get(String className) {
		for (SelectionTags selTags : this.classnamesAndTags) {
			String classname = selTags.getClassname();
			Set<String> tags = selTags.getTags();
			if (className.equals(classname)) return tags;
		}
		return new HashSet<String>();
	}
		
	public void setTagMap(Multimap<String, String> tagMap) {
		for (String key : tagMap.keys()) {
			Collection<String> vals = tagMap.get(key);
			addClassnameAndTags(key, new HashSet<>(vals));
		}
	}
	
	public Multimap<String, String> asMultimap() {
		Multimap<String, String> multimap = HashMultimap.create();
		for (SelectionTags selTags : this.classnamesAndTags) {
			String classname = selTags.getClassname();
			Set<String> tags = selTags.getTags();
			for (String tag : tags) multimap.put(classname, tag);
		}
		return multimap;
	}
	
	public List<SelectionTags> getClassnamesAndTags() {
		return this.classnamesAndTags;
	}

	public class SelectionTags {
		String className;
		Set<String> tags;
		
		public SelectionTags(String className, Set<String> tags) {
			this.className = className;
			this.tags = tags;
		}
		
		public String getClassname() {
			return this.className;
		}
		
		public Set<String> getTags() {
			return this.tags;
		}
	}
	
}