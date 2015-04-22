package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.util.SerializationUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class PatternApplierTest {

    private final static DataStoreClient client = DataStoreClientFactory.local();
    private final static FileResolver fileResolver = FileResolverFactory.create(DataStoreStrategy.TEMPORARY);
    Logger log = LoggerFactory.getLogger(SearchTermPositionTest.class);

    List<String> pattern = new ArrayList<>();
    List<String> files = new ArrayList<>();
    
	private final static List<String> testStrings = Arrays.asList(
			"Please try to find the term in this short text snippet.",
			"Please try to find the _ in this short text snippet.",
			"Please try to find the .term. in this short text snippet."
			);

	private final List<InfolisFile> testFiles = new ArrayList<>();

	@Before
    public void createInputFiles() throws IOException {
		testFiles.clear();
    	for (String str : testStrings) {
    		String hexMd5 = SerializationUtils.getHexMd5(str);
    		InfolisFile file = new InfolisFile();
    		file.setMd5(hexMd5);
    		OutputStream outputStream = fileResolver.openOutputStream(file);
    		IOUtils.write(str, outputStream);
    		client.post(InfolisFile.class, file);
    		testFiles.add(file);
    	}
    }

    @Test
    public void testPatternApplier() throws Exception {
    	
    	Execution execution = new Execution();
    	execution.setPattern(Arrays.asList(".*Please try to find the (.*\\S+.*) short text snippet.*"));
    	execution.setAlgorithm(PatternApplier.class);
    	execution.getInputFiles().add(testFiles.get(0).getUri());

    	Algorithm algo = new PatternApplier();
    	algo.setDataStoreClient(client);
    	algo.setFileResolver(fileResolver);
    	algo.setExecution(execution);
    	algo.run();
        
//        Matcher m = p.matcher(testStrings.get(0));
//        System.out.println(m.matches());
//        
//        String con = m.group();
//        String studyName = m.group(1).trim();
//        System.out.println("con: " + con + " study: " + studyName);
        
//        createInputFiles();
//        pattern.add ("Please try to find the (\\S+?) short text snippet");
//        testContexts(files, pattern);
    }
        
//        try {
//            InfolisFileUtils.writeToFile(new File("1.txt"), "UTF-8", testString1, false);
//            InfolisFileUtils.writeToFile(new File("2.txt"), "UTF-8", testString2, false);
//            InfolisFileUtils.writeToFile(new File("3.txt"), "UTF-8", testString3, false);
//                        
//            //TODO:
//            //files.add all files
//            
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//            System.exit(1);
//        }
//    }
    
//    private List<StudyContext> testContexts(List<String> file, List<String> pattern) throws Exception {
//        Execution exec = new Execution();
//        exec.setAlgorithm(PatternApplier.class);
//        exec.setPattern(pattern);
//        exec.setInputFiles(file);
//        exec.instantiateAlgorithm(DataStoreStrategy.LOCAL).run();
//        log.debug(SerializationUtils.toJSON(exec));
//        ArrayList<StudyContext> contextList = new ArrayList<>();
//        for (String uri : exec.getStudyContexts()) {
//            contextList.add(client.get(StudyContext.class, uri));
//        }
//        return contextList;
//    }

}
