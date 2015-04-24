package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.infolink.luceneIndexing.PatternInducer;
import io.github.infolis.infolink.patternLearner.Learner;
import io.github.infolis.infolink.patternLearner.OutputWriter;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisPattern;
import io.github.infolis.model.StudyContext;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.queryParser.ParseException;

/**
 *
 * @author domi
 */
public class FrequencyBasedBootstrapping extends BaseAlgorithm {

    @Override
    public void execute() throws IOException {

        List<StudyContext> detectedContexts = new ArrayList<>();
        try {
            detectedContexts = bootstrap_frequency(this.getExecution().getTerms(), this.getExecution().getThreshold(), this.getExecution().getMaxIterations(), this.getExecution().getBootstrapStrategy());
        } catch (ParseException ex) {
            Logger.getLogger(FrequencyBasedBootstrapping.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (StudyContext sC : detectedContexts) {
            getDataStoreClient().post(StudyContext.class, sC);
            this.getExecution().getStudyContexts().add(sC.getUri());
        }
        for (StudyContext sC : detectedContexts) {
            getDataStoreClient().post(StudyContext.class, sC);
            this.getExecution().getPattern().add(sC.getPattern().getUri());
        }

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

    private List<StudyContext> bootstrap_frequency(Collection<String> terms, double threshold, int maxIterations, Execution.Strategy strategy) throws IOException, ParseException {
        int numIter = 0;
        List<StudyContext> extractedContexts = new ArrayList<>();
        List<String> processedSeeds = new ArrayList<>();
        List<InfolisPattern> processedPatterns = new ArrayList<>();
        Set<String> seeds = new HashSet<>();
        seeds.addAll(terms);
        while (numIter < maxIterations) {
            Set<InfolisPattern> newPatterns = new HashSet<>();
            List<StudyContext> contexts_currentIteration = new ArrayList<>();
            numIter++;
            for (String seed : seeds) {
                // 1. use lucene index to search for term in corpus
                Execution execution = new Execution();
                execution.setAlgorithm(SearchTermPosition.class);
                execution.setSearchTerm(seed);
                execution.setIndexDirectory(this.getExecution().getIndexDirectory());

                Algorithm algo = new SearchTermPosition();
                algo.run();

                List<StudyContext> detectedContexts = new ArrayList<>();
                for (String sC : execution.getStudyContexts()) {
                    detectedContexts.add(this.getDataStoreClient().get(StudyContext.class, sC));
                }

                contexts_currentIteration.addAll(detectedContexts);
                extractedContexts.addAll(detectedContexts);
                System.out.println("Processing contexts for seed " + seed);
                // 2. generate patterns
                if (strategy == Execution.Strategy.separate) {
                    Set<InfolisPattern> patterns = PatternInducer.inducePatterns(detectedContexts, threshold, processedPatterns);
                    newPatterns.addAll(patterns);

                }
            }
            if (strategy == Execution.Strategy.mergeCurrent) {
                Set<InfolisPattern> patterns = PatternInducer.inducePatterns(contexts_currentIteration, threshold, processedPatterns);
                newPatterns.addAll(patterns);
            }
            //TODO: add mergeAll and mergeNew
            // 3. search for patterns in corpus
            //TODO: RETURN CONTEXT INSTANCE HERE! Adjust regex part for this

            List<StudyContext> res = applyPattern(newPatterns);

            seeds = new HashSet<>();
            for (StudyContext entry : res) {
                seeds.add(entry.getTerm());
            }
            processedSeeds.addAll(terms);
            System.out.println("Found " + seeds.size() + " new seeds in current iteration");
            numIter++;
        }
        return extractedContexts;
    }

    private List<StudyContext> applyPattern(Set<InfolisPattern> patterns) throws IOException, ParseException {
        List<StudyContext> contexts = new ArrayList<>();

        for (InfolisPattern curPat : patterns) {
            Execution exec = new Execution();
            exec.setAlgorithm(SearchTermPosition.class);
            exec.setFirstOutputFile("");
            exec.setSearchTerm("");
            exec.setSearchQuery(curPat.getLuceneQuery());
            try {
                exec.instantiateAlgorithm(DataStoreStrategy.LOCAL).run();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            List<String> candidateCorpus = exec.getMatchingFilenames();
            Set<String> patSet = new HashSet<>();
            patSet.add(curPat.getUri());
            
            for (String filenameIn : candidateCorpus) {
                Execution execution = new Execution();
                execution.setPattern(new ArrayList<>(patSet));
                execution.setAlgorithm(PatternApplier.class);
                execution.getInputFiles().add(filenameIn);

                try {
                    execution.instantiateAlgorithm(DataStoreStrategy.LOCAL).run();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

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
