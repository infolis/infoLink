package io.github.infolis.commandLine;

import static org.junit.Assert.assertTrue;
import io.github.infolis.InfolisBaseTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
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
                "--convert-to-text",
                "--log-level", "DEBUG",
                "--tag", tag,
        });
        Path expectedDump = outputBaseDir.resolve("db").resolve(tag + ".json");
        assertTrue("dump exists at " + expectedDump, Files.exists(expectedDump));
        FileUtils.deleteDirectory(outputBaseDir.toFile());
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
                "--convert-to-text",
                "--tag", "foo-bar"
        });
        FileUtils.forceDelete(outputBaseDir.toFile());
    }

    @Test
    public void testSearchCandidates() throws Exception {
        Path outputBaseDir = mktempdir();
        Path emptyInputDir = outputBaseDir.resolve("dummy-input");
        Files.createDirectories(emptyInputDir);
        CommandLineExecuter.main(new String[] {
                "--pdf-dir", getResourcePath("/examples/minimal-pdf"),
                "--text-dir", outputBaseDir.resolve("text").toString(),
                "--db-dir", outputBaseDir.resolve("db").toString(),
                "--index-dir", outputBaseDir.resolve("index").toString(),
                "--search-candidates",
                "--convert-to-text",
                "--tag", "foo-bar",
                "--queries-file", getResourcePath("/commandLine/queryTerms.csv")
        });
        log.debug("OutputBase exists at " + outputBaseDir.toFile());

        FileUtils.deleteDirectory(outputBaseDir.toFile());
    }


    @Test
    public void testConvertOnly() throws Exception {
        Path outputBaseDir = mktempdir();
        String tag = "foo-bar";
        CommandLineExecuter.main(new String[] {
                "--pdf-dir", getResourcePath("/examples/minimal-pdf/"),
                "--text-dir", outputBaseDir.resolve("text").toString(),
                "--db-dir", outputBaseDir.resolve("db").toString(),
                "--convert-to-text",
                "--tag", tag,
        });
        Path expectedDump = outputBaseDir.resolve("db").resolve(tag + ".json");
        assertTrue("dump exists at " + expectedDump, Files.exists(expectedDump));
        Path expectedText = outputBaseDir.resolve("text").resolve("4493.txt");
        assertTrue("text exists at " + expectedText, Files.exists(expectedText));
        FileUtils.deleteDirectory(outputBaseDir.toFile());
    }

    @Test
    public void testQueryServiceClass() throws Exception {
        Path outputBaseDir = mktempdir();
        String tag = "foo-bar";
        Path emptyInputDir = outputBaseDir.resolve("dummy-input");
        Files.createDirectories(emptyInputDir);
        CommandLineExecuter.main(new String[] {
        	"--json", getResourcePath("/commandLine/algoQueryServiceClasses.json"),
                "--pdf-dir", getResourcePath("/examples/minimal-pdf"),
                "--text-dir", outputBaseDir.resolve("text").toString(),
                "--db-dir", outputBaseDir.resolve("db").toString(),
                "--convert-to-text",
                "--tag", tag
        });
        FileUtils.forceDelete(outputBaseDir.toFile());
    }

    @Test
    public void testTextAndMetaDataExtractorClass() throws Exception {
        Path outputBaseDir = mktempdir();
        String tag = "foo-bar";
        Path emptyInputDir = outputBaseDir.resolve("dummy-input");
        Files.createDirectories(emptyInputDir);
        CommandLineExecuter.main(new String[] {
        	"--json", getResourcePath("/commandLine/textAndMetaDataCall.json"),
                "--pdf-dir", getResourcePath("/examples/minimal-pdf"),
                "--text-dir", outputBaseDir.resolve("text").toString(),
                "--db-dir", outputBaseDir.resolve("db").toString(),
                "--meta-dir", getResourcePath("/metaData"),
                "--convert-to-text",
                "--tag", tag
        });
        FileUtils.forceDelete(outputBaseDir.toFile());
    }
    
}
