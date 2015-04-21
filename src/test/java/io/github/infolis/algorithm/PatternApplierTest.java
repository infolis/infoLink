/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.infolink.luceneIndexing.Indexer;
import io.github.infolis.model.Execution;
import io.github.infolis.model.StudyContext;
import io.github.infolis.util.InfolisFileUtils;
import io.github.infolis.util.SerializationUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class PatternApplierTest {

    private final static DataStoreClient client = DataStoreClientFactory.local();
    Logger log = LoggerFactory.getLogger(SearchTermPositionTest.class);

    List<String> pattern = new ArrayList<>();
    List<String> files = new ArrayList<>();
    
    String testString1 = "Please try to find the term in this short text snippet.";
    String testString2 = "Please try to find the _ in this short text snippet.";
    String testString3 = "Please try to find the .term. in this short text snippet.";

    @Test
    public void testPatternApplier() throws Exception {
        
        Pattern p = Pattern.compile("Please try to find the (\\S+?) short text snippet");
        Matcher m = p.matcher(testString1);
        System.out.println(m.matches());
        
        String con = m.group();
        String studyName = m.group(1).trim();
        System.out.println("con: " + con + " study: " + studyName);
        
        
//        createInputFiles();
//        pattern.add ("Please try to find the (\\S+?) short text snippet");
//        testContexts(files, pattern);
    }
    
    public void createInputFiles() {
        try {
            InfolisFileUtils.writeToFile(new File("1.txt"), "UTF-8", testString1, false);
            InfolisFileUtils.writeToFile(new File("2.txt"), "UTF-8", testString2, false);
            InfolisFileUtils.writeToFile(new File("3.txt"), "UTF-8", testString3, false);
                        
            //TODO:
            //files.add all files
            
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }
    }

    private List<StudyContext> testContexts(List<String> file, List<String> pattern) throws Exception {
        Execution exec = new Execution();
        exec.setAlgorithm(PatternApplier.class);
        exec.setPattern(pattern);
        exec.setInputFiles(file);
        exec.instantiateAlgorithm(DataStoreStrategy.LOCAL).run();
        log.debug(SerializationUtils.toJSON(exec));
        ArrayList<StudyContext> contextList = new ArrayList<>();
        for (String uri : exec.getStudyContexts()) {
            contextList.add(client.get(StudyContext.class, uri));
        }
        return contextList;
    }

}
