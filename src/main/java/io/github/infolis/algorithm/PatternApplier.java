/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.util.LimitedTimeMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 * @author kata
 * @author kba
 */
public class PatternApplier extends BaseAlgorithm {

    public PatternApplier(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(PatternApplier.class);

    private String getFileAsString(InfolisFile file)
            throws IOException {
        InputStream in = getInputFileResolver().openInputStream(file);
        String input = IOUtils.toString(in);
        in.close();
        log.trace("Input: " + input);
        // makes regex matching a bit easier
        return input.replaceAll("\\s+", " ");
    }

    // TODO: use second lucene index here... (case-insensitive whitespaceAnalyzer)
    private List<TextualReference> searchForPatterns(InfolisFile file)
            throws IOException {
        String inputClean = getFileAsString(file);

        List<TextualReference> res = new ArrayList<>();
        for (String patternURI : this.getExecution().getPatterns()) {
            //debug(log, patternURI);
            log.trace(patternURI);
            InfolisPattern pattern = getInputDataStoreClient().get(InfolisPattern.class, patternURI);
            //debug(log, "Searching for pattern '%s'", pattern.getPatternRegex());
            log.trace("Searching for pattern '%s'", pattern.getPatternRegex());
            Pattern p = Pattern.compile(pattern.getPatternRegex());

            // set upper limit for processing time - prevents stack overflow
            // caused by monitoring process
            // (LimitedTimeMatcher)
            // 750000 suitable for -Xmx2g -Xms2g
            // processing time for documents depends on size of the document.
            // Allow 1024 milliseconds per KB
            InputStream openInputStream = getInputFileResolver().openInputStream(file);
            long maxTimeMillis = Math.min(75_000, openInputStream.available());
            openInputStream.close();

            // call m.find() as a thread: catastrophic backtracking may occur
            // which causes application to hang
            // thus monitor runtime of threat and terminate if processing takes
            // too long
            LimitedTimeMatcher ltm = new LimitedTimeMatcher(p, inputClean, maxTimeMillis,
                    file.getFileName() + "\n" + pattern.getPatternRegex());
            ltm.run();
            // thread was aborted due to long processing time
            if (!ltm.finished()) {
                // TODO: what to do if search was aborted?
                log.error("Search was aborted. TODO");
                // InfolisFileUtils.writeToFile(new
                // File("data/abortedMatches.txt"), "utf-8", filenameIn + ";" +
                // curPat + "\n", true);
            }
            while (ltm.matched()) {
                String context = ltm.group();
                String studyName = ltm.group(1).trim();
                log.debug("found pattern " + pattern.getPatternRegex() + " in " + file);
                log.debug("referenced study name: " + studyName);
                // if studyname contains no characters: ignore
                // TODO: not accurate - include accents etc in match... \p{M}?
                if (studyName.matches("\\P{L}+")) {
                    log.debug("Invalid study name \"" + studyName + "\". Searching for next match of pattern " + pattern.getPatternRegex());
                    ltm.run();
                    continue;
                }
                // a study name is supposed to be a named entity and thus should
                // contain at least one upper-case character
                if (this.getExecution().isUpperCaseConstraint()) {
                    if (studyName.toLowerCase().equals(studyName)) {
                        ltm.run();
                        log.debug("Match does not satisfy uppercase-constraint \"" + studyName
                                + "\". Processing new match...");
                        continue;
                    }
                }

                List<TextualReference> references = SearchTermPosition.getContexts(getOutputDataStoreClient(), file.getUri(), studyName, context);
                for (TextualReference ref : references) {
                    ref.setPattern(pattern.getUri());
                    log.debug("added reference: " + ref);
                }
                res.addAll(references);
                log.trace("Added references.");

                log.trace("Searching for next match of pattern " + pattern.getPatternRegex());
                ltm.run();
            }
        }
        log.trace("Done searching for patterns in " + file);
        return res;
    }

    @Override
    public void execute() throws IOException {
        List<TextualReference> detectedContexts = new ArrayList<>();
        int counter = 0, size = getExecution().getInputFiles().size();
        System.out.println("size: " + size);
        for (String inputFileURI : getExecution().getInputFiles()) {
            counter++;
            log.trace("Input file URI: '{}'", inputFileURI);
            InfolisFile inputFile;
            try {
                inputFile = getInputDataStoreClient().get(InfolisFile.class, inputFileURI);
            } catch (Exception e) {
                fatal(log, "Could not retrieve file " + inputFileURI + ": " + e.getMessage());
                getExecution().setStatus(ExecutionStatus.FAILED);
                persistExecution();
                return;
            }
            if (null == inputFile) {
                throw new RuntimeException("File was not registered with the data store: " + inputFileURI);
            }
            if (null == inputFile.getMediaType()) {
                throw new RuntimeException("File has no mediaType: " + inputFileURI);
            }
            // if the input file is not a text file
            if (!inputFile.getMediaType().startsWith("text/plain")) {
                // if the input file is a PDF file, convert it
                if (inputFile.getMediaType().startsWith("application/pdf")) {
                    Execution convertExec = new Execution();
                    convertExec.setAlgorithm(TextExtractorAlgorithm.class);
                    convertExec.setInputFiles(Arrays.asList(inputFile.getUri()));
                    // TODO wire this more efficiently so files are stored temporarily
                    Algorithm algo = convertExec.instantiateAlgorithm(this);
                    // do the actual conversion
                    algo.run();
                    // Set the inputFile to the file we just created
                    InfolisFile convertedInputFile = algo.getOutputDataStoreClient().get(InfolisFile.class, convertExec.getOutputFiles().get(0));
                    log.debug("Converted {} -> {}", inputFile.getUri(), convertedInputFile.getUri());
                    log.trace("Content: " + IOUtils.toString(algo.getInputFileResolver().openInputStream(convertedInputFile)));
                    inputFile = convertedInputFile;
                } else {
                    throw new RuntimeException(getClass() + " execution / inputFiles " + "Can only search through text files or PDF files");
                }
            }
            log.trace("Start extracting from '{}'.", inputFile);
            updateProgress(counter, size);

            detectedContexts.addAll(searchForPatterns(inputFile));
        }

        for (TextualReference sC : detectedContexts) {
            getOutputDataStoreClient().post(TextualReference.class, sC);
            this.getExecution().getTextualReferences().add(sC.getUri());
        }

        getExecution().setStatus(ExecutionStatus.FINISHED);
        log.debug("No. contexts found: {}", getExecution().getTextualReferences().size());
    }

    @Override
    public void validate() {
        if (null == this.getExecution().getInputFiles()
                || this.getExecution().getInputFiles().isEmpty()) {
            throw new IllegalArgumentException("Must set at least one inputFile!");
        }
        if (null == this.getExecution().getPatterns() || this.getExecution().getPatterns().isEmpty()) {
            throw new IllegalArgumentException("No patterns given.");
        }
    }
}
