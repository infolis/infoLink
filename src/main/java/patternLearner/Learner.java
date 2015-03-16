package patternLearner;

import patternLearner.bootstrapping.InternalTrainingSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Collection;
import searching.SearchTermPosition;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import java.text.DateFormat;
import java.util.Map;
import java.util.Set;
import patternLearner.bootstrapping.Bootstrapping;
import patternLearner.bootstrapping.frequency.Baseline2;
import patternLearner.bootstrapping.reliability.Reliability;


/**
 * Class for finding references to scientific datasets in publications using a
 * minimum supervised iterative pattern induction approach. For a description of
 * the basic algorithm see
 * <emph>Boland, Katarina; Ritze, Dominique; Eckert, Kai; Mathiak, Brigitte
 * (2012): Identifying references to datasets in publications. In: Zaphiris,
 * Panayiotis; Buchanan, George; Rasmussen, Edie; Loizides, Fernando (Hrsg.):
 * Proceedings of the Second International Conference on Theory and Practice of
 * Digital Libraries (TDPL 2012), Paphos, Cyprus, September 23-27, 2012. Lecture
 * notes in computer science, 7489, Berlin: Springer, S. 150-161. </emph>. Note
 * that some features are not described in this publication.
 *
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
public class Learner {

    private List<String>[] contextsAsStrings;
    private Set<String> processedSeeds; //all processed seeds
    private Set<String> foundSeeds_iteration; //seeds found at current iteration step
    private Set<String> processedPatterns; //ngram patterns
    //TODO: put the reliable stuff in the according class?
    private Map<String, List<String[]>> reliablePatternsAndContexts;
    private Set<String> reliableInstances;
    // basePath is used for normalizing path names when searching for known dataset names
    // should point to the path of the input text corpus
    private Path basePath;
    private boolean constraint_NP;
    private boolean constraint_upperCase;
    private String language;
    private String corpusPath;
    private String indexPath;
    private String contextPath;
    private String arffPath;
    private String outputPath;
    //Reliability reliability;
    private PatternApplier app;
    private PatternInducer indu;
    //TODO: initialize
    private Reliability reliability;

    /**
     * Class constructor specifying the constraints for patterns.
     *
     * @param constraint_NP	if set, references are only accepted if assumed
     * dataset name occurs in nominal phrase
     * @param constraint_upperCase	if set, references are only accepted if
     * dataset name has at least one upper case character
     *
     */
    public Learner(boolean constraint_NP, boolean constraint_upperCase, boolean german, String corpusPath, String indexPath, String contextPath, String arffPath, String outputPath) {
        this.processedSeeds = new HashSet();
        this.foundSeeds_iteration = new HashSet();
        //his.foundPatterns_iteration = new HashSet();
        //this.reliablePatterns_iteration = new HashSet();
        this.processedPatterns = new HashSet();
        this.constraint_NP = constraint_NP;
        this.constraint_upperCase = constraint_upperCase;
        if (german) {
            this.language = "de";
        } else {
            this.language = "en";
        }
        this.corpusPath = corpusPath;
        this.basePath = Paths.get(corpusPath);
        this.indexPath = indexPath;
        this.contextPath = contextPath;
        this.arffPath = arffPath;
        this.outputPath = outputPath;
        this.reliablePatternsAndContexts =  new HashMap<>();
        this.reliableInstances = new HashSet();
 //       this.reliability = new Reliability();
        app = new PatternApplier(this);
        indu = new PatternInducer(this);
    }


    /**
     * Extracts the name of all seeds from a file listing one seed per line.
     *
     * @param filename	name of the file listing all seeds
     * @return	a set of all seeds contained in the file
     */
    public Set<String> getSeeds(String filename) {
        Set<String> seedList = new HashSet();
        try {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(filename)), "UTF8");
            BufferedReader reader = new BufferedReader(isr);
            String text;
            while ((text = reader.readLine()) != null) {
                seedList.add(text.trim());
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return seedList;
    }

    /**
     * Calls <emph>getContextsForSeed</emph> method for all seeds contained in
     * <emph>seedList</emph>.
     *
     * @param indexDir	name of the directory containing the lucene index to be
     * searched
     * @param seedList	a set of seeds whose contexts to retrieve
     * context file
     */
    public void getContextsForAllSeeds(Collection<String> seedList) throws IOException {
        for (String seed : seedList) {
            getContextsForSeed(seed, outputPath + File.separator + Util.escapeSeed(seed) + ".xml");
        }
    }

    /**
     * Searches for occurrences of the string <emph>seed</emph> in the lucene
     * index at path <emph>indexDir</emph>
     * and saves the contexts of all occurrences to <emph>filename</emph>.
     *
     * @param indexDir	name of the directory containing the lucene index to be
     * searched
     * @param seed	seed for which the contexts to retrieve
     * @param filename	name of the output file
     */
    public void getContextsForSeed(String seed, String filename) throws IOException {
        SearchTermPosition search = new SearchTermPosition(this.indexPath, filename, seed, "\"" + SearchTermPosition.normalizeQuery(seed) + "\"");
        try {
            Util.prepareOutputFile(filename);
            search.complexSearch(new File(filename), true);
            Util.completeOutputFile(filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates extraction patterns using an iterative bootstrapping approach.
     *
     * <ol>
     * <li>searches for seeds in the specified corpus and extracts the
     * surrounding words as contexts</li>
     * <li>analyzes contexts and generates extraction patterns</li>
     * <li>applies extraction patterns on corpus to extract new seeds</li>
     * <li>continues with 1) until maximum number of iterations is reached</li>
     * <li>outputs found seeds, contexts and extraction patterns</li>
     * </ol>
     *
     * Method for assessing pattern validity is frequency-based.
     *
     * @param indexDirectory	name of the directory containing the lucene index
     * to be searched
     * @param seed	the term to be searched as starting point in the current
     * iteration
     * @param outputDirectory	path of the output directory
     * @param contextDirName	path of the directory containing all context files
     * @param arffDirName	path of the directory containing all arff files
     * @param corpusDirectory	name of the directory containing the text corpus
     */
    public void bootstrap(String seed, Bootstrapping method, int maxIter) throws IOException {
        File contextDir = new File(contextPath);
        String[] contextFiles = contextDir.list();
        List<String> contextFileList = new ArrayList<>();
        if(contextFiles.length>0) {
            contextFileList = Arrays.asList(contextFiles); 
        }
        
        File arffDir = new File(arffPath);
        String[] arffFiles = arffDir.list();
        List<String> arffFileList = Arrays.asList(arffFiles);
        String seedEscaped = Util.escapeSeed(seed);
        // 1. use lucene index to search for term in corpus
        // use caches from recent searches on the same corpus to increase performance
        String filenameContext = seedEscaped + ".xml";
        String filenameArff = seedEscaped + ".arff";
        if (!arffFileList.contains(filenameArff)) {
            if (contextFileList.isEmpty() && !contextFileList.contains(filenameContext)) {
                // if no cache files exist, search in corpus and generate contexts
                getContextsForSeed(seed, contextPath + File.separator + filenameContext);
            }
            InternalTrainingSet trainingSet = new InternalTrainingSet(new File(contextDir + File.separator + filenameContext));
            trainingSet.createTrainingSet("True", arffPath + File.separator + filenameArff);
        }
        //3. generate patterns
        //4. search for patterns in corpus: 
        try {
            //TODO: separate steps, replace old readArff method
            PatternInducer induce = new PatternInducer(this);
            induce.readArff(arffDir + File.separator + filenameArff);
            this.getProcessedSeeds().add(seed);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        // use this to compute "worst-case recall" = probability, that mentions of an unseen study will be found
        // exclude the seed study and measure how often it will be found without being seen before
        //this.processedPatterns = new HashSet<String>();
        //this.foundPatterns_iteration = new HashSet<String>();

        Set<String> newSeeds = new HashSet(this.getFoundSeeds_iteration());
        File nextIterPath = Paths.get(outputPath + File.separator + "iteration2").normalize().toFile();
        if (!nextIterPath.exists()) {
            nextIterPath.mkdir();
            System.out.println("Created directory " + nextIterPath);
        } 
        //TODO set threshold if reliability bsaed
        method.bootstrap(newSeeds, 0,maxIter);
    }

    /**
     * Generates extraction patterns using an iterative bootstrapping approach.
     *
     * <ol>
     * <li>searches for seeds in the specified corpus and extracts the
     * surrounding words as contexts</li>
     * <li>analyzes contexts and generates extraction patterns</li>
     * <li>applies extraction patterns on corpus to extract new seeds</li>
     * <li>continues with 1) until maximum number of iterations is reached</li>
     * <li>outputs found seeds, contexts and extraction patterns</li>
     * </ol>
     *
     * Method for assessing pattern validity is reliability-based.
     *
     * @param seeds	reliable terms to be searched as starting point
     * @param threshold	reliability threshold
	 *
     */
//    private void bootstrap(Collection<String> seeds, double threshold) throws IOException {
//        //TODO: located in another package
//        bootstrap_reliabilityBased(seeds, threshold, -1);
//    }

    
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
    private void outputContextsAndPatterns(List<String[]> studyNcontextList, String filenameContexts, String filenamePatterns, String filenameStudies) {
        File contextFile = new File(filenameContexts);
        File patternFile = new File(filenamePatterns);
        File studyFile = new File(filenameStudies);
        // write all these files for validation, do not actually read from them in the program
        //TODO: do not put everything in the try statement, writing everything to file is not mandatory
        try {
            OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(contextFile), "UTF-8");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.getProperty("line.separator") + "<contexts>" + System.getProperty("line.separator"));

            OutputStreamWriter fstream2 = new OutputStreamWriter(new FileOutputStream(patternFile), "UTF-8");
            BufferedWriter out2 = new BufferedWriter(fstream2);

            OutputStreamWriter fstream3 = new OutputStreamWriter(new FileOutputStream(studyFile), "UTF-8");
            BufferedWriter out3 = new BufferedWriter(fstream3);

            HashSet<String> patSet = new HashSet<String>();
            HashSet<String> studySet = new HashSet<String>();

            for (String[] studyNcontext : studyNcontextList) {
                String studyName = studyNcontext[0];
                String context = studyNcontext[1];
                String corpusFilename = studyNcontext[2];
                String usedPat = studyNcontext[3];

                context = Util.escapeXML(context);

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
                    contextList = context.split(Pattern.quote(Util.escapeXML(studyName)));
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
                out.write("\t<context term=\"" + Util.escapeXML(studyName) + "\" document=\"" + Util.escapeXML(corpusFilename) + "\" usedPattern=\"" + Util.escapeXML(usedPat) + "\">" + System.getProperty("line.separator") + "\t\t<leftContext>" + leftContext.trim() + "</leftContext>" + System.getProperty("line.separator") + "\t\t<rightContext>" + rightContext.trim() + "</rightContext>" + System.getProperty("line.separator") + "\t</context>" + System.getProperty("line.separator"));
            }

            out.write(System.getProperty("line.separator") + "</contexts>" + System.getProperty("line.separator"));
            out.close();
            this.getFoundSeeds_iteration().addAll(studySet);

            for (String pat : patSet) {
                out2.write(pat + System.getProperty("line.separator"));
            }
            out2.close();

            for (String stud : studySet) {
                out3.write(stud + System.getProperty("line.separator"));
            }
            out3.close();
        } catch (FileNotFoundException e) {
            System.err.println(e);
            System.exit(1);
        } catch (IOException ioe) {
            System.err.println(ioe);
            System.exit(1);
        }
    }

    //TODO: remove duplicate code... use separate method instead
    /**
     * ...
     *
     * @param studyNcontextList	...
     * @param filenameContexts	...
     * @param filenamePatterns	...
     * @param filenameStudies	...
     */
    private void outputContextsAndPatterns_distinct(List<String[]> studyNcontextList, String filenameContexts, String filenamePatterns, String filenameStudies, boolean train) {
        File contextFile = new File(filenameContexts);
        File patternFile = new File(filenamePatterns);
        File studyFile = new File(filenameStudies);

        try {
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

            Set<String> patSet = new HashSet();
            Set<String> studySet = new HashSet();
            Set<String> distinctContexts = new HashSet();

            for (String[] studyNcontext : studyNcontextList) {
                String studyName = studyNcontext[0];
                String context = studyNcontext[1];
                String corpusFilename = studyNcontext[2];
                String usedPat = studyNcontext[3];

                context = Util.escapeXML(context);

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
                        contextList = context.split(Pattern.quote(Util.escapeXML(studyName)));
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
                    out.write("\t<context term=\"" + Util.escapeXML(studyName) + "\" document=\"" + Util.escapeXML(corpusFilename) + "\" usedPattern=\"" + Util.escapeXML(usedPat) + "\">" + System.getProperty("line.separator") + "\t\t<leftContext>" + leftContext.trim() + "</leftContext>" + System.getProperty("line.separator") + "\t\t<rightContext>" + rightContext.trim() + "</rightContext>" + System.getProperty("line.separator") + "\t</context>" + System.getProperty("line.separator"));
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
        } catch (FileNotFoundException e) {
            System.err.println(e);
        } catch (IOException ioe) {
            System.err.println(ioe);
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
    public void output(List<String[]> studyNcontextList, String[] filenames) {
        String filenameStudies = filenames[0];
        String filenameContexts = filenames[1];
        String filenamePatterns = filenames[2];
        outputContextsAndPatterns(studyNcontextList, filenameContexts, filenamePatterns, filenameStudies);
        InternalTrainingSet.outputArffFile(filenameContexts);
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
    public void output_distinct(List<String[]> studyNcontextList, String[] filenames, boolean train) {
        String filenameStudies = filenames[0];
        String filenameContexts = filenames[1];
        String filenamePatterns = filenames[2];
        outputContextsAndPatterns_distinct(studyNcontextList, filenameContexts, filenamePatterns, filenameStudies, train);
        if (train) {
            InternalTrainingSet.outputArffFile(filenameContexts);
        }
    }

    /**
     * @return the contextsAsStrings
     */
    public List<String>[] getContextsAsStrings() {
        return contextsAsStrings;
    }

    /**
     * @return the processedSeeds
     */
    public Set<String> getProcessedSeeds() {
        return processedSeeds;
    }

    /**
     * @return the foundSeeds_iteration
     */
    public Set<String> getFoundSeeds_iteration() {
        return foundSeeds_iteration;
    }
//
//    /**
//     * @return the foundPatterns_iteration
//     */
//    public Set<String> getFoundPatterns_iteration() {
//        return foundPatterns_iteration;
//    }
//
//    /**
//     * @return the reliablePatterns_iteration
//     */
//    public Set<String> getReliablePatterns_iteration() {
//        return reliablePatterns_iteration;
//    }

    /**
     * @return the processedPatterns
     */
    public Set<String> getProcessedPatterns() {
        return processedPatterns;
    }

    /**
     * @return the reliablePatternsAndContexts
     */
    public Map<String, List<String[]>> getReliablePatternsAndContexts() {
        return reliablePatternsAndContexts;
    }

    /**
     * @return the reliableInstances
     */
    public Set<String> getReliableInstances() {
        return reliableInstances;
    }

    /**
     * @return the basePath
     */
    public Path getBasePath() {
        return basePath;
    }

    /**
     * @return the constraint_NP
     */
    public boolean isConstraint_NP() {
        return constraint_NP;
    }

    /**
     * @return the constraint_upperCase
     */
    public boolean isConstraint_upperCase() {
        return constraint_upperCase;
    }

    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @return the corpusPath
     */
    public String getCorpusPath() {
        return corpusPath;
    }

    /**
     * @return the indexPath
     */
    public String getIndexPath() {
        return indexPath;
    }

    /**
     * @return the contextPath
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * @return the arffPath
     */
    public String getArffPath() {
        return arffPath;
    }

    /**
     * @return the outputPath
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     * @param contextsAsStrings the contextsAsStrings to set
     */
    public void setContextsAsStrings(List<String>[] contextsAsStrings) {
        this.contextsAsStrings = contextsAsStrings;
    }

    /**
     * @return the reliability
     */
    public Reliability getReliability() {
        return reliability;
    }

    /**
     * Runnable implementation of the matcher.find() method for handling
     * catastropic backtracking. May be passed to a thread to be monitored and
     * cancelled in case catastrophic backtracking occurs while searching for a
     * regular expression.
     *
     * @author katarina.boland@gesis.org
     *
     */
    public static class SafeMatching implements Runnable {

        Matcher matcher;
        boolean find;

        /**
         * Class constructor initializing the matcher.
         *
         * @param m	the Matcher instance to be used for matching
         */
        SafeMatching(Matcher m) {
            this.matcher = m;
        }

        @Override
        public void run() {
            this.find = this.matcher.find();
        }
    }   

    //TODO: use safeMatching instead of m.find()!
    /**
     * Returns whether regular expression <emph>pattern</emph> can be found in
     * string <emph>text</emph>.
     *
     * @param pattern	regular expression to be searched in <emph>text</emph>
     * @param text	input string sequence to search <emph>pattern</emph> in
     * @return	true, if <emph>pattern</emph> is found in <emph>text</emph>,
     * false otherwise
     */
    private int patternFound(String pattern, String text) {
        Pattern pat = Pattern.compile(pattern);
        Matcher m = pat.matcher(text);
        boolean patFound = m.find();
        if (patFound) {
            return 1;
        } else {
            return 0;
        }
    }

    //TODO: use negative training examples to measure relevance?
    /**
     * Determines whether a regular expression is suitable for extracting
     * dataset references using a frequency-based measure
     *
     * @param ngramRegex	regex to be tested
     * @param threshold	threshold for frequency-based relevance measure
     * @return	<emph>true</emph>, if regex is found to be relevant,
     * <emph>false</emph> otherwise
     */
    public boolean isRelevant(String ngramRegex, double threshold) {
        System.out.println("Checking if pattern is relevant: " + ngramRegex);
        // compute score for similar to tf-idf...
        List<String> contexts_pos = this.getContextsAsStrings()[0];
        List<String> contexts_neg = this.getContextsAsStrings()[1];
        // count occurrences of ngram in positive vs negative contexts...
        int count_pos = 0;
        int count_neg = 0;
        for (String context : contexts_pos) {
            count_pos += patternFound(ngramRegex, context);
        }
        // contexts neg always empty right now
        for (String context : contexts_neg) {
            count_neg += patternFound(ngramRegex, context);
        }

        //TODO: rename - this is not really tf-idf ;)
        double idf = 0;
        // compute relevance...
        if (count_neg + count_pos > 0) {
            idf = log2((double) (contexts_pos.size() + contexts_neg.size()) / (count_neg + count_pos));
        }

        double tf_idf = ((double) count_pos / contexts_pos.size()) * idf;

        if (tf_idf > threshold & count_pos > 1) {
            return true;
        } else {
            return false;
        }
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

    //TODO: ADD INSTANCE FILTERING FOR GENERIC PATTERNS (need to substitute 
    //google-based method there...)
    /**
     * Checks whether a given word is a stop word
     *
     * @param word	arbitrary string sequence to be checked
     * @return	true if word is found to be a stop word, false otherwise
     */
    public boolean isStopword(String word) {
        // word consists of punctuation, whitespace and digits only
        if (word.matches("[\\p{Punct}\\s\\d]*")) {
            return true;
        }
        // trim word, lower case and remove all punctuation
        word = word.replace("\\p{Punct}+", "").trim().toLowerCase();
        // due to text extraction errors, whitespace is frequently added to words resulting in many single characters
        // TODO: use this as small work-around but work on better methods for automatic text correction
        if (word.length() < 2) {
            return true;
        }
        if (Util.stopwordList().contains(word)) {
            return true;
        }
        // treat concatenations of stopwords as stopword
        for (String stopword : Util.stopwordList()) {
            if (Util.stopwordList().contains(word.replace(stopword, ""))) {
                return true;
            }
        }
        return false;
    }

    /**
     * ...only difference to other processPatterns method: do not add processed
     * patterns to set of processed patterns...
     *
     * @param patSetIn	...
     * @param seed	...
     * @param outputDir	...
     * @param path_index	...
     * @param path_corpus	...
     * @throws FileNotFoundException
     * @throws IOException
     */
    public List<String[]> processPatterns_reliabilityCheck(Set<String[]> patSetIn,String path_corpus) throws FileNotFoundException, IOException {
        //TODO: DO THIS ONLY ONCE
        //String dir = "data/corpus/dgs_ohneQuellen_reducedWhitespace/";
        String dir = path_corpus;
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
        System.out.println("using patterns to extract new contexts...");
        List<String[]> resNgrams = app.getStudyRefs_optimized_reliabilityCheck(patSetIn);
        System.out.println("done. ");
        System.out.println("Done processing patterns. ");

        return resNgrams;
    }

    //TODO: call this function inside of readArff - split into several methods!
    //remember to call output method there as well
    /**
     * ...
     *
     * @param patSetIn	...
     * @param seed	...
     * @param outputDir	...
     * @param path_index	...
     * @param path_corpus	...
     * @throws FileNotFoundException
     * @throws IOException
     */
    public List<String[]> processPatterns(Set<String[]> patSetIn, String seed, String outputDir, String path_index, String path_corpus) throws FileNotFoundException, IOException {
        String dir = path_corpus;
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
        System.out.println("using patterns to extract new contexts...");
        List<String[]> resNgrams = app.getStudyRefs_optimized(patSetIn);
        System.out.println("done. ");
        //outputContextFiles( resNgrams, seed, outputDir);
        System.out.println("Done processing patterns. ");

        //TODO: add after output of results / usage of results, not here?
        for (String[] p : patSetIn) {
            this.getProcessedPatterns().add(p[1]);
        }
        return resNgrams;
    }

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
    

    /**
     * Computes the logarithm (base 2) for a given value
     *
     * @param x	the value for which the log2 value is to be computed
     * @return	the logarithm (base 2) for the given value
     */
    public double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    /**
     * Generates a regular expression to capture given <emph>title</emph> as
     * dataset title along with any number specifications.
     *
     * @param title	name of the dataset to find inside the regex
     * @return	a regular expression for finding the given title along with any
     * number specifications
     */
    public String constructTitleVersionRegex(String title) {
        // at least one whitespace required...
        return "(" + title + ")" + "\\S*?" + "\\s+" + "\\S*?" + "\\s*" + "\\S*?" + "\\s*" + "\\S*?" + "\\s*" + "\\S*?" + "\\s*" + "\\S*?" + "\\s*" + "((" + Util.yearRegex + "\\s*((-)|(–))\\s*\\d\\d(\\d\\d)?" + ")|(" + Util.yearRegex + ")|(\\d+[.,-/\\\\]?\\d*))";
    }

    /**
     * Generates regular expressions for finding dataset names listed in
     * <emph>filename</emph>
     * with titles and number specifications.
     *
     * @param filename	Name of the file containing a list of dataset names (one
     * name per line)
     * @return	A HashSet of Patterns
     */
    public Set<Pattern> constructPatterns(String filename) {
        Set<Pattern> patternSet = new HashSet();
        try {
            File f = new File(filename);
            InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF8");
            BufferedReader reader = new BufferedReader(isr);
            String studyTitle;
            while ((studyTitle = reader.readLine()) != null) {
                if (!studyTitle.matches("\\s*")) {
                    patternSet.add(Pattern.compile(constructTitleVersionRegex(studyTitle)));
                }
            }
            reader.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return new HashSet<Pattern>();
        }
        return patternSet;
    }

    //TODO: s. searchForPatterns getStudyRefs
    /**
     * Searches for known dataset names...
     *
     * @param patternSet	set of regular expressions (containing the names) to
     * search for dataset references
     * @param corpus	filenames of text documents to search for references
     * @return	...
     */
    public Map<String, Set<String[]>> getStudyRefs_unambiguous(Set<Pattern> patternSet, String[] corpus) {
        Map<String, Set<String[]>> refList = new HashMap();
        for (String filename : corpus) {
            try {
                System.out.println("searching for patterns in " + filename);
                char[] content = new char[(int) new File(filename).length()];
                Reader readerStream = new InputStreamReader(new FileInputStream(filename), "UTF-8");
                BufferedReader reader = new BufferedReader(readerStream);
                reader.read(content);
                reader.close();
                String input = new String(content);
                String inputClean = input.replaceAll("\\s+", " ");
                Iterator<Pattern> patIter = patternSet.iterator();
                HashSet<String[]> refs = new HashSet<String[]>();
                while (patIter.hasNext()) {
                    Pattern p = patIter.next();
                    Matcher m = p.matcher(inputClean);
                    System.out.println("Searching for pattern " + p);
                    SafeMatching safeMatch = new SafeMatching(m);
                    Thread thread = new Thread(safeMatch, filename + "\n" + p);
                    long startTimeMillis = System.currentTimeMillis();
                    // processing time for documents depends on size of the document. 1024 milliseconds allowed per KB
                    long fileSize = new File(filename).length();
                    long maxTimeMillis = fileSize;
                    // set upper limit for processing time - prevents stack overflow caused by monitoring process (threadCompleted)
                    //if ( maxTimeMillis > 750000 ) { maxTimeMillis = 750000; }
                    if (maxTimeMillis > 75000) {
                        maxTimeMillis = 75000;
                    }
                    thread.start();
                    boolean matchFound = false;
                    // if thread was aborted due to long processing time, matchFound should be false
                    if (threadCompleted(thread, maxTimeMillis, startTimeMillis)) {
                        matchFound = safeMatch.find;
                    } else {
                        Util.writeToFile(new File("data/output/abortedMatches_studyTitles.txt"), "utf-8", filename + ";" + p + "\n", true);
                    }

                    while (matchFound) {
                        System.out.println("found pattern " + p + " in " + filename);
                        String studyName = m.group(1).trim();
                        String version = m.group(2).trim();
                        System.out.println("version: " + version);
                        System.out.println("studyName: " + studyName);
                        String[] titleVersion = new String[2];
                        titleVersion[0] = studyName;
                        titleVersion[1] = version;
                        refs.add(titleVersion);

                        System.out.println("Searching for next match of pattern " + p);
                        thread = new Thread(safeMatch, filename + "\n" + p);
                        thread.start();
                        matchFound = false;
                        // if thread was aborted due to long processing time, matchFound should be false
                        if (threadCompleted(thread, maxTimeMillis, startTimeMillis)) {
                            matchFound = safeMatch.find;
                        } else {
                            Util.writeToFile(new File("data/output/abortedMatches_studyTitles.txt"), "utf-8", filename + ";" + p + "\n", true);
                        }
                        System.out.println("Processing new match...");
                    }
                }
                Path path = Paths.get(filename);
                try {
                    filename = getBasePath().relativize(path).normalize().toString();
                } catch (IllegalArgumentException iae) {
                    filename = getBasePath().normalize().toString();
                }
                if (!refs.isEmpty()) {
                    refList.put(filename, refs);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                continue;
            }
        }
        return refList;
    }

    /**
     * Reads names of datasets from file, constructs regular expressions and
     * searches them in specified text corpus to extract dataset references.
     *
     * @param path_output	name of path to save output files
     * @param path_corpus	name of path to text corpus
     * @param path_index	name of path to lucene index of text corpus
     * @param path_knownTitles	name of file containing known and unambiguous
     * dataset names
     * @param filename_knownTitlesMentions	name of output file to save contexts
     * of found dataset names
     * @param constraint_NP	if set, only dataset names occuring inside a noun
     * phrase are accepted
     * @param constraint_upperCase	if set, only dataset names having at least
     * one upper case character are accepted
     */
    public static void searchForTerms(String path_output, String path_corpus, String path_index, String path_knownTitles, String filename_knownTitlesMentions, boolean constraint_NP, boolean constraint_upperCase, boolean german) {
        // list previously processed files to allow pausing and resuming of testing operation
        Set<String> processedFiles = new HashSet();
        try {
            File f = new File(path_output + File.separator + "processedDocs.csv");
            InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF8");
            BufferedReader reader = new BufferedReader(isr);
            String processedFile = null;
            while ((processedFile = reader.readLine()) != null) {
                if (!processedFile.matches("\\s*")) {
                    processedFiles.add(processedFile);
                }
            }
            reader.close();
        } catch (IOException ioe) {
            System.err.println("warning: could not read processedDocs file. continuing... ");
        }
        File corpus = new File(path_corpus);
        String[] corpus_complete = corpus.list();
        Set<String> corpus_test_list = new HashSet();
        for (int i = 0; i < corpus_complete.length; i++) {
            if (!processedFiles.contains(new File(path_corpus + File.separator + corpus_complete[i]).getAbsolutePath())) {
                corpus_test_list.add(new File(path_corpus + File.separator + corpus_complete[i]).getAbsolutePath());
            }
        }
        String[] corpus_test = new String[corpus_test_list.size()];
        corpus_test_list.toArray(corpus_test);
        System.out.println(corpus_test.length);
        System.out.println(processedFiles.size());
        System.out.println(corpus_complete.length);

        if (corpus_complete == null) {
            // Either dir does not exist or is not a directory
        } else {
            for (int i = 0; i < corpus_complete.length; i++) {
                corpus_complete[i] = new File(path_corpus + corpus_complete[i]).getAbsolutePath();
            }
        }

        // need new Learner instance for each task - else, previously processed patterns will not be processed again!
        Learner newLearner2 = new Learner(constraint_NP, constraint_upperCase, german, path_corpus, path_index, "", "", path_output);

        // get refs for known unambiguous studies
        // read study names from file
        // add study names to pattern
        Set<Pattern> patternSetKnown = newLearner2.constructPatterns(path_knownTitles);
        Map<String, Set<String[]>> resKnownStudies = newLearner2.getStudyRefs_unambiguous(patternSetKnown, corpus_complete);
        //write to file for use by contextMiner
        for (String f : resKnownStudies.keySet()) {
            if (!resKnownStudies.get(f).isEmpty()) {
                System.out.println(f);
                Set<String[]> titleVersionSet = resKnownStudies.get(f);
                for (String[] titleVersion : titleVersionSet) {
                    System.out.println(titleVersion[0] + " " + titleVersion[1]);
                }
            }
        }
        try {
            File file = new File(path_output + File.separator + filename_knownTitlesMentions);
            FileOutputStream f = new FileOutputStream(file);
            ObjectOutputStream s = new ObjectOutputStream(f);
            s.writeObject(resKnownStudies);
            s.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Reads existing patterns (regular expressions) from file and searches them
     * in specified text corpus to extract dataset references.
     *
     * @param path_patterns	name of file containing the patterns (regex)
     * @param path_output	name of path to save output files
     * @param path_corpus	name of path to text corpus
     * @param path_index	name of path to lucene index of text corpus
     * @param constraint_NP	if set, only dataset names occuring inside a noun
     * phrase are accepted
     * @param constraint_upperCase	if set, only dataset names having at least
     * one upper case character are accepted
     */
    public static void useExistingPatterns(String path_patterns, String path_output, String path_corpus, String path_index, boolean constraint_NP, boolean constraint_upperCase, boolean german) {
        // load saved patterns
        Set<String> patternSet1;
        try {
            patternSet1 = Util.getDisctinctPatterns(new File(path_patterns));
        } catch (IOException ioe) {
            patternSet1 = new HashSet();
        }

        // list previously processed files to allow pausing and resuming of testing operation
        Set<String> processedFiles = new HashSet();
        try {
            File f = new File(path_output + File.separator + "processedDocs.csv");
            InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF-8");
            BufferedReader reader = new BufferedReader(isr);
            String processedFile = null;
            while ((processedFile = reader.readLine()) != null) {
                if (!processedFile.matches("\\s*")) {
                    processedFiles.add(processedFile);
                }
            }
            reader.close();
        } catch (IOException ioe) {
            System.err.println("warning: could not read processedDocs file. continuing... ");
        }
        File corpus = new File(path_corpus);
        String[] corpus_complete = corpus.list();
        Set<String> corpus_test_list = new HashSet();
        for (int i = 0; i < corpus_complete.length; i++) {
            if (!processedFiles.contains(new File(path_corpus + corpus_complete[i]).getAbsolutePath())) {
                corpus_test_list.add(new File(path_corpus + corpus_complete[i]).getAbsolutePath());
            }
        }
        String[] corpus_test = new String[corpus_test_list.size()];
        corpus_test_list.toArray(corpus_test);
        System.out.println(corpus_test.length);
        System.out.println(processedFiles.size());
        System.out.println(corpus_complete.length);

        if (corpus_complete == null) {
            // Either dir does not exist or is not a directory
        } else {
            for (int i = 0; i < corpus_complete.length; i++) {
                corpus_complete[i] = new File(path_corpus + File.separator + corpus_complete[i]).getAbsolutePath();
            }
        }
        // need new Learner instance for each task - else, previously processed patterns will not be processed again
        Learner newLearner = new Learner(constraint_NP, constraint_upperCase, german, path_corpus, path_index, "", "", path_output);
        PatternApplier appl = new PatternApplier(newLearner);
        List<String[]> resNgrams1 = appl.getStudyRefs(patternSet1, corpus_test);
        String[] filenames_grams = new String[3];
        filenames_grams[0] = path_output + File.separator + "datasets_patterns.csv";
        filenames_grams[1] = path_output + File.separator + "contexts_patterns.xml";
        filenames_grams[2] = path_output + File.separator + "patterns_patterns.csv";
        newLearner.output_distinct(resNgrams1, filenames_grams, false);
    }

    /**
     * Bootraps patterns for identifying references to datasets from initial
     * seed (known dataset name). Pattern validity is assessed using
     * frequency-based measure.
     *
     * @param initialSeed	initial term to be searched for as starting point of
     * the algorithm
     * @param path_index	name of the directory of the lucene index to be
     * searched
     * @param path_train	name of the directory containing the training files
     * @param path_corpus	name of the directory containing the text corpus
     * @param path_output	name of the directory containing the output files
     * @param path_contexts	name of the directory containing the context files
     * @param path_arffs	name of the directory containing the arff files
     */
    public void learn(String initialSeed, String path_index, String path_train, String path_corpus, String path_output, String path_contexts, String path_arffs, boolean constraint_NP, boolean constraint_upperCase, boolean german) {
        try {
            Learner learner = new Learner(constraint_NP, constraint_upperCase, german, path_corpus, path_index, path_contexts, path_arffs, path_output); //TODO: invoke with params for thresholds etc here...
            Collection<String> initialSeeds = new HashSet();
            initialSeeds.add(initialSeed);
            learner.outputParameterInfo(initialSeeds, path_index, path_train, path_corpus, path_output, path_contexts, path_arffs, constraint_NP, constraint_upperCase, "frequency", -1);
            learner.getReliableInstances().add(initialSeed);
            learner.bootstrap(initialSeed, new Baseline2(this),5);

            String allPatternsPath = path_train + File.separator + "allPatterns/";
            File ap = Paths.get(allPatternsPath).normalize().toFile();
            if (!ap.exists()) {
                ap.mkdirs();
                System.out.println("Created directory " + ap);
            }

            OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(new File(allPatternsPath + File.separator + "patterns" + initialSeed + "_distinct.txt")), "UTF-8");
            BufferedWriter out = new BufferedWriter(fstream);
            System.out.println(learner.getProcessedPatterns().size());
            for (String pattern : learner.getProcessedPatterns()) {
                out.write(pattern + System.getProperty("line.separator"));
            }
            out.close();
            System.out.println("Saved patterns to file.");
            OutputStreamWriter fstream2 = new OutputStreamWriter(new FileOutputStream(new File(path_train + File.separator + "allPatterns/processedStudies" + initialSeed + "_distinct.txt")), "UTF-8");
            BufferedWriter out2 = new BufferedWriter(fstream2);
            System.out.println(learner.getProcessedSeeds().size());
            for (String seed : learner.getProcessedSeeds()) {
                out2.write(seed + System.getProperty("line.separator"));
            }
            out2.close();
            System.out.println("Saved seeds to file.");

            String[] filenames_grams = new String[3];
            filenames_grams[0] = path_output + File.separator + "datasets.csv";
            filenames_grams[1] = path_output + File.separator + "contexts.xml";
            filenames_grams[2] = path_output + File.separator + "patterns.csv";

            Set<String> processedFiles = new HashSet();
            try {
                File f = new File(path_output + File.separator + "processedDocs.csv");
                InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF8");
                BufferedReader reader = new BufferedReader(isr);
                String processedFile = null;
                while ((processedFile = reader.readLine()) != null) {
                    if (!processedFile.matches("\\s*")) {
                        processedFiles.add(processedFile);
                    }
                }
                reader.close();
            } catch (IOException ioe) {
                System.err.println("warning: could not read processedDocs file. continuing... ");
            }
            File corpus = new File(path_corpus);
            String[] corpus_complete = corpus.list();

            Set<String> corpus_test_list = new HashSet();
            for (int i = 0; i < corpus_complete.length; i++) {
                // Get filename of file or directory
                if (!processedFiles.contains(new File(path_corpus + File.separator + corpus_complete[i]).getAbsolutePath().toLowerCase())) {
                    corpus_test_list.add(new File(path_corpus + File.separator + corpus_complete[i]).getAbsolutePath().toLowerCase());
                }
            }
            String[] corpus_test = new String[corpus_test_list.size()];
            corpus_test_list.toArray(corpus_test);
            System.out.println(corpus_test.length);
            System.out.println(processedFiles.size());
            System.out.println(corpus_complete.length);

            if (corpus_complete == null) {
                // Either dir does not exist or is not a directory
            } else {
                for (int i = 0; i < corpus_complete.length; i++) {
                    corpus_complete[i] = new File(path_corpus + File.separator + corpus_complete[i]).getAbsolutePath();
                }
            }

            // need new Learner instance - else, previously processed patterns will not be processed again
            Learner newLearner = new Learner(constraint_NP, constraint_upperCase, german, path_corpus, path_index, path_contexts, path_arffs, path_output);
            List<String[]> resNgrams1 = newLearner.app.getStudyRefs(learner.getProcessedPatterns(), corpus_test);
            newLearner.output_distinct(resNgrams1, filenames_grams, true);
        } catch (FileNotFoundException e) {
            System.err.println(e);
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
    }

    /**
     * Bootraps patterns for identifying references to datasets from initial set
     * of seeds (known dataset names). This method uses pattern and instance
     * ranking methods proposed by (cite Espresso paper here...)
     *
     * @param initialSeeds	initial terms to be searched for as starting point of
     * the algorithm
     * @param path_index	name of the directory of the lucene index to be
     * searched
     * @param path_train	name of the directory containing the training files
     * @param path_corpus	name of the directory containing the text corpus
     * @param path_output	name of the directory containing the output files
     * @param path_contexts	name of the directory containing the context files
     * @param path_arffs	name of the directory containing the arff files
     */
    public static void learn(Collection<String> initialSeeds, String path_index, String path_train, String path_corpus, String path_output, String path_contexts, String path_arffs, boolean constraint_NP, boolean constraint_upperCase, boolean german, double threshold) throws IOException {
        Learner learner = new Learner(constraint_NP, constraint_upperCase, german, path_corpus, path_index, path_contexts, path_arffs, path_output);
        learner.outputParameterInfo(initialSeeds, path_index, path_train, path_corpus, path_output, path_contexts, path_arffs, constraint_NP, constraint_upperCase, "reliability", threshold);
        learner.getReliableInstances().addAll(initialSeeds);
  //      learner.bootstrap(initialSeeds);
        learner.outputReliableReferences();
    }

    public String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public void outputParameterInfo(Collection<String> initialSeeds, String path_index, String path_train, String path_corpus, String path_output, String path_contexts, String path_arffs, boolean constraint_NP, boolean constraint_upperCase, String method, double threshold) {
        String delimiter = Util.delimiter_csv;
        String timestamp = getDateTime();
        File logFile = new File(path_output + File.separator + "parameterInfo.csv");
        String parameters = "initial_seeds" + delimiter + "index_path" + delimiter + "train_path" + delimiter
                + "corpus_path" + delimiter + "output_path" + delimiter + "context_path" + delimiter
                + "arff_path" + delimiter + "constraint_NP" + delimiter + "constraint_upperCase" + delimiter
                + "method" + delimiter + "threshold" + delimiter + "start_time\n";
        //TODO: SEEDS MAY CONTAIN MULTIPLE WORDS... (if main method is changed accordingly)
        for (String seed : initialSeeds) {
            parameters += seed + " ";
        }
        parameters = parameters.trim() + delimiter + path_index + delimiter + path_train + delimiter + path_corpus
                + delimiter + path_output + delimiter + path_contexts + delimiter + path_arffs + delimiter
                + constraint_NP + delimiter + constraint_upperCase + delimiter + method + delimiter + threshold + delimiter + timestamp;
        try {
            Util.writeToFile(logFile, "utf-8", parameters, false);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.out.println(parameters);
        }
    }

    /**
     * Writes all extracted references of reliable patterns to xml context file
     * at this output path
     */
    public void outputReliableReferences() {
        ArrayList<String[]> reliableContexts = new ArrayList<>();
        for (String pattern : this.getReliablePatternsAndContexts().keySet()) {
            List<String[]> contexts = this.getReliablePatternsAndContexts().get(pattern);
            reliableContexts.addAll(contexts);
            //see getString_reliablePatternOutput( HashMap<String, ArrayList<String[]>> patternsAndContexts, int iteration )
        }
        String[] filenames = new String[3];
        filenames[0] = this.getOutputPath() + File.separator + "datasets.csv";
        filenames[1] = this.getOutputPath() + File.separator + "contexts.xml";
        filenames[2] = this.getOutputPath() + File.separator + "patterns.csv";
        /*String[] studyNcontext = new String[4];
         studyNcontext[0] = studyName;
         studyNcontext[1] = context;
         studyNcontext[2] = filenameIn;
         studyNcontext[3] = curPat;
         res.add( studyNcontext );*/
        output(reliableContexts, filenames);
    }

    /**
     * Monitors the given thread and stops it when it exceeds its time-to-live.
     * Calls itself until the thread ends after completing its task or after
     * being stopped.
     *
     * @param thread	the thread to be monitored
     * @param maxProcessTimeMillis	the maximum time-to-live for thread
     * @param startTimeMillis	thread's birthday :)
     * @return	false, if thread was stopped prematurely; true if thread ended
     * after completion of its task
     */
    public static boolean threadCompleted(Thread thread, long maxProcessTimeMillis, long startTimeMillis) {
        if (thread.isAlive()) {
            long curProcessTime = System.currentTimeMillis() - startTimeMillis;
            System.out.println("Thread " + thread.getName() + " running for " + curProcessTime + " millis.");
            if (curProcessTime > maxProcessTimeMillis) {
                System.out.println("Thread taking too long, aborting (" + thread.getName());
                thread.stop();
                return false;
            }
        } else {
            return true;
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {;
        }
        return threadCompleted(thread, maxProcessTimeMillis, startTimeMillis);
    }

    /**
     * Main method - calls <emph>OptionHandler</emph> to parse command line
     * options and execute Learner methods accordingly.
     *
     * @param args
     * @throws UnsupportedEncodingException
     */
    public static void main(String[] args) throws UnsupportedEncodingException, IOException {
        new OptionHandler().doMain(args);
        System.out.println("Finished all tasks! Bye :)");
    }
}
/**
 * Class for processing command line options using args4j.
 * 
* @author katarina.boland@gesis.org; based on sample program by Kohsuke
 * Kawaguchi (kk@kohsuke.org)
 */
class OptionHandler {

    @Option(name = "-c", usage = "extract references from this corpus", metaVar = "CORPUS_PATH", required = true)
    private String corpusPath;
    @Option(name = "-i", usage = "use this Lucene Index for documents in corpus", metaVar = "INDEX_PATH", required = true)
    private String indexPath;
    @Option(name = "-l", usage = "learn extraction patterns from corpus and save training data to this directory", metaVar = "TRAIN_PATH")
    private String trainPath;
    @Option(name = "-s", usage = "learn extraction patterns using these seeds", metaVar = "SEED", required = true)
    private String seeds;
    @Option(name = "-p", usage = "use existing extraction patterns listed in this file", metaVar = "PATTERNS_FILENAME")
    private String patternPath;
    @Option(name = "-t", usage = "apply term search for dataset names listed in this file", metaVar = "TERMS_FILENAME")
    private String termsPath;
    @Option(name = "-o", usage = "output to this directory", metaVar = "OUTPUT_PATH", required = true)
    private String outputPath;
    @Option(name = "-n", usage = "if set, use NP constraint", metaVar = "CONSTRAINT_NP_FLAG")
    private boolean constraintNP = false;
    @Option(name = "-u", usage = "if set, use upper-case constraint", metaVar = "CONSTRAINT_UC_FLAG")
    private boolean constraintUC = false;
    @Option(name = "-g", usage = "if set, use German language (important for tagging and phrase chunking if NP constraint is used)", metaVar = "LANG_GERMAN_FLAG")
    private boolean german = false;
    // receives other command line parameters than options
    @Argument
    private List<String> arguments = new ArrayList();

    /**
     * Parses all command line options and calls <emph>Learner</emph> methods
     * accordingly.
     *
     * @param args
     * @throws IOException
     */
    public void doMain(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);

        // parse the arguments.
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println("Learner [options...] arguments...");
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return;
        }

        if (trainPath != null) {
            System.out.println("trainPath is set to " + trainPath);
        }

        if (patternPath != null) {
            System.out.println("patternPath is set to " + patternPath);
        }

        String termsOut = "";
        if (termsPath != null) {
            termsOut = new File(termsPath).getName() + "_foundMentions.map";
        }
        // access non-option arguments
        /*
         System.out.println("other arguments are:");
         for( String s : arguments )
         System.out.println(s);
         */

        // call Learner.learn method with appropriate options
        HashSet<String> pathSet = new HashSet<String>();
        File root = new File(corpusPath);

        //add all documents to corpus for pattern- and term-based search
        if (patternPath != null | termsPath != null) {
            for (File file : root.listFiles()) {
                if (file.isDirectory()) {
                    pathSet.add(file.getName());
                    System.out.println("Added path " + file.getName() + " to set.");
                }
            }

            System.out.println("Added all documents to corpus.");

            for (String basePath : pathSet) {
                // create output path if not existent
                File op = Paths.get(outputPath + File.separator + basePath + File.separator).normalize().toFile();
                if (!op.exists()) {
                    op.mkdirs();
                    System.out.println("Created directory " + op);
                }
                if (patternPath != null) {
                    Learner.useExistingPatterns(patternPath, outputPath + File.separator + basePath + File.separator, corpusPath + File.separator + basePath + File.separator, indexPath + "_" + basePath, constraintNP, constraintUC, german);
                }
                if (termsPath != null) {
                    Learner.searchForTerms(outputPath + File.separator + basePath + File.separator, corpusPath + File.separator + basePath + File.separator, indexPath + "_" + basePath, termsPath, termsOut, constraintNP, constraintUC, german);
                }
            }
        }

        if (trainPath != null) {
            // create training and output paths if not existent
            File tp_contexts = Paths.get(trainPath + File.separator + "contexts" + File.separator).normalize().toFile();
            File tp_arffs = Paths.get(trainPath + File.separator + "arffs" + File.separator).normalize().toFile();
            File op = Paths.get(outputPath + File.separator).normalize().toFile();
            if (!tp_contexts.exists()) {
                tp_contexts.mkdirs();
                System.out.println("Created directory " + tp_contexts);
            }
            if (!tp_arffs.exists()) {
                tp_arffs.mkdirs();
                System.out.println("Created directory " + tp_arffs);
            }
            if (!op.exists()) {
                op.mkdirs();
                System.out.println("Created directory " + op);
            }
            //TODO: IMPROVE: SEEDS MAY CONSIST OF MULTIPLE WORDS...
            String[] seedArray = seeds.split("\\s+");
            //TODO: THRESHOLD AS PARAMETER
            double threshold = 0.03; //0.4 and 0.3 in paper...
            Learner.learn(Arrays.asList(seedArray), indexPath, trainPath, corpusPath, outputPath, trainPath + File.separator + "contexts/", trainPath + File.separator + "arffs/", constraintNP, constraintUC, german, threshold);
            //Learner.learn("ISSP", indexPath, trainPath, corpusPath, outputPath, trainPath + File.separator + "contexts" , trainPath + File.separator + "arffs", constraintNP, constraintUC, german);
        }
    }
}