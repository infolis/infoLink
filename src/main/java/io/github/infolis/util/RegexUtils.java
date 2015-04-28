package io.github.infolis.util;

import io.github.infolis.infolink.luceneIndexing.Indexer;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

public class RegexUtils {

	public static final String yearRegex = new String("(\\d{4})");
	public static final String percentRegex = new String("\\d+[.,]?\\d*%");
	public static final String numberRegex = new String("\\d+[.,]?\\d*");
	public static final Pattern[] patterns = getContextMinerYearPatterns();
	public static final String delimiter_csv = "|";
	public static final String delimiter_internal = "--@--";
	public static final String[] enumeratorList = {",", ";", "/", "\\\\"};
	public static final String urlPatString = "((\\w+?://)|(www.*?\\.)).+\\.[a-zA-Z][a-zA-Z][a-zA-Z]*";
	public static final Pattern urlPat = Pattern.compile(urlPatString);
	// restricts study names to contain at most 3 words (and at least 3 characters)
	//public static String studyRegex_ngram = new String("(\\S+?\\s?\\S+?\\s?\\S+?)");
	// restricts study names to contain at most 5 words (and at least 3 characters)
	public static final String studyRegex_ngram = new String("(\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)");
	//public static String studyRegex_ngram = new String("(\\S*?\\s?\\S*?\\s?\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?\\s?\\S*?\\s?\\S*?)");
	// restricts study names to contain at most 6 words (and at least 3 characters)
	// slower and does not yield better results: 5 words seem to be optimal
	//public static String studyRegex_ngram = new String("(\\S*?\\s?\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?)");
	// word = any char sequence not containing whitespace (punctuation is seen as part of the word here)
	public static final String studyRegex = new String("(\\S+?)");
	// use atomic grouping where possible to prevent catastrophic backtracking
	public static final String wordRegex = new String("\\S+?");
	public static final String wordRegex_atomic = new String("\\S++");
	// use greedy variant for last word - normal wordRegex would only extract first character of last word
	public static final String lastWordRegex = new String("\\S+");

    public static final String leftContextPat = "(" + RegexUtils.wordRegex + "\\s+" + RegexUtils.wordRegex + "\\s+" + RegexUtils.wordRegex + "\\s+" + RegexUtils.wordRegex + "\\s+" + RegexUtils.wordRegex + "\\s*?" + ")";
    public static final String rightContextPat = "(\\s*?" + RegexUtils.wordRegex + "\\s+" + RegexUtils.wordRegex + "\\s+" + RegexUtils.wordRegex + "\\s+" + RegexUtils.wordRegex + "\\s+" + RegexUtils.lastWordRegex + ")";

	public static final Pattern patternNumeric = Pattern.compile("\\d+");
	public static final Pattern patternDecimal = Pattern.compile("\\d+\\.\\d+");


	//TODO: change name to denormalizeRegex or something similar...
	/**
	 * Replaces previously inserted placeholders for years, numbers and percent specifications with their 
	 * regular expressions.
	 * 
	 * @param string	input text where placeholders shall be replaced
	 * @return			string with placeholders replaced by regular expressions
	 */
	public static String escapeRegex(String string)
	{	
		String yearNorm = new String("<YEAR>");
		String percentNorm = new String("<PERCENT>");
		String numberNorm = new String("<NUMBER>");
		string = string.replace(yearNorm, yearRegex).replace(percentNorm, percentRegex).replace(numberNorm, numberRegex);
		//return Pattern.quote(string);
		return string;
	}

	public static String normalizeRegex(String term)
	{
		Pattern yearPat = Pattern.compile(yearRegex);
		Pattern percentPat = Pattern.compile(percentRegex);
		Pattern numberPat = Pattern.compile(numberRegex);
		
		String yearNorm = new String("<YEAR>");
		String percentNorm = new String("<PERCENT>");
		String numberNorm = new String("<NUMBER>");
		
		// do not change order of replacements
		Matcher percentMatcher = percentPat.matcher(term);
		term = percentMatcher.replaceAll(percentNorm);
		
		Matcher yearMatcher = yearPat.matcher(term);
		term = yearMatcher.replaceAll(yearNorm);
		
		Matcher numberMatcher = numberPat.matcher(term);
		term = numberMatcher.replaceAll(numberNorm);
		
		return term;
	}
	
	/**
	 * Replaces previously inserted placeholders for years, numbers and percent specifications with their 
	 * regular expressions and quotes all parts of the regular expression that are to be treated as 
	 * strings (all but character classes).
	 * 
	 * @param string	input text where placeholders shall be replaced and all literals quoted
	 * @return			quoted regular expression string
	 */
	public static String normalizeAndEscapeRegex(String string)
	{	//TODO: norm stuff is only needed when writing to and reading from arff files
		//delete additional replacements
		String yearNorm = new String("<YEAR>");
		String percentNorm = new String("<PERCENT>");
		String numberNorm = new String("<NUMBER>");
		string = normalizeRegex(string);
		string = Pattern.quote(string).replace(yearNorm, "\\E" + yearRegex + "\\Q").replace(percentNorm, "\\E" + percentRegex + "\\Q").replace(numberNorm, "\\E" + numberRegex + "\\Q");
		return string;
	}
	
	/**
	 * Normalizes and escapes strings for usage as Lucene queries. 
	 * Replaces placeholders by wildcards, removes characters with special meanings in Lucene and 
	 * normalizes the query using the Lucene Analyzer used for building the Lucene index.
	 * 
	 * @param string	input string to be used as Lucene query
	 * @return			a Lucene query string
	 */
	public static String normalizeAndEscapeRegex_lucene(String string)
	{
		string = string.trim();
		string = normalizeQuery(string, false);
		string = string.replaceAll(yearRegex, "*").replaceAll(percentRegex, "*").replaceAll(numberRegex, "*");
		return string;
	}
	
	/**
	 * Normalizes a query by applying a Lucene analyzer. Make sure the analyzer used here is the 
	 * same as the analyzer used for indexing the text files!
	 * 
	 * @param 	query	the Lucene query to be normalized
	 * @return	a normalized version of the query
	 */
	@SuppressWarnings("deprecation")
	public static String normalizeQuery(String query, boolean quoteIfSpace)
	{
		Analyzer analyzer = Indexer.createAnalyzer();
		String field = "contents";
		String result = new String();
		TokenStream stream = analyzer.tokenStream(field, new StringReader(query));
		try
		{
			while (stream.incrementToken()) {
				result += " " + (stream.getAttribute(TermAttribute.class).term());
			}
		} catch (IOException e) {
			// not thrown due to using a string reader...
		}
		analyzer.close();
		if (quoteIfSpace && result.trim().matches(".*\\s.*")) {
				return "\"" + result.trim() + "\"";
		}
		return result.trim();
	}

	/**
	 * Returns a list of patterns for extracting numerical information. 
	 * 
	 * Patterns should be sorted by priority / reliability (highest priority first), first match is accepted 
	 * by calling method. This way, you can give year specifications a higher weight than other 
	 * number specifications, for example. Currently, only one pattern is used.
	 * 
	 * @return	a list of patterns
	 */
	public static Pattern[] getContextMinerYearPatterns() 
	{
		Pattern[] patterns = new Pattern[1];

		String enumRegex = "(([,;/&\\\\])|(and)|(und))";
		String yearRegex = "(\\d{4})";
		String yearAbbrRegex = "('\\d\\d)";
		String numberRegex = "(\\d+[.,]?\\d*)"; //this includes yearRegex
		String rangeRegex = "(([-–])|(bis)|(to)|(till)|(until))";
		
		String numericInfoRegex = "(" + yearRegex + "|" + yearAbbrRegex + "|" + numberRegex + ")";
		String enumRangeRegex = "(" + enumRegex + "|" + rangeRegex + ")";
		String complexNumericInfoRegex = "(" + numericInfoRegex + "(\\s*" + enumRangeRegex + "\\s*" + numericInfoRegex + ")*)";

		patterns[0] = Pattern.compile(complexNumericInfoRegex);
		return patterns;
	}
	
	/**
	 * Returns a selection of words to be treated as stopwords by the reference extraction algorithm
	 * 
	 * @return	a list of stopwords (English and German)
	 */
	public static Set<String> stopwordList()
	{
		Set<String> stopwords = new HashSet<String>();
		stopwords.add("the");
		stopwords.add("and");
		stopwords.add("on");
		stopwords.add("in");
		stopwords.add("at");
		stopwords.add("from");
		stopwords.add("to");
		stopwords.add("with");
		stopwords.add("for");
		stopwords.add("as");
		stopwords.add("by");
		stopwords.add("of");
		stopwords.add("that");
		stopwords.add("which");
		stopwords.add("who");
		stopwords.add("than");
		
		stopwords.add("als");
		stopwords.add("durch");
		stopwords.add("wegen");
		stopwords.add("der");
		stopwords.add("des");
		stopwords.add("den");
		stopwords.add("deren");
		stopwords.add("dessen");
		stopwords.add("dem");
		stopwords.add("die");
		stopwords.add("das");
		stopwords.add("fuer");
		stopwords.add("für");
		stopwords.add("und");
		stopwords.add("in");
		stopwords.add("im");
		stopwords.add("zu");
		stopwords.add("zur");
		stopwords.add("zum");
		stopwords.add("um");
		stopwords.add("und");
		stopwords.add("mit");
		stopwords.add("ohne");
		stopwords.add("aus");
		stopwords.add("von");
		stopwords.add("vom");
		stopwords.add("nach");
		stopwords.add("zwischen");
		stopwords.add("über");
		stopwords.add("unter");
		stopwords.add("neben");
		stopwords.add("vor");
		stopwords.add("hinter");
		// todo: use list, e.g.  http://de.wiktionary.org/wiki/Wiktionary:Deutsch/Liste_der_Pr%C3%A4positionen
		// + Konjunktionen	
		// placeholders inserted into contexts
		stopwords.add("<year>");
		stopwords.add("<percent>");
		stopwords.add("<number>");
		return stopwords;
	}
	
	/**
	 * Removes all characters that are not allowed in filenames (on windows filesystems).
	 * 
	 * @param seed	the string to be escaped
	 * @return		the escaped string
	 */
	public static String escapeSeed( String seed )
	{
		return seed.replace(":", "_").replace("\\", "_").replace("/", "_").replace("?",  "_").replace(">",  "_").replace("<",  "_");
	}
	
			// TODO needed?
//	/**
//	 * Determines reliablity of instance based on instance ranking: if an instance is extracted by many 
//	 * reliable patterns, it has a high reliability. Reliability of pattern: extracts many reliable instances 
//	 * (in proportion to unreliable instances).
//	 * 
//	 * @param instance	the instance (dataset title) to be assessed
//	 * @return			boolean value: reliablity score above threshold or not
//	 */
//	private double reliability_instance( String instance )
//	{
//		System.out.println("Checking if instance is reliable: " + instance);
//		Reliability.Instance curInstance = this.reliability.instances.get(instance);
//		return reliability(curInstance);
//	}
	//TODO: ADD INSTANCE FILTERING FOR GENERIC PATTERNS (need to substitute 
    //google-based method there...)
    /**
     * Checks whether a given word is a stop word
     *
     * @param word	arbitrary string sequence to be checked
     * @return	true if word is found to be a stop word, false otherwise
     */
    public static boolean isStopword(String word) {
        // word consists of punctuation, whitespace and digits only
        if (word.matches("[\\p{Punct}\\s\\d]*")) {
            return true;
        }
        // trim word, lower case and remove all punctuation
        word = word.replace("\\p{Punct}+", "").trim().toLowerCase();
		// due to text extraction errors, whitespace is frequently added to words resulting in many single characters
        // TODO: use this as small work-around but work on better methods for automatic text correction
        if (word.length() < 2) {
            return true;
        }
        if (RegexUtils.stopwordList().contains(word)) {
            return true;
        }
        // treat concatenations of stopwords as stopword
        for (String stopword : RegexUtils.stopwordList()) {
            if (RegexUtils.stopwordList().contains(word.replace(stopword, ""))) {
                return true;
            }
        }
        return false;
    }
	

}
