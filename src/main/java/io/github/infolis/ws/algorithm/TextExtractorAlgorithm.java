package io.github.infolis.ws.algorithm;

import io.github.infolis.infolink.preprocessing.Cleaner;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.util.SerializationUtils;
import io.github.infolis.ws.client.FrontendClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kba
 */
public class TextExtractorAlgorithm extends BaseAlgorithm {

	public static final String PARAM_PDF_OUTPUT = "pdfOutput";
	public static final String PARAM_REMOVE_BIBLIOGRAPHY = "removeBibliography";
	public static final String PARAM_PDF_INPUT = "pdfInput";
	public static final List<String> cueWords = Arrays.asList("Literatur", "Literaturverzeichnis",
			"Literaturliste",
			"Bibliographie", "Bibliografie", "Quellen", "Quellenangaben", "Quellenverzeichnis",
			"Literaturangaben", "Bibliography",
			"References", "list of literature", "List of Literature", "List of literature",
			"list of references", "List of References",
			"List of references", "reference list", "Reference List", "Reference list");
	public static final Pattern patternNumeric = Pattern.compile("\\d+");
	public static final Pattern patternDecimal = Pattern.compile("\\d+\\.\\d+");

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
		try {// check whether the bibliography shpuld be reomoved
			if (getExecution().isRemoveBib()) {
				asText = extractTextAndRemoveBibliography(pdfIn);
			} else {
				asText = extractText(pdfIn);
			}
		} catch (Exception e) {
			getExecution().getLog().add("Error converting PDF to text.");
			return null;
		}

		InfolisFile outFile = new InfolisFile();
		outFile.setFileName(SerializationUtils.changeFileExtension(inFile.getFileName(), "txt"));
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
	 * @param pdfIn {@link PDDocument} to extract text from
	 * @return text of the PDF
	 * @throws IOException
	 */
	private String extractText(PDDocument pdfIn) throws IOException {
		String asText;
		asText = stripper.getText(pdfIn);

		if (null == asText) {
			throw new NullPointerException();
		}
		asText = Cleaner.removeControlSequences(asText);
		asText = Cleaner.removeLineBreaks(asText);
		return asText;
	}

	/**
	 * Compute the ratio of numbers on page: a high number of numbers is assumed
	 * to be typical for bibliographies as they contain many years, page numbers
	 * and dates.
	 * 
	 * @param pdfIn {@link PDDocument} to extract text from
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
				throw new NullPointerException();
			}
			// clean the page
			pageText = Cleaner.removeControlSequences(pageText);
			pageText = Cleaner.removeLineBreaks(pageText);

			int numNumbers = 0;
			int numDecimals = 0;
			int numChars = pageText.length();
			if (numChars == 0) {
				continue;
			}
			// determine the amount of numbers (numeric and decimal)
			Matcher matcherNumeric = patternNumeric.matcher(pageText);
			Matcher matcherDecimal = patternDecimal.matcher(pageText);
			while (matcherNumeric.find()) {
				numNumbers++;
			}
			while (matcherDecimal.find()) {
				numDecimals++;
			}
			boolean containsCueWord = false;
			for (String s : cueWords) {
				if (pageText.contains(s)) {
					containsCueWord = true;
					break;
				}
			}
			// use hasBibNumberRatio_d method from python scripts
			if (startedBib) {
				if (containsCueWord && ((numNumbers / numChars) >= 0.005)
						&& ((numNumbers / numChars) <= 0.1) && ((numDecimals / numChars) <= 0.004)) {
					startedBib = true;
					textWithoutBib += pageText;
				} else if (((numNumbers / numChars) >= 0.01) && ((numNumbers / numChars) <= 0.1)
						&& ((numDecimals / numChars) <= 0.004)) {
					textWithoutBib += pageText;
				}
			} else {
				if (((numNumbers / numChars) >= 0.008) && ((numNumbers / numChars) <= 0.1)
						&& ((numDecimals / numChars) <= 0.004)) {
					textWithoutBib += pageText;
				}
			}
		}
		return textWithoutBib;
	}

	@Override
	public void execute() {
		for (String inputFileURIString : getExecution().getInputFiles()) {
			log.debug(inputFileURIString);
			URI inputFileURI = URI.create(inputFileURIString);
			InfolisFile inputFile = FrontendClient.get(InfolisFile.class, inputFileURI);
			log.debug("Will extract now");
			InfolisFile outputFile = extract(inputFile);
			log.debug("LOG {}", getExecution().getLog());
			// FrontendClient.post(InfolisFile.class, outputFile);
			if (null == outputFile) {
				getExecution().getLog().add(
						"Conversion failed for input file " + inputFileURIString);
				log.debug(getExecution().getLog().toString());
				getExecution().setStatus(Execution.Status.FAILED);
			} else {
				getExecution().setStatus(Execution.Status.FINISHED);
				FrontendClient.post(InfolisFile.class, outputFile);
				getExecution().getOutputFiles().add(outputFile.getUri());
				log.debug("{}", getExecution().getOutputFiles());
			}
		}
	}

	@Override
	public void validate() {
		if (null == getFileResolver()) {
			throw new RuntimeException("Algorithm was not passed a FileResolver!");
		}
		if (null == getExecution().getInputFiles()) {
			throw new IllegalArgumentException("Required parameter 'pdfInput' is missing!");
		} else if (0 == getExecution().getInputFiles().size()) {
			throw new IllegalArgumentException("No values for parameter 'pdfInput'!");
		}
		if (null == getExecution().getOutputFiles()) {
			getExecution().setOutputFiles(new ArrayList<String>());
		}
	}
}
