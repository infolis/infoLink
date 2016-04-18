package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.SerializationUtils;

/**
 * 
 * @author kata
 *
 */
public class SpringerImporterTest extends InfolisBaseTest {
	
	Logger log = LoggerFactory.getLogger(SpringerImporterTest.class);
	InfolisFile testFile;
	
	public SpringerImporterTest() throws IOException {
		testFile = new InfolisFile();
		String testFilename = this.getClass().getResource("/springerImporter/test.xml").getFile();
		String text = FileUtils.readFileToString(new File(testFilename));
		testFile.setFileName(testFilename.toString());
		testFile.setMd5(SerializationUtils.getHexMd5(text));
		testFile.setMediaType("text/xml; charset=utf-8");
		testFile.setFileStatus("AVAILABLE");

		writeFile(testFile, text);
	}
	
	private void writeFile(InfolisFile inFile, String text) {
		try {
			OutputStream os = fileResolver.openOutputStream(inFile);
			IOUtils.write(text, os);
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		dataStoreClient.post(InfolisFile.class, inFile);
	}
	
	@Test
	public void testExecute() throws IOException {
		Execution execution = new Execution();
		execution.getInputFiles().add(testFile.getUri());
		execution.setAlgorithm(SpringerImporter.class);
		Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
		algo.run();
		
		assertEquals(1, execution.getOutputFiles().size());
		String fileId = algo.getExecution().getOutputFiles().get(0);
		InfolisFile outFile = dataStoreClient.get(InfolisFile.class, fileId);
		InputStream in = fileResolver.openInputStream(outFile);
		String text = IOUtils.toString(in);
		in.close();
		assertEquals("Introduction\nThis is the text of the article.", text.trim());
		log.debug(SerializationUtils.dumpExecutionLog(execution));
	}
}