package searching;



/**
 * Class for saving contexts (= surrounding words of a term).
 * 
 * @author katarina.boland@gesis.org
 *
 */
public class Context {
	
	public String[] leftWords;
	public String left;
	public String term;
	public String[] rightWords;
	public String right;
	public String document;
	public String pattern;
	
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
	public Context(String left, String term, String right, String document, String pattern) {
		this.left = left;
		this.leftWords = left.split("\\s+");
		this.term = term;
		this.right = right;
		this.rightWords = right.split("\\s+");
		this.document = document;
		this.pattern = pattern;
	}
	
	public String toXML() {
		return "\t<context term=\"" + patternLearner.Util.escapeXML(this.term) + 
				"\" document=\"" + this.document + "\">" + System.getProperty("line.separator") + "\t\t" + 
				"<leftContext>" + this.left +"</leftContext>" + System.getProperty("line.separator") + "\t\t" + 
				"<rightContext>" + this.right + "</rightContext>" + System.getProperty("line.separator") + 
				"\t</context>" + System.getProperty("line.separator");
	}
	
	@Override
	public String toString() {
		return this.left + " " + this.term + " " + this.right;
	}
	
}
