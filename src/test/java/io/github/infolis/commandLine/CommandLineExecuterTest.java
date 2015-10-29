package io.github.infolis.commandLine;

import io.github.infolis.InfolisBaseTest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
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

    private String getResourcePath(String resName) throws URISyntaxException {
        return Paths.get(getClass().getResource(resName).toURI()).toString();
    }

    private Path mktempdir() throws IOException {
        return Files.createTempDirectory("infolis-test-" + UUID.randomUUID());
    }

    @Test
    public void test() throws Exception {

        Path outputBaseDir = mktempdir();
        CommandLineExecuter.main(new String[] {
                "--json", getResourcePath("/commandLine/algoDesc.json"),
                "--input-dir", getResourcePath("/examples/pdfs"),
                "--text-dir", outputBaseDir.resolve("text").toString(),
                "--db-dir", outputBaseDir.resolve("db").toString(),
        });
        FileUtils.forceDelete(outputBaseDir.toFile());
    }

    @Test
    public void testDouble() throws Exception {
        Path outputBaseDir = mktempdir();
        Path emptyInputDir = outputBaseDir.resolve("dummy-input");
        Files.createDirectories(emptyInputDir);
        CommandLineExecuter.main(new String[] {
                "--json", getResourcePath("/commandLine/double.json"),
                "--input-dir", emptyInputDir.toString(),
                "--text-dir", outputBaseDir.resolve("text").toString(),
                "--db-dir", outputBaseDir.resolve("db").toString(),
        });
        FileUtils.forceDelete(outputBaseDir.toFile());
    }
}
