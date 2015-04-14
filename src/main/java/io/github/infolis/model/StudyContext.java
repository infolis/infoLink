package io.github.infolis.model;

import io.github.infolis.infolink.patternLearner.Util;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;



/**
 * Class for saving contexts (= surrounding words of a term).
 * 
 * @author katarina.boland@gesis.org
 * @author kba
 *
 */
@XmlRootElement(name="context")
@XmlAccessorType(XmlAccessType.FIELD)
public class StudyContext extends BaseModel {
	
	@XmlTransient
	private List<String> leftWords;
	@XmlTransient
	private List<String> rightWords;
	@XmlElement(name="leftContext")
	private String leftText;
	@XmlElement(name="rightContext")
	private String rightText;
    @XmlAttribute
	private String term;
    @XmlAttribute
    private String document;
    @XmlTransient
	private String pattern;
	
	public StudyContext() { }

	/**
	 * Class constructor specifying the left context, the term, the right context, the document from which 
	 * the context was extracted and the pattern used to extract the context.
	 * 
	 * @param left
	 * @param term
	 * @param right
	 * @param document
	 * @param pattern
	 */
	public StudyContext(String left, String term, String right, String document, String pattern) {
		this.setLeftText(left);
		this.setLeftWords(Arrays.asList(left.split("\\s+")));
		this.setTerm(term);
		this.setRightText(right);
		this.setRightWords(Arrays.asList(right.split("\\s+")));
		this.setDocument(document);
		this.setPattern(pattern);
	}
	
	public String toXML() {
		return "\t<context term=\"" + Util.escapeXML(this.getTerm()) + 
				"\" document=\"" + this.getDocument() + "\">" + System.getProperty("line.separator") + "\t\t" + 
				"<leftContext>" + this.getLeftText() +"</leftContext>" + System.getProperty("line.separator") + "\t\t" + 
				"<rightContext>" + this.getRightText() + "</rightContext>" + System.getProperty("line.separator") + 
				"\t</context>" + System.getProperty("line.separator");
	}
	
	@Override
	public String toString() {
		return this.getLeftText() + " " + this.getTerm() + " " + this.getRightText();
	}

	public List<String> getLeftWords() {
		return leftWords;
	}

	public void setLeftWords(List<String> leftWords) {
		this.leftWords = leftWords;
	}

	public List<String> getRightWords() {
		return rightWords;
	}

	public void setRightWords(List<String> rightWords) {
		this.rightWords = rightWords;
	}

	public String getLeftText() {
		return leftText;
	}

	public void setLeftText(String leftText) {
		this.leftText = leftText;
	}

	public String getRightText() {
		return rightText;
	}

	public void setRightText(String rightText) {
		this.rightText = rightText;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public String getDocument() {
		return document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	
}
