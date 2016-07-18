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
	int charStart = Integer.MIN_VALUE;
	int charEnd = Integer.MIN_VALUE;
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
		setCharStart(new Integer(copyFrom.getCharStart()));
		setCharEnd(new Integer(copyFrom.getCharEnd()));
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
	
	public void setCharStart(int charStart) {
		this.charStart = charStart;
	}
	
	public int getCharStart() {
		return this.charStart;
	}
	
	public void setCharEnd(int charEnd) {
		this.charEnd = charEnd;
	}
	
	public int getCharEnd() {
		return this.charEnd;
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
		id, id_b, id_i,
		creator, creator_b, creator_i,
		publisher, publisher_b, publisher_i,
		geographical_coverage, geographical_coverage_b, geographical_coverage_i,
		year, year_b, year_i,
		number, number_b, number_i,
		version, version_b, version_i,
		url, url_b, url_i,
		topic, topic_b, topic_i,
		sample, sample_b, sample_i,
		project_funder, project_funder_b, project_funder_i,
		none
	}

	public enum Relation {
		describes, uses_sub_part, same_as
	}
	
	@Override
	public String toString() {
		return String.format("word: %s, position: %s, charStart: %s, charEnd: %s, startsNewSentence: %s metadata: %s", 
				this.word, this.position, this.charStart, this.charEnd, this.startsNewSentence, this.metadata);
	}
	
}