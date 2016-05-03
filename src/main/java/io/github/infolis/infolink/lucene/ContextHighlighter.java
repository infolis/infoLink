package io.github.infolis.infolink.lucene;

import java.text.BreakIterator;

import org.apache.lucene.search.postingshighlight.CustomSeparatorBreakIterator;
import org.apache.lucene.search.postingshighlight.PassageScorer;
import org.apache.lucene.search.postingshighlight.PostingsHighlighter;
import org.apache.lucene.search.postingshighlight.WholeBreakIterator;

/**
 * 
 * @author kata
 *
 */
public class ContextHighlighter extends PostingsHighlighter {
	
	public ContextHighlighter() {
		super();
	}
	
	public ContextHighlighter(int maxChars) {
		super(maxChars);
	}

	@Override
	protected BreakIterator getBreakIterator(String field) {
		return new CustomSeparatorBreakIterator(System.getProperty("line.separator").charAt(0));
	}
	/*
	@Override
	protected PassageScorer getScorer(String field) {
		return new PassageScorer(10.0f, 10.0f, 10.0f);
	}
	*/
}