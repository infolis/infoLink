/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.infolink.patternLearner;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.InfolisFileUtils;
import io.github.infolis.util.RegexUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author domi
 */
public class OutputWriter {

    //TODO: remove duplicate code... use separate method instead
    /**
     * TODO hacky
     * ...
     *
     * @param studyNcontextList	...
     * @param filenameContexts	...
     * @param filenamePatterns	...
     * @param filenameStudies	...
     */
    public static void outputContextsAndPatterns_distinct(DataStoreClient client, List<StudyContext> studyNcontextList, String filenameContexts, String filenamePatterns, String filenameStudies, boolean train) throws IOException {
        File contextFile = new File(filenameContexts);
        File patternFile = new File(filenamePatterns);
        File studyFile = new File(filenameStudies);

        //TODO: inserted "true" ...
        OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(contextFile, true), "UTF-8");
        BufferedWriter out = new BufferedWriter(fstream);
        if (train) {
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.getProperty("line.separator") + "<contexts>" + System.getProperty("line.separator"));
        }

        OutputStreamWriter fstream2 = new OutputStreamWriter(new FileOutputStream(patternFile, true), "UTF-8");
        BufferedWriter out2 = new BufferedWriter(fstream2);

        OutputStreamWriter fstream3 = new OutputStreamWriter(new FileOutputStream(studyFile, true), "UTF-8");
        BufferedWriter out3 = new BufferedWriter(fstream3);

        Set<InfolisPattern> patSet = new HashSet<>();
        Set<String> studySet = new HashSet<>();
        Set<StudyContext> distinctContexts = new HashSet<>();

        for (StudyContext studyNcontext : studyNcontextList) {
        	InfolisPattern pat = client.get(InfolisPattern.class, studyNcontext.getPattern());
            patSet.add(pat);
            studySet.add(studyNcontext.getTerm());
            // do not print duplicate contexts
            // extend check for duplicate contexts: context as part of study name...
            if (!distinctContexts.contains(studyNcontext)) {
                out.write(studyNcontext.toXML());
                distinctContexts.add(studyNcontext);
            }
        }
        if (train) {
            out.write(System.getProperty("line.separator") + "</contexts>" + System.getProperty("line.separator"));
        }
        out.close();

        for (InfolisPattern pat : patSet) {
            out2.write(pat.getPatternRegex() + System.getProperty("line.separator"));
        }
        out2.close();

        for (String stud : studySet) {
            out3.write(stud + System.getProperty("line.separator"));
        }
        out3.close();
    }

    //TODO: USE METHODS FROM TOOLS PACKAGE
    // writing the patterns to file here is used for validation and evaluation purposes 
    // storing patterns not as set but as list would allow to rank them according to number 
    // of contexts extracted with it
    /**
     * ...
     *
     * @param studyNcontextList	...
     * @param filenameContexts	...
     * @param filenamePatterns	...
     * @param filenameStudies	...
     */
    public static void outputContextsAndPatterns(List<StudyContext> studyNcontextList, String filenameContexts, String filenamePatterns, String filenameStudies) throws IOException {
        File contextFile = new File(filenameContexts);
        File patternFile = new File(filenamePatterns);
        File studyFile = new File(filenameStudies);
        // write all these files for validation, do not actually read from them in the program
        //TODO: do not put everything in the try statement, writing everything to file is not mandatory
        OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(contextFile), "UTF-8");
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.getProperty("line.separator") + "<contexts>" + System.getProperty("line.separator"));

        OutputStreamWriter fstream2 = new OutputStreamWriter(new FileOutputStream(patternFile), "UTF-8");
        BufferedWriter out2 = new BufferedWriter(fstream2);

        OutputStreamWriter fstream3 = new OutputStreamWriter(new FileOutputStream(studyFile), "UTF-8");
        BufferedWriter out3 = new BufferedWriter(fstream3);

        Set<String> patSet = new HashSet<>();
        Set<String> studySet = new HashSet<>();

        for (StudyContext studyNcontext : studyNcontextList) {
            out.write(studyNcontext.toString());
        }

        out.write(System.getProperty("line.separator") + "</contexts>" + System.getProperty("line.separator"));
        out.close();
        //TODO: important??? why is something set in the print method?
        //this.foundSeeds_iteration.addAll(studySet);

        for (String pat : patSet) {
            out2.write(pat + System.getProperty("line.separator"));
        }
        out2.close();

        for (String stud : studySet) {
            out3.write(stud + System.getProperty("line.separator"));
        }
        out3.close();
    }

    /**
     * Writes instances in TrainingSet at <emph>filename</emph> to Weka's arff
     * file format. All instances in the training set are assumed to be positive
     * training examples (thus receiving the class value <emph>True</emph>).
     * Name of the output file equals the name of the training set file having
     * ".arff" as extension instead of ".xml".
     *
     * @param filename	name of the TrainingSet XML file
     */
    public static void outputArffFile(String filename) throws IOException {
        TrainingSet newTrainingSet = new TrainingSet(new File(filename));
        //TODO: assumes patterns to be correct
        try {
            newTrainingSet.createTrainingSet("True", filename.replace(".xml", ".arff"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw (new IOException());
        }
    }

    /**
     * Writes dataset names, contexts and patterns to the files specified in
     * <emph>filenames</emph>
     * and creates an arff file for using the contexts as training set.
     *
     * @param studyNcontextList list of extracted instances and their contexts
     * @param filenames	array specifying the names for the distinct output files
     * ([0]: dataset names, [1]: contexts, [2]: patterns)
     */
    public static void output(List<StudyContext> studyNcontextList, String[] filenames) throws IOException {
        String filenameStudies = filenames[0];
        String filenameContexts = filenames[1];
        String filenamePatterns = filenames[2];
        try {
            outputContextsAndPatterns(studyNcontextList, filenameContexts, filenamePatterns, filenameStudies);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw (new IOException());
        }
        try {
            outputArffFile(filenameContexts);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw (new IOException());
        }
    }

    /**
     * Writes dataset names, contexts and patterns to the files specified in
     * <emph>filenames</emph>. Filters out any duplicates in the process. If in
     * training mode, additionally creates an arff file for using the contexts
     * as training set.
     *
     * @param studyNcontextList	list of extracted instances and their contexts
     * @param filenames	array specifying the names for the distinct output files
     * ([0]: dataset names, [1]: contexts, [2]: patterns)
     * @param train	flag specifying whether InfoLink is in training mode (i.e.
     * learning new patterns instead of applying known ones)
     */
    public static void output_distinct(DataStoreClient client, List<StudyContext> studyNcontextList, String[] filenames, boolean train) throws IOException {
        String filenameStudies = filenames[0];
        String filenameContexts = filenames[1];
        String filenamePatterns = filenames[2];
        try {
            OutputWriter.outputContextsAndPatterns_distinct(client, studyNcontextList, filenameContexts, filenamePatterns, filenameStudies, train);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw (new IOException());
        }
        if (train) {
            try {
                outputArffFile(filenameContexts);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                throw (new IOException());
            }
        }
    }

    /**
     * Writes all extracted references of reliable patterns to xml context file
     * at this output path
     */
    public static void outputReliableReferences(Map<InfolisPattern, List<StudyContext>> reliablePatternsAndContexts, String output) throws IOException {
        List<StudyContext> reliableContexts = new ArrayList<>();
        for (InfolisPattern pattern : reliablePatternsAndContexts.keySet()) {
            List<StudyContext> contexts = reliablePatternsAndContexts.get(pattern);
            reliableContexts.addAll(contexts);
            //see getString_reliablePatternOutput( Map<String, List<String[]>> patternsAndContexts, int iteration )
        }
        String[] filenames = new String[3];
        filenames[0] = output + File.separator + "datasets.csv";
        filenames[1] = output + File.separator + "contexts.xml";
        filenames[2] = output + File.separator + "patterns.csv";
        try {
            output(reliableContexts, filenames);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw (new IOException());
        }
    }

    public void outputParameterInfo(Collection<String> initialSeeds, String path_index, String path_train, String path_corpus, String path_output, String path_contexts, String path_arffs, boolean constraint_NP, boolean constraint_upperCase, String method, double threshold, int maxIterations) {
        String delimiter = RegexUtils.delimiter_csv;
        String timestamp = getDateTime();
        File logFile = new File(path_output + File.separator + "parameterInfo.csv");
        String parameters = "initial_seeds" + delimiter + "index_path" + delimiter + "train_path" + delimiter
                + "corpus_path" + delimiter + "output_path" + delimiter + "context_path" + delimiter
                + "arff_path" + delimiter + "constraint_NP" + delimiter + "constraint_upperCase" + delimiter
                + "method" + delimiter + "threshold" + delimiter + "maxIterations" + delimiter + "start_time"
                + System.getProperty("line.separator");
        for (String seed : initialSeeds) {
            parameters += seed + RegexUtils.delimiter_internal;
        }
        //remove delimiter at the end of the seed list
        parameters = parameters.substring(0, parameters.length() - RegexUtils.delimiter_internal.length());
        parameters = parameters.trim() + delimiter + path_index + delimiter + path_train + delimiter + path_corpus
                + delimiter + path_output + delimiter + path_contexts + delimiter + path_arffs + delimiter
                + constraint_NP + delimiter + constraint_upperCase + delimiter + method + delimiter + threshold
                + delimiter + maxIterations + delimiter + timestamp;
        try {
            InfolisFileUtils.writeToFile(logFile, "utf-8", parameters, false);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.out.println(parameters);
        }
    }

    public String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

}
