package io.github.infolis.util;

import java.io.IOException;
import java.util.StringTokenizer;


/**
 * Cleans messy text files (text automatically extracted from pdf documents).
 *
 * @author katarina.boland@gesis.org
 * @author farag.saad@gesis.org {@link TextCleaningUtils#removeLineBreaks(String)}
 * @version 2014-01-29
 */
public class TextCleaningUtils
{
	/**
	 * Removes control sequences that may have been inserted during automatic text extraction.
	 *
	 * @param text	the text to clean
	 * @return	the text without control sequences
	 */
	public static String removeControlSequences(String text)
	{
		return text.replaceAll("[^\\P{Cc}\\P{Cf}\\P{Co}\\P{Cs}\\P{Cn}\\s]", "");
	}

	/**
	 * Resolves hyphenation and re-assembles words at line breaks.
	 * Tries to not concatenate words connected by "-" coincidentally occurring at a line break.
	 *
	 * @param content input text to be processed
	 * @return input text without hyphenation at line-breaks
	 * @throws IOException
	 */
	public static String removeLineBreaks(String content) throws IOException {

		// start cleaning the text file
		StringTokenizer loop_content = new StringTokenizer(content, " ");
		String text_content = "", text_content1 = "", text_content2 = "";
		String parsed_content = "";
		while (loop_content.hasMoreTokens()) {

			text_content = loop_content.nextToken();
			int count = text_content.split("\\-", -1).length - 1; int line_break_pos=text_content.indexOf(System.getProperty("line.separator"));
			int Bindestrich =text_content.indexOf("-");

			// tackle the line break problem
			if (text_content.contains("¬")) {
				// replace the symbol "¬" with space
				text_content1 = text_content.replace("¬", " ");
				// remove the produced space by the first step and also remove
				// the line break to concatenate the 2 parts of the broken word
				text_content2 = text_content1.trim().replaceAll("\\s+", "");
				// start concatenate the two parts of the broken word and add
				// the line break in the correct place
				parsed_content = parsed_content + text_content2 + " "
						+ System.getProperty("line.separator");
			} else

			if (text_content.contains("-")
					&& text_content.contains(System
							.getProperty("line.separator")) && count <= 1
					&& !Character.isDigit(text_content.charAt(0))  && line_break_pos>Bindestrich) {
				// replace the symbol "-" with space
				text_content1 = text_content.replace('-', ' ');
				// remove the produced space by the first step and also remove
				// the line break to concatenate the 2 parts of the broken word
				text_content2 = text_content1.trim().replaceAll("\\s+", "");
				// start concatenate the two parts of the broken word and add
				// the line break in the correct place
				parsed_content = parsed_content + text_content2 + " "
						+ System.getProperty("line.separator");
			}
			else
			// case of more than one "-" and in same time with linebreak
			if (text_content.contains("-")
					&& text_content.contains(System
							.getProperty("line.separator")) && count > 1
					&& !Character.isDigit(text_content.charAt(0))  && line_break_pos>Bindestrich) {

				int k=text_content.lastIndexOf("-");
				text_content1 = text_content.substring(0,k)+' '+text_content.substring(k+1);
				// remove the produced space by the first step and also remove
				// the line break to concatenate the 2 parts of the broken word
				text_content2 = text_content1.trim().replaceAll("\\s+", "");
				// start concatenate the two parts of the broken word and add
				// the line break in the correct place
				parsed_content = parsed_content + text_content2 + " "
						+ System.getProperty("line.separator");
			}

			else
				// if no broken word just
				parsed_content = parsed_content + text_content + " ";

		}
		return parsed_content;
	}
}
