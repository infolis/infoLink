/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package patternLearner.bootstrapping.reliability;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for storing instance ranking reliability scores.
 *
 * @author katarina.boland@gesis.org
 * @version 2015-01-05
 */
public class ReliabilityInstance {

    private String name;
    private Map<String, Double> associations;
    private double reliability;

    ReliabilityInstance(String name) {
        this.name = name;
        this.associations = new HashMap();
    }

    /**
     * Adds an association between this instance and a specified pattern.
     *
     * @param pattern	the pattern whose association to store
     * @param score	pmi score for this instance and pattern
     * @return	true, if association is new; false if association was already
     * known
     */
    public boolean addAssociation(String pattern, double score) {
        if (this.getAssociations().containsKey(pattern)) {
            System.err.print("Warning: association between instance " + this.getName() + " and pattern " + pattern + " already known!");
        }
        return (this.getAssociations().put(pattern, score) == null);
    }

    private void setReliability(double reliability) {
        this.reliability = reliability;
    }

    private double getReliability() {
        return this.reliability;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
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
