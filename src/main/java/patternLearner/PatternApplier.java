/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package patternLearner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static patternLearner.Learner.threadCompleted;
import searching.SearchTermPosition;
import tagger.Tagger;

/**
 *
 * @author domi
 */
public class PatternApplier {
    
    private Learner learner;
    
    public PatternApplier(Learner learner) {
        this.learner = learner;
    }

    /**
     * Applies a list of patterns on the text corpus to extract new dataset
     * references.
     *
     * @param patterns	list of patterns. Each pattern consists of a lucene
     * query, a simple regular expression for computing reliability score and a
     * more complex regular expression for extracting references with contexts
     * @param indexPath	path of the lucene index for the text corpus
     * @return	a list of study references
     */
    public List<String[]> getStudyRefs_optimized_reliabilityCheck(Set<String[]> patterns) {
        List<String[]> resAggregated = new ArrayList();

        for (String curPat[] : patterns) {
            try {
                // get list of documents in which to search for the regular expression
                String[] candidateCorpus = getStudyRef_lucene(curPat[0], learner.getIndexPath());
                Set<String> patSet = new HashSet();
                patSet.add(curPat[2]);
                resAggregated.addAll(getStudyRefs(patSet, candidateCorpus));
                continue;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        System.out.println("Done processing complex patterns. Continuing.");
        return resAggregated;
    }

    /**
     * Applies a list of patterns on the text corpus to extract new dataset
     * references.
     *
     * Retrieves candidate documents containing all the words first using lucene
     * index, then searches for regular expressions in these candidates to
     * extract contexts
     *
     * @param patterns	list of patterns. Each pattern consists of a lucene query
     * and a regular expression for extracting references with contexts
     * @param corpus	list of filenames of the text corpus
     * @param indexPath	path of the lucene index for the text corpus
     * @return	a list of study references
     */
    public List<String[]> getStudyRefs_optimized(Set<String[]> patterns) {
        List<String[]> resAggregated = new ArrayList();

        for (String curPat[] : patterns) {
            try {
                String[] candidateCorpus = getStudyRef_lucene(curPat[0], learner.getIndexPath());
                Set<String> patSet = new HashSet();
                patSet.add(curPat[1]);
                //TODO: see above...
                resAggregated.addAll(getStudyRefs(patSet, candidateCorpus));
                learner.getProcessedPatterns().add(curPat[1]);
                continue;
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        System.out.println("Done processing complex patterns. Continuing.");
        return resAggregated;
    }

    /**
     * Search for lucene query in index at indexPath and return documents with
     * hits.
     *
     * @param lucene_pattern	lucene search query
     * @param indexPath	path of the lucene index
     * @return	a list of documents with hits
     */
    public String[] getStudyRef_lucene(String lucene_pattern, String indexPath) {
        String[] candidateCorpus;
        try {
            // lucene query is assumed to be normalized
            SearchTermPosition candidateSearcher = new SearchTermPosition(indexPath, "", "", lucene_pattern);
            candidateCorpus = candidateSearcher.complexSearch();
            System.out.println("Number of candidate docs: " + candidateCorpus.length);
        } catch (Exception e) {
            e.printStackTrace();
            candidateCorpus = new String[0];
        }
        System.out.println("Done searching lucene query. Continuing.");
        return candidateCorpus;
    }

    /**
     * Searches for dataset references in the documents contained in
     * <emph>corpus</emph>
     * using the regular expressions in <emph>patterns</emph>
     *
     * @param patterns	set of regular expressions for identification of dataset
     * references
     * @param corpus	array of filenames of text documents to search patterns in
     * @return	a list of extracted contexts
     */
    public List<String[]> getStudyRefs(Set<String> patterns, String[] corpus) {        
        List<String[]> resAggregated = new ArrayList();
        for (String filename : corpus) {
            try {
                System.out.println("searching for patterns in " + filename);
                List<String[]> resList = searchForPatterns(patterns, filename);
                resAggregated.addAll(resList);
            } catch (FileNotFoundException e) {
                System.err.println(e);
                continue;
            } catch (IOException ioe) {
                System.err.println(ioe);
                continue;
            }
        }
        return resAggregated;
    }

    /**
     * Searches for dataset references in the documents contained in
     * <emph>corpus</emph>
     * using the regular expressions in <emph>patterns</emph> and outputs all
     * found instances, contexts, patterns and processed documents
     *
     * @param patterns	set of regular expressions for identification of dataset
     * references
     * @param corpus	array of filenames of text documents to search patterns in
     * @param path_output	output path
     * @return	a list of extracted contexts
     */
    private List<String[]> getStudyRefs(Set<String> patterns, String[] corpus, String path_output) {
        List<String[]> resAggregated = new ArrayList();

        for (String filename : corpus) {
            try {
                System.out.println("searching for patterns in " + filename);
                List<String[]> resList = searchForPatterns(patterns, filename);
                String fileLogPath = path_output + File.separator + "processedDocs.csv";
                String[] filenames_grams = new String[3];
                filenames_grams[0] = path_output + File.separator + "datasets.csv";
                filenames_grams[1] = path_output + File.separator + "contexts.xml";
                filenames_grams[2] = path_output + File.separator + "patterns.csv";
                learner.output_distinct(resList, filenames_grams, false);
                OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(fileLogPath, true), "UTF-8");
                BufferedWriter out = new BufferedWriter(fstream);
                out.write(new File(filename).getAbsolutePath() + System.getProperty("line.separator"));
                out.close();

                resAggregated.addAll(resList);
            } catch (FileNotFoundException e) {
                System.err.println(e);
                continue;
            } catch (IOException ioe) {
                System.err.println(ioe);
                continue;
            }
        }
        return resAggregated;
    }

    //TODO: remove filenameout
    //TODO: LOGFILE LOCATION AS PARAM
    //rather: returns all mathces as...
    /**
     * Searches all regex in <emph>patternSet</emph> in the specified text file
     * <emph>filenameIn</emph>
     * and writes all matches to the file <emph>filenameOut</emph>.
     *
     * Creates file data/output/abortedMatches.txt to log all documents where
     * matching was aborted in suspicion of catastrophic backtracking
     *
     * @param patternSet	set of regular expressions for searching
     * @param filenameIn	name of the input text file to be searched in
     * @param filenameOut	name of the output file for saving all matches
     * @return	list of extracted contexts
     * @throws FileNotFoundException
     * @throws IOException
     */
    private List<String[]> searchForPatterns(Set<String> patternSet, String filenameIn) throws FileNotFoundException, IOException {
        // search for each given regex in filenameIn
        // write new training examples to context / arff file...
        char[] content = new char[(int) new File(filenameIn).length()];
        Reader readerStream = new InputStreamReader(new FileInputStream(filenameIn), "UTF-8");
        BufferedReader reader = new BufferedReader(readerStream);
        reader.read(content);
        reader.close();
        String input = new String(content);
        // makes regex matching a bit easier
        String inputClean = input.replaceAll("\\s+", " ");

        List<String[]> res = new ArrayList();
        Iterator<String> patIter = patternSet.iterator();
        while (patIter.hasNext()) {
            String curPat = patIter.next();

            System.out.println("Searching for pattern " + curPat);
            // if pattern was processed before, ignore
            if (learner.getProcessedPatterns().contains(curPat)) {
                continue;
            }

            Pattern p = Pattern.compile(curPat);
            Matcher m = p.matcher(inputClean);

            // call m.find() as a thread: catastrophic backtracking may occur which causes application 
            // to hang
            // thus monitor runtime of threat and terminate if processing takes too long
            Learner.SafeMatching safeMatch = new Learner.SafeMatching(m);
            Thread thread = new Thread(safeMatch, filenameIn + "\n" + curPat);
            long startTimeMillis = System.currentTimeMillis();
            // processing time for documents depends on size of the document. 
            // Allow 1024 milliseconds per KB
            long fileSize = new File(filenameIn).length();
            long maxTimeMillis = fileSize;
            // set upper limit for processing time - prevents stack overflow caused by monitoring process 
            // (threadCompleted)
            // 750000 suitable for -Xmx2g -Xms2g
            // if ( maxTimeMillis > 750000 ) { maxTimeMillis = 750000; }
            if (maxTimeMillis > 75000) {
                maxTimeMillis = 75000;
            }
            thread.start();
            boolean matchFound = false;
            // if thread was aborted due to long processing time, matchFound should be false
            if (threadCompleted(thread, maxTimeMillis, startTimeMillis)) {
                matchFound = safeMatch.find;
            } else {
                Util.writeToFile(new File("data/output/abortedMatches.txt"), "utf-8", filenameIn + ";" + curPat + "\n", true);
            }

            while (matchFound) {
                System.out.println("found pattern " + curPat + " in " + filenameIn);
                String context = m.group();
                String studyName = m.group(1).trim();
                // if studyname contains no characters ignore
                //TODO: not accurate - include accents etc in match... \p{M}?
                if (studyName.matches("\\P{L}+")) {
                    System.out.println("Searching for next match of pattern " + curPat);
                    thread = new Thread(safeMatch, filenameIn + "\n" + curPat);
                    thread.start();
                    matchFound = false;
                    // if thread was aborted due to long processing time, matchFound should be false
                    if (threadCompleted(thread, maxTimeMillis, startTimeMillis)) {
                        matchFound = safeMatch.find;
                    } else {
                        Util.writeToFile(new File("data/output/abortedMatches.txt"), "utf-8", filenameIn + ";" + curPat + "\n", true);
                    }
                    System.out.println("Processing new match...");
                    continue;
                }
                // a study name is supposed to be a named entity and thus contain at least one upper-case 
                // character 
                // supposedly does not filter out many wrong names in German though
                if (learner.isConstraint_upperCase()) {
                    if (studyName.toLowerCase().equals(studyName)) {
                        System.out.println("Searching for next match of pattern " + curPat);
                        thread = new Thread(safeMatch, filenameIn + "\n" + curPat);
                        thread.start();
                        matchFound = false;
                        // if thread was aborted due to long processing time, matchFound should be false
                        if (threadCompleted(thread, maxTimeMillis, startTimeMillis)) {
                            matchFound = safeMatch.find;
                        } else {
                            Util.writeToFile(new File("data/output/abortedMatches.txt"), "utf-8", filenameIn + ";" + curPat + "\n", true);
                        }
                        System.out.println("Processing new match...");
                        continue;
                    }
                }

                boolean containedInNP;
                if (learner.isConstraint_NP()) {
                    //TODO: SPECIFY TAGGING COMMANDS SOMEWHERE ELSE!!!
                    Tagger tagger;
                    if (learner.getLanguage().equals("de")) {
                        tagger = new Tagger("c:/TreeTagger/bin/tag-german", "c:/TreeTagger/bin/chunk-german", "utf-8", "data/tempTagFileIn", "data/tempTagFileOut");
                    } else {
                        tagger = new Tagger("c:/TreeTagger/bin/tag-english", "c:/TreeTagger/bin/chunk-english", "utf-8", "data/tempTagFileIn", "data/tempTagFileOut");
                    }

                    ArrayList<Tagger.Chunk> nounPhrase = tagger.chunk(context).get("<NC>");
                    containedInNP = false;
                    if (nounPhrase != null) {
                        for (Tagger.Chunk chunk : nounPhrase) {
                            if (chunk.getString().replaceAll("\\s", "").contains(studyName.replaceAll("\\s", ""))) {
                                containedInNP = true;
                            }
                        }
                    }
                } else {
                    containedInNP = true;
                }

                if (containedInNP) {
                    String[] studyNcontext = new String[4];
                    studyNcontext[0] = studyName;
                    studyNcontext[1] = context;
                    studyNcontext[2] = filenameIn;
                    studyNcontext[3] = curPat;
                    res.add(studyNcontext);
                    System.out.println("Added context.");
                }

                System.out.println("Searching for next match of pattern " + curPat);
                thread = new Thread(safeMatch, filenameIn + "\n" + curPat);
                thread.start();
                matchFound = false;
                // if thread was aborted due to long processing time, matchFound should be false
                if (threadCompleted(thread, maxTimeMillis, startTimeMillis)) {
                    matchFound = safeMatch.find;
                } else {
                    Util.writeToFile(new File("data/output/abortedMatches.txt"), "utf-8", filenameIn + ";" + curPat + "\n", true);
                }
                System.out.println("Processing new match...");
            }
        }
        System.out.println("Done searching for patterns in " + filenameIn);
        return res;
    }
}
