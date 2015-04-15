package io.github.infolis.ws.algorithm;

import io.github.infolis.infolink.preprocessing.Cleaner;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.util.SerializationUtils;
import io.github.infolis.ws.client.FrontendClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

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
	
	private static final Logger log  = LoggerFactory.getLogger(TextExtractorAlgorithm.class);

	public InfolisFile extract(InfolisFile inFile) {
		InputStream inStream = null;
		OutputStream outStream = null;
		PDFTextStripper stripper = null;
		PDDocument pdfIn;
		String asText = null;

		try {
			stripper = new PDFTextStripper();
		} catch (IOException e) {
		}
		try {
			inStream = getFileResolver().openInputStream(inFile);
		} catch (IOException e) {
			log.debug(inFile.getUri());
			log.debug(inFile.getMd5());
			getExecution().getLog().add("Error opening input stream.");
			return null;
		}
		try {
			pdfIn = PDDocument.load(inStream);
		} catch (IOException e) {
			getExecution().getLog().add("Error reading PDF from stream.");
			return null;
		}
		try {

			// TODO run bibRemover.py if removeBiblio, need to keep individual
			// pages in that case
			asText = stripper.getText(pdfIn);
			if (null == asText) {
				throw new NullPointerException();
			}
			asText = Cleaner.removeControlSequences(asText);
			asText = Cleaner.remove_line_break(asText);

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
	
	
    @Override
    public void execute() {
    	for (String inputFileURIString : getExecution().getInputFiles()) {
    		log.debug(inputFileURIString);
            URI inputFileURI = URI.create(inputFileURIString);
            InfolisFile inputFile = FrontendClient.get(InfolisFile.class, inputFileURI);
            
            InfolisFile outputFile = extract(inputFile);
            FrontendClient.post(InfolisFile.class, outputFile);
            if (null == outputFile) {
                getExecution().getLog().add("Conversion failed!");
                log.debug(getExecution().getLog().toString());
                return;
            }
            getExecution().getParamPdfOutput().add(outputFile.getUri());
    	}
    }

	@Override
	public void validate() {
		if (null == getFileResolver()) {
			throw new RuntimeException("Algorithm was not passed a FileResolver!");
		}
		if (null == getExecution().getInputFiles()) {
			throw new IllegalArgumentException("Required parameter 'pdfInput' is missing!");
		}
	}


}
 