/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.infolink.patternLearner;

import io.github.infolis.util.SerializationUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author domi
 */
public class OutputWriter {
    
    //TODO: remove duplicate code... use separate method instead
    /**
     * ...
     *
     * @param studyNcontextList	...
     * @param filenameContexts	...
     * @param filenamePatterns	...
     * @param filenameStudies	...
     */
    public static void outputContextsAndPatterns_distinct(List<String[]> studyNcontextList, String filenameContexts, String filenamePatterns, String filenameStudies, boolean train) throws IOException {
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

        Set<String> patSet = new HashSet<String>();
        Set<String> studySet = new HashSet<String>();
        Set<String> distinctContexts = new HashSet<String>();

        for (String[] studyNcontext : studyNcontextList) {
            String studyName = studyNcontext[0];
            String context = studyNcontext[1];
            String corpusFilename = studyNcontext[2];
            String usedPat = studyNcontext[3];

            context = SerializationUtils.escapeXML(context);

            patSet.add(usedPat);
            studySet.add(studyName);

			// split context into words
            // join first 5 words = left context
            // last 5 words = rightcontext
            // middle word = studyname
            String[] contextList = context.replace(System.getProperty("line.separator"), " ").replace("\n", " ").replace("\r", " ").trim().split("\\s+");
			// do not print duplicate contexts
            // extend check for duplicate contexts: context as part of study name...
            if (!distinctContexts.contains(context.replace("\\s+", ""))) {
                String leftContext;
                String rightContext;
                if (contextList.length != 10 + studyName.trim().split("\\s+").length) {
					// split by study name in this case...
                    // TODO: (s.o.)
                    contextList = context.split(Pattern.quote(SerializationUtils.escapeXML(studyName)));
                    if (contextList.length != 2) {
                        System.err.println("Warning: context has not 10 words and cannot by split around the study name. Check output method.");
                        for (String contextErr : contextList) {
                            System.err.println("###" + contextErr + "###");
                        }
                        System.err.println(studyName);
                        System.err.println(contextList.length);
                        continue;
                    } else {
                        leftContext = contextList[0];
                        rightContext = contextList[1];
                    }
                } else {
                    leftContext = "";
                    rightContext = "";
                    for (int i = 0; i < 5; i++) {
                        leftContext += contextList[i] + " ";
                    }
                    for (int i = contextList.length - 1; i >= contextList.length - 5; i--) {
                        rightContext = contextList[i] + " " + rightContext;
                    }
                }
                out.write("\t<context term=\"" + SerializationUtils.escapeXML(studyName) + "\" document=\"" + SerializationUtils.escapeXML(corpusFilename) + "\" usedPattern=\"" + SerializationUtils.escapeXML(usedPat) + "\">" + System.getProperty("line.separator") + "\t\t<leftContext>" + leftContext.trim() + "</leftContext>" + System.getProperty("line.separator") + "\t\t<rightContext>" + rightContext.trim() + "</rightContext>" + System.getProperty("line.separator") + "\t</context>" + System.getProperty("line.separator"));
                distinctContexts.add(context.replace("\\s+", ""));
            }
        }
        if (train) {
            out.write(System.getProperty("line.separator") + "</contexts>" + System.getProperty("line.separator"));
        }
        out.close();

        for (String pat : patSet) {
            out2.write(pat + System.getProperty("line.separator"));
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
    public static void outputContextsAndPatterns(List<String[]> studyNcontextList, String filenameContexts, String filenamePatterns, String filenameStudies) throws IOException {
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

        Set<String> patSet = new HashSet<String>();
        Set<String> studySet = new HashSet<String>();

        for (String[] studyNcontext : studyNcontextList) {
            String studyName = studyNcontext[0];
            String context = studyNcontext[1];
            String corpusFilename = studyNcontext[2];
            String usedPat = studyNcontext[3];

            context = SerializationUtils.escapeXML(context);

            patSet.add(usedPat);
            studySet.add(studyName);
			// split context into words
            // join first 5 words = left context
            // last 5 words = rightcontext
            // middle word = studyname
            // do not split at study name - in rare cases, it might occur more than once!
            String[] contextList = context.replace(System.getProperty("line.separator"), " ").replace("\n", " ").replace("\r", " ").trim().split("\\s+");
            String leftContext;
            String rightContext;
            // contextList may contain less entries if the word before or after the study name is directly attached to the study name (e.g. ALLBUS-Daten)
            if (contextList.length != 10 + studyName.trim().split("\\s+").length) {
				// split by study name in this case...
                //TODO: simple split at first occurrence only :) (split(term,limit))
                contextList = context.split(Pattern.quote(SerializationUtils.escapeXML(studyName)));
                if (contextList.length != 2) {
                    System.err.println("Warning: context does not have 10 words and cannot be split around the study name. Ignoring.");
                    for (String contextErr : contextList) {
                        System.err.println("###" + contextErr + "###");
                    }
                    System.err.println(studyName);
                    System.err.println(contextList.length);
                    continue;
                } else {
                    leftContext = contextList[0];
                    rightContext = contextList[1];
                }
            } else {
                leftContext = "";
                rightContext = "";
                for (int i = 0; i < 5; i++) {
                    leftContext += contextList[i] + " ";
                }
                for (int i = contextList.length - 1; i >= contextList.length - 5; i--) {
                    rightContext = contextList[i] + " " + rightContext;
                }
            }
            out.write("\t<context term=\"" + SerializationUtils.escapeXML(studyName) + "\" document=\"" + SerializationUtils.escapeXML(corpusFilename) + "\" usedPattern=\"" + SerializationUtils.escapeXML(usedPat) + "\">" + System.getProperty("line.separator") + "\t\t<leftContext>" + leftContext.trim() + "</leftContext>" + System.getProperty("line.separator") + "\t\t<rightContext>" + rightContext.trim() + "</rightContext>" + System.getProperty("line.separator") + "\t</context>" + System.getProperty("line.separator"));
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
    public static void output(List<String[]> studyNcontextList, String[] filenames) throws IOException {
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
    public static void output_distinct(List<String[]> studyNcontextList, String[] filenames, boolean train) throws IOException {
        String filenameStudies = filenames[0];
        String filenameContexts = filenames[1];
        String filenamePatterns = filenames[2];
        try {
            OutputWriter.outputContextsAndPatterns_distinct(studyNcontextList, filenameContexts, filenamePatterns, filenameStudies, train);
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
    public static void outputReliableReferences(Map<String, List<String[]>> reliablePatternsAndContexts, String output) throws IOException {
        List<String[]> reliableContexts = new ArrayList<String[]>();
        for (String pattern : reliablePatternsAndContexts.keySet()) {
            List<String[]> contexts = reliablePatternsAndContexts.get(pattern);
            reliableContexts.addAll(contexts);
            //see getString_reliablePatternOutput( Map<String, List<String[]>> patternsAndContexts, int iteration )
        }
        String[] filenames = new String[3];
        filenames[0] = output + File.separator + "datasets.csv";
        filenames[1] = output + File.separator + "contexts.xml";
        filenames[2] = output + File.separator + "patterns.csv";
        /*String[] studyNcontext = new String[4];
         studyNcontext[0] = studyName;
         studyNcontext[1] = context;
         studyNcontext[2] = filenameIn;
         studyNcontext[3] = curPat;
         res.add( studyNcontext );*/
        try {
            output(reliableContexts, filenames);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw (new IOException());
        }
    }
    
}
