/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package webservice;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import patternLearner.Learner;

/**
 *
 * @author domi
 */
public class PatternRecognizerWS {

    private static Set<String> seeds;
    private static String corpusPath = "C:\\Users\\domi\\InFoLiS2\\InfoLink\\infoLink\\in";
    private static String indexDir = "C:\\Users\\domi\\InFoLiS2\\InfoLink\\infoLink\\index";
    private static String outputDirectory = "C:\\Users\\domi\\InFoLiS2\\InfoLink\\infoLink\\out";
    private static Set<String> processedSeeds;
    private static Set<String> processedPatterns;
    private static Set<String> foundSeeds_iteration;
    private static List<String>[] contextsAsStrings;
    private static String contextDir = "context/";
    private static String arffDirName = "arff/";
    private static String train = "train/";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {

        Learner l = new Learner(false, false, true, corpusPath, indexDir, contextDir, arffDirName, outputDirectory);
   //     seeds = l.getSeeds(args[0]);
        //TODO: how to initialize indexDir
        //TODO: initialize output
   //     outputDirectory = args[1];
        //l.bootstrap("OECD");
        //Learner.searchForTerms(outputDirectory, corpusPath, indexDir, "seeds.txt", "out.txt" , true, true, true);
        l.learn("OECD", indexDir, train , corpusPath, outputDirectory, contextDir, arffDirName, true, true, true);
    }

}
