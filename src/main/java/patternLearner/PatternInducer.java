/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package patternLearner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import patternLearner.bootstrapping.ArffFile;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author domi
 */
public class PatternInducer {
    
    private Learner learner;
    
    public PatternInducer(Learner learner) {
        this.learner = learner;
    }
    
     /**
     * rest of deprecated and deleted method readArff - delete after having
     * integrated the remaining functionality in calling methods.
     *
     * @param path_corpus
     */
    public void readArff(String filename) throws FileNotFoundException, IOException {
        Set<String[]> ngramPats = induceRelevantPatternsFromArff(new ArffFile(filename));
        String dir = learner.getCorpusPath();
        File corpus = new File(dir);
        String[] corpus_test = corpus.list();
        if (corpus_test == null) {
            // Either dir does not exist or is not a directory
        } else {
            for (int i = 0; i < corpus_test.length; i++) {
                // Get filename of file or directory
                corpus_test[i] = dir + File.separator + corpus_test[i];
            }
        }
        System.out.println("inserted all text filenames to corpus");

        String[] filenames_grams = new String[3];
        filenames_grams[0] = learner.getOutputPath() + File.separator + new File(filename).getName().replace(".arff", "") + "_foundStudies.txt";
        filenames_grams[1] = learner.getOutputPath() + File.separator + new File(filename).getName().replace(".arff", "") + "_foundContexts.xml";
        filenames_grams[2] = learner.getOutputPath() + File.separator + new File(filename).getName().replace(".arff", "") + "_usedPatterns.txt";
        // before getting new refs, append all patterns to file
        // note: all induced patterns, not only new ones
        System.out.println("appending patterns to file...");
        String allPatsFile = learner.getOutputPath() + File.separator + new File("newPatterns.txt");
        OutputStreamWriter fstreamw = new OutputStreamWriter(new FileOutputStream(allPatsFile, true), "UTF-8");
        BufferedWriter outw = new BufferedWriter(fstreamw);
        for (String p[] : ngramPats) {
            outw.write(p[1] + System.getProperty("line.separator"));
        }
        outw.close();
        System.out.println("done. ");

        System.out.println("using patterns to extract new contexts...");
        //TODO: use this instead?
        //ArrayList<String[]> processPatterns(HashSet<String[]> patSetIn, String seed, String outputDir, String path_index, String path_corpus) throws FileNotFoundException, IOException
        PatternApplier app = new PatternApplier(learner);
        List<String[]> resNgrams = app.getStudyRefs_optimized(ngramPats);

        System.out.println("starting output of found studies and contexts (and used patterns)");
        learner.output(resNgrams, filenames_grams);
        //outputArffFile(filenames_grams[1]);
        System.out.println("done. ");

        System.out.println("writing patterns to file...");
        String allNgramPatsFile = learner.getOutputPath() + File.separator + new File(filename).getName().replace(".arff", "") + "_foundPatterns_all.txt";
        OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(allNgramPatsFile), "UTF-8");
        BufferedWriter outp = new BufferedWriter(fstream);
        for (String p[] : ngramPats) {
            outp.write(p[1] + System.getProperty("line.separator"));
        }
        outp.close();
        System.out.println("done. ");
    }

    private Set<String[]> induceRelevantPatternsFromArff(ArffFile arf) throws FileNotFoundException, IOException {
        Instances data_positive = arf.getInstancesByClassAttribute("True");
        //TODO: what about the reliable stuff? 
        learner.setContextsAsStrings(arf.getStrings(arf.getData()));
        data_positive.setClassIndex(data_positive.numAttributes() - 1);
        System.out.println(data_positive.toSummaryString());

        Enumeration<Instance> posInstanceEnum = data_positive.enumerateInstances();
        Set<String[]> patterns = new HashSet();
        int n = 0;
        int m = data_positive.numInstances();
        while (posInstanceEnum.hasMoreElements()) {
            Instance curInstance = posInstanceEnum.nextElement();
            n++;
            //save patterns and output...
            System.out.println("Inducing relevant patterns for instance " + n + " of " + m + " for " + " \"" + arf.getFileName() + "\"");
            patterns.addAll(getRelevantNgramPatterns(curInstance, arf.getData()));
            System.out.println("Added all ngram-patterns for instance " + n + " of " + m + " to pattern set");
        }
        return patterns;
    }

    /**
     * Construct extraction patterns, assess their validity and return relevant
     * patterns
     *
     * @param instance	...
     * @param data	training data
     * @return	...
     */
    private List<String[]> getRelevantNgramPatterns(Instance instance, Instances data) throws IOException {
        String attVal0 = instance.stringValue(0); //l5
        String attVal1 = instance.stringValue(1); //l4
        String attVal2 = instance.stringValue(2); //l3
        String attVal3 = instance.stringValue(3); //l2
        String attVal4 = instance.stringValue(4); //l1
        String attVal5 = instance.stringValue(5); //r1
        String attVal6 = instance.stringValue(6); //r2
        String attVal7 = instance.stringValue(7); //r3
        String attVal8 = instance.stringValue(8); //r4
        String attVal9 = instance.stringValue(9); //r5

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
        String regex_ngram1_quoted = attVal4_quoted + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_quoted;
        String regex_ngram1_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

        // phrase consisting of 2 words behind study title + fixed word before
        String luceneQueryA = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + "\"";
        String regex_ngramA_quoted = Pattern.quote(attVal4) + "\\s?" + Util.studyRegex_ngram + "\\s?" + Pattern.quote(attVal5) + "\\s" + Pattern.quote(attVal6);
        String regex_ngramA_normalizedAndQuoted = Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

        // phrase consisting of 2 words behind study title + (any) word found in data before!
        // (any word cause this pattern is induced each time for each different instance having this phrase...)
        String luceneQueryA_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + "\"";
        String regex_ngramA_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted;
        //String regex_ngramA_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

        // phrase consisting of 3 words behind study title + fixed word before
        String luceneQueryB = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + "\"";
        String regex_ngramB_quoted = attVal4_quoted + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted;
        String regex_ngramB_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

        String luceneQueryB_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + "\"";
        String regex_ngramB_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted;
        //String regex_ngramB_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

        // phrase consisting of 4 words behind study title + fixed word before
        String luceneQueryC = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + "\"";
        String regex_ngramC_quoted = attVal4_quoted + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted + "\\s" + attVal8_quoted;
        String regex_ngramC_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.normalizeAndEscapeRegex(attVal8) + "\\s" + Util.lastWordRegex;

        String luceneQueryC_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + "\"";
        String regex_ngramC_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted + "\\s" + attVal8_quoted;
        //String regex_ngramC_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.normalizeAndEscapeRegex(attVal8) + "\\s" + Util.lastWordRegex;

        // phrase consisting of 5 words behind study title + fixed word before
        String luceneQueryD = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + " " + attVal9_lucene + "\"";
        String regex_ngramD_quoted = attVal4_quoted + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted + "\\s" + attVal8_quoted + "\\s" + attVal9_quoted;
        String regex_ngramD_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.normalizeAndEscapeRegex(attVal8) + "\\s" + Util.normalizeAndEscapeRegex(attVal9);

        // now the pattern can emerge at other positions, too, and is counted here as relevant...
        String luceneQueryD_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + " " + attVal9_lucene + "\"";
        String regex_ngramD_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted + "\\s" + attVal8_quoted + "\\s" + attVal9_quoted;
        //String regex_ngramD_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.normalizeAndEscapeRegex(attVal6) + "\\s" + Util.normalizeAndEscapeRegex(attVal7) + "\\s" + Util.normalizeAndEscapeRegex(attVal8) + "\\s" + Util.normalizeAndEscapeRegex(attVal9);

        // phrase consisting of 2 words before study title + fixed word behind
        String luceneQuery2 = "\"" + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
        String regex_ngram2_quoted = attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_quoted;
        String regex_ngram2_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

        String luceneQuery2_flex = "\"" + attVal3_lucene + " " + attVal4_lucene + "\"";
        String regex_ngram2_flex_quoted = attVal3_quoted + "\\s" + attVal4_quoted;
        //String regex_ngram2_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

        // phrase consisting of 3 words before study title + fixed word behind
        String luceneQuery3 = "\"" + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
        String regex_ngram3_quoted = attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_quoted;
        String regex_ngram3_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

        String luceneQuery3_flex = "\"" + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + "\"";
        String regex_ngram3_flex_quoted = attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted;
        //String regex_ngram3_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.wordRegex_atomic + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

        // phrase consisting of 4 words before study title + fixed word behind
        String luceneQuery4 = "\"" + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
        String regex_ngram4_quoted = attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_quoted;
        String regex_ngram4_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.normalizeAndEscapeRegex(attVal1) + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

        String luceneQuery4_flex = "\"" + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + "\"";
        String regex_ngram4_flex_quoted = attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted;
        //String regex_ngram4_flex_normalizedAndQuoted = Util.wordRegex_atomic + "\\s" + Util.normalizeAndEscapeRegex(attVal1) + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

        // phrase consisting of 5 words before study title + fixed word behind
        String luceneQuery5 = "\"" + attVal0_lucene + " " + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
        String regex_ngram5_quoted = attVal0_quoted + "\\s" + attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_quoted;
        String regex_ngram5_normalizedAndQuoted = Util.normalizeAndEscapeRegex(attVal0) + "\\s" + Util.normalizeAndEscapeRegex(attVal1) + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

        String luceneQuery5_flex = "\"" + attVal0_lucene + " " + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + "\"";
        String regex_ngram5_flex_quoted = attVal0_quoted + "\\s" + attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted;
        //String regex_ngram5_flex_normalizedAndQuoted = Util.normalizeAndEscapeRegex(attVal0) + "\\s" + Util.normalizeAndEscapeRegex(attVal1) + "\\s" + Util.normalizeAndEscapeRegex(attVal2) + "\\s" + Util.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + Util.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.wordRegex + "\\s" + Util.lastWordRegex;

        List<String[]> ngramPats = new ArrayList();
        // constraint for ngrams: at least one component not be a stopword

        // prevent induction of patterns less general than already known patterns:
        // check whether pattern is known before continuing
        // also improves performance
        if (learner.getProcessedPatterns().contains(regex_ngram1_normalizedAndQuoted)) {
            return ngramPats;
        }
        if (!(learner.isStopword(attVal4) & learner.isStopword(attVal5)) & learner.isRelevant(regex_ngram1_quoted, 0.2))//0.2
        {
            // substitute normalized numbers etc. with regex
            String[] newPat = {luceneQuery1, regex_ngram1_normalizedAndQuoted};
            ngramPats.add(newPat);
            System.out.println("found relevant type 1 pattern (most general): " + regex_ngram1_normalizedAndQuoted);
        } else {
            //TODO: do not return here, instead process Type phrase behind study title terms also"
            if (learner.getProcessedPatterns().contains(regex_ngram2_normalizedAndQuoted)) {
                return ngramPats;
            }
            if (!(learner.isStopword(attVal4) & learner.isStopword(attVal5) | learner.isStopword(attVal3) & learner.isStopword(attVal5) | learner.isStopword(attVal3) & learner.isStopword(attVal4)) & learner.isRelevant(regex_ngram2_quoted, 0.18))//0.18
            {
                System.out.println("found relevant type 2 pattern: " + regex_ngram2_normalizedAndQuoted);
                String[] newPat = {luceneQuery2, regex_ngram2_normalizedAndQuoted};
                ngramPats.add(newPat);
            } else {
                if (learner.getProcessedPatterns().contains(regex_ngram3_normalizedAndQuoted)) {
                    return ngramPats;
                }
                if (!(learner.isStopword(attVal2) & learner.isStopword(attVal3) & learner.isStopword(attVal4) & learner.isStopword(attVal5)) & learner.isRelevant(regex_ngram3_quoted, 0.16))//0.16
                {
                    System.out.println("found relevant type 3 pattern: " + regex_ngram3_normalizedAndQuoted);
                    //ngramPats.add(regex_ngram3_normalizedAndQuoted);
                    String[] newPat = {luceneQuery3, regex_ngram3_normalizedAndQuoted};
                    ngramPats.add(newPat);
                } else {
                    if (learner.getProcessedPatterns().contains(regex_ngram4_normalizedAndQuoted)) {
                        return ngramPats;
                    }
                    if (!(learner.isStopword(attVal1) & learner.isStopword(attVal2) & learner.isStopword(attVal3) & learner.isStopword(attVal4) & learner.isStopword(attVal5)) & learner.isRelevant(regex_ngram4_quoted, 0.14))//0.14
                    {
                        System.out.println("found relevant type 4 pattern: " + regex_ngram4_normalizedAndQuoted);
                        //ngramPats.add(regex_ngram4_normalizedAndQuoted);
                        String[] newPat = {luceneQuery4, regex_ngram4_normalizedAndQuoted};
                        ngramPats.add(newPat);
                    } else {
                        if (learner.getProcessedPatterns().contains(regex_ngram5_normalizedAndQuoted)) {
                            return ngramPats;
                        }
                        if (!(learner.isStopword(attVal0) & learner.isStopword(attVal1) & learner.isStopword(attVal2) & learner.isStopword(attVal3) & learner.isStopword(attVal4) & learner.isStopword(attVal5)) & learner.isRelevant(regex_ngram5_quoted, 0.12))//0.12
                        {
                            System.out.println("found relevant type 5 pattern: " + regex_ngram5_normalizedAndQuoted);
                            //ngramPats.add(regex_ngram5_normalizedAndQuoted);
                            String[] newPat = {luceneQuery5, regex_ngram5_normalizedAndQuoted};
                            ngramPats.add(newPat);
                        }
                    }
                }
            }
            if (learner.getProcessedPatterns().contains(regex_ngramA_normalizedAndQuoted)) {
                return ngramPats;
            }
            if (!(learner.isStopword(attVal5) & learner.isStopword(attVal6) | learner.isStopword(attVal4) & learner.isStopword(attVal6) | learner.isStopword(attVal4) & learner.isStopword(attVal5)) & learner.isRelevant(regex_ngramA_quoted, 0.18))//0.18
            {
                System.out.println("found relevant type A pattern: " + regex_ngramA_normalizedAndQuoted);
                //ngramPats.add(regex_ngramA_normalizedAndQuoted);
                String[] newPat = {luceneQueryA, regex_ngramA_normalizedAndQuoted};
                ngramPats.add(newPat);
            } else {
                if (learner.getProcessedPatterns().contains(regex_ngramB_normalizedAndQuoted)) {
                    return ngramPats;
                }
                if (!(learner.isStopword(attVal4) & learner.isStopword(attVal5) & learner.isStopword(attVal6) & learner.isStopword(attVal7)) & learner.isRelevant(regex_ngramB_quoted, 0.16))//0.16
                {
                    System.out.println("found relevant type B pattern: " + regex_ngramB_normalizedAndQuoted);
                    //ngramPats.add(regex_ngramB_normalizedAndQuoted);
                    String[] newPat = {luceneQueryB, regex_ngramB_normalizedAndQuoted};
                    ngramPats.add(newPat);
                } else {
                    if (learner.getProcessedPatterns().contains(regex_ngramC_normalizedAndQuoted)) {
                        return ngramPats;
                    }
                    if (!(learner.isStopword(attVal4) & learner.isStopword(attVal5) & learner.isStopword(attVal6) & learner.isStopword(attVal7) & learner.isStopword(attVal8)) & learner.isRelevant(regex_ngramC_quoted, 0.14))//0.14
                    {
                        System.out.println("found relevant type C pattern: " + regex_ngramC_normalizedAndQuoted);
                        //ngramPats.add(regex_ngramC_normalizedAndQuoted);
                        String[] newPat = {luceneQueryC, regex_ngramC_normalizedAndQuoted};
                        ngramPats.add(newPat);
                    } else {
                        if (learner.getProcessedPatterns().contains(regex_ngramD_normalizedAndQuoted)) {
                            return ngramPats;
                        }
                        if (!(learner.isStopword(attVal4) & learner.isStopword(attVal5) & learner.isStopword(attVal6) & learner.isStopword(attVal7) & learner.isStopword(attVal8) & learner.isStopword(attVal9)) & learner.isRelevant(regex_ngramD_quoted, 0.12))//0.12
                        {
                            System.out.println("found relevant type D pattern: " + regex_ngramD_normalizedAndQuoted);
                            //ngramPats.add(regex_ngramD_normalizedAndQuoted);
                            String[] newPat = {luceneQueryD, regex_ngramD_normalizedAndQuoted};
                            ngramPats.add(newPat);
                        }
                    }
                }
            }
        }
        return ngramPats;
    }
}
