package io.github.infolis.infolink.patternLearner;

import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.MathUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.queryParser.ParseException;
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

    Map<String, Instance> instances;
    Map<String, InfolisPattern> patterns;
    Set<String> seedInstances;
    double maximumPmi;
    private static final Logger log = LoggerFactory.getLogger(Reliability.class);

    /**
     * Class constructor initializing empty sets for instances and patterns.
     */
    public Reliability() {
        this.instances = new HashMap<>();
        this.patterns = new HashMap<>();
        this.maximumPmi = -100.0;
        this.seedInstances = new HashSet<>();
    }
    
    public void setSeedInstances(Set<String> seedInstances) {
    	this.seedInstances = seedInstances;
    }
    
    public Set<String> getSeedInstances() {
    	return this.seedInstances;
    }
    
    public Collection<Instance> getInstances() {
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
    public boolean addInstance(Instance instance) {
        if (this.instances.containsKey(instance.name)) {
            Instance curInstance = this.instances.get(instance.name);
            Map<String, Double> curAssociations = curInstance.associations;
            curAssociations.putAll(instance.associations);
            instance.associations = curAssociations;
            this.instances.put(instance.name, instance);
            return false;
        }
        this.instances.put(instance.name, instance);
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
        if (this.patterns.containsKey(pattern.getMinimal())) {
            InfolisPattern curPattern = this.patterns.get(pattern.getMinimal());
            Map<String, Double> curAssociations = curPattern.getAssociations();
            curAssociations.putAll(pattern.getAssociations());
            pattern.setAssociations(curAssociations);
            this.patterns.put(pattern.getMinimal(), pattern);
            return false;
        }
        this.patterns.put(pattern.getMinimal(), pattern);
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
     * Class for Instances (= terms recognized as candidates for dataset titles). 
     *
     * @author kata
     */
    public class Instance {

        String name;
        Map<String, Double> associations;

        public Instance(String name) {
            this.name = name;
            this.associations = new HashMap<>();
        }
        
        public String getName() {
        	return this.name;
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
            if (this.associations.containsKey(pattern)) {
                log.debug("association between instance " + this.name + 
                		" and pattern " + pattern + 
                		" already known, overwriting previously saved score.");
            }
            return (this.associations.put(pattern, score) == null);
        }
        
        public Map<String, Double> getAssociations() {
            return this.associations;
        }

        public boolean isReliable(Set<StudyContext> contexts_patterns, int dataSize, Set<String> reliablePatterns, Set<StudyContext> contexts_seeds, Reliability r, double threshold) throws IOException, ParseException {
        	double instanceReliability = r.computeReliability(contexts_patterns, dataSize, reliablePatterns, contexts_seeds, this);
            if (instanceReliability >= threshold) {
                return true;
            } else {
                return false;
            }
        }
   
    }
    
    /**
     * Counts occurrences of instance and joint occurrences of instance and regex. 
     * Needed for computation of probabilities for computation of pmi scores. 
     * 
     * @param contexts_seeds	contexts extracted through term search of all currently known instances/candidates
     * @param regex				pattern regex string
     * @param instance			name of the instance
     * @return
     */
    public int[] countInstanceAndJointOccurrences(Set<StudyContext> contexts_seeds, String regex, String instance) {
    	int instanceCount = 0;
    	int jointOccurrences = 0;
    	
    	for (StudyContext sc : contexts_seeds) {
            if (sc.getTerm().equals(instance)) {
                instanceCount++;
                // check whether the pattern can be found in the context
                // note: this is not the same as checking whether the context was extracted by it!
                // more so because these contexts here were generated by term search (searching for the seed), not by applying patterns
                // for matching minimal patterns, limited time matcher should not be required (low probability of catastrophic backtracking)
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(sc.toString());
                if (m.find()) jointOccurrences++;
            }
        }
    	int[] counts = {instanceCount, jointOccurrences};
    	return counts;
    }
    
    public int[] countPatternAndJointOccurrences(Set<StudyContext> contexts_patterns, String regex, String instance) {
    	int patternCount = 0;
    	int jointOccurrences = 0;
    	
    	for (StudyContext sc : contexts_patterns) {
    		Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(sc.toString());
            if (m.find()) {
            	patternCount++;
            	if (sc.getTerm().equals(instance)) jointOccurrences++;
            }
        }
    	int[] counts = {patternCount, jointOccurrences};
    	return counts;
    }
    /**
     * Computes the point-wise mutual information score for instance and pattern belonging to regex
     * based on the given data. Needed for computing reliability score of pattern. 
     * 
     * @param patternCount		count of all occurrences of regex in the complete input data
     * @param dataSize			size of input data (number of input documents)
     * @param contexts_seeds	contexts of all currently known seeds, extracted by term search
     * @param regex				pattern regex string
     * @param instance			name of the instance
     * @return					point-wise mutual information score of instance and pattern (belonging to regex)
     */
    public double computePmi(int patternCount, int dataSize, Set<StudyContext> contexts_seeds, String regex, String instance) {
    	int[] counts = countInstanceAndJointOccurrences(contexts_seeds, regex, instance);
    	int instanceCount = counts[0];
    	int jointOccurrences = counts[1];
        
        // p_x: probability of instance occurring in the data
        double p_x = (double) instanceCount / (double) dataSize;
        // p_y: probability of pattern occurring in the data
        double p_y = (double) patternCount / (double) dataSize;
        // p_xy: joint probability of pattern and instance occurring in the data
        double p_xy = (double) jointOccurrences / (double) dataSize;
        double pmi_pattern = MathUtils.pmi(p_xy, p_x, p_y);
        log.debug("instance: " + instance);
	    log.debug("data size: " + (double) dataSize);
	    log.debug("total studycontexts where instance can be found: " + instanceCount);
	    log.debug("total studycontexts where pattern can be found: " + patternCount);
	    log.debug("total studycontexts where both instance and pattern can be found: " + jointOccurrences);
	    log.debug("p_xy: " + p_xy);
	    log.debug("p_x: " + p_x);
	    log.debug("p_y: " + p_y);
	    log.debug("pmi_pattern: " + pmi_pattern);
	    
	    return pmi_pattern;
    }
    
    /**
     * Computes the point-wise mutual information score for instance and pattern 
     * based on the given data. Needed for computing reliability score of instance. 
     * 
     * @param patternCount		count of all occurrences of regex in the complete input data
     * @param dataSize			size of input data (number of input documents)
     * @param contexts_seeds	contexts of all currently known seeds, extracted by term search
     * @param regex				pattern regex string
     * @param instance			name of the instance
     * @return					point-wise mutual information score of instance and pattern (belonging to regex)
     */
    public double computePmi_instance(int instanceCount, int dataSize, Set<StudyContext> contexts_patterns, String minimalRegex, String instance) {
    	int[] counts = countPatternAndJointOccurrences(contexts_patterns, minimalRegex, instance);
    	int patternCount = counts[0];
    	int jointOccurrences = counts[1];
        
        // p_x: probability of instance occurring in the data
        double p_x = (double) instanceCount / (double) dataSize;
        // p_y: probability of pattern occurring in the data
        double p_y = (double) patternCount / (double) dataSize;
        // p_xy: joint probability of pattern and instance occurring in the data
        double p_xy = (double) jointOccurrences / (double) dataSize;
        double pmi_instance = MathUtils.pmi(p_xy, p_x, p_y);
        log.debug("pattern: " + minimalRegex);
	    log.debug("data size: " + (double) dataSize);
	    log.debug("total studycontexts where instance can be found: " + instanceCount);
	    log.debug("total studycontexts where pattern can be found: " + patternCount);
	    log.debug("total studycontexts where both instance and pattern can be found: " + jointOccurrences);
	    log.debug("p_xy: " + p_xy);
	    log.debug("p_x: " + p_x);
	    log.debug("p_y: " + p_y);
	    log.debug("pmi_instance: " + pmi_instance);
	    
	    return pmi_instance;
    }
    
    /**
     * Computes the reliability score of pattern based on the given data.
     * 
     * @param contexts_pattern		all contexts extracted by pattern on the complete input documents
     * @param dataSize				size of the input data (number of input documents)
     * @param reliableInstances		all currently known instances
     * @param contexts_seeds		contexts of all currently known instances, extracted by term search
     * @param pattern				pattern to compute the reliability score for
     * @return						pattern reliability score
     */
    public double computeReliability(List<String> contexts_pattern, int dataSize, Set<String> reliableInstances, Set<StudyContext> contexts_seeds, InfolisPattern pattern) {
    	// Pattern needs to be searched in complete corpus to compute p_y. Searching extracted contexts is not sufficient
    	// save extracted contexts in case pattern is deemed reliable - no need to repeat the search   	
    	int patternCount = contexts_pattern.size();
    	
        // for every known instance, check whether pattern is associated with it   
        for (String instanceName : reliableInstances) {
        	double pmi = this.computePmi(patternCount, dataSize, contexts_seeds, pattern.getMinimal(), instanceName);
        	// instance and pattern do not occur together in the data and are thus not associated
        	if (Double.isNaN(pmi) || Double.isInfinite(pmi)) continue; 
	        pattern.addAssociation(instanceName, pmi);
	        Instance instance = new Instance(instanceName);//
	        instance.addAssociation(pattern.getMinimal(), pmi);//
	        //TODO: why use regex for storing association? Shouldn't the URI be used?
	        this.addPattern(pattern);
	        this.addInstance(instance);//
	        this.setMaxPmi(pmi);
        }
        System.out.println("Computing reliability of " + pattern.getMinimal());
        return this.reliability(pattern, "");
    }
    
    /**
     * Computes the reliability score of instance based on the given data. 
     * 
     * @param contexts_patterns		all contexts extracted by pattern on the complete input documents
     * @param dataSize				size of the input data (number of input documents)
     * @param reliablePatterns		all currently known patterns
     * @param contexts_seeds		contexts of all currently known instances, extracted by term search
     * @param instance				instance to compute the reliability score for
     * @return						instance reliability score
     */
    public double computeReliability(Set<StudyContext> contexts_patterns, int dataSize, Set<String> reliablePatterns, Set<StudyContext> contexts_seeds, Reliability.Instance instance) {
    	int instanceCount = 0;
    	for (StudyContext context : contexts_patterns) {
    		if (context.getTerm().equals(instance.name)) instanceCount++;
    	}
    	
        // for every known pattern, check whether instance is associated with it   
        for (String regex : reliablePatterns) {
        	double pmi = this.computePmi_instance(instanceCount, dataSize, contexts_patterns, regex, instance.name);
        	// instance and pattern never occur together and thus are not associated
        	if (Double.isNaN(pmi) || Double.isInfinite(pmi)) { continue; }
        	instance.addAssociation(regex, pmi);
        	this.addInstance(instance);
        	InfolisPattern pattern = this.getPattern(regex);//
        	pattern.addAssociation(instance.name, pmi);//
	        this.addPattern(pattern);
	        this.setMaxPmi(pmi);
        }
        System.out.println("Computing reliability of " + instance.name);
        return this.reliability(instance, "");
    }

    /**
     * Computes the reliability of an instance.
     *
     * @return the reliability score
     */
    public double reliability(Reliability.Instance instance, String callingPattern) {
    	log.debug("Computing reliability of instance: " + instance.name);
    	if (this.seedInstances.contains(instance.name)) {
    		return 1.0;
    	}
        double rp = 0.0;
        Map<String, Double> patternsAndPmis = instance.getAssociations();
        float P = Float.valueOf(patternsAndPmis.size());
        for (String patternString : patternsAndPmis.keySet()) {
        	// avoid circles
        	if (patternString.equals(callingPattern)) { continue; }
            double pmi = patternsAndPmis.get(patternString);
            InfolisPattern pattern = this.patterns.get(patternString);
            if (maximumPmi != 0) {
            	rp += ((pmi / maximumPmi) * reliability(pattern, instance.name));
            }
        }
        log.debug("instance max pmi: " + maximumPmi);
		log.debug("instance number of associations: " + P);
		log.debug("instance rp: " + rp);
		log.debug("instance returned reliability: " + rp / P);
        return rp / P;
    }

    /**
     * Computes the reliability of a pattern. 
     * 
     * @return the reliability score
     */
    public double reliability(InfolisPattern pattern, String callingInstance) {
    	log.debug("Computing reliability of pattern: " + pattern.getMinimal());
        double rp = 0.0;
        Map<String, Double> instancesAndPmis = pattern.getAssociations();
        float P = Float.valueOf(instancesAndPmis.size());
        for (String instanceName : instancesAndPmis.keySet()) {
        	if (instanceName.equals(callingInstance)) { continue; }
            double pmi = instancesAndPmis.get(instanceName);
            Reliability.Instance instance = instances.get(instanceName);
            double reliability_instance = reliability(instance, pattern.getPatternRegex());
            log.debug("stored pmi for pattern and instance \"" + instanceName +"\": " + pmi);
            log.debug("reliability instance: " + reliability_instance);
            if (maximumPmi != 0) {
            	rp += ((pmi / maximumPmi) * reliability_instance);
            }
        }
        log.debug("max pmi: " + maximumPmi);
		log.debug("number of associations: " + P);
		log.debug("pattern rp: " + rp);
		log.debug("returned pattern reliability: " + rp / P);
        return rp / P;
    }
}
