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
import io.github.infolis.util.RegexUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
public class RegexSearcher extends BaseAlgorithm {

    public RegexSearcher(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(RegexSearcher.class);

    private String getFileAsString(InfolisFile file)
            throws IOException {
        InputStream in = getInputFileResolver().openInputStream(file);
        String input = IOUtils.toString(in);
        in.close();
        return input;
    }

    private List<TextualReference> searchForPatterns(InfolisFile file)
            throws IOException {
        String inputClean = getFileAsString(file);

        List<TextualReference> res = new ArrayList<>();
        for (String patternURI : this.getExecution().getPatterns()) {
            log.trace(patternURI);
            InfolisPattern pattern = getOutputDataStoreClient().get(InfolisPattern.class, patternURI);
            log.trace("Searching for pattern '{}'", pattern.getPatternRegex());
            Pattern p = Pattern.compile(pattern.getPatternRegex());

            // call m.find() as a thread: catastrophic backtracking may occur
            // which causes application to hang
            // thus monitor runtime of threat and terminate if processing takes
            // too long
            LimitedTimeMatcher ltm = new LimitedTimeMatcher(p, inputClean, RegexUtils.maxTimeMillis,
                    file.getFileName() + "\n" + pattern.getPatternRegex());
            ltm.run();
            // thread was aborted due to long processing time
            if (!ltm.finished()) {
                // TODO: what to do if search was aborted?
                log.warn("Search was aborted. TODO");
            }
            while (ltm.matched()) {
                log.debug(String.format("found pattern %s in file %s, match: %s", pattern.getPatternRegex(), file, ltm.group()));
                String referencedTerm = ltm.group(getExecution().getReferenceGroup()).trim();
                log.trace("referenced term: " + referencedTerm);
                String leftContext = ltm.group(getExecution().getLeftContextGroup());
                String rightContext = ltm.group(getExecution().getRightContextGroup());
                log.trace("leftContext: " + leftContext);
                log.trace("rightContext: " + rightContext);
                if (null == leftContext || leftContext.isEmpty()) leftContext = " ";
                if (null == rightContext || rightContext.isEmpty()) rightContext = " ";
                Set<String> tagsToSet = getExecution().getTags();
            	tagsToSet.addAll(file.getTags());
                TextualReference textRef = new TextualReference(leftContext, referencedTerm, rightContext, 
                		file.getUri(), patternURI, file.getManifestsEntity());
                textRef.setTags(tagsToSet);
                log.trace("added reference: " + textRef);
                res.add(textRef);

                log.trace("Searching for next match of pattern " + pattern.getPatternRegex());
                ltm.run();
            }
        }
        log.trace("Done searching for patterns in " + file);
        return res;
    }

    @Override
    public void execute() throws IOException {
    	Execution tagExec = getExecution().createSubExecution(TagSearcher.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.getInfolisPatternTags().addAll(getExecution().getInfolisPatternTags());
    	tagExec.instantiateAlgorithm(this).run();
    	
    	getExecution().getPatterns().addAll(tagExec.getPatterns());
    	getExecution().getInputFiles().addAll(tagExec.getInputFiles());
    	
        List<TextualReference> detectedContexts = new ArrayList<>();
        int counter = 0, size = getExecution().getInputFiles().size();
        System.out.println("number of documents to process: " + size);
        for (String inputFileURI : getExecution().getInputFiles()) {
            counter++;
            log.trace("Input file URI: '{}'", inputFileURI);
            InfolisFile inputFile;
            try {
                inputFile = getInputDataStoreClient().get(InfolisFile.class, inputFileURI);
            } catch (Exception e) {
                error(log, "Could not retrieve file " + inputFileURI + ": " + e.getMessage());
                getExecution().setStatus(ExecutionStatus.FAILED);
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
                    Execution convertExec = getExecution().createSubExecution(TextExtractor.class);
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
    	Execution exec = this.getExecution();
		if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) && 
    		(null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())) {
            throw new IllegalArgumentException("Must set at least one inputFile!");
    	}
        if ((null == exec.getPatterns() || exec.getPatterns().isEmpty()) && 
        		(null == exec.getInfolisPatternTags() || exec.getInfolisPatternTags().isEmpty())) {
            throw new IllegalArgumentException("No patterns given.");
        }
    }
}
