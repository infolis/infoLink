package io.github.infolis.infolink.patternLearner;

import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.util.MathUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for storing espresso-like pattern ranking and instance ranking reliability scores.
 *
 * Implemented based on algorithm in:
 * Patrick Pantel and Marco Pennacchiotti. 2006.
 * Espresso: leveraging generic patterns for automatically harvesting semantic relations.
 * In Proceedings of the 21st International Conference on Computational Linguistics and
 * the 44th annual meeting of the Association for Computational Linguistics (ACL-44).
 * Association for Computational Linguistics, Stroudsburg, PA, USA, 113-120.
 * DOI=10.3115/1220175.1220190 http://dx.doi.org/10.3115/1220175.1220190
 * http://www.anthology.aclweb.org/P/P06/P06-1.pdf#page=153
 *
 * @author kata
 */
public class Reliability {

    Map<String, Entity> instances;
    Map<String, InfolisPattern> patterns;
    Set<String> seedTerms;
    double maximumPmi;
    private static final Logger log = LoggerFactory.getLogger(Reliability.class);
    // reliability scores may change between iterations in bootstrapping but not during one iteration
    // avoid multiple computations of score for the same entities inside the same iteration
    // to allow scores to change between iterations, reset all scores at beginning of new iteration
    Map<String, Double> scoreCache;

    /**
     * Class constructor initializing empty sets for instances and patterns.
     */
    public Reliability() {
        this.instances = new HashMap<>();
        this.patterns = new HashMap<>();
        this.maximumPmi = -100.0;
        this.seedTerms = new HashSet<>();
        this.scoreCache = new HashMap<>();
    }

    public void deleteScoreCache() {
    	this.scoreCache = new HashMap<>();
    }

    public void setSeedTerms(Set<String> seedTerms) {
    	this.seedTerms = seedTerms;
    }

    public Set<String> getSeedTerms() {
    	return this.seedTerms;
    }

    public Collection<Entity> getInstances() {
    	return this.instances.values();
    }

    public Collection<InfolisPattern> getPatterns() {
    	return this.patterns.values();
    }

    public InfolisPattern getPattern(String regex) {
    	return this.patterns.get(regex);
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
    public boolean addInstance(Entity instance) {
        if (this.instances.containsKey(instance.getName())) {
            Entity curInstance = this.instances.get(instance.getName());
            Map<String, Double> curAssociations = curInstance.getAssociations();
            curAssociations.putAll(instance.getAssociations());
            instance.setAssociations(curAssociations);
            this.instances.put(instance.getName(), instance);
            return false;
        }
        this.instances.put(instance.getName(), instance);
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
    public boolean addPattern(InfolisPattern pattern) {
        if (this.patterns.containsKey(pattern.getPatternRegex())) {
            InfolisPattern curPattern = this.patterns.get(pattern.getPatternRegex());
            Map<String, Double> curAssociations = curPattern.getAssociations();
            curAssociations.putAll(pattern.getAssociations());
            pattern.setAssociations(curAssociations);
            this.patterns.put(pattern.getPatternRegex(), pattern);
            return false;
        }
        this.patterns.put(pattern.getPatternRegex(), pattern);
        return true;
    }

    /**
     * Sets this maximumPmi to pmi if higher than the current maximum.
     *
     * @param pmi	the new value to maybe become the new maximum
     * @return	true, if pmi is the new maximum (or equal to the existing one),
     * false otherwise (if lesser than maximum)
     */
    public boolean setMaxPmi(double pmi) {
        if (pmi >= this.maximumPmi) {
            this.maximumPmi = pmi;
            return true;
        } else {
            return false;
        }
    }

    public double getMaxPmi() {
    	return this.maximumPmi;
    }

    /**
     * Counts joint occurrences of instance and pattern.
     * Needed for computation of probabilities for computation of pmi scores.
     *
     * @param instance
     * @param pattern
     * @return
     */
    private int countJointOccurrences(Entity instance, InfolisPattern pattern) {
    	int jointOccurrences = 0;
    	// joint occurrences can be calculated in two different ways:
    	// either search for pattern in textual references of instance (note: search pattern in the
    	// context strings, not search for pattern listed as extracting pattern there as the contexts
    	// were extracted by term search, not by applying patterns)
    	// or search for term in contexts of pattern
    	// Most efficient solution: search for term in contexts of pattern
    	for (TextualReference context : pattern.getTextualReferences()) {
            if (context.getReference().equals(instance.getName())) jointOccurrences++;
        }
    	return jointOccurrences;
    }

    /**
     * Computes the point-wise mutual information score for instance and pattern.
     *
     * @param patternCount		count of all occurrences of regex in the complete input data
     * @param dataSize			size of input data (number of input documents)
     * @param contexts_seeds	contexts of all currently known seeds, extracted by term search
     * @param regex				pattern regex string
     * @param instance			name of the instance
     * @return					point-wise mutual information score of instance and pattern (belonging to regex)
     */
    private double computePmi(int dataSize, Entity instance, InfolisPattern pattern) {
    	log.debug("computing pmi of instance \"" + instance.getName() + "\" and pattern \"" + pattern.getPatternRegex() + "\"");
    	int patternCount = pattern.getTextualReferences().size();
    	int instanceCount = instance.getTextualReferences().size();
    	int jointOccurrences = countJointOccurrences(instance, pattern);
        // p_x: probability of instance occurring in the data
        double p_x = (double) instanceCount / (double) dataSize;
        // p_y: probability of pattern occurring in the data
        double p_y = (double) patternCount / (double) dataSize;
        // p_xy: joint probability of pattern and instance occurring in the data
        double p_xy = (double) jointOccurrences / (double) dataSize;
        double pmi_score = MathUtils.pmi(p_xy, p_x, p_y);
	    log.debug("data size: " + (double) dataSize);
	    log.debug("total studycontexts where instance can be found: " + instanceCount);
	    log.debug("total studycontexts where pattern can be found: " + patternCount);
	    log.debug("total studycontexts where both instance and pattern can be found: " + jointOccurrences);
	    log.trace("p_xy: " + p_xy);
	    log.trace("p_x: " + p_x);
	    log.trace("p_y: " + p_y);
	    log.trace("pmi: " + pmi_score);
	    return pmi_score;
    }

    /**
     * Computes the reliability score of pattern based on the given data.
     *
     * @param dataSize				size of the input data (number of input documents)
     * @param reliableInstances		all currently known instances
     * @param pattern				pattern to compute the reliability score for
     * @return						pattern reliability score
     */
    public double computeReliability(int dataSize, Set<Entity> reliableInstances, InfolisPattern pattern) {
    	//TODO: use custom comparator for Entities to avoid necessity of building this map
    	Map<String, Entity> reliableInstanceNames = new HashMap<>();
    	for (Entity i : reliableInstances) { reliableInstanceNames.put(i.getName(), i); }

        // compute pmi for every known instance referenced using pattern
    	for (TextualReference ref : pattern.getTextualReferences()) {
    		// do not try to compute reliability of unknown instances at this step
    		if (!reliableInstanceNames.containsKey(ref.getReference()))  continue;
    		Entity instance = reliableInstanceNames.get(ref.getReference());
        	double pmi = this.computePmi(dataSize, instance, pattern);
        	// instance and pattern do not occur together in the data and are thus not associated
        	// should not happen here because instance is found as term in the textual references of pattern
        	if (Double.isNaN(pmi) || Double.isInfinite(pmi)) throw new IllegalStateException(
        			"Spurious association of pattern \"" + pattern.getPatternRegex() + " and instance\"" + instance.getName());
	        pattern.addAssociation(instance.getName(), pmi);
	        //Instance instance = new Instance(instanceName);
	        instance.addAssociation(pattern.getPatternRegex(), pmi);
	        //TODO: why use regex for storing association? Shouldn't the URI be used?
	        this.addPattern(pattern);
	        this.addInstance(instance);
	        this.setMaxPmi(pmi);
        }
        return this.reliability(pattern, "");
    }

    /**
     * Computes the reliability score of instance based on the given data.
     *
     * @param dataSize				size of the input data (number of input documents)
     * @param reliablePatterns		currently known reliable patterns
     * @param contexts_seeds		contexts of all currently known instances, extracted by term search
     * @param instance				instance to compute the reliability score for
     * @return						instance reliability score
     */
    public double computeReliability(int dataSize, Collection<InfolisPattern> reliablePatterns, Entity instance) {
        // for every known pattern, check whether instance is associated with it
        for (InfolisPattern pattern : reliablePatterns) {
        	//double pmi = this.computePmi_instance(dataSize, pattern, instance);
        	double pmi = this.computePmi(dataSize, instance, pattern);
        	// instance and pattern never occur together and thus are not associated
        	// this may happen here and is not an error
        	if (Double.isNaN(pmi) || Double.isInfinite(pmi)) { continue; }
        	instance.addAssociation(pattern.getPatternRegex(), pmi);
        	this.addInstance(instance);
        	//InfolisPattern pattern = this.getPattern(regex);
        	pattern.addAssociation(instance.getName(), pmi);
	        this.addPattern(pattern);
	        this.setMaxPmi(pmi);
        }
        return this.reliability(instance, "");
    }

    /**
     * Computes the reliability of an instance.
     *
     * @return the reliability score
     */
    public double reliability(Entity instance, String callingEntity) {
    	log.debug("Computing reliability of instance: " + instance.getName());
    	if (this.seedTerms.contains(instance.getName())) {
    		return 1.0;
    	}
    	if (scoreCache.containsKey(instance.getName())) return scoreCache.get(instance.getName());
        double rp = 0.0;
        Map<String, Double> patternsAndPmis = instance.getAssociations();
        float P = Float.valueOf(patternsAndPmis.size());
        for (String patternString : patternsAndPmis.keySet()) {
        	// avoid circles
        	if (patternString.equals(callingEntity)) { continue; }
            double pmi = patternsAndPmis.get(patternString);
            InfolisPattern pattern = this.patterns.get(patternString);
            if (maximumPmi != 0) {
            	rp += ((pmi / maximumPmi) * reliability(pattern, instance.getName()));
            }
        }
        log.debug("instance max pmi: " + maximumPmi);
		log.debug("instance number of associations: " + P);
		log.debug("instance rp: " + rp);
		log.debug("instance returned reliability: " + rp / P);
		double score = rp / P;
		scoreCache.put(instance.getName(), score);
        return score;
    }

    /**
     * Computes the reliability of a pattern.
     *
     * @return the reliability score
     */
    public double reliability(InfolisPattern pattern, String callingEntity) {
    	log.debug("Computing reliability of pattern: " + pattern.getPatternRegex());
    	if (scoreCache.containsKey(pattern.getPatternRegex())) return scoreCache.get(pattern.getPatternRegex());
        double rp = 0.0;
        Map<String, Double> instancesAndPmis = pattern.getAssociations();
        float P = Float.valueOf(instancesAndPmis.size());
        for (String instanceName : instancesAndPmis.keySet()) {
        	if (instanceName.equals(callingEntity)) { continue; }
            double pmi = instancesAndPmis.get(instanceName);
            Entity instance = instances.get(instanceName);
            double reliability_instance = reliability(instance, pattern.getPatternRegex());
            log.debug("stored pmi for pattern \"" + pattern.getPatternRegex() + "\" and instance \"" + instanceName +"\": " + pmi);
            if (maximumPmi != 0) {
            	rp += ((pmi / maximumPmi) * reliability_instance);
            }
        }
        log.debug("max pmi: " + maximumPmi);
		log.debug("pattern number of associations: " + P);
		log.debug("pattern rp: " + rp);
		log.debug("returned pattern reliability: " + rp / P);
		double score = rp / P;
		scoreCache.put(pattern.getPatternRegex(), score);
		return score;
    }


}
