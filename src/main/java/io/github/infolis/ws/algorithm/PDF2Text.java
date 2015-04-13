package io.github.infolis.ws.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

public class PDF2Text {
	
	private boolean removeBiblio = false;
	
	public PDF2Text(boolean removeBiblio) {
		this.removeBiblio = removeBiblio;
	}
	
	public void extract(InputStream inStream, OutputStream outStream) throws IOException {
		PDFTextStripper stripper = new PDFTextStripper();
		PDDocument pdfIn = PDDocument.load(inStream);
		// TODO run bibRemover.py if removeBiblio, need to keep individual pages in that case
		String asText = stripper.getText(pdfIn);
		IOUtils.write(asText, outStream);
	}
	
}