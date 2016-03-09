package io.github.infolis.infolink.annotations;

import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author kata
 *
 */
public class Annotation {
	String word;
	int position;
	//int: start position of word
	Map<Integer, Relation> relationMap = new HashMap<>();
	Metadata metadata;
	
	public void setWord(String word) {
		this.word = word;
	}
	
	public String getWord() {
		return this.word;
	}
	
	public void setPosition(int position) {
		this.position = position;
	}
	
	public int getPosition() {
		return this.position;
	}
	
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
	
	public Metadata getMetadata() {
		return this.metadata;
	}
	
	public void addRelation(int pos, Relation relation) {
		this.relationMap.put(pos, relation);
	}
	
	public Map<Integer, Relation> getRelationMap() {
		return this.relationMap;
	}
	
	public enum Metadata {
		title, vagueTitle, scale, year, number, version, creator, id, url, geographical_coverage, topic, sample, project_title, project_funder, none
	}

	public enum Relation {
		describes, uses_sub_part, same_as
	}
	
}