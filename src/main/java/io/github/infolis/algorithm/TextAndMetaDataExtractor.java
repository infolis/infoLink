package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.InfolisFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            //TODO automatically parse metadata, e.g. based on the fileName
            List<String> authors = new ArrayList<>();
            authors.add("abc.def");
            e.setAuthors(authors);
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
        if (null == exec.isTokenize()) {
            warn(log, "\"tokenize\" field unspecified. Defaulting to \"false\".");
            this.getExecution().setTokenize(false);
        }
    }

}
