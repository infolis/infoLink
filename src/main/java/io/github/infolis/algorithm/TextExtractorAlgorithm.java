package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.util.RegexUtils;
import io.github.infolis.util.SerializationUtils;
import io.github.infolis.util.TextCleaningUtils;
import io.github.infolis.ws.server.InfolisConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.regex.Matcher;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kba
 */
public class TextExtractorAlgorithm extends BaseAlgorithm {

	private static final Logger log = LoggerFactory.getLogger(TextExtractorAlgorithm.class);

	private PDFTextStripper stripper;

	public TextExtractorAlgorithm() {
		super();
		try {
			this.stripper = new PDFTextStripper();
		} catch (IOException e) {
			getExecution().getLog().add("Error instantiating PDFTextStripper.");
			throw new RuntimeException(e);
		}
	}

	public InfolisFile extract(InfolisFile inFile) {
		InputStream inStream = null;
		OutputStream outStream = null;
		PDDocument pdfIn;
		String asText = null;

		try {
			inStream = getFileResolver().openInputStream(inFile);
		} catch (IOException e) {
			getExecution().getLog().add("Error opening input stream.");
			return null;
		}
		try {
			pdfIn = PDDocument.load(inStream);
		} catch (IOException e) {
			getExecution().getLog().add("Error reading PDF from stream.");
			return null;
		}
		try {// check whether the bibliography should be reomoved
			if (getExecution().isRemoveBib()) {
				asText = extractTextAndRemoveBibliography(pdfIn);
			} else {
				asText = extractText(pdfIn);
			}
		} catch (Exception e) {
			getExecution().getLog().add("Error converting PDF to text.");
			return null;
		}

		String outFileName = SerializationUtils.changeFileExtension(inFile.getFileName(), "txt");
		if (null != getExecution().getOutputDirectory()) {
			outFileName = SerializationUtils.changeBaseDir(outFileName, getExecution().getOutputDirectory());
		}
		InfolisFile outFile = new InfolisFile();
		outFile.setFileName(outFileName);
		outFile.setMediaType("text/plain");
		outFile.setMd5(SerializationUtils.getHexMd5(asText));
		outFile.setFileStatus("AVAILABLE");

		try {
			outStream = getFileResolver().openOutputStream(outFile);
		} catch (IOException e) {
			getExecution().getLog().add("Error opening output stream to text file.");
		}
		try {
			IOUtils.write(asText, outStream);
		} catch (IOException e) {
			getExecution().getLog().add("Error copying text to output stream.");
		}
		return outFile;
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
			throw new NullPointerException();
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
	protected String extractTextAndRemoveBibliography(PDDocument pdfIn) throws IOException {
		String textWithoutBib = "";
		boolean startedBib = false;
		// convert PDF pagewise and remove pages belonging to the bibliography
		for (int i = 1; i <= pdfIn.getNumberOfPages(); i++) {
			stripper.setStartPage(i);
			stripper.setEndPage(i);
			String pageText = stripper.getText(pdfIn);
			if (null == pageText) {
				throw new NullPointerException();
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
			if (containsCueWord && ((numNumbers / numChars) >= 0.005)
					&& ((numNumbers / numChars) <= 0.1) && ((numDecimals / numChars) <= 0.004)) {
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
		for (String inputFileURI : getExecution().getInputFiles()) {
			log.debug(inputFileURI);
			InfolisFile inputFile = getDataStoreClient().get(InfolisFile.class, inputFileURI);
			if (null == inputFile) {
				throw new RuntimeException("File was not registered with the data store: "+ inputFileURI);
			}
			log.debug("Start extracting from " + inputFile);
			InfolisFile outputFile = extract(inputFile);
//			log.debug("LOG {}", getExecution().getLog());
			// FrontendClient.post(InfolisFile.class, outputFile);
			if (null == outputFile) {
				getExecution().getLog().add(
						"Conversion failed for input file " + inputFileURI);
				log.debug("Log of this execution: " + getExecution().getLog());
				getExecution().setStatus(ExecutionStatus.FAILED);
			} else {
				getExecution().setStatus(ExecutionStatus.FINISHED);
				getDataStoreClient().post(InfolisFile.class, outputFile);
				getExecution().getOutputFiles().add(outputFile.getUri());
				log.debug("OutputFiles of this execution: {}", getExecution().getOutputFiles());
			}
		}
	}

	@Override
	public void validate() {
		if (null == getExecution().getInputFiles()) {
			throw new IllegalArgumentException("Required parameter 'inputFiles' is missing!");
		} else if (0 == getExecution().getInputFiles().size()) {
			throw new IllegalArgumentException("No values for parameter 'inputFiles'!");
		}
//		if (null == getExecution().getOutputFiles()) {
//			getExecution().setOutputFiles(new ArrayList<String>());
//		}
	}

	/**
	 * Class for processing command line options using args4j.
	 *
	 * @author katarina.boland@gesis.org
	 * @author kba
	 */
	static class OptionHandler {

		@Option(name = "-i", usage = "path to read PDF documents from", metaVar = "INPUT_PATH")
		private String inputPathOption = System.getProperty("user.dir");

		@Option(name = "-o", usage = "directory to save converted documents to", metaVar = "OUTPUT_PATH")
		private String outputPathOption = System.getProperty("user.dir");

		@Option(name = "-p", usage = "remove bibliography", metaVar = "REMOVE_BIB")
		private boolean removeBib = false;

		public void parse(String[] args)
		{
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
			Algorithm algo = new TextExtractorAlgorithm();
			execution.setAlgorithm(algo.getClass());
			algo.setExecution(execution);
			algo.setFileResolver(FileResolverFactory.create(DataStoreStrategy.LOCAL));
			algo.setDataStoreClient(DataStoreClientFactory.create(DataStoreStrategy.LOCAL));

			Path inputPath = Paths.get(inputPathOption);
			if (Files.isDirectory(inputPath)) {
				try {
					Iterator<Path> directoryStream = Files.newDirectoryStream(inputPath, "*.pdf").iterator();
					while (directoryStream.hasNext()) {
						InfolisFile fileToPost = new InfolisFile();
						fileToPost.setFileName(directoryStream.next().toString());
						fileToPost.setMediaType("application/pdf");
						algo.getDataStoreClient().post(InfolisFile.class, fileToPost);
						execution.getInputFiles().add(fileToPost.getUri());
					}
				} catch (IOException e) {
					log.error("Could not read '*.pdf' in directory {}.", inputPath);
					System.exit(1);
				}
			} else {
				execution.getInputFiles().add(inputPathOption.toString());
			}

			Path outputPath = Paths.get(inputPathOption);
			if (! Files.exists(outputPath)) {
				try {
					Files.createDirectories(outputPath);
				} catch (IOException e) {
					log.error("Output directory {} doesn't exist and can't be created.", outputPath);
					System.exit(1);
				}
			} else if (! Files.isDirectory(outputPath)) {
				log.error("Output directory {} is no directory.", outputPath);
				System.exit(1);
			}
			execution.setOutputDirectory(outputPath.toString());
			
			execution.setRemoveBib(removeBib);

			algo.run();
		}
	}

	public static void main(String[] args) {
		new OptionHandler().parse(args);
	}
}
