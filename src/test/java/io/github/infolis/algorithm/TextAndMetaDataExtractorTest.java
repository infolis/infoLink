package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.SerializationUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class TextAndMetaDataExtractorTest extends InfolisBaseTest {

    Logger log = LoggerFactory.getLogger(TextExtractorTest.class);
    private byte[] pdfBytes;
    Path tempFile;

    @Before
    public void setUp() throws IOException {
        dataStoreClient.clear();
        pdfBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/trivial.pdf"));
        tempFile = Files.createTempFile("infolis-", ".pdf");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnknownMediaType() throws Exception {
        InfolisFile inFile = new InfolisFile();
        inFile.setFileName(tempFile.toString());
        System.out.println(inFile.getFileName());
        inFile.setMd5(SerializationUtils.getHexMd5(pdfBytes));
        System.out.println("md5 :" +inFile.getMd5());
        inFile.setMediaType("application/pdf");
        inFile.setFileStatus("AVAILABLE");
        writeFile(inFile);

        Execution execution = new Execution();
        execution.getInputFiles().add(inFile.getUri());
        execution.getInfolisFileTags().add("domi");
        execution.setAlgorithm(TextAndMetaDataExtractor.class);
        dataStoreClient.post(Execution.class, execution);
        Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
        algo.run();
        
        //TODO: can we get the getEndpointForClass? then we don't need something hard coded like entity_
        //TODO: md5 from the original file (PDF) or from the text file?
        Entity e = dataStoreClient.get(Entity.class, "entity_"+inFile.getMd5());
        assertTrue(e.getUri().equals("entity_"+inFile.getMd5()));
        
    }

    private void writeFile(InfolisFile inFile) {
        dataStoreClient.post(InfolisFile.class, inFile);
        try {
            OutputStream os = fileResolver.openOutputStream(inFile);
            IOUtils.write(pdfBytes, os);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
