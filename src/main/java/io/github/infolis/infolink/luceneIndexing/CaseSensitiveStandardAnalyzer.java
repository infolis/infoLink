package io.github.infolis.infolink.luceneIndexing;

import org.apache.lucene.analysis.Analyzer;
import java.io.Reader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.util.Version;

/**
 * A lucene StandardAnalyzer that is case-sensitive.
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 */
public final class CaseSensitiveStandardAnalyzer extends Analyzer
{

	/**
	 * Implements the tokenStream method to add StandardTokenizer with StandardFilter out 
	 * without LowerCaseFilter and without StopFilter.
	 * 
	 * @param fieldName	name of the field to convert to tokenStream
	 * @param reader	reader for conversion to tokenStream
	 */
	public final TokenStream tokenStream(String fieldName, Reader reader)
 	{
 	    TokenStream t = null;
 	    t = new StandardTokenizer(Version.LUCENE_35, reader);
 	    t = new StandardFilter(Version.LUCENE_35, t);
	    return t;
 	}
}