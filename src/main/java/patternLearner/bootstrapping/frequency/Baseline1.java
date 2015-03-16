/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package patternLearner.bootstrapping.frequency;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;
import patternLearner.Learner;
import patternLearner.PatternInducer;
import patternLearner.bootstrapping.InternalTrainingSet;
import patternLearner.Util;
import patternLearner.bootstrapping.Bootstrapping;

/**
 *
 * @author domi
 */
public class Baseline1 extends Bootstrapping {
    
    public Baseline1(Learner l) {
        super(l);
    }

    /**
     * Frequency-based pattern induction baseline 1: merges contexts of all
     * previously found instances and continues bootstrapping with this new set
     *
     * @param indexDirectory	name of the directory containing the lucene index
     * to be searched
     * @param terms	instances to process in this iteration
     * @param outputDirectory	path of the output directory
     * @param corpusDirectory	path of the corpus directory
     * @param numIter	current iteration
     */
    @Override
    public void bootstrap(Set<String> terms, int numIter, int maxIter) {
        numIter++;
        try {
            l.getContextsForAllSeeds(terms);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Util.mergeContexts(l.getOutputPath(), "all.xml", "_all_");
        InternalTrainingSet trainingSet = new InternalTrainingSet(new File(l.getOutputPath() + File.separator + "all.xml"));
        trainingSet.createTrainingSet("True", l.getOutputPath() + File.separator + "all.arff");
        try {
            PatternInducer induce = new PatternInducer(l);
            induce.readArff(l.getOutputPath() + File.separator + "all.arff");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (numIter == 6) {
            return;
        } //TODO: MAX NUM ITER AS PARAM OR CLASS VAL
        Set<String> newSeeds = l.getSeeds(l.getOutputPath() + File.separator + "_all_datasets.csv");
        File nextIterPath = Paths.get(l.getOutputPath() + File.separator + "iteration" + (numIter + 2)).normalize().toFile();
        if (!nextIterPath.exists()) {
            nextIterPath.mkdir();
            System.out.println("Created directory " + nextIterPath);
        }
        bootstrap(newSeeds,numIter,maxIter);
    }
}
