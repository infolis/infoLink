package io.github.infolis.model;

import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author kata
 *
 */
public class TagMap {
	
	private Set<String> infolisPatternTags = new HashSet<>();
	private Set<String> infolisFileTags = new HashSet<>();

	public Set<String> getInfolisPatternTags()
	{
		return infolisPatternTags;
	}
	public void setInfolisPatternTags(Set<String> infolisPatternTags)
	{
		this.infolisPatternTags = infolisPatternTags;
	}
	public Set<String> getInfolisFileTags()
	{
		return infolisFileTags;
	}
	public void setInfolisFileTags(Set<String> infolisFileTags)
	{
		this.infolisFileTags = infolisFileTags;
	}
	
	
	
}