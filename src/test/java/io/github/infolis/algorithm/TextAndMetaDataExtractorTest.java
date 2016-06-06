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

    @SuppressWarnings("unchecked")
    @Test
    public void testMetaExtraction() throws Exception {

        //TODO: why did we introduce tempFiles?         
        InfolisFile inFile = new InfolisFile();
        inFile.setFileName(tempFile.toString());        
        inFile.setOriginalName(filePath);
        inFile.setMd5(SerializationUtils.getHexMd5(pdfBytes));
        inFile.setMediaType("application/pdf");
        inFile.setFileStatus("AVAILABLE");
        
        writeFile(inFile);
           
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
        assertTrue(e.getAuthors().size()==1);
        assertTrue(e.getAbstractText().equals("A traditional way of thinking about the exchange rate (XR) regime and capital account openness has been framed in "
                + "terms of the 'impossible trinity' or 'trilemma', in which policymakers can only have 2 of 3 possible outcomes: open capital markets, monetary "
                + "independence and pegged XRs. This paper is an extension of Escude (A DSGE Model for a SOE with Systematic Interest and Foreign Exchange Policies "
                + "in Which Policymakers Exploit the Risk Premium for Stabilization Purposes, 2013), which focused on interest rate and XR policies, since it introduces"
                + " the third vertex of the 'trinity' in the form of taxes on private foreign debt. These affect the risk-adjusted uncovered interest parity equation and"
                + " hence influence the SOE's international financial flows. A useful way to illustrate the range of policy alternatives is to associate them with the"
                + " faces of a triangle. Each of 3 possible government intervention policies taken individually (in the domestic currency bond market, in the FX market,"
                + " and in the foreign currency bonds market) corresponds to one of the vertices of the triangle, each of the 3 possible pairs of intervention policies"
                + " corresponds to one of its 3 edges, and the 3 simultaneous intervention policies taken jointly correspond to its interior. This paper shows that this "
                + "interior, or 'possible trinity' is quite generally not only possible but optimal, since the CB obtains a lower loss when it implements a policy with"
                + " all three interventions."));
        assertTrue(e.getSubjects().size()==9);
        assertTrue(e.getIdentifier().equals("oai:econstor.eu:10419/100000"));
        System.out.println(e.getURL());
        assertTrue(e.getURL().equals("doi:10.5018/economics-ejournal.ja.2014-25"));
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
