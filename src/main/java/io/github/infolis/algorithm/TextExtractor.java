package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.SerializationUtils;
import io.github.infolis.util.TextCleaningUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;

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
public class TextExtractor extends BaseAlgorithm {

    public TextExtractor(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
            FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
        try {
            stripper = new PDFTextStripper();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TextExtractor.class
    );

    private final PDFTextStripper stripper;

    private String removeBibSection(String text) {
        BibliographyExtractor bibExtractor = new BibliographyExtractor(
                getInputDataStoreClient(), getOutputDataStoreClient(), getInputFileResolver(), getOutputFileResolver());
        //TODO: Test optimal section size
        return bibExtractor.removeBibliography(bibExtractor.tokenizeSections(text, 10));
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
        //TODO either set or list for all tags
		for (String tag : getExecution().getTags())
        	outFile.getTags().add(tag);
		for (String tag : inFile.getTags())
        	outFile.getTags().add(tag);
        if (getExecution().getOverwriteTextfiles() == false) {
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
                asText = extractText(pdfIn);
                if (null == asText) {
                    throw new IOException("extractText returned null!");
                }
                if (getExecution().isRemoveBib()) {
                    asText = removeBibSection(asText);
                }

                outFile.setMd5(SerializationUtils.getHexMd5(asText));
                outFile.setFileStatus("AVAILABLE");

                try (OutputStream outStream = getOutputFileResolver().openOutputStream(outFile)) {
                    try {
                        IOUtils.write(asText, outStream);
                    } catch (IOException e) {
                        warn(log, "Error copying text to output stream: " + e);
                        throw e;
                    }
                } catch (IOException e) {
                    warn(log, "Error opening output stream to text file: " + e);
                    throw e;
                }
                return outFile;
            } catch (Exception e) {
                warn(log, "Error reading PDF from stream: " + e);
                throw e;
            }
        } catch (IOException e) {
            warn(log, "Error opening input stream: " + e);
            throw e;
        } catch (Exception e) {
            warn(log, "Error converting PDF to text: " + e);
            throw e;
        }
    }

    /**
     * Extract the text of a PDF and remove control sequences and line breaks.
     *
     * @param pdfIn {@link PDDocument} to extract text from
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

    @Override
    public void execute() {
        Execution tagExec = getExecution().createSubExecution(TagResolver.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.getInfolisPatternTags().addAll(getExecution().getInfolisPatternTags());
        tagExec.instantiateAlgorithm(this).run();
        if(tagExec.getStatus()==ExecutionStatus.FAILED) {
            getExecution().setSubExecutionFailed(true);
        }
        
        getExecution().getPatterns().addAll(tagExec.getPatterns());
        getExecution().getInputFiles().addAll(tagExec.getInputFiles());
        
        int counter =0;
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
            if (null == inputFile.getMediaType() || !inputFile.getMediaType().equals(MediaType.PDF.toString())) {
                error(log, "File is not a PDF: " + inputFileURI);
                getExecution().setStatus(ExecutionStatus.FAILED);
                persistExecution();
                return;
            }
            debug(log, "Start extracting from %s", inputFile);
            InfolisFile outputFile;
            try {                
                outputFile = extract(inputFile);
                debug(log, "Converted to file %s", outputFile);
            } catch (IOException e) {
                // invalid pdf file cannot be read by pdfBox
                // log warning, skip file and continue with next file
                warn(log, "Extraction caused exception in file %s - PdfBox cannot extract from this file, is it a valid pdf file? Trace: \n%s", inputFile, ExceptionUtils.getStackTrace(e));
                outputFile = null;
                continue;
            } catch (RuntimeException e) {
                // warn but not error: do not terminate execution but continue with next file.
                // RuntimeErrors caused by DataFormatExceptions in pdfBox may occur when 
                // pdfBox cannot handle a (valid) pdf file due to its encoding
                warn(log, "Extraction caused exception in file %s - PdfBox cannot extract from this file due to its encoding or similar issues: \n%s", inputFile, ExceptionUtils.getStackTrace(e));
                outputFile = null;
                continue;
            }
            updateProgress(counter, getExecution().getInputFiles().size());
            if (null == outputFile) {
                warn(log, "Conversion failed for input file %s", inputFileURI);
            } else {
                getOutputDataStoreClient().post(InfolisFile.class, outputFile);
                getExecution().getOutputFiles().add(outputFile.getUri());
            }
        }
        debug(log, "No of OutputFiles of this execution: %s", getExecution().getOutputFiles().size());
        getExecution().setStatus(ExecutionStatus.FINISHED);
        persistExecution();
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        Execution exec = this.getExecution();
		if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) && 
    		(null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())) {
            throw new IllegalArgumentException("Must set at least one inputFile!");
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

        @Option(name = "-b", usage = "remove bibliographies", metaVar = "REMOVE_BIBLIOGRAPHIES")
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
            execution.setAlgorithm(TextExtractor.class);
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
