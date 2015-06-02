package io.github.infolis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class for storing words along with their POS tags
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaggedWord
{
	private String string;
	private String tag;

	/**
	 * Class constructor specifying the word and its POS tag
	 * 
	 * @param word
	 *            string representation of the word
	 * @param tag
	 *            string representation of the word's POS tag
	 */
	public TaggedWord(String word, String tag)
	{
		this.setString(word);
		this.tag = tag;
	}

	/**
	 * Overrides the toString method: the String representation of a
	 * TaggedWord consists of the string representation of the word + the
	 * string representation of its tag separated by whitespace
	 */
	@Override
	public String toString()
	{
		return this.getString() + " " + this.tag;
	}

	/**
	 * Overrides the equals method: two TaggedWords are equal if the string
	 * representations of their words are equal (case-insensitive!) and they
	 * share the same POS-tag
	 * 
	 */
	@Override
	public boolean equals(Object w2)
	{
		return (w2 instanceof TaggedWord && (this.getString().toLowerCase() + this.tag)
				.equals(((TaggedWord) w2).getString().toLowerCase() + ((TaggedWord) w2).tag));
	}

	/**
	 * Overrides the hashCode method: computes the hashCode for the string
	 * representation of the TaggedWord
	 */
	@Override
	public int hashCode()
	{
		return (this.getString().toLowerCase() + this.tag).hashCode();
	}

	/**
	 * Converts a TaggedWord to lowerCase by applying toLowerCase on the
	 * string representation of the word and leaving the tag unaltered
	 * 
	 * @return a new TaggedWord in lowerCase
	 */
	public TaggedWord toLowerCase()
	{
		return (new TaggedWord(this.getString().toLowerCase(), this.tag));
	}

	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}
}