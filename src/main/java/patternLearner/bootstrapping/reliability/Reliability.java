/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package patternLearner.bootstrapping.reliability;

//TODO: cite paper in docstring
import java.util.HashMap;
import java.util.Map;
import patternLearner.Learner;

/**
 * Class for storing pattern ranking and instance ranking reliability scores.
 * (see:
 *
 * @author katarina.boland@gesis.org
 * @version 2015-01-05
 */
public class Reliability {

    private Map<String, ReliabilityInstance> instances;
    private Map<String, ReliabilityPattern> patterns;
    private double maximumPmi;

    /**
     * Class constructor initializing empty sets for instances and patterns.
     */
    public Reliability() {
        this.instances = new HashMap();
        this.patterns = new HashMap();
        this.maximumPmi = 0;
    }

    /**
     * Adds a new Instance instance. The instance may have been added before
     * with only a subset of all initializing patterns. Thus, when adding a new
     * instance, checks if an instance with the same name is already known and
     * if so, the new associations are added to the existing instance.
     *
     * @param instance	Instance instance to be added
     * @return	true, if instance was not included in this instances before,
     * false if already in this instances
     */
    boolean addInstance(ReliabilityInstance instance) {
        if (this.getInstances().containsKey(instance.getName())) {
            ReliabilityInstance curInstance = this.getInstances().get(instance.getName());
            Map<String, Double> curAssociations = curInstance.getAssociations();
            curAssociations.putAll(instance.getAssociations());
            instance.setAssociations(curAssociations);
            this.getInstances().put(instance.getName(), instance);
            return false;
        }
        this.getInstances().put(instance.getName(), instance);
        return true;
    }

    /**
     * Adds a new Pattern instance. The pattern may have been added before with
     * only a subset of all extracted instances. Thus, when adding a new
     * pattern, checks if a pattern with the same name is already known and if
     * so, the new associations are added to the existing pattern.
     *
     * @param pattern	Pattern instance to be added
     * @return	true, if pattern was not included in this patterns before, false
     * if already in this patterns
     */
    boolean addPattern(ReliabilityPattern pattern) {
        if (this.getPatterns().containsKey(pattern.getPattern())) {
            ReliabilityPattern curPattern = this.getPatterns().get(pattern.getPattern());
            Map<String, Double> curAssociations = curPattern.getAssociations();
            curAssociations.putAll(pattern.getAssociations());
            pattern.setAssociations(curAssociations);
            this.getPatterns().put(pattern.getPattern(), pattern);
            return false;
        }
        this.getPatterns().put(pattern.getPattern(), pattern);
        return true;
    }

    /**
     * Set this maximum to pmi if higher than the current maximum.
     *
     * @param pmi	the new value to maybe become the new maximum
     * @return	true, if pmi is the new maximum (or equal to the existing one),
     * false otherwise (if lesser than maximum)
     */
    boolean setMaxPmi(double pmi) {
        if (pmi >= this.getMaximumPmi()) {
            this.maximumPmi = pmi;
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return the instances
     */
    public Map<String, ReliabilityInstance> getInstances() {
        return instances;
    }

    /**
     * @return the patterns
     */
    public Map<String, ReliabilityPattern> getPatterns() {
        return patterns;
    }

    /**
     * @return the maximumPmi
     */
    public double getMaximumPmi() {
        return maximumPmi;
    }

    /**
     * Computes the point-wise mutual information of two strings given their
     * probabilities. see http://www.aclweb.org/anthology/P06-1#page=153
     *
     * @param p_xy	probability P(x,y), i.e. ...
     * @param p_x probability P(x), i.e. ...
     * @param p_y	probability P(y), i.e. ...
     * @return
     */
    public double pmi(double p_xy, double p_x, double p_y) {
        return log2(p_xy / (p_x * p_y));
    }

    /**
     * Computes the logarithm (base 2) for a given value
     *
     * @param x	the value for which the log2 value is to be computed
     * @return	the logarithm (base 2) for the given value
     */
    public double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}