/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.algorithm;

import static io.github.infolis.algorithm.SearchTermPosition.getContexts;
import io.github.infolis.infolink.tagger.Tagger;
import io.github.infolis.model.Chunk;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.StudyContext;
import io.github.infolis.ws.server.InfolisConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class PatternApplier extends BaseAlgorithm {

    private Execution execution;
    private static final Logger log = LoggerFactory.getLogger(PatternApplier.class);

    @Override
    public Execution getExecution() {
        return execution;
    }

    @Override
    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    private List<StudyContext> searchForPatterns(InfolisFile file) throws IOException {
        InputStream in = getFileResolver().openInputStream(file);
        StringWriter writer = new StringWriter();
        IOUtils.copy(in, writer, "UTF-8");
        String input = writer.toString();
        // makes regex matching a bit easier
        String inputClean = input.replaceAll("\\s+", " ");

        List<StudyContext> res = new ArrayList<>();
        for (String pattern : this.getExecution().getPattern()) {
            System.out.println("Searching for pattern " + pattern);
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(inputClean);

            // call m.find() as a thread: catastrophic backtracking may occur which causes application 
            // to hang
            // thus monitor runtime of threat and terminate if processing takes too long
            SafeMatching safeMatch = new SafeMatching(m);
            Thread thread = new Thread(safeMatch, file + "\n" + pattern);
            long startTimeMillis = System.currentTimeMillis();
            // processing time for documents depends on size of the document. 
            // Allow 1024 milliseconds per KB
            long fileSize = in.available();
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
                //TODO: what to do if search was aborted?
                //InfolisFileUtils.writeToFile(new File("data/abortedMatches.txt"), "utf-8", filenameIn + ";" + curPat + "\n", true);
            }
            while (matchFound) {
                System.out.println("found pattern " + pattern + " in " + file);
                String context = m.group();
                String studyName = m.group(1).trim();
                // if studyname contains no characters ignore
                //TODO: not accurate - include accents etc in match... \p{M}?
                if (studyName.matches("\\P{L}+")) {
                    System.out.println("Searching for next match of pattern " + pattern);
                    thread = new Thread(safeMatch, file + "\n" + pattern);
                    thread.start();
                    matchFound = false;
                    // if thread was aborted due to long processing time, matchFound should be false
                    if (threadCompleted(thread, maxTimeMillis, startTimeMillis)) {
                        matchFound = safeMatch.find;
                    } else {
                        //TODO: what to do if search was aborted?
                        //InfolisFileUtils.writeToFile(new File("data/abortedMatches.txt"), "utf-8", filenameIn + ";" + curPat + "\n", true);
                    }
                    System.out.println("Processing new match...");
                    continue;
                }
                // a study name is supposed to be a named entity and thus contain at least one upper-case 
                // character 
                // supposedly does not filter out many wrong names in German though
                if (this.getExecution().isUpperCaseConstraint()) {
                    if (studyName.toLowerCase().equals(studyName)) {
                        System.out.println("Searching for next match of pattern " + pattern);
                        thread = new Thread(safeMatch, file + "\n" + pattern);
                        thread.start();
                        matchFound = false;
                        // if thread was aborted due to long processing time, matchFound should be false
                        if (threadCompleted(thread, maxTimeMillis, startTimeMillis)) {
                            matchFound = safeMatch.find;
                        } else {
                            //TODO: what to do if search was aborted?
                            //InfolisFileUtils.writeToFile(new File("data/abortedMatches.txt"), "utf-8", filenameIn + ";" + curPat + "\n", true);
                        }
                        System.out.println("Processing new match...");
                        continue;
                    }
                }
                boolean containedInNP;
                if (this.getExecution().isRequiresContainedInNP()) {
                    Tagger tagger;
                    try {
                        tagger = new Tagger(InfolisConfig.getTagCommand(), InfolisConfig.getChunkCommand(), "utf-8");
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new IOException("\nerror initializing tagger\n");
                    }
                    List<Chunk> nounPhrase = tagger.chunk(context).get("<NC>");
                    containedInNP = false;
                    if (nounPhrase != null) {
                        for (Chunk chunk : nounPhrase) {
                            if (chunk.getString().replaceAll("\\s", "").contains(studyName.replaceAll("\\s", ""))) {
                                containedInNP = true;
                            }
                        }
                    }
                } else {
                    containedInNP = true;
                }
                if (containedInNP) {
                    //String left, String term, String right, String document, String pattern
                    //String filename, String term, String text
                    List<StudyContext> con = SearchTermPosition.getContexts(file.getFileName(), studyName, context);
                    for (StudyContext oneContext : con) {
                        oneContext.setPattern(pattern);
                    }
                    res.addAll(con);
                    System.out.println("Added context.");
                }

                System.out.println("Searching for next match of pattern " + pattern);
                thread = new Thread(safeMatch, file + "\n" + pattern);
                thread.start();
                matchFound = false;
                // if thread was aborted due to long processing time, matchFound should be false
                if (threadCompleted(thread, maxTimeMillis, startTimeMillis)) {
                    matchFound = safeMatch.find;
                } else {
                    //TODO: what to do if search was aborted?
                    //InfolisFileUtils.writeToFile(new File("data/abortedMatches.txt"), "utf-8", filenameIn + ";" + curPat + "\n", true);
                }
                System.out.println("Processing new match...");
            }
        }
        System.out.println("Done searching for patterns in " + file);
        return res;
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
    private static class SafeMatching implements Runnable {

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
    @SuppressWarnings("deprecation")
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

    @Override
    public void execute() throws IOException {
        List<StudyContext> detectedContexts = new ArrayList<>();
        for (String inputFileURI : getExecution().getInputFiles()) {
            log.debug(inputFileURI);
            InfolisFile inputFile = getDataStoreClient().get(InfolisFile.class, inputFileURI);
            if (null == inputFile) {
                throw new RuntimeException("File was not registered with the data store: " + inputFileURI);
            }
            log.debug("Start extracting from " + inputFile);
            detectedContexts.addAll(searchForPatterns(inputFile));

        }

        for (StudyContext sC : detectedContexts) {
            getDataStoreClient().post(StudyContext.class, sC);
            this.execution.getStudyContexts().add(sC.getUri());
        }
        
        if (detectedContexts.isEmpty()) {
            getExecution().logFatal("Pattern applier did not find anything.");
            log.error("Log of this execution: " + getExecution().getLog());
            getExecution().setStatus(ExecutionStatus.FAILED);
            throw new RuntimeException("Pattern applier did not find anything.");
        } else {
            getExecution().setStatus(ExecutionStatus.FINISHED);
            log.debug("No context found: {}", getExecution().getStudyContexts().size());
        }
    }

    @Override
    public void validate() {
        if (null == this.getExecution().getInputFiles()
                || this.getExecution().getInputFiles().isEmpty()) {
            throw new IllegalArgumentException("Must set more than one inputFile!");
        }
        if (null == this.getExecution().getPattern()
                || this.getExecution().getPattern().isEmpty()) {
            throw new IllegalArgumentException("No patterns given.");
        }
    }
}
