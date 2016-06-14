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
	boolean startsNewSentence;
	
	public Annotation() {
		
	}
	
	public Annotation(Annotation copyFrom) {
		setWord(new String(copyFrom.getWord()));
		setPosition(new Integer(copyFrom.getPosition()));
		setRelationMap(new HashMap<>(copyFrom.getRelationMap()));
		setMetadata(copyFrom.getMetadata());
		this.startsNewSentence = copyFrom.getStartsNewSentence();
	}
	
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
	
	public void setStartsNewSentence() {
		this.startsNewSentence = true;
	}
	
	public boolean getStartsNewSentence() {
		return this.startsNewSentence;
	}
	
	public void addRelation(int pos, Relation relation) {
		this.relationMap.put(pos, relation);
	}
	
	public void setRelationMap(Map<Integer, Relation> relationMap) {
		this.relationMap = relationMap;
	}
	
	public Map<Integer, Relation> getRelationMap() {
		return this.relationMap;
	}
	
	public enum Metadata {
		title, title_b, title_i, 
		vagueTitle, vagueTitle_b, vagueTitle_i, 
		scale, scale_b, scale_i,
		project_title, project_title_b, project_title_i, 
		creator, creator_b, creator_i,
		year, number, version, id, url, geographical_coverage, topic, sample, project_funder, none
	}

	public enum Relation {
		describes, uses_sub_part, same_as
	}
	
	@Override
	public String toString() {
		return String.format("word: %s, position: %s, startsNewSentence: %s metadata: %s", 
				this.word, this.position, this.startsNewSentence, this.metadata);
	}
	
}