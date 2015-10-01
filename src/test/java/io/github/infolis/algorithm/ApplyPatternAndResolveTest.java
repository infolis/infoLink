package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.infolink.datasetMatcher.HTMLQueryService;
import io.github.infolis.infolink.datasetMatcher.QueryService;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.util.SerializationUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the ApplyPatternAndResolve algorithm.
 * 
 * @author domi
 */
public class ApplyPatternAndResolveTest extends InfolisBaseTest {

    /**
     * Applies a given set of pattern (loaded from a file) and resolves the
     * references.
     *
     * @throws IOException
     */
    @Test
    public void applyPatternAndResolveRefs() throws IOException {

        File txtDir = new File(getClass().getResource("/examples/txts").getFile());
        File patternFile = new File(getClass().getResource("/examples/pattern.txt").getFile());

        //post all improtant stuff
        List<String> pattern = postPattern(patternFile);
        List<String> txt = postTxtFiles(txtDir);
        List<String> qServices = postQueryServices();

        Execution e = new Execution();
        e.setAlgorithm(ApplyPatternAndResolve.class);
        e.setPatternUris(pattern);
        e.setInputFiles(txt);
        e.setQueryServices(qServices);
        dataStoreClient.post(Execution.class, e);
        e.instantiateAlgorithm(dataStoreClient, fileResolver).run();

        List<EntityLink> createdLinks = dataStoreClient.get(EntityLink.class, e.getLinks());
        //check the amount of created links
        //TODO might change?
        Assert.assertEquals(22, createdLinks.size());

        for (EntityLink el : createdLinks) {
            //TODO any nice tests?
            if (el.getReferenceEntity().getName().equals("Flash Eurobarometer 35")) {
                
            }
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

    public List<String> postPattern(File pattern) throws IOException {
        BufferedReader read = new BufferedReader(new FileReader(pattern));
        String line = read.readLine();
        List<String> postedPattern = new ArrayList<>();
        while (line != null) {
            InfolisPattern p = new InfolisPattern(line);
            dataStoreClient.post(InfolisPattern.class, p);
            postedPattern.add(p.getUri());
            line = read.readLine();
        }
        return postedPattern;
    }

    public List<String> postQueryServices() throws IOException {
        List<String> postedQueryServices = new ArrayList<>();
        QueryService p1 = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show", 0.5);
        dataStoreClient.post(QueryService.class, p1);
        postedQueryServices.add(p1.getUri());
        return postedQueryServices;
    }

}
