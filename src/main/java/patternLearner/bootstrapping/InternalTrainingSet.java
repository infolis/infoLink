package patternLearner.bootstrapping;

import java.io.File;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import patternLearner.Util;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Class for representing training sets in weka's arff file format.
 *
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
public class InternalTrainingSet {

    private File examples;
    private ExampleReader exReader;

    /**
     * Class constructor specifying the file containing the training examples to
     * be processed.
     *
     * @param examples	a file containing training examples
     */
    public InternalTrainingSet(File examples) {
        this.examples = examples;
        this.exReader = new ExampleReader(examples);
    }

    /**
     * Class constructor.
     */
    InternalTrainingSet() {
        ;
    }

    /**
     * Returns a set of all document filenames occurring in this examples.
     *
     * @return	set of all document filenames occurring in this examples
     */
    public Set<String> getDocuments() {
        return this.exReader.getDocuments();
    }

    /**
     * Returns a set of all contexts occurring in this examples.
     *
     * @return	set of all contexts occurring in this examples
     */
    public Set<String[]> getContexts() {
        return this.exReader.getContexts();
    }
//
//    /**
//     * Creates an ArffFile representation of this training set.
//     */
//    public void createArff() {
//        Set<String[]> contextSet = this.getContexts();
//        new ArffFile(contextSet);
//    }

    /**
     * Creates an ArffFile representation of all items in this examples
     * (InfoLink XML output file containing extracted dataset references),
     * assigns the specified class value classVal and writes the training
     * examples to an arff file </emph>filename</emph>. Uses an
     * <emph>ExampleReader</emph> instance to parse this examples and creates an
     * ArffFile instance using the derived context set.
     *
     * @param classVal	the class value to be set for all instances in
     * </emph>this examples</emph> (either "True" or "False")
     * @param filename	name of the arff output file
     * @return	an ArffFile instance representing the input example set
     */
    public ArffFile createTrainingSet(String classVal, String filename) {
        ExampleReader exReader = new ExampleReader(this.examples);
        Set<String[]> contextSet = exReader.getContexts();
        Set<String[]> contextSetMerged = new HashSet();
        for (String[] leftNrightContext : contextSet) {
            String leftContext = leftNrightContext[0];
            String rightContext = leftNrightContext[1];
            String[] mergedContext = new String[11];
            String[] _leftContext = leftContext.trim().split("\\s+");
            String[] _rightContext = rightContext.trim().split("\\s+");

            if (_leftContext.length < 5) {
                System.out.println("Warning: ignoring context: " + leftContext);
                continue;
            }
            if (_rightContext.length < 5) {
                System.out.println("Warning: ignoring context: " + rightContext);
                continue;
            }

            for (int i = 0; i < 5; i++) {
                mergedContext[i] = Util.normalizeRegex(Util.unescapeXML(_leftContext[i]));
                mergedContext[i + 5] = Util.normalizeRegex(Util.unescapeXML(_rightContext[i]));
            }
            mergedContext[10] = classVal;
            contextSetMerged.add(mergedContext);
        }
        ArffFile test = new ArffFile(contextSetMerged, filename);
        test.write();
        return test;
    }

    /**
     * Calls the <emph>createTrainingSet</emph> method with the specified
     * parameters
     *
     * @param args args[0]: path of the file containing the training examples in
     * InfoLink output XML format; args[1]: path of the output file
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: TrainingSet <inputFile> <outputFile>");
            System.out.println("	<inputFile>	path of the file containing the training examples in InfoLink output XML format");
            System.out.println("	<outputFile>	name of the output File");
            System.exit(1);
        }
        String filename_examples = args[0];
        String filename_output = args[1];
        InternalTrainingSet newSet = new InternalTrainingSet(new File(filename_examples));
        newSet.createTrainingSet("True", filename_output);
    }

    /**
     * Writes instances in TrainingSet at <emph>filename</emph> to Weka's arff
     * file format. All instances in the training set are assumed to be positive
     * training examples (thus receiving the class value <emph>True</emph>).
     * Name of the output file equals the name of the training set file having
     * ".arff" as extension instead of ".xml".
     *
     * @param filename	name of the TrainingSet XML file
     */
    public static void outputArffFile(String filename) {
        InternalTrainingSet newTrainingSet = new InternalTrainingSet(new File(filename));
        //TODO: assumes patterns to be correct
        newTrainingSet.createTrainingSet("True", filename.replace(".xml", ".arff"));
    }
    
}
