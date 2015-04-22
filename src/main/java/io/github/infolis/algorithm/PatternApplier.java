/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.algorithm;

import io.github.infolis.infolink.tagger.Tagger;
import io.github.infolis.model.Chunk;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.SafeMatching;
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

    private static final Logger log = LoggerFactory.getLogger(PatternApplier.class);

    private List<StudyContext> searchForPatterns(InfolisFile file) throws IOException {
        InputStream in = getFileResolver().openInputStream(file);
        StringWriter writer = new StringWriter();
        IOUtils.copy(in, writer, "UTF-8");
        String input = writer.toString();
        System.out.println("input: " +input);
        // makes regex matching a bit easier
        String inputClean = input.replaceAll("\\s+", " ");

        List<StudyContext> res = new ArrayList<>();
        for (String pattern : this.getExecution().getPattern()) {
            log.debug("Searching for pattern '{}'", pattern);
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(inputClean);

            // call m.find() as a thread: catastrophic backtracking may occur which causes application 
            // to hang
            // thus monitor runtime of threat and terminate if processing takes too long
            SafeMatching safeMatch = new SafeMatching(m);
            Thread thread = new Thread(safeMatch, file.getFileName() + "\n" + pattern);
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
            if (safeMatch.threadCompleted(thread, maxTimeMillis, startTimeMillis)) {
                matchFound = safeMatch.isFind();
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
                    if (safeMatch.threadCompleted(thread, maxTimeMillis, startTimeMillis)) {
                        matchFound = safeMatch.isFind();
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
                        if (safeMatch.threadCompleted(thread, maxTimeMillis, startTimeMillis)) {
                            matchFound = safeMatch.isFind();
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
                if (safeMatch.threadCompleted(thread, maxTimeMillis, startTimeMillis)) {
                    matchFound = safeMatch.isFind();
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

    @Override
    public void execute() throws IOException {
        List<StudyContext> detectedContexts = new ArrayList<>();
        for (String inputFileURI : getExecution().getInputFiles()) {
            log.debug("Input file URI: '{}'", inputFileURI);
            InfolisFile inputFile = getDataStoreClient().get(InfolisFile.class, inputFileURI);
            if (null == inputFile) {
                throw new RuntimeException("File was not registered with the data store: " + inputFileURI);
            }            
            log.debug("Start extracting from '{}'.", inputFile);
            detectedContexts.addAll(searchForPatterns(inputFile));
        }

        for (StudyContext sC : detectedContexts) {
            getDataStoreClient().post(StudyContext.class, sC);
            this.getExecution().getStudyContexts().add(sC.getUri());
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
            throw new IllegalArgumentException("Must set at least one inputFile!");
        }
        if (null == this.getExecution().getPattern()
                 || this.getExecution().getPattern().isEmpty()) {
            throw new IllegalArgumentException("No patterns given.");
        }
    }
}
