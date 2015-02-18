package preprocessing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.io.PrintStream;
import java.nio.file.Paths;

import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;


/**
 * Converts pdf documents to txt using the PdfBox library (https://pdfbox.apache.org/).
 * Text may be extracted for single pages or whole documents.
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 */
public class TextExtractor {

	String filename;
	PDDocument document;
	PDFTextStripper stripper;
	String encoding;
	
	/**
	 * Class constructor specifying the file to be converted and the encoding. 
	 * 
	 * @param filenameIn	name of the pdf file to be converted to txt
	 * @param encoding		character encoding for text extraction
	 * @throws IOException
	 */
	public TextExtractor(String filenameIn, String encoding) throws IOException
	{
		this.filename = filenameIn;
		this.document = PDDocument.loadNonSeq(new File(filenameIn), null);
		this.stripper = new PDFTextStripper(encoding);
		this.encoding = encoding; 
	}
	
	/**
	 * Calls the getText method of <emph>this stripper</emph> to extract text from <emph>this document</emph>.
	 * 
	 * @return	Returns the text content of <emph>this document</emph> or an empty string if an IOExeption occurred during extraction.
	 */
	public String getContent() 
	{
		try { return this.stripper.getText(this.document); }
		catch ( IOException ioe ) { ioe.printStackTrace(); return ""; }
	}
	
	/**
	 * Returns the number of pages of <emph>this document</emph>.
	 * 
	 * @return	the number of pages of <emph>this document</emph>
	 */
	public int getPageNum()
	{
		return this.document.getNumberOfPages();
	}
	
	/**
	 * Prints the contents of all pages of <emph>this document</emph>.
	 */
	public void printPages()
	{
		for (int i = 1; i <= this.getPageNum(); i++)
		{
			this.stripper.setStartPage(i);
			this.stripper.setEndPage(i);
			System.out.println(getContent());
		}
	}
	
	/**
	 * Saves the content of each page of <emph>this document</emph> as an individual text file if that text file does not 
	 * already exist.
	 * The name of the output file is constructed by replacing the suffix ".pdf" with "_" followed by the 
	 * page number followed by the suffix ".txt" and by replacing the input path with the desired output path.
	 * 
	 * @param inPath	absolute path of the pdf document to be converted
	 * @param outPath	absolute path of the resulting txt file
	 * @throws IOException
	 */
	public void writePages( String inPath, String outPath) throws IOException
	{
		for (int i = 1; i <= this.getPageNum(); i++)
		{
			this.stripper.setStartPage(i);
			this.stripper.setEndPage(i);
			String filenameOut = this.filename.replace(inPath, outPath).replace(".pdf", "_" + String.valueOf(i) + ".txt");
			System.out.println("writing " + filenameOut);
			File fileOut = new File(filenameOut);
			//convert only pdf files that have not been converted before
			if (!fileOut.exists()) { FileUtils.write(fileOut, getContent(), this.encoding, false); }
		}
		this.document.close();
	}
	
	/**
	 * Saves the content of <emph>this document</emph> as a text file.
	 * The name of the output file is constructed by replacing the suffix ".pdf" with ".txt" and by replacing 
	 * the input path with the desired output path.
	 * 
	 * @param inPath	absolute path of the pdf document to be converted
	 * @param outPath	absolute path of the resulting txt file
	 * @throws IOException
	 */
	public void writeDocument(String inPath, String outPath) throws IOException
	{
		String filenameOut = this.filename.replace(inPath, outPath).replace(".pdf", ".txt");
		System.out.println("writing " + filenameOut);
		File fileOut = new File(filenameOut);
		//convert only pdf files that have not been converted before
		if (!fileOut.exists()) { FileUtils.write(fileOut, getContent(), this.encoding, false); }
		this.document.close();
	}
	
	/**
	 * Recursively gathers all filenames of pdf documents contained in the specified directory and all subdirectories.
	 * 
	 * @param path		the parent directory of all pdf files to process
	 * @param filenames	list of all pdf files that have already been found or are known in advance
	 * @return			all filenames of pdf documents contained in path and all subdirectories
	 * @throws IOException
	 */
	public static ArrayList<String> getFilenames( File path, ArrayList<String> filenames ) throws IOException
	{
		for (final File fileEntry : path.listFiles())
		{
			if (fileEntry.isDirectory()) { getFilenames(fileEntry, filenames); }
			else 
			{ 
				String canonicalPath = fileEntry.getCanonicalPath();
				if (canonicalPath.endsWith(".pdf")) { filenames.add(canonicalPath); System.out.println("added " + canonicalPath);}
			}
		}
		return filenames;
	}
	
	/**
	 * Converts all pdf documents in the specified <emph>inputPath</emph> to txt and saves them in <emph>outputPath</emph>.
	 * A log file is saved in the specified <emph>outputPath</emph>.
	 * 
	 * @param inputPath		path of pdf documents to convert
	 * @param outputPath	path to save converted documents
	 * @param pageWise		specifies whether documents are to be converted page-wise (one txt file per page instead of one txt file per document)
	 */
	public static void convert(String inputPath, String outputPath, boolean pageWise) throws IOException, RuntimeException 
	{
		String pathIn = (Paths.get(new File(inputPath).getAbsolutePath()).normalize()).toString();
		String pathOut = (Paths.get(new File(outputPath).getAbsolutePath()).normalize()).toString();

		try { System.setErr(new PrintStream(new File(pathOut + File.separator + "_pdfBox.log"))); }
		catch (FileNotFoundException fnfe) { fnfe.printStackTrace(); }
		try {
			for (String pdfDocument : TextExtractor.getFilenames(new File(pathIn), new ArrayList<String>()))
			{
				System.out.println("Processing " + pdfDocument);
				try { 
					TextExtractor extractor = new TextExtractor(pdfDocument, "utf-8"); 
					if (pageWise) {	extractor.writePages( pathIn, pathOut ); }
					else { extractor.writeDocument(pathIn, pathOut); }
				}
				catch (IOException ioe)  { System.err.append(pdfDocument); ioe.printStackTrace(); }
				catch (RuntimeException re) { System.err.append(pdfDocument); re.printStackTrace();  }
			}
		}
		catch (IOException ioe) { ioe.printStackTrace(); }
	}
	
	/**
	 * Main method - calls <emph>OptionHandler</emph> to parse command line options and executes 
	 * <emph>main</emph> method accordingly.
	 * 
	 */
	public static void main(String []args) throws IOException
	{
		new OptionHandler().doMain(args);
	}
}

/**
* Class for processing command line options using args4j.
*
* @author katarina.boland@gesis.org
* @version 2014-01-27
*/
class OptionHandler {
		
	@Option(name="-i",usage="path of pdf documents to convert", metaVar = "INPUT_PATH")
    private String inputPath;
		
	@Option(name="-o",usage="path to save converted documents", metaVar = "OUTPUT_PATH")
    private String outputPath;
		
	@Option(name="-p",usage="set to convert document page-wise", metaVar = "PAGEWISE")
    private boolean pagewise = false;
		
	/**
     * Parses all command line options and calls <emph>main</emph> method accordingly.
     * 
     * @param args			
     * @throws IOException
     */
	public void doMain(String[] args) throws IOException 
	{
		CmdLineParser parser = new CmdLineParser(this); 

        try { parser.parseArgument(args); } 
        catch(CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java OptionHandler [options...] arguments...");
            parser.printUsage(System.err);
            System.err.println();
            return;
        }
        TextExtractor.convert(inputPath, outputPath, pagewise);
	}
}
