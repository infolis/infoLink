package io.github.infolis.algorithm;

import io.github.infolis.InfolisConfig;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.RegexUtils;
import io.github.infolis.util.SerializationUtils;
import io.github.infolis.util.TextCleaningUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.MediaType;

/**
 *
 * @author kba
 * @author kata
 */
public class TextExtractorAlgorithm extends BaseAlgorithm {

    public TextExtractorAlgorithm(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
            FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(TextExtractorAlgorithm.class);

    private static final PDFTextStripper stripper;

    static {
        try {
            stripper = new PDFTextStripper();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public InfolisFile extract(InfolisFile inFile) throws IOException {
    	String asText = null;
    	
    	// TODO make configurable
        String outFileName = SerializationUtils.changeFileExtension(inFile.getFileName(), "txt");
        if (null != getExecution().getOutputDirectory()) {
            outFileName = SerializationUtils.changeBaseDir(outFileName, getExecution().getOutputDirectory());
        }

        InfolisFile outFile = new InfolisFile();
        outFile.setFileName(outFileName);
        outFile.setMediaType("text/plain");
        
        if (getExecution().getOverwriteTextfiles()) {
	        File _outFile = new File(outFileName);
	        if (_outFile.exists()) { 
	        	debug(log, "File exists: %s, skipping text extraction for %s", _outFile, inFile);
	        	asText = FileUtils.readFileToString(_outFile, "utf-8"); 
	        	outFile.setMd5(SerializationUtils.getHexMd5(asText));
	            outFile.setFileStatus("AVAILABLE");
	            return outFile;
	        }
        }
        
        try (InputStream inStream = getInputFileResolver().openInputStream(inFile)) {
            try (PDDocument pdfIn = PDDocument.load(inStream)) {
                // check whether the bibliography should be removed
                if (getExecution().isRemoveBib()) {
                    asText = extractTextAndRemoveBibliography(pdfIn);
                } else {
                    asText = extractText(pdfIn);
                }
                if (null == asText) {
                    throw new IOException("extractText/extractTextAndRemoveBibliography Returned null!");
                }

                outFile.setMd5(SerializationUtils.getHexMd5(asText));
                outFile.setFileStatus("AVAILABLE");

                try (OutputStream outStream = getOutputFileResolver().openOutputStream(outFile)) {
                    try {
                        IOUtils.write(asText, outStream);
                    } catch (IOException e) {
                        fatal(log, "Error copying text to output stream: " + e);
                        throw e;
                    }
                } catch (IOException e) {
                    fatal(log, "Error opening output stream to text file: " + e);
                    throw e;
                }
                return outFile;
            } catch (Exception e) {
                fatal(log, "Error reading PDF from stream: " + e);
                throw e;
            }
        } catch (IOException e) {
            fatal(log, "Error opening input stream: " + e);
            throw e;
        } catch (Exception e) {
            fatal(log, "Error converting PDF to text: " + e);
            throw e;
        }
    }

    /**
     * Extract the text of a PDF and remove control sequences and line breaks.
     *
     * @param pdfIn
     *            {@link PDDocument} to extract text from
     * @return text of the PDF
     * @throws IOException
     */
    private String extractText(PDDocument pdfIn) throws IOException {
        String asText;
        asText = stripper.getText(pdfIn);

        if (null == asText) {
            throw new IOException("PdfStripper returned null!");
        }
        asText = TextCleaningUtils.removeControlSequences(asText);
        asText = TextCleaningUtils.removeLineBreaks(asText);
        return asText;
    }

    /**
     * Compute the ratio of numbers on page: a high number of numbers is assumed
     * to be typical for bibliographies as they contain many years, page numbers
     * and dates.
     *
     * @param pdfIn
     *            {@link PDDocument} to extract text from
     * @return text of the PDF sans the bibliography
     * @throws IOException
     */
    private String extractTextAndRemoveBibliography(PDDocument pdfIn) throws IOException {
        String textWithoutBib = "";
        boolean startedBib = false;
        // convert PDF pagewise and remove pages belonging to the bibliography
        for (int i = 1; i <= pdfIn.getNumberOfPages(); i++) {
            stripper.setStartPage(i);
            stripper.setEndPage(i);
            String pageText = stripper.getText(pdfIn);
            if (null == pageText) {
            	error(log, "Extraction failed for page %s in file %s", i, pdfIn);
            	continue;
            }
            // clean the page
            pageText = TextCleaningUtils.removeControlSequences(pageText);
            pageText = TextCleaningUtils.removeLineBreaks(pageText);

            double numNumbers = 0.0;
            double numDecimals = 0.0;
            double numChars = pageText.length();
            if (numChars == 0.0) {
                continue;
            }
            // determine the amount of numbers (numeric and decimal)
            Matcher matcherNumeric = RegexUtils.patternNumeric.matcher(pageText);
            Matcher matcherDecimal = RegexUtils.patternDecimal.matcher(pageText);
            while (matcherNumeric.find()) {
                numNumbers++;
            }
            while (matcherDecimal.find()) {
                numDecimals++;
            }
            boolean containsCueWord = false;
            for (String s : InfolisConfig.getBibliographyCues()) {
                if (pageText.contains(s)) {
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
                    textWithoutBib += pageText;
                }
            } else {
                if (((numNumbers / numChars) >= 0.008) && ((numNumbers / numChars) <= 0.1)
                        && ((numDecimals / numChars) <= 0.004)) {
                } else {
                    textWithoutBib += pageText;
                }
            }
        }
        return textWithoutBib;
    }

    @Override
    public void execute() {
        int counter =0;
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
            if (null == inputFile.getMediaType() || !inputFile.getMediaType().equals(MediaType.PDF.toString())) {
                fatal(log, "File is not a PDF: " + inputFileURI);
                getExecution().setStatus(ExecutionStatus.FAILED);
                persistExecution();
                return;
            }
            debug(log, "Start extracting from %s", inputFile);
            InfolisFile outputFile = null;
            try {                
                outputFile = extract(inputFile);
                updateProgress(counter, getExecution().getInputFiles().size());
            } catch (IOException e) {
            	// invalid pdf file cannot be read by pdfBox
            	// log error, skip file and continue with next file
            	error(log, "Extraction caused exception in file %s: %s\n%s", inputFile, e, ExceptionUtils.getStackTrace(e));
            	continue;
            } catch (RuntimeException e) {
            	// error but not fatal: do not terminate execution but continue with next file.
            	// RuntimeErrors caused by DataFormatExceptions in pdfBox may occur when 
            	// pdfBox cannot handle a (valid) pdf file due to its encoding
            	error(log, "Extraction caused exception in file %s: %s\n%s", inputFile, e, ExceptionUtils.getStackTrace(e));
            	continue;
            }
            debug(log, "Converted to file %s", outputFile);
            getOutputDataStoreClient().post(InfolisFile.class, outputFile);
            if (null == outputFile) {
            	error(log, "Conversion failed for input file %s", inputFileURI);
            } else {
                getExecution().getOutputFiles().add(outputFile.getUri());
            }
        }
        debug(log, "No of OutputFiles of this execution: %s", getExecution().getOutputFiles().size());
        getExecution().setStatus(ExecutionStatus.FINISHED);
        persistExecution();
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

    /**
     * Class for processing command line options using args4j.
     *
     * @author kata
     * @author kba
     */
    static class OptionHandler {

        @Option(name = "-i", usage = "path to read PDF documents from", metaVar = "INPUT_PATH")
        private String inputPathOption = System.getProperty("user.dir");

        @Option(name = "-o", usage = "directory to save converted documents to", metaVar = "OUTPUT_PATH")
        private String outputPathOption = System.getProperty("user.dir");

        @Option(name = "-p", usage = "remove bibliography", metaVar = "REMOVE_BIB")
        private boolean removeBib = false;
        
        @Option(name = "-w", usage = "overwrite existing text files", metaVar = "OVERWRITE")
        private boolean overwriteTextfiles = true;

        public void parse(String[] args) {
            CmdLineParser parser = new CmdLineParser(this);
            try {
                parser.parseArgument(args);
            } catch (CmdLineException e) {
                System.err.println(e.getMessage());
                parser.printSingleLineUsage(System.err);
                parser.printUsage(System.err);
                System.exit(1);
            }

            Execution execution = new Execution();
            execution.setAlgorithm(TextExtractorAlgorithm.class);
            FileResolver ifr = FileResolverFactory.create(DataStoreStrategy.LOCAL);
            DataStoreClient idsc = DataStoreClientFactory.create(DataStoreStrategy.LOCAL);
            Algorithm algo = execution.instantiateAlgorithm(idsc, idsc, ifr, ifr);

            Path inputPath = Paths.get(inputPathOption);
            if (Files.isDirectory(inputPath)) {
                try {
                    Iterator<Path> directoryStream = Files.newDirectoryStream(inputPath, "*.pdf").iterator();
                    while (directoryStream.hasNext()) {
                        InfolisFile fileToPost = new InfolisFile();
                        fileToPost.setFileName(directoryStream.next().toString());
                        fileToPost.setMediaType("application/pdf");
                        algo.getInputDataStoreClient().post(InfolisFile.class, fileToPost);
                        execution.getInputFiles().add(fileToPost.getUri());
                    }
                } catch (IOException e) {
                    log.error("Could not read '*.pdf' in directory {}.", inputPath);
                    System.exit(1);
                }
            } else {
                execution.getInputFiles().add(inputPathOption.toString());
            }

            Path outputPath = Paths.get(outputPathOption);
            if (!Files.exists(outputPath)) {
                try {
                    Files.createDirectories(outputPath);
                } catch (IOException e) {
                    log.error("Output directory {} doesn't exist and can't be created.", outputPath);
                    System.exit(1);
                }
            } else if (!Files.isDirectory(outputPath)) {
                log.error("Output directory {} is no directory.", outputPath);
                System.exit(1);
            }
            execution.setOutputDirectory(outputPath.toString());

            execution.setRemoveBib(removeBib);
            execution.setOverwriteTextfiles(overwriteTextfiles);

            algo.run();
        }
    }

    public static void main(String[] args) {
        new OptionHandler().parse(args);
    }
}
