package io.github.infolis.infolink.patternLearner;

import io.github.infolis.algorithm.Bootstrapping;
import io.github.infolis.model.TextualReference;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.queryParser.ParseException;

/**
 * 
 * @author kata
 *
 */
public interface BootstrapLearner {
	
	List<TextualReference> bootstrap() throws ParseException, IOException, InstantiationException, IllegalAccessException;
	
	public Bootstrapping.PatternInducer getPatternInducer();

    public Bootstrapping.PatternRanker getPatternRanker();
}