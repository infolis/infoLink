package io.github.infolis.algorithm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.resolve.DaraHTMLQueryService;
import io.github.infolis.resolve.QueryService;
import io.github.infolis.util.SerializationUtils;

/**
 * Tests for the ApplyPatternAndResolve algorithm.
 *
 * @author kata
 * @author domi
 */
public class ApplyPatternAndResolveTest extends InfolisBaseTest {

	private static final Logger log = LoggerFactory.getLogger(ApplyPatternAndResolve.class);
    /**
     * Applies a given set of pattern (loaded from a file) and resolves the
     * references.
     *
     * @throws IOException
     */
    @Test
    public void applyPatternAndResolveRefs() throws IOException {

        File txtDir = new File(getClass().getResource("/examples/minimal-txt").getFile());

        InfolisPattern infolisPattern = new InfolisPattern();
        infolisPattern.setPatternRegex(".*?Datenbasis: (\\S*?\\s?\\S+?\\s?\\S+?\\s?\\S+?\\s?\\S*?), eigene Berechnung.*?");
        infolisPattern.setLuceneQuery("Datenbasis * eigene Berechnung");
        HashSet<String> tags = new HashSet<String>();
        tags.add("test");
        infolisPattern.setTags(tags);

        //post all important stuff
        dataStoreClient.post(InfolisPattern.class, infolisPattern);
        List<String> txt = postTxtFiles(txtDir);
        List<String> qServices = postQueryServices();

        Execution e = new Execution();
        e.getInfolisPatternTags().add("test");
        e.setAlgorithm(ApplyPatternAndResolve.class);
        e.setInputFiles(txt);
        e.setQueryServices(qServices);
        e.setSearchResultRankerClass(BestMatchRanker.class);
        dataStoreClient.post(Execution.class, e);
        e.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        for (String ref : e.getTextualReferences()) {
        	log.debug(ref);
        }

        List<EntityLink> createdLinks = dataStoreClient.get(EntityLink.class, e.getLinks());
        //check the amount of created links
        //TODO might change?
//        Assert.assertEquals(22, createdLinks.size());

        for (EntityLink el : createdLinks) {
            //TODO any nice tests?
//            if (el.getToEntity().getName().equals("Flash Eurobarometer 35")) {
//
//            }
        }
    }

    public List<String> postTxtFiles(File dir) throws IOException {
        List<String> txtFiles = new ArrayList<>();
        for (File f : dir.listFiles()) {
            Path tempFile = Files.createTempFile("infolis-", ".txt");
            InfolisFile inFile = new InfolisFile();
            FileInputStream inputStream = new FileInputStream(f.getAbsolutePath());
            int numberBytes = inputStream.available();
            byte pdfBytes[] = new byte[numberBytes];
            inputStream.read(pdfBytes);
            inputStream.close();
            IOUtils.write(pdfBytes, Files.newOutputStream(tempFile));
            inFile.setFileName(tempFile.toString());
            inFile.setMd5(SerializationUtils.getHexMd5(pdfBytes));
            inFile.setMediaType("text/plain");
            inFile.setFileStatus("AVAILABLE");
            try {
                OutputStream os = fileResolver.openOutputStream(inFile);
                IOUtils.write(pdfBytes, os);
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            dataStoreClient.post(InfolisFile.class, inFile);
            txtFiles.add(inFile.getUri());
        }
        return txtFiles;
    }

    public List<String> postQueryServices() throws IOException {
        List<String> postedQueryServices = new ArrayList<>();
        QueryService p1 = new DaraHTMLQueryService();
        p1.setMaxNumber(10);
        dataStoreClient.post(QueryService.class, p1);
        postedQueryServices.add(p1.getUri());
        return postedQueryServices;
    }

}
