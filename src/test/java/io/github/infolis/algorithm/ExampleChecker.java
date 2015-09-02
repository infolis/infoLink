/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.Publication;
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

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author domi
 */
@Ignore
public class ExampleChecker extends InfolisBaseTest {

//    FileResolver fileResolver = FileResolverFactory.global();
//    DataStoreClient dataStoreClient = DataStoreClientFactory.global();

    @Test
    public void checkExamples() throws IOException {

        File pdfDir = new File(getClass().getResource("/examples/pdfs").getFile());
        File txtDir = new File(getClass().getResource("/examples/txts").getFile());
        File patternFile = new File(getClass().getResource("/examples/pattern.txt").getFile());
        
        learn(pdf2txt(pdfDir));
        //searchSeed("ALLBUS",pdf2txt(pdfDir));
        //searchPattern(learn(pdf2txt(pdfDir)), pdf2txt(pdfDir));
        
        List<String> pattern = postPattern(patternFile);
        List<String> txt = postTxtFiles(txtDir);
        List<String> contexts = searchPattern(pattern, txt);
        ArrayList<TextualReference> contextList = new ArrayList<>();
        for (String uri : contexts) {
            contextList.add(dataStoreClient.get(TextualReference.class, (uri)));
        }
        for (TextualReference sc : contextList) {
            System.out.println("context: " + sc.toString());
            printFileNameOfContext(sc);
            System.out.println("study: " + sc.getTerm());
        }
        
    }

protected void printFileNameOfContext(TextualReference sc) throws BadRequestException, ProcessingException {
	String fileUri = sc.getFile();
	InfolisFile file = dataStoreClient.get(InfolisFile.class, fileUri);
	System.out.println("file: " + file.getFileName());
}
    
    public List<String> postPattern(File pattern) throws IOException {
        BufferedReader read = new BufferedReader(new FileReader(pattern));
        String line = read.readLine();
        List<String> postedPattern = new ArrayList<>();
        while(line !=null) {
            InfolisPattern p = new InfolisPattern(line);
            dataStoreClient.post(InfolisPattern.class, p);
            postedPattern.add(p.getUri());
            line = read.readLine();
        }
        return postedPattern;
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
    
    public List<String> searchPattern(List<String> pattern, List<String> input) {
        Execution search = new Execution();
        search.setAlgorithm(PatternApplier.class);
        search.setPattern(pattern);
        search.setInputFiles(input);
        dataStoreClient.post(Execution.class, search);
        Algorithm algo = search.instantiateAlgorithm(dataStoreClient, fileResolver);
        try {
			algo.run();
		} catch (Exception e) {
			e.printStackTrace();
			throw(e);
		}
        
        ArrayList<TextualReference> contextList = new ArrayList<>();
        for (String uri : search.getStudyContexts()) {
            contextList.add(dataStoreClient.get(TextualReference.class, uri));
        }
        for (TextualReference sc : contextList) {
            System.out.println("context: " + sc.toString());
        }
        
        return search.getStudyContexts();
    }

    public List<String> searchSeed(String seed, List<String> input) {
        Execution search = new Execution();
        search.setAlgorithm(SearchTermPosition.class);
        search.setSearchTerm(seed);
        search.setSearchQuery(seed);
        search.setInputFiles(input);
        dataStoreClient.post(Execution.class, search);
        Algorithm algo = search.instantiateAlgorithm(dataStoreClient, fileResolver);
        algo.run();
        
        ArrayList<TextualReference> contextList = new ArrayList<>();
        for (String uri : search.getStudyContexts()) {
            contextList.add(dataStoreClient.get(TextualReference.class, uri));
        }
        for (TextualReference sc : contextList) {
            System.out.println("context: " + sc.toString());
        }
        
        return search.getStudyContexts();
    }

    public List<String> pdf2txt(File dir) throws IOException {
        Execution execution = new Execution();

        dataStoreClient.post(Execution.class, execution);
    
        for (File f : dir.listFiles()) {

            Path tempFile = Files.createTempFile("infolis-", ".pdf");
            InfolisFile inFile = new InfolisFile();

            FileInputStream inputStream = new FileInputStream(f.getAbsolutePath());

            int numberBytes = inputStream.available();
            byte pdfBytes[] = new byte[numberBytes];
            inputStream.read(pdfBytes);

            IOUtils.write(pdfBytes, Files.newOutputStream(tempFile));

            inFile.setFileName(tempFile.toString());
            inFile.setMd5(SerializationUtils.getHexMd5(pdfBytes));
            inFile.setMediaType("application/pdf");
            inFile.setFileStatus("AVAILABLE");

            try {
                OutputStream os = fileResolver.openOutputStream(inFile);
                IOUtils.write(pdfBytes, os);
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            dataStoreClient.post(InfolisFile.class, inFile);
            execution.getInputFiles().add(inFile.getUri());

        }
        execution.setAlgorithm(TextExtractorAlgorithm.class);

        Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, fileResolver);
        algo.run();
        return execution.getOutputFiles();
    }

    public List<String> learn(List<String> input) {
        Execution bootstrapping = new Execution();
        bootstrapping.setAlgorithm(FrequencyBasedBootstrapping.class);

        bootstrapping.getTerms().add("ALLBUS");
        bootstrapping.setInputFiles(input);
        bootstrapping.setSearchTerm("ALLBUS");
        bootstrapping.setMaxIterations(4);
        bootstrapping.setThreshold(0.1);
        bootstrapping.setBootstrapStrategy(Execution.Strategy.mergeAll);
        dataStoreClient.post(Execution.class, bootstrapping);

        Algorithm algo3 = bootstrapping.instantiateAlgorithm(dataStoreClient, fileResolver);
        algo3.run();

        ArrayList<InfolisPattern> patternList = new ArrayList<>();
        for (String uri : bootstrapping.getPattern()) {
            patternList.add(dataStoreClient.get(InfolisPattern.class, uri));
        }

        ArrayList<TextualReference> contextList = new ArrayList<>();
        for (String uri : bootstrapping.getStudyContexts()) {
            contextList.add(dataStoreClient.get(TextualReference.class, uri));
        }
        for (TextualReference sc : contextList) {
            System.out.println("context: " + sc.toString());
            printFileNameOfContext(sc);
        }

        for (InfolisPattern p : patternList) {
            System.out.println("pattern: " + p.getPatternRegex());
        }

        for (String uri : bootstrapping.getStudies()) {
            System.out.println("study: " + uri);
        }
        return bootstrapping.getPattern();
    }

}
