package io.github.infolis.algorithm;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import io.github.infolis.InfolisConfig;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.RegexUtils;
import io.github.infolis.util.SerializationUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.MediaType;

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
    BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(Locale.ROOT);   
    
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
                if (((numNumbers / numChars) >= 0.008) && ((numNumbers / numChars) <= 0.1)
                        && ((numDecimals / numChars) <= 0.004)) {
                } else {
                    textWithoutBib += section;
                }
            }
        }
        return textWithoutBib;
    }
    
    protected List<String> tokenizeSections(String text) {
    	List<String> sections = new ArrayList<String>();
    	//TODO: Test optimal section size
    	int sentencesPerSection = 10;
    	int n = 0;
    	String section = "";
    	sentenceIterator.setText(text);
    	int start = sentenceIterator.first();
        for (int end = sentenceIterator.next();	end != BreakIterator.DONE; start = end, end = sentenceIterator.next()) {
        	n++;
        	if (n <= sentencesPerSection) section += System.getProperty("line.separator") + text.substring(start,end);
        	else { 
        		sections.add(section);
        		section = "";
        		n = 0;
        	}
        }
        if (n != sentencesPerSection) sections.add(section);
        for (String sec : sections) log.debug("----" + sec);
        return sections;
    }
    
    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        if (null == getExecution().getInputFiles()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "inputFiles",
                    "Required parameter 'inputFiles' is missing!");
        } else if (0 == getExecution().getInputFiles().size()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "inputFiles",
                    "No values for parameter 'inputFiles'!");
        }        
    }
    
    
    @Override
    public void execute() { 
    	int counter = 0;
    	for (String inputFileURI : getExecution().getInputFiles()) {
    		counter++;
            log.debug(inputFileURI);
            InfolisFile inputFile;
            try {
                inputFile = getInputDataStoreClient().get(InfolisFile.class, inputFileURI);
            } catch (Exception e) {
                fatal(log, "Could not retrieve file " + inputFileURI + ": " + e.getMessage());
                getExecution().setStatus(ExecutionStatus.FAILED);
                persistExecution();
                return;
            }
            if (null == inputFile) {
                fatal(log, "File was not registered with the data store: " + inputFileURI);
                getExecution().setStatus(ExecutionStatus.FAILED);
                persistExecution();
                return;
            }
            if (null == inputFile.getMediaType() || !inputFile.getMediaType().equals(MediaType.PLAIN_TEXT_UTF_8.toString())) {
                fatal(log, "File is not PLAIN_TEXT_UTF_8: " + inputFileURI);
                getExecution().setStatus(ExecutionStatus.FAILED);
                persistExecution();
                return;
            }
            
            debug(log, "Start removing bib from %s", inputFile);
            String text = "";
            try { text = FileUtils.readFileToString(new File(inputFile.getFileName()), "utf-8"); 
            } catch (IOException e) { 
            	fatal(log, "Error reading text file: " + e); 
            	getExecution().setStatus(ExecutionStatus.FAILED);
            	persistExecution();
                return;
            }
            
            List<String> inputSections = tokenizeSections(text);
            text = removeBibliography(inputSections);
            InfolisFile outFile = new InfolisFile();
            //TODO: really overwrite file?
            outFile.setFileName(inputFile.getFileName());
            outFile.setMediaType("text/plain");
            outFile.setMd5(SerializationUtils.getHexMd5(text));
            outFile.setFileStatus("AVAILABLE");
            
            try {
            	OutputStream outStream = getOutputFileResolver().openOutputStream(outFile);
            	IOUtils.write(text, outStream);
            } catch (IOException e) {
            	fatal(log, "Error copying text to output stream: " + e);
            	getExecution().setStatus(ExecutionStatus.FAILED);
            	persistExecution();
            	return;
            }
            
            updateProgress(counter, getExecution().getInputFiles().size());
            
            debug(log, "Removed bibliography from file %s", outFile);
            getOutputDataStoreClient().post(InfolisFile.class, outFile);
            getExecution().getOutputFiles().add(outFile.getUri());
        }
        debug(log, "No of OutputFiles of this execution: %s", getExecution().getOutputFiles().size());
        getExecution().setStatus(ExecutionStatus.FINISHED);
        persistExecution();
    }
    
}