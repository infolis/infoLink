package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.infolink.luceneIndexing.PatternInducer;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.StudyContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.queryParser.ParseException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class FrequencyBasedBootstrapping extends BaseAlgorithm {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(FrequencyBasedBootstrapping.class);

    @Override
    public void execute() throws IOException {

        List<StudyContext> detectedContexts = new ArrayList<>();
        try {
            detectedContexts = bootstrapFrequencyBased();
        } catch (ParseException | IOException ex) {
            log.error("Could not apply frequency bootstrapping: " + ex);
            getExecution().setStatus(ExecutionStatus.FAILED);
        }

        for (StudyContext sC : detectedContexts) {
            getDataStoreClient().post(StudyContext.class, sC);
            this.getExecution().getStudyContexts().add(sC.getUri());
        }
//        for (StudyContext sC : detectedContexts) {
//            getDataStoreClient().post(StudyContext.class, sC);
//            this.getExecution().getPattern().add(sC.getPattern().getUri());
//        }
        getExecution().setStatus(ExecutionStatus.FINISHED);

    }

    @Override
    public void validate() {
        //TODO: what about the index path? need to be given!
        if (null == this.getExecution().getTerms()
                || this.getExecution().getTerms().isEmpty()) {
            throw new IllegalArgumentException("Must set at least one term as seed!");
        }
        if (null == this.getExecution().getInputFiles()
                || this.getExecution().getInputFiles().isEmpty()) {
            throw new IllegalArgumentException("Must set at least one input file!");
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
     * @param seed	the term to be searched as starting point in the current
     * iteration
     * @param threshold	threshold for accepting patterns
     * @param maxIterations	maximum number of iterations for algorithm
     *
     */
    private List<StudyContext> bootstrapFrequencyBased() throws ParseException, IOException {
        int numIter = 0;
        List<StudyContext> extractedContexts = new ArrayList<>();
        List<String> processedSeeds = new ArrayList<>();
        List<InfolisPattern> processedPatterns = new ArrayList<>();
        Set<String> seeds = new HashSet<>();
        seeds.addAll(getExecution().getTerms());
        while (numIter < getExecution().getMaxIterations()) {
            Set<InfolisPattern> newPatterns = new HashSet<>();
            List<StudyContext> contexts_currentIteration = new ArrayList<>();
            numIter++;
            for (String seed : seeds) {
                //TODO: not in the original code, processed seeds is not checked there
                if (processedSeeds.contains(seed)) {
                    continue;
                }
                // 1. use lucene index to search for term in corpus
                Execution execution = new Execution();
                execution.setAlgorithm(SearchTermPosition.class);
                execution.setSearchTerm(seed);
                execution.setSearchQuery(getExecution().getSearchQuery()); // TODO: dommi this looks wrong
                execution.setInputFiles(getExecution().getInputFiles());

                try {
                    execution.instantiateAlgorithm(getDataStoreClient(), getFileResolver()).run();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                List<StudyContext> detectedContexts = new ArrayList<>();
                for (String sC : execution.getStudyContexts()) {
                    detectedContexts.add(this.getDataStoreClient().get(StudyContext.class, sC));
                }

                contexts_currentIteration.addAll(detectedContexts);
                extractedContexts.addAll(detectedContexts);
                log.debug("Processing contexts for seed " + seed);
                // 2. generate patterns
                if (getExecution().getBootstrapStrategy() == Execution.Strategy.separate) {
                    Set<InfolisPattern> patterns = PatternInducer.inducePatterns(detectedContexts, getExecution().getThreshold(), processedPatterns);
                    newPatterns.addAll(patterns);

                }
            }
            if (getExecution().getBootstrapStrategy() == Execution.Strategy.mergeCurrent) {
                Set<InfolisPattern> patterns = PatternInducer.inducePatterns(contexts_currentIteration, getExecution().getThreshold(), processedPatterns);
                newPatterns.addAll(patterns);
            }
            //TODO: add mergeAll and mergeNew
            // 3. search for patterns in corpus
            //TODO: RETURN CONTEXT INSTANCE HERE! Adjust regex part for this

            List<StudyContext> res = applyPattern(newPatterns);
            processedSeeds.addAll(seeds);

            seeds = new HashSet<>();
            for (StudyContext entry : res) {
                seeds.add(entry.getTerm());
            }
            log.debug("Found " + seeds.size() + " new seeds in current iteration");
            numIter++;
        }
        return extractedContexts;
    }

    private List<StudyContext> applyPattern(Set<InfolisPattern> patterns) throws IOException, ParseException {
        List<StudyContext> contexts = new ArrayList<>();

        for (InfolisPattern curPat : patterns) {
            Execution exec = new Execution();
            exec.setAlgorithm(SearchTermPosition.class);
            exec.setSearchTerm("");
            exec.setSearchQuery(curPat.getLuceneQuery());
            Algorithm algo = new SearchTermPosition();
            algo.setFileResolver(FileResolverFactory.create(DataStoreStrategy.TEMPORARY));
            algo.setExecution(exec);
            algo.setDataStoreClient(DataStoreClientFactory.local());
            algo.run();

            List<String> candidateCorpus = exec.getMatchingFilenames();
            Set<String> patSet = new HashSet<>();
            patSet.add(curPat.getUri());

            for (String filenameIn : candidateCorpus) {
                
                Execution execution = new Execution();
                execution.setPattern(new ArrayList<>(patSet));
                execution.setAlgorithm(PatternApplier.class);                
                execution.getInputFiles().add(filenameIn);
                Algorithm algo2 = new PatternApplier();
                algo2.setFileResolver(FileResolverFactory.create(DataStoreStrategy.TEMPORARY));
                algo2.setExecution(execution);
                algo2.setDataStoreClient(DataStoreClientFactory.local());
                algo2.run();
//
//                try {
//                    execution.instantiateAlgorithm(DataStoreStrategy.LOCAL).run();
//                } catch (InstantiationException | IllegalAccessException e) {
//                    throw new RuntimeException(e);
//                }

                DataStoreClient client = DataStoreClientFactory.local();

                for (String uri : execution.getStudyContexts()) {
                    StudyContext sc = client.get(StudyContext.class, uri);
                    contexts.add(sc);
                }
            }
        }
        return contexts;
    }
}
