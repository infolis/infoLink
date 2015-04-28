/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.infolink.luceneIndexing.Indexer;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.InfolisFileUtils;
import io.github.infolis.util.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class FrequencyBasedBootstrappingTest {

    private final static DataStoreClient client = DataStoreClientFactory.local();
    private final static FileResolver fileResolver = FileResolverFactory.create(DataStoreStrategy.TEMPORARY);
    Logger log = LoggerFactory.getLogger(SearchTermPositionTest.class);

    List<String> pattern = new ArrayList<>();
    List<String> files = new ArrayList<>();
    String indexDir;
    String testCorpus = "testCorpus";

    private final static List<String> testStrings = Arrays.asList(
            "Please try to find the ALLBUS 2003 short text snippet that I provided to you.",
            "Please try to find the allbus in this short text snippet that I provided to you.",
            "Please try to find the .allbus. in this short text snippet that I provided to you.",
            "Please try to find the ALLBUS in this short text snippet that I provided to you.",
            "Please try to find the OECD in this short text snippet that I provided to you."
    );
    String testDocument1 = testCorpus + File.separator + "testDocument1";
    String testDocument2 = testCorpus + File.separator + "testDocument2";
    String testDocument3 = testCorpus + File.separator + "testDocument3";
    String testDocument4 = testCorpus + File.separator + "testDocument4";
    String testDocument5 = testCorpus + File.separator + "testDocument5";

    private final static List<String> terms = Arrays.asList("allbus");

    private final List<String> testFiles = new ArrayList<>();

    @Before
    public void createInputFiles() throws IOException {
        testFiles.clear();
        InfolisFileUtils.writeToFile(new File(testDocument1), "UTF-8", testStrings.get(0), false);
        InfolisFileUtils.writeToFile(new File(testDocument2), "UTF-8", testStrings.get(1), false);
        InfolisFileUtils.writeToFile(new File(testDocument3), "UTF-8", testStrings.get(2), false);
        InfolisFileUtils.writeToFile(new File(testDocument4), "UTF-8", testStrings.get(3), false);
        InfolisFileUtils.writeToFile(new File(testDocument5), "UTF-8", testStrings.get(4), false);
        
        for (String str : testStrings) {
    		String hexMd5 = SerializationUtils.getHexMd5(str);
    		InfolisFile file = new InfolisFile();
    		file.setMd5(hexMd5);
    		OutputStream outputStream = fileResolver.openOutputStream(file);
    		IOUtils.write(str, outputStream);
    		client.post(InfolisFile.class, file);
    		testFiles.add(file.getUri());
    	}
        
//        for (String str : testStrings) {
//            String hexMd5 = SerializationUtils.getHexMd5(str);
//            InfolisFile file = new InfolisFile();
//            file.setMd5(hexMd5);
//            OutputStream outputStream = fileResolver.openOutputStream(file);
//            IOUtils.write(str, outputStream);
//            client.post(InfolisFile.class, file);
//            testFiles.add(file.getUri());
//        }
        indexDir = Files.createTempDirectory("infolis-test-").toString();
        Indexer.main(new String[]{testCorpus, indexDir});
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File(indexDir));
    }

    @Test
    public void testBootstrapping() throws Exception {

        Execution execution = new Execution();
        execution.getTerms().addAll(terms);
        execution.setAlgorithm(FrequencyBasedBootstrapping.class);
        execution.getInputFiles().addAll(testFiles);
        execution.setSearchTerm(terms.get(0));
        execution.setSearchQuery("\""+terms.get(0)+"\"");
        execution.setThreshold(0.0);
        

        Algorithm algo = new FrequencyBasedBootstrapping();
        algo.setDataStoreClient(client);
        algo.setFileResolver(fileResolver);
        algo.setExecution(execution);
        algo.run();

        for(String s : execution.getStudyContexts()) {
            System.out.println("found: " +client.get(StudyContext.class, s));
            System.out.println("term: " +client.get(StudyContext.class, s).getTerm());
            System.out.println("pattern: " +client.get(StudyContext.class, s).getPattern());
        }
    }

}
