package io.github.infolis.algorithm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import io.github.infolis.InfolisConfig;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.RegexUtils;
import io.github.infolis.util.SerializationUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 *
 */
public class BibliographyExtractor extends BaseAlgorithm {

    public BibliographyExtractor(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
            FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(BibliographyExtractor.class);

    /**
     * Compute the ratio of numbers on page: a high number of numbers is assumed
     * to be typical for bibliographies as they contain many years, page numbers
     * and dates.
     *
     * @param sections	sections of text read from a text file
     * @return text	of sections that are not classified as bibliography
     * @throws IOException
     */
    protected String removeBibliography(List<String> sections) {
        String textWithoutBib = "";
        boolean startedBib = false;
        for (int i = 0; i < sections.size(); i++) {
        	String section = sections.get(i);
            double numNumbers = 0.0;
            double numDecimals = 0.0;
            double numChars = section.length();
            if (numChars == 0.0) continue;
            // determine the amount of numbers (numeric and decimal)
            Matcher matcherNumeric = RegexUtils.patternNumeric.matcher(section);
            Matcher matcherDecimal = RegexUtils.patternDecimal.matcher(section);
            while (matcherNumeric.find()) numNumbers++;
            while (matcherDecimal.find()) numDecimals++;

            boolean containsCueWord = false;
            for (String s : InfolisConfig.getBibliographyCues()) {
                if (section.contains(s)) {
                    containsCueWord = true;
                    break;
                }
            }

            // use hasBibNumberRatio_d method from python scripts
            // TODO learn thresholds
            if (containsCueWord && ((numNumbers / numChars) >= 0.005) && ((numNumbers / numChars) <= 0.1)
                    && ((numDecimals / numChars) <= 0.004)) {
                startedBib = true;
                continue;
            }

            if (startedBib) {
            	if (((numNumbers / numChars) >= 0.01) && ((numNumbers / numChars) <= 0.1)
                        && ((numDecimals / numChars) <= 0.004)) {
                } else {
                    textWithoutBib += section;
                }
            } else {
            	if (((numNumbers / numChars) >= 0.08) && ((numNumbers / numChars) <= 0.1)
                        && ((numDecimals / numChars) <= 0.004)) {
                } else {
                    textWithoutBib += section;
                }
            }
        }
        return textWithoutBib;
    }

    protected List<String> tokenizeSections(String text, int sentencesPerSection) {
    	List<String> sections = new ArrayList<String>();
    	int n = 0;
    	String section = "";

    	String[] lines = text.split(System.getProperty("line.separator"));
    	for (int i = 0; i < lines.length; i++) {
    		n++;
    		section += lines[i] + System.getProperty("line.separator");
    		if (n >= sentencesPerSection) {
        		sections.add(section);
        		section = "";
        		n = 0;
        	}
    	}
        if (n < sentencesPerSection) sections.add(section);
        return sections;
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
    	Execution exec = this.getExecution();
		if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) &&
    		(null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())) {
             throw new IllegalArgumentException("Must set at least one inputFile!");
    	}
    }

    public String transformFilename(String filename, String outputDir) {
    	 String outFileName = SerializationUtils.changeFileExtension(filename, "bibless.txt");
         if (null != outputDir && !outputDir.isEmpty()) {
             outFileName = SerializationUtils.changeBaseDir(outFileName, outputDir);
         }
         return outFileName;
    }


    @Override
    public void execute() throws IOException {
    	Execution tagExec = getExecution().createSubExecution(TagSearcher.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.getInfolisPatternTags().addAll(getExecution().getInfolisPatternTags());
    	tagExec.instantiateAlgorithm(this).run();

    	getExecution().getPatterns().addAll(tagExec.getPatterns());
    	getExecution().getInputFiles().addAll(tagExec.getInputFiles());
    	
    	int counter = 0;
    	for (String inputFileURI : getExecution().getInputFiles()) {
    		counter++;
            log.debug(inputFileURI);
            InfolisFile inputFile;
            try {
                inputFile = getInputDataStoreClient().get(InfolisFile.class, inputFileURI);
            } catch (Exception e) {
                error(log, "Could not retrieve file " + inputFileURI + ": " + e.getMessage());
                getExecution().setStatus(ExecutionStatus.FAILED);
                persistExecution();
                return;
            }
            if (null == inputFile) {
                error(log, "File was not registered with the data store: " + inputFileURI);
                getExecution().setStatus(ExecutionStatus.FAILED);
                persistExecution();
                return;
            }
            if (null == inputFile.getMediaType() || !inputFile.getMediaType().equals("text/plain")) {
                error(log, "File \"%s\" is not text/plain but is %s ", inputFileURI, inputFile.getMediaType());
                getExecution().setStatus(ExecutionStatus.FAILED);
                persistExecution();
                return;
            }

            debug(log, "Start removing bib from {}", inputFile);
            String text;
            InputStream is = null;
			try {
				is =  getInputFileResolver().openInputStream(inputFile);
				text = IOUtils.toString(is);
			} catch (IOException e) {
				fatal(log, "Error reading text file: " + e);
				getExecution().setStatus(ExecutionStatus.FAILED);
				persistExecution();
				return;
			} finally {
				is.close();
			}
            //TODO: Test optimal section size
            List<String> inputSections = tokenizeSections(text, 10);
            text = removeBibliography(inputSections);
            InfolisFile outFile = new InfolisFile();
            
        	// if no output directory is given, create temporary output files
        	if (null == getExecution().getOutputDirectory() || getExecution().getOutputDirectory().equals("")) {
        		 String REMOVED_BIB_DIR_PREFIX = "removedBib-";
                 String tempDir = Files.createTempDirectory(InfolisConfig.getTmpFilePath().toAbsolutePath(), REMOVED_BIB_DIR_PREFIX).toString();
                 FileUtils.forceDeleteOnExit(new File(tempDir));
                 getExecution().setOutputDirectory(tempDir);
             }
        	
            // creates a new file for each text document
            outFile.setFileName(transformFilename(inputFile.getFileName(), getExecution().getOutputDirectory()));
            outFile.setMediaType("text/plain");
            outFile.setMd5(SerializationUtils.getHexMd5(text));
            outFile.setFileStatus("AVAILABLE");
            outFile.setTags(getExecution().getTags());

            OutputStream outStream = null;
            try {
            	outStream = getOutputFileResolver().openOutputStream(outFile);
            	IOUtils.write(text, outStream);
            } catch (IOException e) {
            	error(log, "Error copying text to output stream: " + e);
            	getExecution().setStatus(ExecutionStatus.FAILED);
            	persistExecution();
            	return;
            } finally {
            	outStream.close();
            }

            updateProgress(counter, getExecution().getInputFiles().size());

            debug(log, "Removed bibliography from file {}", outFile);
            getOutputDataStoreClient().post(InfolisFile.class, outFile);
            getExecution().getOutputFiles().add(outFile.getUri());
        }
        debug(log, "No of OutputFiles of this execution: {}", getExecution().getOutputFiles().size());
        getExecution().setStatus(ExecutionStatus.FINISHED);
        persistExecution();
    }

}
