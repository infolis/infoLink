/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.algorithm;

import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.Study;
import io.github.infolis.util.LimitedTimeMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class VersionPatternApplier extends BaseAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(PatternApplier.class);

    private List<Study> searchForStudyPatterns(InfolisFile file) throws IOException {
        List<Study> foundStudies = new ArrayList<>();
        InputStream in = getFileResolver().openInputStream(file);
        String input = IOUtils.toString(in);
        in.close();
        System.out.println("input: " + input);
        // makes regex matching a bit easier
        String inputClean = input.replaceAll("\\s+", " ");
        for (String patternURI : this.getExecution().getPattern()) {

            InfolisPattern pattern = getDataStoreClient().get(InfolisPattern.class, patternURI);
            log.debug("Searching for pattern '{}'", pattern.getPatternRegex());
            Pattern p = Pattern.compile(pattern.getPatternRegex());

            long maxTimeMillis = Math.min(75_000, getFileResolver().openInputStream(file).available());
            LimitedTimeMatcher ltm = new LimitedTimeMatcher(p, inputClean, maxTimeMillis, file.getFileName() + "\n" + pattern.getPatternRegex());
            ltm.run();
            // if thread was aborted due to long processing time, matchFound should be false
            if (! ltm.finished()) {
                //TODO: what to do if search was aborted?
                log.error("Search was aborted. TODO");
            }
            while (ltm.finished() && ltm.matched()) {
                String studyName = ltm.group(1).trim();
                String version = ltm.group(2).trim();
                Study study = new Study();
                study.setName(studyName);
                study.setVersion(version);
                foundStudies.add(study);
            }
        }
        return foundStudies;
    }

    @Override
    public void execute() throws IOException {
        List<Study> detectedStudies = new ArrayList<>();
        for (String inputFileURI : getExecution().getInputFiles()) {
            log.debug("Input file URI: '{}'", inputFileURI);
            InfolisFile inputFile = getDataStoreClient().get(InfolisFile.class, inputFileURI);
            if (null == inputFile) {
                throw new RuntimeException("File was not registered with the data store: " + inputFileURI);
            }
            log.debug("Start extracting from '{}'.", inputFile);
            detectedStudies.addAll(searchForStudyPatterns(inputFile));
        }

        for (Study s : detectedStudies) {
            getDataStoreClient().post(Study.class, s);
            this.getExecution().getStudyContexts().add(s.getUri());
        }

        getExecution().setStatus(ExecutionStatus.FINISHED);
        log.debug("No study found: {}", getExecution().getStudyContexts().size());
    }

    @Override
    public void validate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
