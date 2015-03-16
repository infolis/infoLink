/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package patternLearner.bootstrapping.reliability;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for storing pattern ranking reliability scores.
 *
 * @author katarina.boland@gesis.org
 * @version 2015-01-05
 */
public class ReliabilityPattern {

    private String pattern;
    private Map<String, Double> associations;
    double reliability;

    ReliabilityPattern(String pattern) {
        this.pattern = pattern;
        this.associations = new HashMap();
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
            System.err.print("Warning: association between pattern " + this.getPattern() + " and instance " + instanceName + " already known! ");
        }
        return (this.getAssociations().put(instanceName, score) == null);
    }

    private void setReliability(double reliability) {
        this.reliability = reliability;
    }

    private double getReliability() {
        return this.reliability;
    }

    /**
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
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
}
