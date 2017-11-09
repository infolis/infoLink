package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.InfolisFile;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Algorithm to extract the meta data of the according publications.
 * If necessary, the PDFs are first converted to text. 
 * All information found in the meta data files like title, author etc.
 * are stored in the entity and the entity gets as URI the MD5-hash
 * of the underlying Infolis file.
 *
 * @author domi
 */
public class TextAndMetaDataExtractor extends BaseAlgorithm {

    public TextAndMetaDataExtractor(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(TextAndMetaDataExtractor.class);

    @Override
    public void execute() throws IOException {
        int counter = 0;
        for (String fileURI : getExecution().getInputFiles()) {
            counter++;
            InfolisFile infoFile = getOutputDataStoreClient().get(InfolisFile.class, fileURI);

            //extract the text from the pdf if pdfs are given
            if (null == infoFile.getMediaType() || !infoFile.getMediaType().equals("text/plain")) {
                Execution textExtractor = getExecution().createSubExecution(TextExtractor.class);
                textExtractor.setInputFiles(getExecution().getInputFiles());
                textExtractor.setInfolisFileTags(getExecution().getInfolisFileTags());
                textExtractor.instantiateAlgorithm(this).run();
            }
            Entity e = getOutputDataStoreClient().get(Entity.class, infoFile.getManifestsEntity());

            for (String metaFile : getExecution().getMetaDataFiles()) {
                Path p = null;
                if (null == infoFile.getOriginalName()) infoFile.setOriginalName(infoFile.getFileName());
                //problems with leading slashs if using Windows...
                if (infoFile.getOriginalName().startsWith("/")) {
                    p = Paths.get(infoFile.getOriginalName().substring(1));
                } else {
                    p = Paths.get(infoFile.getOriginalName());
                }
                String fileName = p.getFileName().toString().split("\\.")[0];
                if (metaFile.replaceAll(".*/", "").replace(".xml","").equals(fileName.replaceAll(".*/","").replace(".txt", "").replace(".pdf",""))) {
                    try {
                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder db = dbf.newDocumentBuilder();
                        Document doc = db.parse(metaFile);
                        // identifier here describes metadata record, not the publication. 
                        // thus, ignore
                        /*try {
                        	e.addIdentifier(doc.getElementsByTagName("identifier").item(0).getTextContent());
                        } catch (NullPointerException npe) {
                        	;
                        }*/
                        
                        try {
                        	e.setAbstractText(doc.getElementsByTagName("dc:description").item(0).getTextContent());
                        } catch (NullPointerException npe) {
                        	warn(log, "metadata file '{}' does not contain field 'dc:description'", metaFile);
                        }
                        
                        try {
                        	e.setName(doc.getElementsByTagName("dc:title").item(0).getTextContent());
                        } catch (NullPointerException npe) {
                        	warn(log, "metadata file '{}' does not contain field 'dc:title'", metaFile);
                        }
                        
                        //determine the language and set it to a uniform abbreviation
                        //TODO: any other abbreviations in the data?
                        try {
	                        String lang = doc.getElementsByTagName("dc:language").item(0).getTextContent();
	                        if(lang.equals("eng") || lang.equals("en")) {
	                            e.setLanguage("en");
	                        }
	                        else if(lang.equals("deu") || lang.equals("de")) {
	                            e.setLanguage("de");
	                        }
                        } catch (NullPointerException npe) {
                        	warn(log, "metadata file '{}' does not contain field 'dc:language'", metaFile);
                        }

                        try {
	                        NodeList ids = doc.getElementsByTagName("dc:identifier");
	                        for (int i = 0; i < ids.getLength(); i++) {
	                            e.addIdentifier(ids.item(i).getTextContent());
	                        }
                        } catch (NullPointerException npe) {
                        	warn(log, "metadata file '{}' does not contain field 'dc:identifier'", metaFile);
                        }
                        
                        try {
	                        NodeList authors = doc.getElementsByTagName("dc:creator");
	                        for (int i = 0; i < authors.getLength(); i++) {
	                            e.addAuthor(authors.item(i).getTextContent());
	                        }
                        } catch (NullPointerException npe) {
                        	warn(log, "metadata file '{}' does not contain field 'dc:creator'", metaFile);
                        }
                        
                        try {
	                        NodeList subjects = doc.getElementsByTagName("dc:subject");
	                        for (int i = 0; i < subjects.getLength(); i++) {
	                            e.addSubject(subjects.item(i).getTextContent());
	                        }
                        } catch (NullPointerException npe) {
                        	warn(log, "metadata file '{}' does not contain field 'dc:subject'", metaFile);
                        }
                        updateProgress(counter, getExecution().getInputFiles().size());

                    } catch (SAXException | ParserConfigurationException ex) {
                        error(log, "File \"{}\" could not be parsed!", metaFile);
                        //should it fail if one file could not be parsed?
                        //getExecution().setStatus(ExecutionStatus.FAILED);
                    }
                }
            }
            //put the entity with the new data
            getOutputDataStoreClient().put(Entity.class, e, e.getUri());
        }

        getExecution().setStatus(ExecutionStatus.FINISHED);
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        Execution exec = this.getExecution();
        if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty())
                && (null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())) {
            throw new IllegalArgumentException("Must set at least one inputFile!");
        }
        if ((null == exec.getMetaDataFiles() || exec.getMetaDataFiles().isEmpty())) {
            throw new IllegalArgumentException("Must set at least one metadata file to the according input file!");
        }
    }

}
