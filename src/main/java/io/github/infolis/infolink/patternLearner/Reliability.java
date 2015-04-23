package io.github.infolis.infolink.patternLearner;

import io.github.infolis.model.InfolisPattern;
import java.util.HashMap;
import java.util.Map;

//TODO: cite paper in docstring
    /**
     * Class for storing pattern ranking and instance ranking reliability
     * scores. (see:
     *
     * @author katarina.boland@gesis.org
     * @version 2015-01-05
     */
    class Reliability {

        Map<String, Instance> instances;
        Map<String, InfolisPattern> patterns;
        double maximumPmi;

        /**
         * Class constructor initializing empty sets for instances and patterns.
         */
        Reliability() {
            this.instances = new HashMap<String, Instance>();
            this.patterns = new HashMap<String, InfolisPattern>();
            this.maximumPmi = 0;
        }

        /**
         * Adds a new Instance instance. The instance may have been added before
         * with only a subset of all initializing patterns. Thus, when adding a
         * new instance, checks if an instance with the same name is already
         * known and if so, the new associations are added to the existing
         * instance.
         *
         * @param instance	Instance instance to be added
         * @return	true, if instance was not included in this instances before,
         * false if already in this instances
         */
        boolean addInstance(Instance instance) {
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
         * Adds a new Pattern instance. The pattern may have been added before
         * with only a subset of all extracted instances. Thus, when adding a
         * new pattern, checks if a pattern with the same name is already known
         * and if so, the new associations are added to the existing pattern.
         *
         * @param pattern	Pattern instance to be added
         * @return	true, if pattern was not included in this patterns before,
         * false if already in this patterns
         */
        boolean addPattern(InfolisPattern pattern) {
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
         * Set this maximum to pmi if higher than the current maximum.
         *
         * @param pmi	the new value to maybe become the new maximum
         * @return	true, if pmi is the new maximum (or equal to the existing
         * one), false otherwise (if lesser than maximum)
         */
        boolean setMaxPmi(double pmi) {
            if (pmi >= this.maximumPmi) {
                this.maximumPmi = pmi;
                return true;
            } else {
                return false;
            }
        }

        /**
         * Class for storing instance ranking reliability scores.
         *
         * @author katarina.boland@gesis.org
         * @version 2015-01-05
         */
        class Instance {

            String name;
            Map<String, Double> associations;
//			private double reliability;

            Instance(String name) {
                this.name = name;
                this.associations = new HashMap<String, Double>();
            }

            /**
             * Adds an association between this instance and a specified
             * pattern.
             *
             * @param pattern	the pattern whose association to store
             * @param score	pmi score for this instance and pattern
             * @return	true, if association is new; false if association was
             * already known
             */
            boolean addAssociation(String pattern, double score) {
                if (this.associations.containsKey(pattern)) {
                    System.err.print("Warning: association between instance " + this.name + " and pattern " + pattern + " already known!");
                }
                return (this.associations.put(pattern, score) == null);
            }
        }
    }