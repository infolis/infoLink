package io.github.infolis.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class WekaUtils {
	
    /**
     * Retrieves all training instances from the specified data having the
     * specified class attribute. Note: each instance is required to have
     * exactly 10 attributes representing 5 words before and 5 words after the
     * dataset name + one class attribute. Instances having class attribute
     * <emph>True</emph> are positive examples for the relation
     * <emph>IsStudyReference</emph>, instances having class attribute
     * <emph>False</emph> are negative examples.
     *
     * @param data	the training examples to learn from
     * @return	Instances having class <emph>classVal</emph>
     */
    public static Instances getInstances(Instances data, String classVal) {
        FastVector atts = new FastVector();
        FastVector attVals;
        atts.addElement(new Attribute("l5", (FastVector) null));
        atts.addElement(new Attribute("l4", (FastVector) null));
        atts.addElement(new Attribute("l3", (FastVector) null));
        atts.addElement(new Attribute("l2", (FastVector) null));
        atts.addElement(new Attribute("l1", (FastVector) null));
        atts.addElement(new Attribute("r1", (FastVector) null));
        atts.addElement(new Attribute("r2", (FastVector) null));
        atts.addElement(new Attribute("r3", (FastVector) null));
        atts.addElement(new Attribute("r4", (FastVector) null));
        atts.addElement(new Attribute("r5", (FastVector) null));
        attVals = new FastVector();
        attVals.addElement(classVal);
        Attribute classAttr = new Attribute("class", attVals);
        atts.addElement(classAttr);
        Instances data_matchingClass = new Instances("IsStudyReference_" + classVal, atts, 0);
        data_matchingClass.setClass(classAttr);

		// iterate over instances, check value of class attribute
        // return only instances with classVal: disregard instances with other class
        @SuppressWarnings("unchecked")
        Enumeration<Instance> instanceEnum = data.enumerateInstances();
        while (instanceEnum.hasMoreElements()) {
            Instance curInstance = instanceEnum.nextElement();
            String curClassVal = curInstance.stringValue(curInstance.classAttribute());
            if (curClassVal.equals(new String(classVal))) {
                Instance newInstance = new Instance(11);
                newInstance.setDataset(data_matchingClass);
    			// loop over all attributes and fill in values
                // copying values from an existing instance using 
                // Instance newInstance = new Instance(curInstance);
                // does not work...
                for (int i = 0; i < 11; i++) {
                    newInstance.setValue(i, curInstance.stringValue(i));
                }
                data_matchingClass.add(newInstance);
            }
        }
        return data_matchingClass;
    }

    /**
     * Returns the attributes of the instances in data as strings
     *
     * @param data	the training examples
     * @return	first list containing all sentences of positive training
     * instances, second list containing all sentences of negative training
     * instances
     */
    public static List<List<String>> getStrings(Instances data) {
        String studySubstitute = "<STUDYNAME> ";
        List<String> sentences_pos = new ArrayList<String>();
        List<String> sentences_neg = new ArrayList<String>();
        List<String> sentences;
        @SuppressWarnings("unchecked")
        Enumeration<Instance> instanceEnum = data.enumerateInstances();
        while (instanceEnum.hasMoreElements()) {
            Instance curInstance = instanceEnum.nextElement();
            String curClassVal = curInstance.stringValue(curInstance.classAttribute());

            @SuppressWarnings("unchecked")
            Enumeration<Attribute> attributeEnum = data.enumerateAttributes();
            String contextString = "";
            if (curClassVal.equals(new String("True"))) {
                sentences = sentences_pos;
            } else {
                sentences = sentences_neg;
            }

            while (attributeEnum.hasMoreElements()) {
                Attribute curAtt = attributeEnum.nextElement();
                String attVal = curInstance.stringValue(curAtt);
                contextString += attVal + " ";
                if (curAtt.index() == 4) {
                    contextString += studySubstitute;
                }
            }
            sentences.add(contextString);
        }
        List<List<String>> resList = new ArrayList<List<String>>();
        resList.add(sentences_pos);
        resList.add(sentences_neg);
        return resList;
    }

    /**
     * rest of deprecated and deleted method readArff - delete after having
     * integrated the remaining functionality in calling methods. TODO has this
     * been done?
     *
     * @param path_corpus
     * @deprecated
     */
//    private void readArff(String corpusPath, String filename, String outputDir, double threshold) throws FileNotFoundException, IOException, ParseException {
//        Set<String[]> ngramPats = induceRelevantPatternsFromArff(filename, threshold);
//        File corpus = new File(corpusPath);
//        String[] corpus_test = corpus.list();
//        if (corpus_test == null) {
//            // Either dir does not exist or is not a directory
//        } else {
//            for (int i = 0; i < corpus_test.length; i++) {
//                // Get filename of file or directory
//                corpus_test[i] = corpusPath + File.separator + corpus_test[i];
//            }
//        }
//        System.out.println("inserted all text filenames to corpus");
//
//        String[] filenames_grams = new String[3];
//        filenames_grams[0] = outputDir + File.separator + new File(filename).getName().replace(".arff", "") + "_foundDatasets.csv";
//        filenames_grams[1] = outputDir + File.separator + new File(filename).getName().replace(".arff", "") + "_foundContexts.xml";
//        filenames_grams[2] = outputDir + File.separator + new File(filename).getName().replace(".arff", "") + "_usedPatterns.csv";
//	    // before getting new refs, append all patterns to file
//        // note: all induced patterns, not only new ones
//        System.out.println("appending patterns to file...");
//        String allPatsFile = outputDir + File.separator + new File("newPatterns.txt");
//        OutputStreamWriter fstreamw = new OutputStreamWriter(new FileOutputStream(allPatsFile, true), "UTF-8");
//        BufferedWriter outw = new BufferedWriter(fstreamw);
//        for (String p[] : ngramPats) {
//            outw.write(p[1] + System.getProperty("line.separator"));
//        }
//        outw.close();
//        System.out.println("done. ");
//
//        System.out.println("using patterns to extract new contexts...");
//	    //TODO: use this instead?
//        //List<String[]> processPatterns(Set<String[]> patSetIn, String seed, String outputDir, String path_index, String path_corpus) throws FileNotFoundException, IOException
//        try {
//            List<String[]> resNgrams = getStudyRefs_optimized(ngramPats, corpus_test);
//            System.out.println("starting output of found studies and contexts (and used patterns)");
//            output(resNgrams, filenames_grams);
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//            throw (new IOException());
//        } catch (ParseException pe) {
//            pe.printStackTrace();
//            throw pe;
//        }
//        //outputArffFile(filenames_grams[1]);
//        System.out.println("done. ");
//
//        System.out.println("writing patterns to file...");
//        String allNgramPatsFile = outputDir + File.separator + new File(filename).getName().replace(".arff", "") + "_foundPatterns_all.txt";
//        OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(allNgramPatsFile), "UTF-8");
//        BufferedWriter outp = new BufferedWriter(fstream);
//        for (String p[] : ngramPats) {
//            outp.write(p[1] + System.getProperty("line.separator"));
//        }
//        outp.close();
//        System.out.println("done. ");
//    }

    /**
     * Analyse given Instances and return relevant patterns.
     *
     * @param filename	location of the arff file containing the Instances to
     * analyse
     * @return	set of relevant patterns (each pattern consists of x elements:
     * ... ...)
     * @throws FileNotFoundException
     * @throws IOException
     */
    private Set<String[]> induceRelevantPatternsFromArff(String filename, double threshold) throws FileNotFoundException, IOException {
        Instances data = getInstances(filename);
        Instances data_positive = WekaUtils.getInstances(data, "True");
        // TODO
//        this.contextsAsStrings = WekaUtils.getStrings(data);
        data_positive.setClassIndex(data_positive.numAttributes() - 1);
        System.out.println(data_positive.toSummaryString());

        @SuppressWarnings("unchecked")
        Enumeration<Instance> posInstanceEnum = data_positive.enumerateInstances();
        Set<String[]> patterns = new HashSet<String[]>();
        int n = 0;
        int m = data_positive.numInstances();
        while (posInstanceEnum.hasMoreElements()) {
            Instance curInstance = posInstanceEnum.nextElement();
            n++;
            //save patterns and output...
            System.out.println("Inducing relevant patterns for instance " + n + " of " + m + " for " + " \"" + filename + "\"");
            // TODO
//            patterns.addAll(getRelevantNgramPatterns(curInstance, data, threshold));
            System.out.println("Added all ngram-patterns for instance " + n + " of " + m + " to pattern set");
        }
        return patterns;
    }

    private static Instances getInstances(String arffFilename) throws FileNotFoundException, IOException {
        Reader reader = new InputStreamReader(new FileInputStream(arffFilename), "UTF-8");
        Instances data = new Instances(reader);
        reader.close();
        // setting class attribute
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

//    /**
//     * Construct extraction patterns, assess their validity and return relevant
//     * patterns
//     *
//     * @param instance	...
//     * @param data	training data
//     * @return	...
//     */
//    @SuppressWarnings("unused")
//    private static List<String[]> getRelevantNgramPatterns(Instance instance, Instances data, double threshold) {
//        String attVal0 = instance.stringValue(0); //l5
//        String attVal1 = instance.stringValue(1); //l4
//        String attVal2 = instance.stringValue(2); //l3
//        String attVal3 = instance.stringValue(3); //l2
//        String attVal4 = instance.stringValue(4); //l1
//        String attVal5 = instance.stringValue(5); //r1
//        String attVal6 = instance.stringValue(6); //r2
//        String attVal7 = instance.stringValue(7); //r3
//        String attVal8 = instance.stringValue(8); //r4
//        String attVal9 = instance.stringValue(9); //r5
//
//        //TODO: CONSTRUCT LUCENE QUERIES ONLY WHEN NEEDED (BELOW)
//        String attVal0_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal0);
//        String attVal1_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal1);
//        String attVal2_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal2);
//        String attVal3_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal3);
//        String attVal4_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal4);
//        String attVal5_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal5);
//        String attVal6_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal6);
//        String attVal7_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal7);
//        String attVal8_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal8);
//        String attVal9_lucene = RegexUtils.normalizeAndEscapeRegex_lucene(attVal9);
//
//        String attVal0_quoted = Pattern.quote(attVal0);
//        String attVal1_quoted = Pattern.quote(attVal1);
//        String attVal2_quoted = Pattern.quote(attVal2);
//        String attVal3_quoted = Pattern.quote(attVal3);
//        String attVal4_quoted = Pattern.quote(attVal4);
//        String attVal5_quoted = Pattern.quote(attVal5);
//        String attVal6_quoted = Pattern.quote(attVal6);
//        String attVal7_quoted = Pattern.quote(attVal7);
//        String attVal8_quoted = Pattern.quote(attVal8);
//        String attVal9_quoted = Pattern.quote(attVal9);
//
//        String attVal4_regex = RegexUtils.normalizeAndEscapeRegex(attVal4);
//        String attVal5_regex = RegexUtils.normalizeAndEscapeRegex(attVal5);
//
//        //...
//        if (attVal4.matches(".*\\P{Punct}")) {
//            attVal4_quoted += "\\s";
//            attVal4_regex += "\\s";
//        }
//        if (attVal5.matches("\\P{Punct}.*")) {
//            attVal5_quoted = "\\s" + attVal5_quoted;
//            attVal5_regex = "\\s" + attVal5_regex;
//        }
//
//        // two words enclosing study name
//        String luceneQuery1 = "\"" + attVal4_lucene + " * " + attVal5_lucene + "\"";
//        String regex_ngram1_quoted = attVal4_quoted + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_quoted;
//        String regex_ngram1_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
//
//        // phrase consisting of 2 words behind study title + fixed word before
//        String luceneQueryA = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + "\"";
//        String regex_ngramA_quoted = Pattern.quote(attVal4) + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + Pattern.quote(attVal5) + "\\s" + Pattern.quote(attVal6);
//        String regex_ngramA_normalizedAndQuoted = RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
//
//		// phrase consisting of 2 words behind study title + (any) word found in data before!
//        // (any word cause this pattern is induced each time for each different instance having this phrase...)
//        String luceneQueryA_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + "\"";
//        String regex_ngramA_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted;
//		//String regex_ngramA_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
//
//        // phrase consisting of 3 words behind study title + fixed word before
//        String luceneQueryB = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + "\"";
//        String regex_ngramB_quoted = attVal4_quoted + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted;
//        String regex_ngramB_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
//
//        String luceneQueryB_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + "\"";
//        String regex_ngramB_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted;
//		//String regex_ngramB_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
//
//        // phrase consisting of 4 words behind study title + fixed word before
//        String luceneQueryC = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + "\"";
//        String regex_ngramC_quoted = attVal4_quoted + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted + "\\s" + attVal8_quoted;
//        String regex_ngramC_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal8) + "\\s" + RegexUtils.lastWordRegex;
//
//        String luceneQueryC_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + "\"";
//        String regex_ngramC_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted + "\\s" + attVal8_quoted;
//		//String regex_ngramC_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal8) + "\\s" + RegexUtils.lastWordRegex;
//
//        // phrase consisting of 5 words behind study title + fixed word before
//        String luceneQueryD = "\"" + attVal4_lucene + " * " + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + " " + attVal9_lucene + "\"";
//        String regex_ngramD_quoted = attVal4_quoted + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted + "\\s" + attVal8_quoted + "\\s" + attVal9_quoted;
//        String regex_ngramD_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal8) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal9);
//
//        // now the pattern can emerge at other positions, too, and is counted here as relevant...
//        String luceneQueryD_flex = "\"" + attVal5_lucene + " " + attVal6_lucene + " " + attVal7_lucene + " " + attVal8_lucene + " " + attVal9_lucene + "\"";
//        String regex_ngramD_flex_quoted = attVal5_quoted + "\\s" + attVal6_quoted + "\\s" + attVal7_quoted + "\\s" + attVal8_quoted + "\\s" + attVal9_quoted;
//		//String regex_ngramD_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal6) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal7) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal8) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal9);
//
//        // phrase consisting of 2 words before study title + fixed word behind
//        String luceneQuery2 = "\"" + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
//        String regex_ngram2_quoted = attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_quoted;
//        String regex_ngram2_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
//
//        String luceneQuery2_flex = "\"" + attVal3_lucene + " " + attVal4_lucene + "\"";
//        String regex_ngram2_flex_quoted = attVal3_quoted + "\\s" + attVal4_quoted;
//		//String regex_ngram2_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize-2) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
//
//        // phrase consisting of 3 words before study title + fixed word behind
//        String luceneQuery3 = "\"" + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
//        String regex_ngram3_quoted = attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_quoted;
//        String regex_ngram3_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal2) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
//
//        String luceneQuery3_flex = "\"" + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + "\"";
//        String regex_ngram3_flex_quoted = attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted;
//		//String regex_ngram3_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize-3) + "\\s" + leftWords_regex.get(windowSize-2) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
//
//        // phrase consisting of 4 words before study title + fixed word behind
//        String luceneQuery4 = "\"" + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
//        String regex_ngram4_quoted = attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_quoted;
//        String regex_ngram4_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal1) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal2) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
//
//        String luceneQuery4_flex = "\"" + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + "\"";
//        String regex_ngram4_flex_quoted = attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted;
//		//String regex_ngram4_flex_normalizedAndQuoted = RegexUtils.wordRegex_atomic + "\\s" + leftWords_regex.get(windowSize-4) + "\\s" + leftWords_regex.get(windowSize-3) + "\\s" + leftWords_regex.get(windowSize-2) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
//
//        // phrase consisting of 5 words before study title + fixed word behind
//        String luceneQuery5 = "\"" + attVal0_lucene + " " + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + " * " + attVal5_lucene + "\"";
//        String regex_ngram5_quoted = attVal0_quoted + "\\s" + attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_quoted;
//        String regex_ngram5_normalizedAndQuoted = RegexUtils.normalizeAndEscapeRegex(attVal0) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal1) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal2) + "\\s" + RegexUtils.normalizeAndEscapeRegex(attVal3) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
//
//        String luceneQuery5_flex = "\"" + attVal0_lucene + " " + attVal1_lucene + " " + attVal2_lucene + " " + attVal3_lucene + " " + attVal4_lucene + "\"";
//        String regex_ngram5_flex_quoted = attVal0_quoted + "\\s" + attVal1_quoted + "\\s" + attVal2_quoted + "\\s" + attVal3_quoted + "\\s" + attVal4_quoted;
//		//String regex_ngram5_flex_normalizedAndQuoted = leftWords_regex.get(windowsize-5) + "\\s" + leftWords_regex.get(windowSize-4) + "\\s" + leftWords_regex.get(windowSize-3) + "\\s" + leftWords_regex.get(windowSize-2) + "\\s" + attVal4_regex + "\\s?" + RegexUtils.studyRegex_ngram + "\\s?" + attVal5_regex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.wordRegex + "\\s" + RegexUtils.lastWordRegex;
//
//        List<String[]> ngramPats = new ArrayList<String[]>();
//		// constraint for ngrams: at least one component not be a stopword
//
//		// prevent induction of patterns less general than already known patterns:
//        // check whether pattern is known before continuing
//        // also improves performance
//        if (this.processedPatterns.contains(regex_ngram1_normalizedAndQuoted)) {
//            return ngramPats;
//        }
//        if (!(RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5)) & isRelevant(regex_ngram1_quoted, threshold))//0.2
//        {
//            // substitute normalized numbers etc. with regex
//            String[] newPat = {luceneQuery1, regex_ngram1_normalizedAndQuoted};
//            ngramPats.add(newPat);
//            System.out.println("found relevant type 1 pattern (most general): " + regex_ngram1_normalizedAndQuoted);
//        } else {
//            //TODO: do not return here, instead process Type phrase behind study title terms also"
//            if (this.processedPatterns.contains(regex_ngram2_normalizedAndQuoted)) {
//                return ngramPats;
//            }
//            if (!(RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5) | RegexUtils.isStopword(attVal3) & RegexUtils.isStopword(attVal5) | RegexUtils.isStopword(attVal3) & RegexUtils.isStopword(attVal4)) & isRelevant(regex_ngram2_quoted, threshold - 0.02))//0.18
//            {
//                System.out.println("found relevant type 2 pattern: " + regex_ngram2_normalizedAndQuoted);
//                String[] newPat = {luceneQuery2, regex_ngram2_normalizedAndQuoted};
//                ngramPats.add(newPat);
//            } else {
//                if (this.processedPatterns.contains(regex_ngram3_normalizedAndQuoted)) {
//                    return ngramPats;
//                }
//                if (!(RegexUtils.isStopword(attVal2) & RegexUtils.isStopword(attVal3) & RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5)) & isRelevant(regex_ngram3_quoted, threshold - 0.04))//0.16
//                {
//                    System.out.println("found relevant type 3 pattern: " + regex_ngram3_normalizedAndQuoted);
//                    //ngramPats.add(regex_ngram3_normalizedAndQuoted);
//                    String[] newPat = {luceneQuery3, regex_ngram3_normalizedAndQuoted};
//                    ngramPats.add(newPat);
//                } else {
//                    if (this.processedPatterns.contains(regex_ngram4_normalizedAndQuoted)) {
//                        return ngramPats;
//                    }
//                    if (!(RegexUtils.isStopword(attVal1) & RegexUtils.isStopword(attVal2) & RegexUtils.isStopword(attVal3) & RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5)) & isRelevant(regex_ngram4_quoted, threshold - 0.06))//0.14
//                    {
//                        System.out.println("found relevant type 4 pattern: " + regex_ngram4_normalizedAndQuoted);
//                        //ngramPats.add(regex_ngram4_normalizedAndQuoted);
//                        String[] newPat = {luceneQuery4, regex_ngram4_normalizedAndQuoted};
//                        ngramPats.add(newPat);
//                    } else {
//                        if (this.processedPatterns.contains(regex_ngram5_normalizedAndQuoted)) {
//                            return ngramPats;
//                        }
//                        if (!(RegexUtils.isStopword(attVal0) & RegexUtils.isStopword(attVal1) & RegexUtils.isStopword(attVal2) & RegexUtils.isStopword(attVal3) & RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5)) & isRelevant(regex_ngram5_quoted, threshold - 0.08))//0.12
//                        {
//                            System.out.println("found relevant type 5 pattern: " + regex_ngram5_normalizedAndQuoted);
//                            //ngramPats.add(regex_ngram5_normalizedAndQuoted);
//                            String[] newPat = {luceneQuery5, regex_ngram5_normalizedAndQuoted};
//                            ngramPats.add(newPat);
//                        }
//                    }
//                }
//            }
//            if (this.processedPatterns.contains(regex_ngramA_normalizedAndQuoted)) {
//                return ngramPats;
//            }
//            if (!(RegexUtils.isStopword(attVal5) & RegexUtils.isStopword(attVal6) | RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal6) | RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5)) & isRelevant(regex_ngramA_quoted, threshold - 0 - 02))//0.18
//            {
//                System.out.println("found relevant type A pattern: " + regex_ngramA_normalizedAndQuoted);
//                //ngramPats.add(regex_ngramA_normalizedAndQuoted);
//                String[] newPat = {luceneQueryA, regex_ngramA_normalizedAndQuoted};
//                ngramPats.add(newPat);
//            } else {
//                if (this.processedPatterns.contains(regex_ngramB_normalizedAndQuoted)) {
//                    return ngramPats;
//                }
//                if (!(RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5) & RegexUtils.isStopword(attVal6) & RegexUtils.isStopword(attVal7)) & isRelevant(regex_ngramB_quoted, threshold - 0.04))//0.16
//                {
//                    System.out.println("found relevant type B pattern: " + regex_ngramB_normalizedAndQuoted);
//                    //ngramPats.add(regex_ngramB_normalizedAndQuoted);
//                    String[] newPat = {luceneQueryB, regex_ngramB_normalizedAndQuoted};
//                    ngramPats.add(newPat);
//                } else {
//                    if (this.processedPatterns.contains(regex_ngramC_normalizedAndQuoted)) {
//                        return ngramPats;
//                    }
//                    if (!(RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5) & RegexUtils.isStopword(attVal6) & RegexUtils.isStopword(attVal7) & RegexUtils.isStopword(attVal8)) & isRelevant(regex_ngramC_quoted, threshold - 0.06))//0.14
//                    {
//                        System.out.println("found relevant type C pattern: " + regex_ngramC_normalizedAndQuoted);
//                        //ngramPats.add(regex_ngramC_normalizedAndQuoted);
//                        String[] newPat = {luceneQueryC, regex_ngramC_normalizedAndQuoted};
//                        ngramPats.add(newPat);
//                    } else {
//                        if (this.processedPatterns.contains(regex_ngramD_normalizedAndQuoted)) {
//                            return ngramPats;
//                        }
//                        if (!(RegexUtils.isStopword(attVal4) & RegexUtils.isStopword(attVal5) & RegexUtils.isStopword(attVal6) & RegexUtils.isStopword(attVal7) & RegexUtils.isStopword(attVal8) & RegexUtils.isStopword(attVal9)) & isRelevant(regex_ngramD_quoted, threshold - 0.08))//0.12
//                        {
//                            System.out.println("found relevant type D pattern: " + regex_ngramD_normalizedAndQuoted);
//                            //ngramPats.add(regex_ngramD_normalizedAndQuoted);
//                            String[] newPat = {luceneQueryD, regex_ngramD_normalizedAndQuoted};
//                            ngramPats.add(newPat);
//                        }
//                    }
//                }
//            }
//        }
//        return ngramPats;
//    }


//    //TODO: use negative training examples to measure relevance?
//    /**
//     * Determines whether a regular expression is suitable for extracting
//     * dataset references using a frequency-based measure
//     *
//     * @param ngramRegex	regex to be tested
//     * @param threshold	threshold for frequency-based relevance measure
//     * @return				<emph>true</emph>, if regex is found to be relevant,
//     * <emph>false</emph> otherwise
//     */
//    private boolean isRelevant(String ngramRegex, double threshold) {
//        System.out.println("Checking if pattern is relevant: " + ngramRegex);
//        // compute score for similar to tf-idf...
//        List<String> contexts_pos = this.contextsAsStrings.get(0);
//        List<String> contexts_neg = this.contextsAsStrings.get(1);
//        // count occurrences of ngram in positive vs negative contexts...
//        int count_pos = 0;
//        int count_neg = 0;
//        for (String context : contexts_pos) {
//            count_pos += patternFound(ngramRegex, context);
//        }
//        // contexts neg always empty right now
//        for (String context : contexts_neg) {
//            count_neg += patternFound(ngramRegex, context);
//        }
//
//        //TODO: rename - this is not really tf-idf ;)
//        double idf = 0;
//        // compute relevance...
//        if (count_neg + count_pos > 0) {
//            idf = log2((double) (contexts_pos.size() + contexts_neg.size()) / (count_neg + count_pos));
//        }
//
//        double tf_idf = ((double) count_pos / contexts_pos.size()) * idf;
//
//        if (tf_idf > threshold & count_pos > 1) {
//            return true;
//        } else {
//            return false;
//        }
//    }



}
