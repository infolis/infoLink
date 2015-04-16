package io.github.infolis.model;

import java.util.ArrayList;

/**
 * Class for representing phrase chunks
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
public class Chunk
{
	String startTag;
	String endTag;
	ArrayList<TaggedWord> words;

	/**
	 * Class constructor specifying the startTag and endTag symbols and a
	 * list of words constituting this phrase chunk
	 * 
	 * @param startTag
	 *            symbol representing the startTag
	 * @param endTag
	 *            symbol representing the endTag
	 * @param words
	 *            list of words contained in this phrase chunk
	 */
	public Chunk(String startTag, String endTag, ArrayList<TaggedWord> words)
	{
		this.startTag = startTag;
		this.endTag = endTag;
		this.words = words;
	}

	/**
	 * Overrides the toString method: the string representation of a phrase
	 * chunk consists of the string representation of the contained
	 * TaggedWords enclosed by phrase chunk start and end tags
	 */
	@Override
	public String toString()
	{
		String string = "";
		for (TaggedWord word : this.words) {
			string += " " + word;
		}
		return this.startTag + " " + string.trim() + " " + this.endTag;
	}

	/**
	 * Returns the string representation of the TaggedWords contained in
	 * this phrase chunk
	 * 
	 * @return the string representation of the TaggedWords contained in
	 *         this phrase chunk
	 */
	public String getString()
	{
		String string = "";
		for (TaggedWord word : this.words) {
			string += " " + word.getString();
		}
		return string.trim();
	}
}