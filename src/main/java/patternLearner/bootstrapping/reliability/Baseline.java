/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package patternLearner.bootstrapping.reliability;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import patternLearner.Learner;
import patternLearner.Util;
import patternLearner.bootstrapping.ArffFile;
import patternLearner.bootstrapping.Bootstrapping;
import patternLearner.bootstrapping.InternalTrainingSet;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author domi
 */
public class Baseline extends Bootstrapping {
    
    public Baseline(Learner l) {
        super(l);
    }

    private Set<String> reliablePatterns_iteration;
    private Set<String> foundSeeds_iteration;
    private Set<String> foundPatterns_iteration;
    private Set<String> reliableInstances;
    private Map<String, List<String[]>> reliablePatternsAndContexts;
    private double threshold;
    
    
    
    /**
     * Main method for reliability-based bootstrapping.
     *
     * @param terms	reliable seed terms for current iteration
     * @param threshold	reliability threshold
     * @param numIter	current iteration
     */
    @Override
    public void bootstrap(Set<String> terms, int numIter, int maxIter) {
        numIter++;
        System.out.println("Bootstrapping... Iteration: " + numIter);
        File logFile = new File(l.getOutputPath() + File.separator + "output.txt");
        this.foundSeeds_iteration = new HashSet();
        this.reliablePatterns_iteration = new HashSet();
        File contextDir = new File(l.getContextPath());
        String[] contextFiles = contextDir.list();
        List<String> contextFileList = Arrays.asList(contextFiles);
        File arffDir = new File(l.getArffPath());
        String[] arffFiles = arffDir.list();
        List<String> arffFileList = Arrays.asList(arffFiles);
        InternalTrainingSet trainingSet;
        List<String> newArffFiles = new ArrayList();
        // 0. filter seeds, select only reliable ones
        // alternatively: use all seeds extracted by reliable patterns
        this.reliableInstances.addAll(terms);
        // 1. search for all seeds and save contexts to arff training set
        for (String seed : terms) {
            System.out.println("Bootstrapping with seed " + seed);
            String seedEscaped = Util.escapeSeed(seed);
            String filenameContext = seedEscaped + ".xml";
            String filenameArff = seedEscaped + ".arff";
            String newContextName = l.getContextPath() + File.separator + filenameContext;
            String newArffName = l.getArffPath() + File.separator + filenameArff;
            if (!arffFileList.contains(filenameArff)) {
                if (!contextFileList.contains(filenameContext)) {
                    try {
                        l.getContextsForSeed(seed, newContextName);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
                trainingSet = new InternalTrainingSet(new File(newContextName));
                trainingSet.createTrainingSet("True", newArffName);
                System.out.println("Created " + newArffName);
            }
            newArffFiles.add(newArffName);
        }

        // 2. get reliable patterns, save their data to this.reliablePatternsAndContexts and new seeds to 
        // this.foundSeeds_iteration
        try {
            saveReliablePatternData(l, newArffFiles, getThreshold());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        String output_iteration = getString_reliablePatternOutput(this.reliablePatternsAndContexts, numIter);
        //TODO: output trace of decisions... (reliability of patterns, change in trusted patterns, instances...)
        try {
            Util.writeToFile(logFile, "utf-8", output_iteration, true);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        //TODO: USE DIFFERENT STOP CRITERION: continue until patterns stable...
        //TODO: NUM ITER...
        if (numIter == 9) {
            System.out.println("Reached maximum number of iterations! Returning.");
            return;
        }
        bootstrap(this.foundSeeds_iteration, numIter, maxIter);
    }

    private String getString_reliablePatternOutput(Map<String, List<String[]>> patternsAndContexts, int iteration) {
        String string = "Iteration " + iteration + ":\n";
        for (String pattern : patternsAndContexts.keySet()) {
            string += "\tPattern " + pattern + "\n";
            for (String[] context : patternsAndContexts.get(pattern)) {
                String contextString = "";
                for (String entry : context) {
                    contextString += entry + " ";
                }
                string += "\t\t" + contextString.trim() + "\n";
            }
        }
        return string;
    }

    //TODO: compare with BL4 - use method there
    private InternalTrainingSet searchForInstanceInCorpus(String instance) throws IOException {
        File contextDir = new File(l.getContextPath());
        String[] contextFiles = contextDir.list();
        List<String> contextFileList = Arrays.asList(contextFiles);
        InternalTrainingSet trainingSet;

        String seedEscaped = Util.escapeSeed(instance);
        String filenameContext = seedEscaped + ".xml";

        if (!contextFileList.contains(filenameContext)) {
            l.getContextsForSeed(instance, l.getContextPath() + File.separator + filenameContext);
        }
        trainingSet = new InternalTrainingSet(new File(l.getContextPath() + File.separator + filenameContext));
        return trainingSet;
    }

    //TODO: DOCSTRING
    /**
     * Computes reliability of extraction pattern newPat: if above threshold,
     * saves newPat along with its extracted contexts
     *
     * @param newPat	the extraction pattern (...)
     * @param threshold	reliability threshold
     * @return	true if pattern is deemed reliable, false otherwise
     */
    private boolean saveRelevantPatternsAndContexts(String[] newPat, double threshold) throws IOException {
        List<String[]> extractedContexts = getReliable_pattern(newPat, threshold);
        if (extractedContexts != null) {
            this.reliablePatternsAndContexts.put(newPat[1], extractedContexts);
            for (String[] studyNcontext : extractedContexts) {
                String studyName = studyNcontext[0];
                this.foundSeeds_iteration.add(studyName);
            }
            System.out.println("found relevant pattern: " + newPat[1]);
            return true;
        }
        return false;
    }

    /**
     * Computes the reliability of an instance... see
     * http://www.aclweb.org/anthology/P06-1#page=153
     *
     * @return
     */
    public double computeInstanceReliability(ReliabilityInstance instance) {
        if (this.reliableInstances.contains(instance.getName())) {
            return 1;
        }
        double rp = 0;
        Map<String, Double> patternsAndPmis = instance.getAssociations();
        int P = patternsAndPmis.size();
        for (String patternString : patternsAndPmis.keySet()) {
            double pmi = patternsAndPmis.get(patternString);
            ReliabilityPattern pattern = l.getReliability().getPatterns().get(patternString);
            rp += ((pmi / l.getReliability().getMaximumPmi()) * computePatternReliability(pattern));
        }
        return rp / P;
    }

    /**
     * Computes the reliability of a pattern... see
     * http://www.aclweb.org/anthology/P06-1#page=153
     *
     * @return
     */
    public double computePatternReliability(ReliabilityPattern pattern) {
        double rp = 0;
        Map<String, Double> instancesAndPmis = pattern.getAssociations();
        int P = instancesAndPmis.size();
        for (String instanceName : instancesAndPmis.keySet()) {
            double pmi = instancesAndPmis.get(instanceName);
            ReliabilityInstance instance = l.getReliability().getInstances().get(instanceName);
            rp += ((pmi / this.l.getReliability().getMaximumPmi()) * computeInstanceReliability(instance));
        }
        return rp / P;
    }

    /**
     * Determines reliability of pattern based on pattern ranking: if a pattern
     * extracts many reliable instances (dataset titles), it has a high
     * reliability. Reliability of instance: extracted by many other patterns as
     * patterns as well = high agreement.
     *
     * @param ngramRegex	the pattern to be assessed
     * @return	list of extracted contexts of pattern if pattern reliablity score
     * is above threshold, null else
     *
     */
    //see: http://www.aclweb.org/anthology/P06-1#page=153 (cite here...)
	/*"Espresso ranks all patterns in P according to reliability rπ and discards all but the top-k, where
     * k is set to the number of patterns from the previous iteration plus one. In general, we expect that
     * the set of patterns is formed by those of the previous iteration plus a new one. Yet, new 
     * statistical evidence can lead the algorithm to discard a	pattern that was previously discovered. "
     */
    private List<String[]> getReliable_pattern(String[] ngramRegex, double threshold) throws IOException {
        System.out.println("Checking if pattern is reliable: " + ngramRegex[1]);
        ReliabilityPattern newPat = new ReliabilityPattern(ngramRegex[1]);
        // pattern hast to be searched in complete corpus to compute p_y
        //TODO: HOW DOES IT AFFECT PRECISION / RECALL IF KNOWN CONTEXT FILES ARE USED INSTEAD?
        //TODO: count occurrences of pattern in negative contexts and compute pmi etc...
        // count sentences or count only one occurrence per document?
        File corpus = new File(l.getCorpusPath());
        double data_size = corpus.list().length;

        // store results for later use (if pattern deemed reliable)
        Set<String[]> pattern = new HashSet();
        pattern.add(ngramRegex);
        //
        List<String[]> extractedInfo_check = l.processPatterns_reliabilityCheck(pattern, "?");
        //TODO: similar to Python, does division of two ints yield an int in Java? or is it not necessary to convert one operand to double?
        //double p_y = extractedInfo.size() / data_size;
        // this yields the number of documents at least one occurrence of the pattern was found
        // multiple occurrences inside of one document are not considered
        //double p_y = matchingDocs.length / data_size;
        // this counts multiple occurrences inside of documents, not only document-wise
        double p_y = extractedInfo_check.size() / data_size;

        // for every known instance, check whether pattern is associated with it
        for (String instance : this.reliableInstances) {
            int totalSentences = 0;
            int occurrencesPattern = 0;
            /*//Alternatively, read context xml file instead of arff file
             //using arff file allows usage of positive vs. negative annotations though
             String instanceContext = this.contextPath + File.separator + Util.escapeSeed( instance ) + ".xml";
             TrainingSet trainingset_instance = new TrainingSet( new File( instanceContext ));
             HashSet<String[]> contexts = trainingset_instance.getContexts();*/
            String instanceArff = l.getArffPath() + File.separator + Util.escapeSeed(instance) + ".arff";
            //search patterns in all context sentences..-.
            Reader reader = new InputStreamReader(new FileInputStream(instanceArff), "UTF-8");
            Instances data = new Instances(reader);
            reader.close();
            data.setClassIndex(data.numAttributes() - 1);
            List<String> contexts_pos = l.getContextsAsStrings()[0];            
            //ArrayList<String> contexts_neg = this.contextsAsStrings[1];
            System.out.println("Searching for pattern " + ngramRegex[1] + " in contexts of " + instance);
            //TODO: USE SAFEMATCHING
            for (String context : contexts_pos) {
                totalSentences++;
                Pattern p = Pattern.compile(ngramRegex[1]);
                Matcher m = p.matcher(context);
                if (m.find()) {
                    occurrencesPattern++;
                }
            }

            //double p_xy = occurrencesPattern / totalSentences;
            double p_xy = occurrencesPattern / data_size;
            // another way to count joint occurrences

            /*for ( String[] studyNcontext : extractedInfo )
             {
             String studyName = studyNcontext[0];
             String context = studyNcontext[1];
             String corpusFilename = studyNcontext[2];
             String usedPat = studyNcontext[3];
             context = Util.escapeXML(context);
             //replace all non-characters (utf-8) -> count ALLBUS 2000 and ALLBUS 2001 as instances of ALLBUS...
             String datasetSeries = studyName.replaceAll( "[^\\p{L}]", "" ).trim();
             datasetNames.add( datasetSeries );
             if ( jointOccurrences.containsKey( datasetSeries ))
             {
             jointOccurrences.put( datasetSeries, jointOccurrences.get( datasetSeries ) + 1 );
             }
             else { jointOccurrences.put( datasetSeries, 1 ); }
             }
             */
            ReliabilityInstance newInstance = new ReliabilityInstance(instance);
            //p_xy: P(x,y) - joint probability of pattern and instance ocurring in data 
            // all entries in the current context file belong to one instance (seed)
            // select those entries having the current pattern

            //additional searching step here is not necessary... change that
            //int jointOccurrences_xy = jointOccurrences.get( instance );
            //double p_xy = jointOccurrences_xy / data_size;

            //1. search for instance in the corpus
            //creates context xml files - if dataset is searched again as seed, saved file can be used
            //TrainingSet instanceContexts = searchForInstanceInCorpus( instance );
            //2. process context files
            //info needed: (1) contexts, (2) filenames (when counting occurrences per file)

            //HashSet<String[]> contexts = instanceContexts.getContexts();
            //HashSet<String> filenames = instanceContexts.getDocuments();

            //p_x: P(x) - probability of instance occurring in the data
            //number of times the instance occurs in the corpus
            //int totalOccurrences_x = contexts.size();
            //double p_x = totalOccurrences_x / data_size;
            double p_x = totalSentences / data_size;
            //p_x: P(y) - probability of pattern occurring in the data				

            System.out.println("Computing pmi of " + ngramRegex[1] + " and " + instance);
            double pmi_pattern = l.getReliability().pmi(p_xy, p_x, p_y);
            newPat.addAssociation(instance, pmi_pattern);
            newInstance.addAssociation(newPat.getPattern(), pmi_pattern);

            //newInstance.setReliability( reliability_instance( instance ));
            // addPattern and addIstance take care of adding connections to 
            // consisting associations of entities
            l.getReliability().addPattern(newPat);
            l.getReliability().addInstance(newInstance);
            l.getReliability().setMaxPmi(pmi_pattern);
        }
        System.out.println("Computing relevance of " + ngramRegex[1]);
        double patternReliability = computePatternReliability(newPat);
        //newPat.setReliability( patternReliability );
        //this.reliability.addPattern( newPat );
        //double[] pmis, double[] instanceReliabilities, double max_pmi
        if (patternReliability >= threshold) {
            System.out.println("Pattern " + ngramRegex[1] + " deemed reliable");
            List<String[]> extractedInfo = l.processPatterns(pattern, "?", "", l.getIndexPath(), l.getCorpusPath());
            // number of found contexts = number of occurrences of patterns in the corpus
            // note: not per file though but in total
            // (with any arbitrary dataset title = instance)
            return extractedInfo;
        } else {
            System.out.println("Pattern " + ngramRegex[1] + " deemed unreliable");
            return null;
        }
    }

    /**
     * Determines reliablity of instance based on instance ranking: if an
     * instance is extracted by many reliable patterns, it has a high
     * reliability. Reliability of pattern: extracts many reliable instances (in
     * proportion to unreliable instances).
     *
     * @param instance	the instance (dataset title) to be assessed
     * @return	boolean value: reliablity score above threshold or not
     */
    private double reliability_instance(String instance) {
        System.out.println("Checking if instance is reliable: " + instance);
        ReliabilityInstance curInstance = l.getReliability().getInstances().get(instance);        
        return computeInstanceReliability(curInstance);
    }

    /**
     * Generates extraction patterns, computes their reliability and saves
     * contexts extracted by reliable patterns
     *
     * @param filenames_arff	training files containing dataset references, basis
     * for pattern generation
     * @param threshold	threshold for pattern reliability
     * @return	...
     */
    private void saveReliablePatternData(Learner l, Collection<String> filenames_arff, double threshold) throws IOException {
        for (String filename_arff : filenames_arff) {
            ArffFile arf = new ArffFile(filename_arff);
            Reader reader = new InputStreamReader(new FileInputStream(filename_arff), "UTF-8");
            Instances data = new Instances(reader);
            reader.close();
            // setting class attribute
            data.setClassIndex(data.numAttributes() - 1);
            System.out.println(data.toSummaryString());
            Instances data_positive = arf.getInstancesByClassAttribute("True");
            l.setContextsAsStrings(arf.getStrings(data));
            data_positive.setClassIndex(data_positive.numAttributes() - 1);
            System.out.println(data_positive.toSummaryString());

            // only check positive instances for patterns
            Enumeration<Instance> posInstanceEnum = data_positive.enumerateInstances();
            int n = 0;
            int m = data_positive.numInstances();
            while (posInstanceEnum.hasMoreElements()) {
                Instance curInstance = posInstanceEnum.nextElement();
                n++;
                System.out.println("Inducing relevant patterns for instance " + n + " of " + m + " for " + " \"" + filename_arff + "\"");

                String attVal0 = curInstance.stringValue(0); //l5
                String attVal1 = curInstance.stringValue(1); //l4
                String attVal2 = curInstance.stringValue(2); //l3
                String attVal3 = curInstance.stringValue(3); //l2
                String attVal4 = curInstance.stringValue(4); //l1
                String attVal5 = curInstance.stringValue(5); //r1
                String attVal6 = curInstance.stringValue(6); //r2
                String attVal7 = curInstance.stringValue(7); //r3
                String attVal8 = curInstance.stringValue(8); //r4
                String attVal9 = curInstance.stringValue(9); //r5

                //TODO: CONSTRUCT LUCENE QUERIES ONLY WHEN NEEDED (BELOW) 
                String attVal0_lucene = Util.normalizeAndEscapeRegex_lucene(attVal0);
                String attVal1_lucene = Util.normalizeAndEscapeRegex_lucene(attVal1);
                String attVal2_lucene = Util.normalizeAndEscapeRegex_lucene(attVal2);
                String attVal3_lucene = Util.normalizeAndEscapeRegex_lucene(attVal3);
                String attVal4_lucene = Util.normalizeAndEscapeRegex_lucene(attVal4);
                String attVal5_lucene = Util.normalizeAndEscapeRegex_lucene(attVal5);
                String attVal6_lucene = Util.normalizeAndEscapeRegex_lucene(attVal6);
                String attVal7_lucene = Util.normalizeAndEscapeRegex_lucene(attVal7);
                String attVal8_lucene = Util.normalizeAndEscapeRegex_lucene(attVal8);
                String attVal9_lucene = Util.normalizeAndEscapeRegex_lucene(attVal9);

                String attVal0_quoted = Pattern.quote(attVal0);
                String attVal1_quoted = Pattern.quote(attVal1);
                String attVal2_quoted = Pattern.quote(attVal2);
                String attVal3_quoted = Pattern.quote(attVal3);
                String attVal4_quoted = Pattern.quote(attVal4);
                String attVal5_quoted = Pattern.quote(attVal5);
                String attVal6_quoted = Pattern.quote(attVal6);
                String attVal7_quoted = Pattern.quote(attVal7);
                String attVal8_quoted = Pattern.quote(attVal8);
                String attVal9_quoted = Pattern.quote(attVal9);

                String attVal4_regex = Util.normalizeAndEscapeRegex(attVal4);
                String attVal5_regex = Util.normalizeAndEscapeRegex(attVal5);

                //...
                if (attVal4.matches(".*\\P{Punct}")) {
                    attVal4_quoted += "\\s";
                    attVal4_regex += "\\s";
                }
                if (attVal5.matches("\\P{Punct}.*")) {
                    attVal5_quoted = "\\s" + attVal5_quoted;
                    attVal5_regex = "\\s" + attVal5_regex;
                }


                // two words enclosing study name
                String luceneQuery1 = "\"" + attVal4_lucene + " * " + attVal5_lucene + "\"";
                String regex_ngram1_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;
                String regex_ngram1_minimal = attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex;

                // phrase consisting of 2 words behind study title + fixed word before
                String luceneQueryA = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + "\"";
                String regex_ngramA_normalizedAndQuoted = Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;
                String regex_ngramA_minimal = attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6);

                // phrase consisting of 2 words behind study title + (any) word found in data before
                // (any word cause this pattern is induced each time for each different instance having this phrase...)
                String luceneQueryA_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + "\"";
                String regex_ngramA_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted;
                //String regex_ngramA_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

                // phrase consisting of 3 words behind study title + fixed word before
                String luceneQueryB = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + "\"";
                String regex_ngramB_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;
                String regex_ngramB_minimal = attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7);

                String luceneQueryB_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + "\"";
                String regex_ngramB_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted;
                //String regex_ngramB_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

                //phrase consisting of 4 words behind study title + fixed word before
                String luceneQueryC = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + "\"";
                String regex_ngramC_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.normalizeAndEscapeRegex(attVal8) + "\\s" + Util.lastWordRegex;
                String regex_ngramC_minimal = attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.normalizeAndEscapeRegex(attVal8);

                String luceneQueryC_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + "\"";
                String regex_ngramC_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted + "\\s" + attVal8_quoted;
                //String regex_ngramC_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.normalizeAndEscapeRegex(attVal8) + "\\s" + Util.lastWordRegex;

                //phrase consisting of 5 words behind study title + fixed word before
                String luceneQueryD = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + " " + attVal9_lucene + "\"";
                String regex_ngramD_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.normalizeAndEscapeRegex(attVal8) + "\\s" + Util.normalizeAndEscapeRegex(attVal9);
                String regex_ngramD_minimal = attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.normalizeAndEscapeRegex(attVal8) + "\\s" + Util.normalizeAndEscapeRegex(attVal9);

                // now the pattern can emerge at other positions, too, and is counted here as relevant...
                String luceneQueryD_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + " " + attVal9_lucene + "\"";
                String regex_ngramD_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted + "\\s" + attVal8_quoted + "\\s" + attVal9_quoted;
                //String regex_ngramD_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.normalizeAndEscapeRegex(attVal8) + "\\s" + Util.normalizeAndEscapeRegex(attVal9);

                // phrase consisting of 2 words before study title + fixed word behind
                String luceneQuery2 = "\"" + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
                String regex_ngram2_quoted = attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_quoted;
                String regex_ngram2_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;
                String regex_ngram2_minimal = Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex;

                String luceneQuery2_flex = "\"" + attVal3_lucene + " " + attVal4_lucene + "\"";
                String regex_ngram2_flex_quoted = attVal3_quoted + "\\s" + attVal4_quoted;
                //String regex_ngram2_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

                // phrase consisting of 3 words before study title + fixed word behind
                String luceneQuery3 = "\"" + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
                String regex_ngram3_quoted = attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_quoted;
                String regex_ngram3_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;
                String regex_ngram3_minimal = Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex;

                String luceneQuery3_flex = "\"" + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + "\"";
                String regex_ngram3_flex_quoted = attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted;
                //String regex_ngram3_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

                //phrase consisting of 4 words before study title + fixed word behind
                String luceneQuery4 = "\"" + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
                String regex_ngram4_quoted = attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_quoted;
                String regex_ngram4_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.normalizeAndEscapeRegex(attVal1) + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;
                String regex_ngram4_minimal = Util.normalizeAndEscapeRegex(attVal1) + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex;

                String luceneQuery4_flex = "\"" + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + "\"";
                String regex_ngram4_flex_quoted = attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted;
                //String regex_ngram4_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.normalizeAndEscapeRegex(attVal1) + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

                // phrase consisting of 5 words before study title + fixed word behind
                String luceneQuery5 = "\"" + attVal0_lucene + " " + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
                String regex_ngram5_quoted = attVal0_quoted + "\\s" + attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_quoted;
                String regex_ngram5_normalizedAndQuoted = Util.normalizeAndEscapeRegex(attVal0) + "\\s" + Util.normalizeAndEscapeRegex(attVal1) + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;
                String regex_ngram5_minimal = Util.normalizeAndEscapeRegex(attVal0) + "\\s" + Util.normalizeAndEscapeRegex(attVal1) + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex;

                String luceneQuery5_flex = "\"" + attVal0_lucene + " " + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + "\"";
                String regex_ngram5_flex_quoted = attVal0_quoted + "\\s" + attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted;
                //String regex_ngram5_flex_normalizedAndQuoted = Util.normalizeAndEscapeRegex(attVal0) + "\\s" + Util.normalizeAndEscapeRegex(attVal1) + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

                // constraint for ngrams: at least one component not be a stopword
                //TODO: GOT ORDER WRONG IN PREVIOUS DOCSTRING
                // first entry: luceneQuery; second entry: normalized and quoted version; third entry: minimal version (for reliability checks...)
                String[] newPat = new String[3];
                // prevent induction of patterns less general than already known patterns:
                // check whether pattern is known before continuing
                // also improves performance
                //TODO: RELIABILITY SCORE HAS TO BE COMPUTED AGAIN, MAY CHANGE?
                // use pmi scores that are already stored... only compute reliability again, max may have changed
                if (l.getProcessedPatterns().contains(regex_ngram1_normalizedAndQuoted)) {
                    continue;
                }
                newPat[0] = luceneQuery1;
                newPat[1] = regex_ngram1_normalizedAndQuoted;
                newPat[2] = regex_ngram1_minimal;

                if (!(l.isStopword(attVal4) & l.isStopword(attVal5))) {
                    if (saveRelevantPatternsAndContexts(newPat, threshold)) {
                        this.reliablePatterns_iteration.add(newPat[1]);
                        continue;
                    }
                }

                //TODO: do not return here, instead process Type phrase behind study title terms also!
                if (l.getProcessedPatterns().contains(regex_ngram2_normalizedAndQuoted)) {
                    continue;
                }
                newPat[0] = luceneQuery2;
                newPat[1] = regex_ngram2_normalizedAndQuoted;
                newPat[2] = regex_ngram2_minimal;
                if (!((l.isStopword(attVal4) & l.isStopword(attVal5)) | (l.isStopword(attVal3) & l.isStopword(attVal5)) | (l.isStopword(attVal3) & l.isStopword(attVal4)))) {
                    if (saveRelevantPatternsAndContexts(newPat, threshold)) {
                        this.reliablePatterns_iteration.add(newPat[1]);
                        continue;
                    }
                }

                if (l.getProcessedPatterns().contains(regex_ngram3_normalizedAndQuoted)) {
                    continue;
                }
                newPat[0] = luceneQuery3;
                newPat[1] = regex_ngram3_normalizedAndQuoted;
                newPat[2] = regex_ngram3_minimal;
                if (!(l.isStopword(attVal2) & l.isStopword(attVal3) & l.isStopword(attVal4) & l.isStopword(attVal5))) {
                    if (saveRelevantPatternsAndContexts(newPat, threshold)) {
                        this.reliablePatterns_iteration.add(newPat[1]);
                        continue;
                    }
                }

                if (l.getProcessedPatterns().contains(regex_ngram4_normalizedAndQuoted)) {
                    continue;
                }
                newPat[0] = luceneQuery4;
                newPat[1] = regex_ngram4_normalizedAndQuoted;
                newPat[2] = regex_ngram4_minimal;
                if (!(l.isStopword(attVal1) & l.isStopword(attVal2) & l.isStopword(attVal3) & l.isStopword(attVal4) & l.isStopword(attVal5))) {
                    if (saveRelevantPatternsAndContexts(newPat, threshold)) {
                        this.reliablePatterns_iteration.add(newPat[1]);
                        continue;
                    }
                }

                if (l.getProcessedPatterns().contains(regex_ngram5_normalizedAndQuoted)) {
                    continue;
                }
                newPat[0] = luceneQuery5;
                newPat[1] = regex_ngram5_normalizedAndQuoted;
                newPat[2] = regex_ngram5_minimal;
                if (!(l.isStopword(attVal0) & l.isStopword(attVal1) & l.isStopword(attVal2) & l.isStopword(attVal3) & l.isStopword(attVal4) & l.isStopword(attVal5))) {
                    if (saveRelevantPatternsAndContexts(newPat, threshold)) {
                        this.reliablePatterns_iteration.add(newPat[1]);
                        continue;
                    }
                }

                //...
                if (l.getProcessedPatterns().contains(regex_ngramA_normalizedAndQuoted)) {
                    continue;
                }
                newPat[0] = luceneQueryA;
                newPat[1] = regex_ngramA_normalizedAndQuoted;
                newPat[2] = regex_ngramA_minimal;
                if (!((l.isStopword(attVal5) & l.isStopword(attVal6)) | (l.isStopword(attVal4) & l.isStopword(attVal6)) | (l.isStopword(attVal4) & l.isStopword(attVal5)))) {
                    if (saveRelevantPatternsAndContexts(newPat, threshold)) {
                        this.reliablePatterns_iteration.add(newPat[1]);
                        continue;
                    }
                }

                if (l.getProcessedPatterns().contains(regex_ngramB_normalizedAndQuoted)) {
                    continue;
                }
                newPat[0] = luceneQueryB;
                newPat[1] = regex_ngramB_normalizedAndQuoted;
                newPat[2] = regex_ngramB_minimal;
                if (!(l.isStopword(attVal4) & l.isStopword(attVal5) & l.isStopword(attVal6) & l.isStopword(attVal7))) {
                    if (saveRelevantPatternsAndContexts(newPat, threshold)) {
                        this.reliablePatterns_iteration.add(newPat[1]);
                        continue;
                    }
                }

                if (l.getProcessedPatterns().contains(regex_ngramC_normalizedAndQuoted)) {
                    continue;
                }
                newPat[0] = luceneQueryC;
                newPat[1] = regex_ngramC_normalizedAndQuoted;
                newPat[2] = regex_ngramC_minimal;
                if (!(l.isStopword(attVal4) & l.isStopword(attVal5) & l.isStopword(attVal6) & l.isStopword(attVal7) & l.isStopword(attVal8))) {
                    if (saveRelevantPatternsAndContexts(newPat, threshold)) {
                        this.reliablePatterns_iteration.add(newPat[1]);
                        continue;
                    }
                }

                if (l.getProcessedPatterns().contains(regex_ngramD_normalizedAndQuoted)) {
                    continue;
                }
                newPat[0] = luceneQueryD;
                newPat[1] = regex_ngramD_normalizedAndQuoted;
                newPat[2] = regex_ngramD_minimal;
                if (!(l.isStopword(attVal4) & l.isStopword(attVal5) & l.isStopword(attVal6) & l.isStopword(attVal7) & l.isStopword(attVal8) & l.isStopword(attVal9))) {
                    if (saveRelevantPatternsAndContexts(newPat, threshold)) {
                        this.reliablePatterns_iteration.add(newPat[1]);
                        continue;
                    }
                }
            }
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
}
