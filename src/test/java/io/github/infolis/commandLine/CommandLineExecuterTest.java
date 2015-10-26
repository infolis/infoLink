
package io.github.infolis.commandLine;

import io.github.infolis.InfolisBaseTest;
import java.io.File;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.Ignore;
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
//    @Ignore
    @Test
    public void test() throws FileNotFoundException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IOException, URISyntaxException {
    	String tempdir = System.getProperty("java.io.tmpdir")+"/infolis-test-" + UUID.randomUUID();
        File f = new File(tempdir);
        f.mkdir();
    	CommandLineExecuter.parseJson(
    			Paths.get(getClass().getResource("/commandLine/algoDesc.json").toURI()),
    			Paths.get(tempdir));
    	log.debug("Dumped execution to {}", tempdir);
    }
}
