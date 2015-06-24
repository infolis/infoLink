package io.github.infolis.infolink.patternLearner;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.algorithm.FrequencyBasedBootstrapping;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.LocalClient;
import io.github.infolis.datastore.TempFileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.Execution.Strategy;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.RegexUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.lucene.queryParser.ParseException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for finding references to scientific datasets in publications using a
 * minimum supervised iterative pattern induction approach. For a description of
 * the basic algorithm see
 * <emph>Boland, Katarina; Ritze, Dominique; Eckert, Kai; Mathiak, Brigitte
 * (2012): Identifying references to datasets in publications. In: Zaphiris,
 * Panayiotis; Buchanan, George; Rasmussen, Edie; Loizides, Fernando (Hrsg.):
 * Proceedings of the Second International Conference on Theory and Practice of
 * Digital Libraries (TDPL 2012), Paphos, Cyprus, September 23-27, 2012. Lecture
 * notes in computer science, 7489, Berlin: Springer, S. 150-161. </emph>. Note
 * that some features are not described in this publication.
 *
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
// TODO kba: Improper Algorithm implementation
public class Learner implements Algorithm {

    Logger log = LoggerFactory.getLogger(Learner.class);

    private boolean constraint_upperCase;
    private String corpusPath;
    private String outputPath;
    private Strategy startegy;
    private String[] fileCorpus;

    /**
     * Class constructor specifying the constraints for patterns.
     *
     * @param constraint_NP	if set, references are only accepted if assumed
     * dataset name occurs in nominal phrase
     * @param constraint_upperCase	if set, references are only accepted if
     * dataset name has at least one upper case character
     *
     */
    public Learner(boolean constraint_upperCase, String corpusPath, String outputPath, Strategy strategy) {
        this.constraint_upperCase = constraint_upperCase;
        this.corpusPath = corpusPath;
        this.outputPath = outputPath;
        this.startegy = strategy;
        this.fileCorpus = generateCorpus();
    }

    private String[] generateCorpus() {
        File corpus = new File(this.corpusPath);
        String[] corpus_test = corpus.list();
        if (corpus_test == null) {
            // Either dir does not exist or is not a directory
        } else {
            for (int i = 0; i < corpus_test.length; i++) {
                // Get filename of file or directory
                corpus_test[i] = this.corpusPath + File.separator + corpus_test[i];
            }
        }
        return corpus_test;
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
    private List<String> bootstrap_frequency(Collection<String> terms, double threshold, int maxIterations, Execution.Strategy strategy) throws IOException, ParseException {
        Execution e = new Execution();
        e.setAlgorithm(FrequencyBasedBootstrapping.class);
        e.setInputFiles(Arrays.asList(fileCorpus));
        e.setTerms(new ArrayList<>(terms));
        e.setBootstrapStrategy(strategy);
        e.setThreshold(threshold);
        e.setMaxIterations(maxIterations);
        e.setUpperCaseConstraint(this.constraint_upperCase);
        e.instantiateAlgorithm(getDataStoreClient(), getFileResolver()).run();
        return e.getStudyContexts();
    }

    /**
     * Main method for reliability-based bootstrapping.
     *
     * @param terms	reliable seed terms for current iteration
     * @param threshold	reliability threshold
     * @param numIter	current iteration
     */
    private List<String> bootstrap_reliability(Collection<String> terms, double threshold, int maxIter) throws IOException, ParseException {
        Execution e = new Execution();
        e.setAlgorithm(FrequencyBasedBootstrapping.class);
        e.setInputFiles(Arrays.asList(fileCorpus));
        e.setTerms(new ArrayList<>(terms));
        e.setThreshold(threshold);
        e.setMaxIterations(maxIter);
        e.setUpperCaseConstraint(this.constraint_upperCase);
        e.instantiateAlgorithm(getDataStoreClient(), getFileResolver()).run();
        return e.getStudyContexts();
    }


    public List<StudyContext> searchForStudynames(String studyNamePath) throws IOException {
    	return new ArrayList<>();
    }

    /**
     * Reads existing patterns (regular expressions) from file and searches them
     * in specified text corpus to extract dataset references.
     */
    public void useExistingPatterns(DataStoreClient client, String patternPath) {
    }

    /**
     * Bootraps patterns for identifying references to datasets from initial
     * seed (known dataset name).
     *
     * @param seeds initial term to be searched for as starting point of the
     * algorithm
     */
    public void learn(Collection<String> seeds, double threshold, int maxIterations) {
        try {
            if (this.startegy == Strategy.reliability) {
                List<String> foundContexts = bootstrap_reliability(seeds, threshold, maxIterations);
                //OutputWriter.outputReliableReferences(foundContexts, this.outputPath);
                //TODO: resolve URIs and return study contexts
            } else {
            	List<String> foundContexts = bootstrap_frequency(seeds, threshold, maxIterations, this.startegy);
            }
        } catch (FileNotFoundException e) {
            System.err.println(e);
        } catch (IOException ioe) {
            System.err.println(ioe);
        } catch (ParseException pe) {
            pe.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Main method - calls <emph>OptionHandler</emph> to parse command line
     * options and execute Learner methods accordingly.
     *
     * @param args
     * @throws UnsupportedEncodingException
     */
    public static void main(String[] args) throws UnsupportedEncodingException, IOException {
        new OptionHandler().doMain(args);
        System.out.println("Finished all tasks! Bye :)");
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

    @Override
    public void execute() {
        // TODO Auto-generated method stub

    }

    @Override
    public void validate() {
        // TODO Auto-generated method stub

    }

    @Override
    public Execution getExecution() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setExecution(Execution execution) {
        // TODO Auto-generated method stub

    }

    @Override
    public FileResolver getFileResolver() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setFileResolver(FileResolver fileResolver) {
        // TODO Auto-generated method stub

    }

    @Override
    public DataStoreClient getDataStoreClient() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setDataStoreClient(DataStoreClient dataStoreClient) {
        // TODO Auto-generated method stub

    }

	@Override
	public void debug(Logger log, String fmt, Object... args) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(Logger log, String fmt, Object... args) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fatal(Logger log, String fmt, Object... args) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TempFileResolver getTempFileResolver() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LocalClient getTempDataStoreClient() {
		// TODO Auto-generated method stub
		return null;
	}
}

/**
 * Class for processing command line options using args4j.
 * 
* @author bolandka; based on sample program by Kohsuke
 * Kawaguchi (kk@kohsuke.org)
 */
class OptionHandler {

    @Option(name = "-c", usage = "extract references from this corpus", metaVar = "CORPUS_PATH", required = true)
    private String corpusPath;

    @Option(name = "-i", usage = "use this Lucene Index for documents in corpus", metaVar = "INDEX_PATH", required = true)
    private String indexPath;

    @Option(name = "-l", usage = "learn extraction patterns from corpus and save training data to this directory", metaVar = "TRAIN_PATH")
    private String trainPath;

    @Option(name = "-s", usage = "learn extraction patterns using these seeds", metaVar = "SEED", required = true)
    private String seeds;

    @Option(name = "-p", usage = "use existing extraction patterns listed in this file", metaVar = "PATTERNS_FILENAME")
    private String patternPath;

    @Option(name = "-t", usage = "apply term search for dataset names listed in this file", metaVar = "TERMS_FILENAME")
    private String termsPath;

    @Option(name = "-o", usage = "output to this directory", metaVar = "OUTPUT_PATH", required = true)
    private String outputPath;

    @Option(name = "-n", usage = "if set, use NP constraint with the specified tree tagger arguments TAGGER_ARGS", metaVar = "TAGGER_ARGS")
    private String taggerArgs = null;

    @Option(name = "-u", usage = "if set, use upper-case constraint", metaVar = "CONSTRAINT_UC_FLAG")
    private boolean constraintUC = false;

    @Option(name = "-f", usage = "apply frequency-based pattern validation method using the specified threshold", metaVar = "FREQUENCY_THRESHOLD")
    private String frequencyThreshold;

    @Option(name = "-r", usage = "apply reliability-based pattern validation method using the specified threshold", metaVar = "RELIABILITY_THRESHOLD")
    private String reliabilityThreshold;

    @Option(name = "-N", usage = "sets the maximum number of iterations to MAX_ITERATIONS. If not set, defaults to 4.", metaVar = "MAX_ITERATIONS")
    private String maxIterations;

    @Option(name = "-F", usage = "sets the strategy to use for processing new seeds within the frequency-based framework to FREQUENCY_STRATEGY. If not set, defaults to \"separate\"", metaVar = "FREQUENCY_STRATEGY")
    private Execution.Strategy strategy;

    // receives other command line parameters than options
    @Argument
    private List<String> arguments = new ArrayList<>();

    /**
     * Parses all command line options and calls <emph>Learner</emph> methods
     * accordingly.
     *
     * @param args
     * @throws IOException
     */
    public void doMain(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);
        DataStoreClient client = new LocalClient(UUID.randomUUID());

        // parse the arguments.
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println("Learner [options...] arguments...");
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return;
        }

        if (trainPath != null) {
            System.out.println("trainPath is set to " + trainPath);
        }

        if (patternPath != null) {
            System.out.println("patternPath is set to " + patternPath);
        }
        // access non-option arguments
        /*
         System.out.println("other arguments are:");
         for( String s : arguments )
         System.out.println(s);
         */
        int maxIter = 4;
        if (maxIterations != null) {
            maxIter = Integer.valueOf(maxIterations);
        }

        if (strategy != null) {
            strategy = Execution.Strategy.separate;
        }

        Learner l = new Learner(constraintUC, corpusPath, outputPath, strategy);
        // call Learner.learn method with appropriate options
        Set<String> pathSet = new HashSet<>();
        File root = new File(corpusPath);

        //add all documents to corpus for pattern- and term-based search
        if (patternPath != null | termsPath != null) {
            for (File file : root.listFiles()) {
                if (file.isDirectory()) {
                    pathSet.add(file.getName());
                    System.out.println("Added path " + file.getName() + " to set.");
                }
            }

            System.out.println("Added all documents to corpus.");

            for (String basePath : pathSet) {
                // create output path if not existent
                File op = Paths.get(outputPath + File.separator + basePath + File.separator).normalize().toFile();
                if (!op.exists()) {
                    op.mkdirs();
                    System.out.println("Created directory " + op);
                }
                if (patternPath != null) {
                    l.useExistingPatterns(client, patternPath);
                }
                if (termsPath != null) {
                    l.searchForStudynames(termsPath);
                }
            }
        }
        //TODO: train path not needed anymore. Use flag
        if (trainPath != null) {
            // create training and output paths if not existent
            File tp_contexts = Paths.get(trainPath + File.separator + "contexts" + File.separator).normalize().toFile();
            File tp_arffs = Paths.get(trainPath + File.separator + "arffs" + File.separator).normalize().toFile();
            File op = Paths.get(outputPath + File.separator).normalize().toFile();
            if (!tp_contexts.exists()) {
                tp_contexts.mkdirs();
                System.out.println("Created directory " + tp_contexts);
            }
            if (!tp_arffs.exists()) {
                tp_arffs.mkdirs();
                System.out.println("Created directory " + tp_arffs);
            }
            if (!op.exists()) {
                op.mkdirs();
                System.out.println("Created directory " + op);
            }
            String[] seedArray = seeds.split(RegexUtils.delimiter_internal);
            if (reliabilityThreshold != null) {
                double threshold = Double.parseDouble(reliabilityThreshold);
                l.learn(Arrays.asList(seedArray), threshold, maxIter);
            }
            if (frequencyThreshold != null) {
                double threshold = Double.parseDouble(frequencyThreshold);
                l.learn(Arrays.asList(seedArray), threshold, maxIter);
            }
        }
    }
}
