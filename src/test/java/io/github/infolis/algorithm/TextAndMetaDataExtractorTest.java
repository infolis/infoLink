package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.SerializationUtils;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
    String filePath;
    String metaDataFile;

    @Before
    public void setUp() throws IOException {
        dataStoreClient.clear();
        pdfBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/trivial.pdf"));
        tempFile = Files.createTempFile("infolis-", ".pdf");
        filePath = this.getClass().getResource("/trivial.pdf").getFile();
        metaDataFile = this.getClass().getResource("/metaData/trivial.xml").getFile();
    }
    private static final boolean IS_WINDOWS = System.getProperty( "os.name" ).contains( "indow" );

    @SuppressWarnings("unchecked")
    @Test
    public void testUnknownMediaType() throws Exception {
        
        //TODO: why did we introduce tempFiles?         
        //if temp files are introduced, we lose the name of the publication
        //TODO: possible to pass a map<pub,metadata> to the execution?
        InfolisFile inFile = new InfolisFile();
        String testFilename = this.getClass().getResource("/trivial.pdf").getFile();
        //String osAppropriatePath = IS_WINDOWS ? testFilename.substring(1) : testFilename;
        System.out.println(testFilename);
        //System.out.println(osAppropriatePath);   
        String text = FileUtils.readFileToString(new File(testFilename));        
        inFile.setFileName(testFilename);
        inFile.setMd5(SerializationUtils.getHexMd5(text));        
        //TODO: does not work with application/pdf! (could be some windows specific thing...)
        //java.nio.file.InvalidPathException: Illegal char <:> at index 2: /C:/Users/domi/Infolis21.08/infoLink/build/resources/test/trivial.txt 
        //inFile.setMediaType("application/pdf");
        inFile.setMediaType("text/xml; charset=utf-8");
        inFile.setFileStatus("AVAILABLE");

        writeFile(inFile, text);
        
        Execution execution = new Execution();
        execution.getInputFiles().add(inFile.getUri());
        execution.getMetaDataFiles().add(metaDataFile);
        execution.getInfolisFileTags().add("domi");
        execution.setAlgorithm(TextAndMetaDataExtractor.class);
        dataStoreClient.post(Execution.class, execution);
        Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
        algo.run();

        Entity e = dataStoreClient.get(Entity.class, inFile.getMd5());
        assertTrue(e.getUri().equals(inFile.getMd5()));
        assertTrue(e.getName().equals("The possible trinity: Optimal interest rate, exchange rate, and taxes on capital flows in a DSGE model for a small open economy"));
    }

    private void writeFile(InfolisFile inFile, String text) {
        try {
            OutputStream os = fileResolver.openOutputStream(inFile);
            IOUtils.write(text, os);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        dataStoreClient.post(InfolisFile.class, inFile);
    }
}
