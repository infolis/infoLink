/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.model;

import io.github.infolis.infolink.patternLearner.Reliability;
import io.github.infolis.util.MathUtils;
import io.github.infolis.util.RegexUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.queryParser.ParseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 *
 * @author kata
 * @author domi
 * @author kba
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InfolisPattern extends BaseModel {

    private String patternRegex;
    private String luceneQuery;
    private String minimal;
    private Map<String, Double> associations;
    
    public InfolisPattern(String patternRegex, String luceneQuery) {
    	this.setLuceneQuery(luceneQuery);
    	this.setPatternRegex(patternRegex);
    	this.associations = new HashMap<>();
	}

    public InfolisPattern(String patternRegex) {
    	this.setPatternRegex(patternRegex);
	}

	public InfolisPattern() {
		this.associations = new HashMap<>();
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
     * Adds an association between this pattern and a specified instance.
     *
     * @param instance	the instance whose association to store
     * @param score	pmi score for this pattern and instance
     * @return	true, if association is new; false if association was already
     * known
     */
    public boolean addAssociation(String instanceName, double score) {
        if (this.getAssociations().containsKey(instanceName)) {
            System.err.print("Warning: association between pattern " + this.getPatternRegex() + " and instance " + instanceName + " already known! ");
        }
        return (this.getAssociations().put(instanceName, score) == null);
    }

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

    //TODO: the regex are not created as Patterns in the PatternInducer
    /**
     * Determines whether a regular expression is suitable for extracting
     * dataset references using a frequency-based measure
     *
     * @param regex	regex to be tested
     * @param threshold	threshold for frequency-based relevance measure
     * @param contextStrings	set of extracted contexts as strings
     * @return				<emph>true</emph>, if regex is found to be relevant,
     * <emph>false</emph> otherwise
     */
    public static boolean isRelevant(String regex, List<String> contextStrings, double threshold) {
        System.out.println("Checking if pattern is relevant: " + regex);
        // compute score for similar to tf-idf...
        // count occurrences of regex in positive vs negative contexts...
        int count_pos = 0;
        //int count_neg = 0;
        List<String> contexts_neg = new ArrayList<>();
        for (String context : contextStrings) {
            count_pos += patternFound(regex, context);
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
        if ((tf_idf >= threshold) & (count_pos > 1)) {
            return true;
        } else {
            return false;
        }
    }

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

    public List<StudyContext> isReliable(double threshold, int dataSize, Set<String> reliableInstances, Set<StudyContext> extractedContexts, Reliability r) throws IOException, ParseException {
        int patternCounter = 0;
        List<StudyContext> contextOfPattern = new ArrayList<>();
        for (StudyContext sc : extractedContexts) {
            if (sc.getPattern().equals(this)) {
                contextOfPattern.add(sc);
                patternCounter++;
            }
        }

        // for every known instance, check whether pattern is associated with it            
        for (String instance : reliableInstances) {
            int occurrencesPattern = 0;
            int totalSentences = 0;
            for (StudyContext sc : extractedContexts) {
                if (sc.getTerm().equals(instance)) {
                    totalSentences++;
                    if (sc.getPattern().equals(this)) {
                        occurrencesPattern++;
                    }
                }
                double p_x = (double) totalSentences / (double) dataSize;
                double p_y = (double) patternCounter / (double) dataSize;
                double p_xy = (double) occurrencesPattern / (double) dataSize;
                Reliability.Instance newInstance = r.new Instance(instance);
                double pmi_pattern = MathUtils.pmi(p_xy, p_x, p_y);
                this.addAssociation(instance, pmi_pattern);
                newInstance.addAssociation(this.getPatternRegex(), pmi_pattern);
                r.addPattern(this);
                r.addInstance(newInstance);
                r.setMaxPmi(pmi_pattern);
            }
        }

        System.out.println("Computing relevance of " + this.getPatternRegex());
        double patternReliability = r.reliability(this);

        if (patternReliability >= threshold) {
            return contextOfPattern;
        } else {
            System.out.println("Pattern " + this.getPatternRegex() + " deemed unreliable");
            return new ArrayList<>();
        }
    }

	public void setPatternRegex(String patternRegex) {
		this.patternRegex = patternRegex;
	}

	public void setLuceneQuery(String luceneQuery) {
		this.luceneQuery = luceneQuery;
	}

}
