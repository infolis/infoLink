package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.InfolisFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
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

        Execution textExtractor = getExecution().createSubExecution(TextExtractor.class);
        textExtractor.setInputFiles(getExecution().getInputFiles());
        textExtractor.setInfolisFileTags(getExecution().getInfolisFileTags());
        textExtractor.instantiateAlgorithm(this).run();

        for (String fileURI : getExecution().getInputFiles()) {
            InfolisFile infoFile = getOutputDataStoreClient().get(InfolisFile.class, fileURI);
            Entity e = new Entity();

            for (String metaFile : getExecution().getMetaDataFiles()) {
                if (metaFile.contains(infoFile.getFileName().substring(infoFile.getFileName().lastIndexOf("/")).split("\\.")[0])) {

                    try {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document doc = db.parse(metaFile);
                    e.setIdentifier(doc.getElementsByTagName("identifier").item(0).getTextContent());
                    e.setAbstractText(doc.getElementsByTagName("dc:description").item(0).getTextContent());
                    e.setName(doc.getElementsByTagName("dc:title").item(0).getTextContent());
                    //TODO: unify the language? either EN or ENG?
                    e.setLanguage(doc.getElementsByTagName("dc:language").item(0).getTextContent());                    
                    //TODO: which URI to use? urn? hdl?                                       
                    e.setURL(doc.getElementsByTagName("dc:identifier").item(0).getTextContent());
                    
                    NodeList authors = doc.getElementsByTagName("dc:creator");
                    for(int i=0; i<authors.getLength(); i++) {
                        e.addAuthor(authors.item(i).getTextContent());
                    }
                    NodeList subjects = doc.getElementsByTagName("dc:subject");
                    for(int i=0; i<subjects.getLength(); i++) {
                        e.addSubject(subjects.item(i).getTextContent());
                    }                    
                    
                    }catch(Exception ex) {
                        //TODO: log
                    }
                }
            }
            //put the entity with the according md5 as URI fragment
            getOutputDataStoreClient().put(Entity.class, e, infoFile.getMd5());
        }
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
        if (null == exec.isTokenize()) {
            warn(log, "\"tokenize\" field unspecified. Defaulting to \"false\".");
            this.getExecution().setTokenize(false);
        }
    }

}
