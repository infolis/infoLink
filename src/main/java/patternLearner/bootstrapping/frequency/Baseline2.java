/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package patternLearner.bootstrapping.frequency;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import patternLearner.Learner;
import patternLearner.PatternInducer;
import patternLearner.Util;
import patternLearner.bootstrapping.Bootstrapping;
import patternLearner.bootstrapping.InternalTrainingSet;

/**
 *
 * @author domi
 */
public class Baseline2 extends Bootstrapping {
    
    public Baseline2(Learner l) {
        super(l);
    }

    /**
     * Frequency-based pattern induction baseline 2: merges contexts of all
     * previously found and hitherto unseen studies and continues bootstrapping
     * with this new set
     *
     * @param terms	instances to process in this iteration
     * @param numIter	current iteration
     */
    @Override
    public void bootstrap(Set<String> terms, int numIter, int maxIter) {
        numIter++;
        try {
            l.getContextsForAllSeeds(terms);
            Util.mergeNewContexts(l.getOutputPath(), "allNew.xml", "");
            InternalTrainingSet trainingSet = new InternalTrainingSet(new File(l.getOutputPath() + File.separator + "allNew.xml"));
            trainingSet.createTrainingSet("True", l.getOutputPath() + File.separator + "allNew.arff");

            PatternInducer induce = new PatternInducer(l);
            induce.readArff(l.getOutputPath() + File.separator + "allNew.arff");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (numIter == 6) {
            return;
        }//TODO: MAX NUM ITER AS PARAM OR CLASS VAL
        Set<String> newSeeds = l.getSeeds(l.getOutputPath() + File.separator + "_all_datasets.csv");
        File nextIterPath = Paths.get(l.getOutputPath() + File.separator + "iteration" + (numIter + 2)).normalize().toFile();
        if (!nextIterPath.exists()) {
            nextIterPath.mkdir();
            System.out.println("Created directory " + nextIterPath);
        }
        bootstrap(newSeeds, numIter, maxIter);
    }
}
