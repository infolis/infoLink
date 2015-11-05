package io.github.infolis.scheduler;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.algorithm.TextExtractorAlgorithm;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.SerializationUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author domi
 */
public class SocketTest extends InfolisBaseTest {

    private byte[] pdfBytes;
    Path tempFile;

    @Before
    public void setUp() throws IOException {
        dataStoreClient.clear();
        pdfBytes = IOUtils.toByteArray(getClass().getResourceAsStream("/trivial.pdf"));
        tempFile = Files.createTempFile("infolis-", ".pdf");
    }

    @Test
    public void testSocket() throws InterruptedException {

        InfolisFile inFile = new InfolisFile();
        Execution execution = new Execution();
        inFile.setFileName(tempFile.toString());
        inFile.setMd5(SerializationUtils.getHexMd5(pdfBytes));
        inFile.setMediaType("application/pdf");
        inFile.setFileStatus("AVAILABLE");
        writeFile(inFile);

        execution.getInputFiles().add(inFile.getUri());
        execution.setAlgorithm(TextExtractorAlgorithm.class);
        dataStoreClient.post(Execution.class, execution);
        Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, fileResolver);
        ExecutionScheduler exe = ExecutionScheduler.getInstance();

        Socket socket = null;
        try {
            socket = new Socket("localhost", 1234);
        } catch (UnknownHostException e) {
            System.out.println("Unknown Host...");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IOProbleme...");
            e.printStackTrace();
        }

        exe.execute(algo);

        while (exe.getOpenExecutions().size() > 0 || exe.getRunningExecutions().size() > 0) {
            try {
                InputStream rein = socket.getInputStream();
                if (rein.available() > 0) {                    
                    BufferedReader buff = new BufferedReader(new InputStreamReader(rein));
                    while (buff.ready()) {
                        System.out.println(buff.readLine());
                    }
                }    
                }catch(Exception e){                
            }

        }
        exe.shutDown();

    }

    private void writeFile(InfolisFile inFile) {
        dataStoreClient.post(InfolisFile.class, inFile);
        try {
            OutputStream os = fileResolver.openOutputStream(inFile);
            IOUtils.write(pdfBytes, os);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
