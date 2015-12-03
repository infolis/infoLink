package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test whether the progress of an execution is corretly set (also in the datastore).
 *
 * @author domi
 */
public class ProgressUpdates extends InfolisBaseTest {

    private static final Logger log = LoggerFactory.getLogger(ProgressUpdates.class);

    @Test
    public void testProgress() throws InterruptedException {
        Execution e = new Execution();
        e.setAlgorithm(DumpAlgo.class);
        dataStoreClient.post(Execution.class, e);
        e.instantiateAlgorithm(dataStoreClient, fileResolver).run();

        DumpAlgo da = new DumpAlgo(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
        da.setExecution(e);

        int done = 0;
        for (int i=0; i<4; i++) {
            try {
                Thread.sleep(1100);
            } catch (InterruptedException ex) {
                log.error("{}", ex);
            }
            done++;
            da.updateProgress(done, 4);

            switch(i) {
                case 0:
                    assertEquals(25, da.getExecution().getProgress());
                    assertEquals(25, da.getOutputDataStoreClient().get(Execution.class, da.getExecution().getUri()).getProgress());
                    break;
                case 1:
                    assertEquals(50, da.getExecution().getProgress());
                    assertEquals(50, da.getOutputDataStoreClient().get(Execution.class, da.getExecution().getUri()).getProgress());
                    break;
                case 2:
                    assertEquals(75, da.getExecution().getProgress());
                    assertEquals(75, da.getOutputDataStoreClient().get(Execution.class, da.getExecution().getUri()).getProgress());
                    break;
                case 3:
                    assertEquals(100, da.getExecution().getProgress());
                    assertEquals(100, da.getOutputDataStoreClient().get(Execution.class, da.getExecution().getUri()).getProgress());
                    break;
            }
        }
    }

}
