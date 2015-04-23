package io.github.infolis.algorithm;

import static org.junit.Assert.*;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.model.Execution;
import io.github.infolis.model.InfolisFile;
import io.github.infolis.model.InfolisPattern;
import io.github.infolis.util.SerializationUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
			"Please try to find the term in this 2003 short text snippet that I provided to you.",
			"Please try to find the _ in this short text snippet that I provided to you.",
			"Please try to find the .term. in this short text snippet that I provided to you."
			);
        private final static List<String> testPatterns = Arrays.asList(
                ".*Please try to find the (.*\\S+.*\\d*.*) short text snippet that I provided to you.*");
        

	private final List<InfolisFile> testFiles = new ArrayList<>();
        private final List<InfolisPattern> testInfolisPatterns = new ArrayList<>();

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
        for (String str : testPatterns) {
    		InfolisPattern p = new InfolisPattern();
                p.setPatternRegex(str);     
    		client.post(InfolisPattern.class, p);
                testInfolisPatterns.add(p);
    	}       
    }

    @Test
    public void testPatternApplier() throws Exception {
    	
    	Execution execution = new Execution();   
    	execution.getPattern().add(testInfolisPatterns.get(0).getUri());
    	execution.setAlgorithm(PatternApplier.class);
    	execution.getInputFiles().add(testFiles.get(0).getUri());

    	Algorithm algo = new PatternApplier();
    	algo.setDataStoreClient(client);
    	algo.setFileResolver(fileResolver);
    	algo.setExecution(execution);
    	algo.run();
    	
    	assertEquals(1, execution.getStudyContexts().size());
    }

}
