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
import io.github.infolis.util.LimitedTimeMatcher;
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
    
    
    
    private String getFileAsString(InfolisFile file) throws IOException {
        InputStream in = getFileResolver().openInputStream(file);
        StringWriter writer = new StringWriter();
        IOUtils.copy(in, writer, "UTF-8");
        String input = writer.toString();
        System.out.println("input: " + input);
        // makes regex matching a bit easier
        String inputClean = input.replaceAll("\\s+", " ");
        return inputClean;
    }
 
    private List<StudyContext> searchForPatterns(InfolisFile file) throws IOException {
        String inputClean = getFileAsString(file);

        List<StudyContext> res = new ArrayList<>();
		boolean matchFound;
		for (String patternURI : this.getExecution().getPattern()) {
            System.out.println(patternURI);
            io.github.infolis.model.InfolisPattern pattern = getDataStoreClient().get(io.github.infolis.model.InfolisPattern.class, patternURI);
            log.debug("Searching for pattern '{}'", pattern.getPatternRegex());
            Pattern p = Pattern.compile(pattern.getPatternRegex());

            // set upper limit for processing time - prevents stack overflow caused by monitoring process 
            // (threadCompleted)
            // 750000 suitable for -Xmx2g -Xms2g
            // if ( maxTimeMillis > 750000 ) { maxTimeMillis = 750000; }
            // processing time for documents depends on size of the document. 
            // Allow 1024 milliseconds per KB
            long maxTimeMillis = Math.min(75_000, getFileResolver().openInputStream(file).available());

            // call m.find() as a thread: catastrophic backtracking may occur which causes application to hang
            // thus monitor runtime of threat and terminate if processing takes too long
            LimitedTimeMatcher safeMatch = new LimitedTimeMatcher(p, inputClean, maxTimeMillis, file.getFileName() + "\n" + pattern.getPatternRegex()); 
            safeMatch.run();
            // if thread was aborted due to long processing time, matchFound should be false
            if (! safeMatch.finished()) {
                //TODO: what to do if search was aborted?
            	log.error("Search was aborted. TODO");
                //InfolisFileUtils.writeToFile(new File("data/abortedMatches.txt"), "utf-8", filenameIn + ";" + curPat + "\n", true);
            }
            matchFound = safeMatch.matched();
            while (matchFound) {
            	Matcher m = safeMatch.getMatcher();
                log.debug("found pattern " + pattern.getPatternRegex() + " in " + file);
                String context = m.group();
                String studyName = m.group(1).trim();
                // if studyname contains no characters ignore
                //TODO: not accurate - include accents etc in match... \p{M}?
                if (studyName.matches("\\P{L}+")) {
                    log.debug("Searching for next match of pattern " + pattern.getPatternRegex());
                    safeMatch.run();
                    matchFound = safeMatch.matched();
                    // if thread was aborted due to long processing time, matchFound should be false
                    log.debug("Processing new match...");
                    continue;
                }
                // a study name is supposed to be a named entity and thus contain at least one upper-case 
                // character 
                // supposedly does not filter out many wrong names in German though
                if (this.getExecution().isUpperCaseConstraint()) {
                    if (studyName.toLowerCase().equals(studyName)) {
                    	safeMatch.run();
                    	matchFound = safeMatch.matched();
                        log.debug("Processing new match...");
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
                    List<StudyContext> con = SearchTermPosition.getContexts(file.getUri(), studyName, context);
                    for (StudyContext oneContext : con) {
                        oneContext.setPattern(pattern);
                    }
                    res.addAll(con);
                    log.debug("Added context.");
                }

                log.debug("Searching for next match of pattern " + pattern.getPatternRegex());
                safeMatch.run();
                matchFound = safeMatch.matched();
                log.debug("Processing new match...");
            }
        }
        log.debug("Done searching for patterns in " + file);
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

        getExecution().setStatus(ExecutionStatus.FINISHED);
        log.debug("No context found: {}", getExecution().getStudyContexts().size());

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
