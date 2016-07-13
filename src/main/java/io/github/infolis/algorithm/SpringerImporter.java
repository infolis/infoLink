package io.github.infolis.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.net.MediaType;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.SerializationUtils;

/**
 * Class for importing documents in Springer's A++ format.
 * 
 * @author kata
 *
 */
public class SpringerImporter extends BaseAlgorithm {

	private static final Logger log = LoggerFactory.getLogger(SpringerImporter.class);
	
	public SpringerImporter(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
			FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	/**
	 * Removes all markup information and returns the plain text of the document.
	 * 
	 * @param springerFile
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private String getText(InputStream springerFile) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder(); 
		Document doc = db.parse(springerFile);
		NodeList articleContent = doc.getElementsByTagName("Body");
		return articleContent.item(0).getTextContent();
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
            InfolisFile inFile;
            try {
                inFile = getInputDataStoreClient().get(InfolisFile.class, inputFileURI);
            } catch (Exception e) {
                error(log, "Could not retrieve file " + inputFileURI + ": " + e.getMessage());
                getExecution().setStatus(ExecutionStatus.FAILED);
                return;
            }
            if (null == inFile) {
                error(log, "File was not registered with the data store: " + inputFileURI);
                getExecution().setStatus(ExecutionStatus.FAILED);
                return;
            }
            if (null == inFile.getMediaType() || !inFile.getMediaType().equals(MediaType.XML_UTF_8.toString())) {
                error(log, "File is not an XML: " + inputFileURI);
                error(log, "file type: \"{}\"", inFile.getMediaType());
                log.debug(MediaType.XML_UTF_8.toString());
                getExecution().setStatus(ExecutionStatus.FAILED);
                return;
            }
            debug(log, "Start extracting from {}", inFile);

            // TODO make configurable
            String outFileName = SerializationUtils.changeFileExtension(inFile.getFileName(), "txt");
            if (null != getExecution().getOutputDirectory()) {
                outFileName = SerializationUtils.changeBaseDir(outFileName, getExecution().getOutputDirectory());
            }
            InfolisFile outFile = new InfolisFile();
            outFile.setFileName(outFileName);
            outFile.setMediaType("text/plain");
            InputStream inStream = null;
            OutputStream outStream = null;
            
            inStream = getInputFileResolver().openInputStream(inFile);
            String text = null;
            try {
				text = getText(inStream);
			} catch (SAXException | ParserConfigurationException e) {
				warn(log, "Error parsing file: {}", e.getMessage());
			}
            
            outFile.setMd5(SerializationUtils.getHexMd5(text));
            outFile.setFileStatus("AVAILABLE");

            try {
            	outStream = getOutputFileResolver().openOutputStream(outFile);
                try {
                    IOUtils.write(text, outStream);
                    
                    updateProgress(counter, getExecution().getInputFiles().size());
                    outFile.setManifestsEntity(inFile.getManifestsEntity());
                    getOutputDataStoreClient().post(InfolisFile.class, outFile);
                    getExecution().getOutputFiles().add(outFile.getUri());
                } catch (IOException e) {
                    warn(log, "Error copying text to output stream: " + e);
                    throw e;
                } 
            } catch (IOException e) {
                warn(log, "Error opening output stream to text file: " + e);
                throw e;
            } finally {
            	if (null != outStream) outStream.close();
            	if (null != inStream) inStream.close();
            }
        }
        debug(log, "No of OutputFiles of this execution: {}", getExecution().getOutputFiles().size());
        getExecution().setStatus(ExecutionStatus.FINISHED);
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub
		
	}
	
}