/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package patternLearner.bootstrapping.frequency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import patternLearner.Learner;
import patternLearner.PatternInducer;
import patternLearner.Util;
import patternLearner.bootstrapping.Bootstrapping;
import patternLearner.bootstrapping.InternalTrainingSet;

/**
 *
 * @author domi
 */
public class Baseline4 extends Bootstrapping {

    public Baseline4(Learner l) {
        super(l);
    }

    /**
     * Frequency-based pattern induction baseline 4: does not merge contexts,
     * instead processes each context of each new seed separately and continues
     * bootstrapping with this new set ...
     *
     * @param indexDirectory	name of the directory containing the lucene index
     * to be searched
     * @param terms	instances to process in this iteration
     * @param outputDirectory	path of the output directory
     * @param contextDirName	path of the directory containing all context files
     * @param arffDirName	path of the directory containing all arff files
     * @param corpusDirectory	path of the corpus directory
     * @param numIter	current iteration
     */
    @Override
    public void bootstrap(Set<String> terms, int numIter, int maxIter) {
        //l.getFoundSeeds_iteration() = new HashSet<String>();
        numIter++;
        File contextDir = new File(l.getContextPath());
        String[] contextFiles = contextDir.list();
        List<String> contextFileList = Arrays.asList(contextFiles);
        File arffDir = new File(l.getArffPath());
        String[] arffFiles = arffDir.list();
        List<String> arffFileList = Arrays.asList(arffFiles);
        InternalTrainingSet trainingSet;
        for (String seed : terms) {
            //use this to compute "worst-case recall" = probability, that mentions of an unseen study will be found
            //exclude a particular study here and measure, how often it will be found without being seen before
			/*if ( seed.equals("Westdeutsche Lebensverlaufsstudie") )
             {
             continue;
             }*/
            String seedEscaped = Util.escapeSeed(seed);
            String filenameContext = seedEscaped + ".xml";
            String filenameArff = seedEscaped + ".arff";
            if (!arffFileList.contains(filenameArff)) {
                if (!contextFileList.contains(filenameContext)) {
                    try {
                        l.getContextsForSeed(seed, l.getContextPath() + File.separator + filenameContext);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                trainingSet = new InternalTrainingSet(new File(l.getContextPath() + File.separator + filenameContext));
                trainingSet.createTrainingSet("True", l.getArffPath() + File.separator + filenameArff);
            }
            System.out.println("Processing " + l.getArffPath() + File.separator + filenameArff);
            // only process previously unseen studies
            //TODO: only createTrainingSet for unseen studies...
            if (l.getProcessedSeeds().contains(seed)) {
                try {
                    PatternInducer induce = new PatternInducer(l);
                    induce.readArff(l.getArffPath() + File.separator + filenameArff);
                    l.getProcessedSeeds().add(seed);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //TODO: what about the new detected seesd in the current iteration???
        Set<String> newSeeds = new HashSet();
        try {
            OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(new File(l.getOutputPath() + File.separator + "newSeeds.txt")), "UTF-8");
            BufferedWriter out = new BufferedWriter(fstream);
            System.out.println("Found " + newSeeds.size() + " new seeds in current iteration");

            for (String seed : newSeeds) {
                out.write(seed + System.getProperty("line.separator"));
            }
            out.close();
            System.out.println("Saved seeds to file \"" + File.separator + "iteration" + (numIter + 1) + File.separator + "newSeeds.txt\"");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }
        //TODO: NUMITER...
        if (numIter == 9) {
            System.out.println("Reached maximum number of iterations! Returning.");
            return;
        }
        File nextIterPath = Paths.get(l.getOutputPath() + File.separator + "iteration" + (numIter + 2)).normalize().toFile();
        if (!nextIterPath.exists()) {
            nextIterPath.mkdir();
            System.out.println("Created directory " + nextIterPath);
        }
        bootstrap(terms, numIter, maxIter);
    }
}
