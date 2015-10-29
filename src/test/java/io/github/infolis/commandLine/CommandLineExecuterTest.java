package io.github.infolis.commandLine;

import static org.junit.Assert.assertTrue;
import io.github.infolis.InfolisBaseTest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandLineExecuterTest extends InfolisBaseTest {

    private static final Logger log = LoggerFactory.getLogger(CommandLineExecuterTest.class);

    private String getResourcePath(String resName) throws URISyntaxException {
    	URL resource = CommandLineExecuterTest.class.getResource(resName);
    	log.debug("{}", resource);
        Path uri = Paths.get(resource.toURI());
		return uri.toString();
    }

    private Path mktempdir() throws IOException {
        return Files.createTempDirectory("infolis-test-" + UUID.randomUUID());
    }
    
    public CommandLineExecuterTest() {
        System.setProperty("testing", "true");
    }

    @Test
    public void test() throws Exception {
        Path outputBaseDir = mktempdir();
        String tag = "foo-bar";
        CommandLineExecuter.main(new String[] {
                "--json", getResourcePath("/commandLine/algoDesc.json"),
                "--pdf-dir", getResourcePath("/examples/minimal-pdf/"),
                "--text-dir", outputBaseDir.resolve("text").toString(),
                "--db-dir", outputBaseDir.resolve("db").toString(),
                "--log-level", "INFO",
                "--tag", tag,
        });
        Path expectedDump = outputBaseDir.resolve("db").resolve(tag + ".json");
        assertTrue("dump exists at " + expectedDump, Files.exists(expectedDump));
    }

    @Test
    public void testDouble() throws Exception {
        Path outputBaseDir = mktempdir();
        Path emptyInputDir = outputBaseDir.resolve("dummy-input");
        Files.createDirectories(emptyInputDir);
        CommandLineExecuter.main(new String[] {
        		"--json", getResourcePath("/commandLine/double.json"),
                "--pdf-dir", emptyInputDir.toString(),
                "--text-dir", outputBaseDir.resolve("text").toString(),
                "--db-dir", outputBaseDir.resolve("db").toString(),
                "--tag", "foo-bar"
        });
        FileUtils.forceDelete(outputBaseDir.toFile());
    }
    @Test
    public void testSearchTermPosition() throws Exception {
        Path outputBaseDir = mktempdir();
        Path emptyInputDir = outputBaseDir.resolve("dummy-input");
        Files.createDirectories(emptyInputDir);
        CommandLineExecuter.main(new String[] {
                "--json", getResourcePath("/commandLine/searchTermPositionCall.json"),
                "--pdf-dir", getResourcePath("/examples/minimal-pdf"),
                "--text-dir", outputBaseDir.resolve("text").toString(),
                "--db-dir", outputBaseDir.resolve("db").toString(),
                "--index-dir", outputBaseDir.resolve("index").toString(),
                "--tag", "foo-bar"
        });
        log.debug("OutputBase exists at " + outputBaseDir.toFile());
        log.debug("OutputBase exists at (toFile) " + outputBaseDir);

        FileUtils.deleteDirectory(outputBaseDir.toFile());
    }


}
