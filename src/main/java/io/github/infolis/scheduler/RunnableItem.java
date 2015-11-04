package io.github.infolis.scheduler;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.model.Execution;

/**
 *
 * @author domi
 */
public class RunnableItem implements Runnable {

    private Execution exec;
    private DataStoreClient dataStoreClient = DataStoreClientFactory.create(DataStoreStrategy.CENTRAL);
    private FileResolver fileResolver = FileResolverFactory.create(DataStoreStrategy.CENTRAL);

    public RunnableItem(Execution e) {
        this.setExec(e);
    }

    /**
     * @return the exec
     */
    public Execution getExec() {
        return exec;
    }

    /**
     * @param exec the exec to set
     */
    private void setExec(Execution exec) {
        this.exec = exec;
    }

    @Override
    public void run() {
        Algorithm algo = exec.instantiateAlgorithm(dataStoreClient, fileResolver);
        algo.run();
    }

}
