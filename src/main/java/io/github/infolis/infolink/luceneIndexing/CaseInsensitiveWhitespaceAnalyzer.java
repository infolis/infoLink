package io.github.infolis.infolink.luceneIndexing;

import org.apache.lucene.analysis.Analyzer;
import java.io.Reader;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.util.Version;

/**
 * A lucene whitespaceAnalyzer that is case insensitive.
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 */
public class CaseInsensitiveWhitespaceAnalyzer extends Analyzer
{

	/**
	 * Implements the tokenStream method to add WhitespaceTokenizer and LowerCaseFilter.
	 * 
	 * @param fieldName	name of the field to convert to tokenStream
	 * @param reader	reader for conversion to tokenStream
	 */
	public TokenStream tokenStream(String fieldName, Reader reader)
 	{
 	    TokenStream t = null;
 	    t = new WhitespaceTokenizer(Version.LUCENE_35, reader);
 	    t = new LowerCaseFilter(Version.LUCENE_35, t);
	    return t;
 	}
}