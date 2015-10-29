
package io.github.infolis.commandLine;

import io.github.infolis.InfolisBaseTest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class CommandLineExecuterTest extends InfolisBaseTest {
    
	private static final Logger log = LoggerFactory.getLogger(CommandLineExecuterTest.class);
    
    //TODO: paths in the JSON are absolute like the inputFiles
    @Test
    public void test() throws FileNotFoundException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IOException, URISyntaxException {
    	Path tempdir = Files.createTempDirectory("infolis-test-" + UUID.randomUUID());
    	Path pdfDir = Paths.get(getClass().getResource("/examples/pdfs").toURI());

    	Path jsonin = Paths.get(getClass().getResource("/commandLine/algoDesc.json").toURI());
    	Path tempjson = Paths.get(System.getProperty("java.io.tmpdir")+"/infolis-test-" + UUID.randomUUID() + ".json");

    	String jsonString = IOUtils.toString(Files.newInputStream(jsonin));
    	jsonString = jsonString.replace("INPUT_FILES_PATH", pdfDir.toString());
        if(System.getProperty("os.name").contains("Windows")) {
            jsonString = jsonString.replace("\\","\\\\");
        }
        
    	IOUtils.write(jsonString, Files.newOutputStream(tempjson));
    	log.debug(jsonString);
    	CommandLineExecuter.parseJson(tempjson, tempdir);
    	log.debug("Dumped execution to {}", tempdir);
    }
    
    @Test
    public void testSearchTermPositionCall() throws FileNotFoundException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IOException, URISyntaxException {
    	Path tempdir = Files.createTempDirectory("infolis-test-" + UUID.randomUUID());
    	Path pdfDir = Paths.get(getClass().getResource("/examples/pdfs").toURI());
    	Path jsonin = Paths.get(getClass().getResource("/commandLine/searchTermPositionCall.json").toURI());
    	Path tempjson = Paths.get(System.getProperty("java.io.tmpdir")+"/infolis-test-" + UUID.randomUUID() + ".json");
    	String jsonString = IOUtils.toString(Files.newInputStream(jsonin));
    	jsonString = jsonString.replace("INPUT_FILES_PATH", pdfDir.toString());
        if(System.getProperty("os.name").contains("Windows")) {
            jsonString = jsonString.replace("\\","\\\\");
        }
    	IOUtils.write(jsonString, Files.newOutputStream(tempjson));
    	log.debug(jsonString);
    	CommandLineExecuter.parseJson(tempjson, tempdir);
    	log.debug("Dumped execution to {}", tempdir);
    }
    
    @Test
    public void testDouble() throws FileNotFoundException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IOException, URISyntaxException {
    	Path tempdir = Files.createTempDirectory("infolis-test-" + UUID.randomUUID());
    	Path pdfDir = Paths.get(getClass().getResource("/examples/pdfs").toURI());

    	Path jsonin = Paths.get(getClass().getResource("/commandLine/double.json").toURI());
    	Path tempjson = Paths.get(System.getProperty("java.io.tmpdir")+"/infolis-test-" + UUID.randomUUID() + ".json");

    	String jsonString = IOUtils.toString(Files.newInputStream(jsonin));
    	jsonString = jsonString.replace("INPUT_FILES_PATH", pdfDir.toString());
    	IOUtils.write(jsonString, Files.newOutputStream(tempjson));
    	log.debug(jsonString);
    	CommandLineExecuter.parseJson(tempjson, tempdir);
    	log.debug("Dumped execution to {}", tempdir);
    }
}
