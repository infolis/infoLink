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
import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.LimitedTimeMatcher;
import io.github.infolis.ws.server.InfolisConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 * @author kata
 */
public class PatternApplier extends BaseAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(PatternApplier.class);
    
    private String getFileAsString(InfolisFile file) throws IOException {
        InputStream in = getFileResolver().openInputStream(file);
        String input = IOUtils.toString(in);
        in.close();
        log.debug("input: " + input);
        // makes regex matching a bit easier
        return input.replaceAll("\\s+", " ");
    }
 
    private List<StudyContext> searchForPatterns(InfolisFile file) throws IOException {
        String inputClean = getFileAsString(file);

        List<StudyContext> res = new ArrayList<>();
		for (String patternURI : this.getExecution().getPattern()) {
            log.debug(patternURI);
            InfolisPattern pattern = getDataStoreClient().get(InfolisPattern.class, patternURI);
            log.debug("Searching for pattern '{}'", pattern.getPatternRegex());
            Pattern p = Pattern.compile(pattern.getPatternRegex());

            // set upper limit for processing time - prevents stack overflow caused by monitoring process 
            // (LimitedTimeMatcher)
            // 750000 suitable for -Xmx2g -Xms2g
            // processing time for documents depends on size of the document. 
            // Allow 1024 milliseconds per KB
            InputStream openInputStream = getFileResolver().openInputStream(file);
			long maxTimeMillis = Math.min(75_000, openInputStream.available());
			openInputStream.close();

            // call m.find() as a thread: catastrophic backtracking may occur which causes application to hang
            // thus monitor runtime of threat and terminate if processing takes too long
            LimitedTimeMatcher ltm = new LimitedTimeMatcher(p, inputClean, maxTimeMillis, file.getFileName() + "\n" + pattern.getPatternRegex()); 
            ltm.run();
            // thread was aborted due to long processing time
            if (! ltm.finished()) {
                //TODO: what to do if search was aborted?
            	log.error("Search was aborted. TODO");
                //InfolisFileUtils.writeToFile(new File("data/abortedMatches.txt"), "utf-8", filenameIn + ";" + curPat + "\n", true);
            }
            while (ltm.matched()) {
                log.debug("found pattern " + pattern.getPatternRegex() + " in " + file);
                String context = ltm.group();
                String studyName = ltm.group(1).trim();
                // if studyname contains no characters: ignore
                //TODO: not accurate - include accents etc in match... \p{M}?
                if (studyName.matches("\\P{L}+")) {
                    log.debug("Searching for next match of pattern " + pattern.getPatternRegex());
                    ltm.run();
                    continue;
                }
                // a study name is supposed to be a named entity and thus should contain at least one upper-case 
                // character 
                // supposedly does not filter out many wrong names in German though
                if (this.getExecution().isUpperCaseConstraint()) {
                    if (studyName.toLowerCase().equals(studyName)) {
                    	ltm.run();
                        log.debug("Match does not satisfy uppercase-constraint \"" + studyName + "\". Processing new match...");
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
                	SearchTermPosition stp = new SearchTermPosition();
                	stp.setDataStoreClient(getDataStoreClient());
                	stp.setFileResolver(getFileResolver());
                    List<StudyContext> con = stp.getContexts(file.getUri(), studyName, context);
                    for (StudyContext oneContext : con) {
                        oneContext.setPattern(pattern.getUri());
                    }
                    res.addAll(con);
                    log.debug("Added context.");
                }

                log.debug("Searching for next match of pattern " + pattern.getPatternRegex());
                ltm.run();
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
