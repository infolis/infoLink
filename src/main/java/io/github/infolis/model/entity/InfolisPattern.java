/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.model.entity;

import io.github.infolis.infolink.patternLearner.Reliability;
import io.github.infolis.util.RegexUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.queryParser.ParseException;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.infolis.model.TextualReference;
import java.util.HashMap;

/**
 *
 * @author kata
 * @author domi
 * @author kba
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InfolisPattern extends Entity {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(InfolisPattern.class);
    // TODO can this be final?
    private String patternRegex;
    private String luceneQuery;
    private String minimal;
    private List<String> words = new ArrayList<>();
    private double threshold;
    private double reliability;
    private Map<String, Double> associations = new HashMap<>();
    
    public InfolisPattern(String patternRegex, String luceneQuery, String minimal, List<String> words, double threshold) {
    	super(minimal);
    	this.setLuceneQuery(luceneQuery);
    	this.setPatternRegex(patternRegex);
    	this.setMinimal(minimal);
    	this.setWords(words);
    	this.setThreshold(threshold);
	}

    public InfolisPattern(String patternRegex) {
    	super();
    	this.setPatternRegex(patternRegex);
	}

	public InfolisPattern() {
		super();
	}

	/**
     * @return the patternRegex
     */
    public String getPatternRegex() {
        return patternRegex;
    }

    /**
     * @return the luceneQuery
     */
    public String getLuceneQuery() {
        return luceneQuery;
    }
    
    /**
     * @return the words
     */
    public List<String> getWords() {
        return words;
    }
    
	public void setPatternRegex(String patternRegex) {
		this.patternRegex = patternRegex;
	}

	public void setLuceneQuery(String luceneQuery) {
		this.luceneQuery = luceneQuery;
	}

    /**
     * @return the minimal
     */
    public String getMinimal() {
        return minimal;
    }

    /**
     * @param minimal the minimal to set
     */
    public void setMinimal(String minimal) {
        this.minimal = minimal;
    }
    
    /**
     * 
     * @param words the words to set
     */
    public void setWords(List<String> words) {
    	this.words = words;
    }

    /**
     * Determines whether this pattern is suitable for extracting
     * dataset references using a frequency-based measure. As threshold, 
     * this pattern's threshold is used. 
     *
     * @param contextStrings	set of extracted contexts as strings
     * @return				<emph>true</emph>, if regex is found to be relevant,
     * <emph>false</emph> otherwise
     */
    public boolean isRelevant(List<String> contextStrings) {
        // compute score for similar to tf-idf...
        // count occurrences of regex in positive vs negative contexts...
        int count_pos = 0;
        //int count_neg = 0;
        //List<String> contexts_neg = new ArrayList<>();
        for (String context : contextStrings) {
            count_pos += patternFound(this.minimal, context);
        }
        /*
        for (String context : contexts_neg) {
            count_neg += patternFound(regex, context);
        }*/

        //TODO: rename - this is not really tf-idf ;)
        // compute relevance...
        /*
        double idf = 0;
        if (count_neg + count_pos > 0) {
            idf = MathUtils.log2((double) (contextStrings.size() + contexts_neg.size()) / (count_neg + count_pos));
        }*/
        int idf = 1;

        double tf_idf = ((double) count_pos / contextStrings.size()) * idf;
        log.debug("Relevance score: " + tf_idf);
        log.debug("Occurrences: " + count_pos);
        if ((tf_idf >= this.getThreshold()) & (count_pos > 0)) {
            return true;
        } else {
            return false;
        }
    }

    // TODO return boolean
    //TODO: use safeMatching instead of m.find()!
    /**
     * Returns whether regular expression <emph>pattern</emph> can be found in
     * string <emph>text</emph>.
     *
     * @param pattern	regular expression to be searched in <emph>text</emph>
     * @param text	input string sequence to search <emph>pattern</emph> in
     * @return	true, if <emph>pattern</emph> is found in <emph>text</emph>,
     * false otherwise
     */
    private static int patternFound(String pattern, String text) {
        Pattern pat = Pattern.compile(pattern);
        Matcher m = pat.matcher(text);
        boolean patFound = m.find();
        if (patFound) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Generates a regular expression to capture given <emph>title</emph> as
     * dataset title along with any number specifications.
     *
     * @param title	name of the dataset to find inside the regex
     * @return	a regular expression for finding the given title along with any
     * number specifications
     */
    private static String constructTitleVersionRegex(String title) {
        // at least one whitespace required...
        return "(" + title + ")" + "\\S*?" + "\\s+" + "\\S*?" + "\\s*" + "\\S*?" + "\\s*" + "\\S*?" + "\\s*" + "\\S*?" + "\\s*" + "\\S*?" + "\\s*" + "((" + RegexUtils.yearRegex + "\\s*((-)|(–))\\s*\\d\\d(\\d\\d)?" + ")|(" + RegexUtils.yearRegex + ")|(\\d+[.,-/\\\\]?\\d*))";
    }

    /**
     * Generates regular expressions for finding dataset names listed in
     * <emph>filename</emph>
     * with titles and number specifications.
     *
     * @param filename	Name of the file containing a list of dataset names (one
     * name per line)
     * @return	A Set of Patterns
     */
    public static Set<InfolisPattern> constructPatterns(String filename) {
        Set<InfolisPattern> patternSet = new HashSet<>();
        try {
            File f = new File(filename);
            InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF8");
            BufferedReader reader = new BufferedReader(isr);
            String studyTitle;
            while ((studyTitle = reader.readLine()) != null) {
                if (!studyTitle.matches("\\s*")) {
                    InfolisPattern p = new InfolisPattern(constructTitleVersionRegex(studyTitle));
                    patternSet.add(p);
                }
            }
            reader.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return patternSet;
    }
    
    public boolean isReliable(int dataSize, Set<Instance> reliableInstances, Reliability r) throws IOException, ParseException {
    	this.reliability = r.computeReliability(dataSize, reliableInstances, this);
        if (this.getReliability() >= this.getThreshold()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return the threshold
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * @param threshold the threshold to set
     */
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    /**
     * @return the reliability
     */
    public double getReliability() {
        return reliability;
    }

    /**
     * @param reliability the reliability to set
     */
    /*
    public void setReliability(double reliability) {
        this.reliability = reliability;
    }*/

    
    /**
     * @return the associations
     */
    public Map<String, Double> getAssociations() {
        return associations;
    }

    /**
     * @param associations the associations to set
     */
    public void setAssociations(Map<String, Double> associations) {
        this.associations = associations;
    }
    
        public boolean addAssociation(String entityName, double score) {
        if (this.getAssociations().containsKey(entityName)) {
            log.debug("association between entity " + this.getName()
                    + " and entity " + entityName
                    + " already known, overwriting previously saved score.");
        }
        return (this.getAssociations().put(entityName, score) == null);
    }
    
    
}