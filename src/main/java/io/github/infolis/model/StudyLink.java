//package io.github.infolis.model;
//
//import io.github.infolis.util.RegexUtils;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//
///**
// * Class representing the link between a publication and a dataset.
// * 
// * Instances of <emph>StudyLink</emph> have the following fields:
// * <ul>
// * <li>publication: URI of the publication</li>
// * <li>name: title of the linked dataset</li>
// * <li>alt_name: title of the referenced dataset</li>
// * <li>version: year or number of the referenced dataset</li>
// * <li>link: the linked dataset</li>
// * <li>type: a StudyType instance denoting whether <emph>link</emph> contains a
// * DOI, a URL or a string</li>
// * <li>snippet: text snippet from which the dataset reference was extracted</li>
// * <li>confidence: probability that the StudyLink instance is correct</li>
// * </ul>
// * 
// * @author kata
// *
// */
//@JsonIgnoreProperties(ignoreUnknown = true)
//public class StudyLink
//{
//	StudyType type;
//	float confidence; // 1.0 denotes highest confidence, 0 lowest...
//	String name;
//	String version;
//	private String link;
//	private String altName;
//	private String snippet;
//	private ExtractionMethod method;
//	private String publication;
//
//	/**
//	 * Class constructor specifying the publication URI, name, version, alt_name, link, type,
//	 * confidence, snippet and extraction method of a <emph>StudyLink</emph>
//	 * instance.
//	 * 
//	 * @param publication
//	 * 			  URI of the publication
//	 * @param name
//	 *            title of the linked dataset
//	 * @param version
//	 *            year or number of the referenced dataset
//	 * @param alt_name
//	 *            title of the referenced dataset
//	 * @param link
//	 *            the linked dataset
//	 * @param type
//	 *            StudyType instance denoting whether the linked dataset is
//	 *            identified by a DOI, a URL or a string
//	 * @param confidence
//	 *            probability that the StudyLink instance is correct
//	 * @param snippet
//	 *            text snippet from which the dataset reference was extracted
//	 * @param method
//	 *            method used for extraction (pattern-based vs. term-based
//	 *            search)
//	 */
//	public StudyLink(String publication, String name, String version, String alt_name, String link, StudyType type,
//			float confidence, String snippet, ExtractionMethod method)
//	{
//		this.publication = publication;
//		this.name = name;
//		this.type = type;
//		this.confidence = confidence;
//		this.version = version;
//		this.setLink(link);
//		this.setAltName(alt_name);
//		this.setSnippet(snippet);
//		this.setMethod(method);
//	}
//
//	@Override
//	public String toString()
//	{
//		String delimiter = RegexUtils.delimiter_csv;
//		String _alt_name;
//		String _name;
//		String _version;
//		String _link;
//		// some fields may be null, trying to replace chars will provoke
//		// NullPointerException
//		try {
//			_alt_name = this.getAltName().replace(delimiter, "").trim();
//		} catch (NullPointerException npe) {
//			_alt_name = "";
//		}
//		try {
//			_name = this.name.replace(delimiter, "").trim();
//		} catch (NullPointerException npe) {
//			_name = "";
//		}
//		try {
//			_version = this.version.replace(delimiter, "").trim();
//		} catch (NullPointerException npe) {
//			_version = "";
//		}
//		try {
//			_link = this.getLink().replace(delimiter, "").trim();
//		} catch (NullPointerException npe) {
//			_link = "";
//		}
//		String _snippet = this.snippet.replace(delimiter, "").trim();
//
//		return publication + delimiter + _name + delimiter + _alt_name + delimiter + _version + delimiter + _link
//				+ delimiter  + this.type.toString() + delimiter + _snippet
//				+ delimiter + String.valueOf(this.confidence) 
//				+ delimiter + this.getMethod().toString();
//	}
//
//	public String getPublication() 
//	{
//		return this.publication;
//	}
//	
//	public String getName()
//	{
//		return this.name;
//	}
//
//	public String getVersion()
//	{
//		return this.version;
//	}
//
//	public StudyType getType()
//	{
//		return this.type;
//	}
//
//	public float getConfidence()
//	{
//		return confidence;
//	}
//
//	public void setConfidence(int confidence)
//	{
//		this.confidence = confidence;
//	}
//
//	public String getAltName() {
//		return altName;
//	}
//
//	public void setAltName(String altName) {
//		this.altName = altName;
//	}
//
//	public String getLink() {
//		return link;
//	}
//
//	public void setLink(String link) {
//		this.link = link;
//	}
//
//	public ExtractionMethod getMethod() {
//		return method;
//	}
//
//	public void setMethod(ExtractionMethod method) {
//		this.method = method;
//	}
//
//	public String getSnippet() {
//		return snippet;
//	}
//
//	public void setSnippet(String snippet) {
//		this.snippet = snippet;
//	}
//}