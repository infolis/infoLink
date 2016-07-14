package io.github.infolis.scheduler;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.github.infolis.model.ExecutionStatus.PENDING;
import static io.github.infolis.model.ExecutionStatus.STARTED;
import static io.github.infolis.model.ExecutionStatus.FINISHED;
import static io.github.infolis.model.ExecutionStatus.FAILED;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
/**
 *
 * Class to pool threads of {@link Execution Executions}s.
 *
 *
 * @author domi
 */
public class ExecutionScheduler {

    private static ExecutionScheduler instance = null;
    private static ThreadPoolExecutor executor;

    /**
     * @param aExecutor the executor to set
     */
    public static void setExecutor(ThreadPoolExecutor aExecutor) {
        executor = aExecutor;
    }
    private final Map<String, ExecutionStatus> statusForExecution = new HashMap<>();
    private final Map<String, Future> futureList = new HashMap<>();

    private ExecutionScheduler() { }

    private void setStatus(String uri, ExecutionStatus status)
    {
        synchronized (statusForExecution) {
            statusForExecution.put(uri, status);
        }
    }

    public void execute(final Algorithm r) {
        final String uri = r.getExecution().getUri();
        setStatus(uri, PENDING);
        //execute vs. submit
        Future future = executor.submit(new Runnable() {
            @Override
            public void run() {
                setStatus(uri, STARTED);
                try {
                    r.run();
                    setStatus(uri, r.getExecution().getStatus());
                } catch (Exception e) {
                    setStatus(uri, FAILED);
                }
            }
        });
        futureList.put(uri, future);
    }

    public ExecutionStatus getStatus(Execution e) {
        return statusForExecution.get(e.getUri());
    }

    public static ExecutionScheduler getInstance() {
        if (instance == null) {
            instance = new ExecutionScheduler();
            executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        }
        return instance;
    }

    public Collection<String> getByStatus(ExecutionStatus status)
    {
        List<String> ret = new ArrayList<>();
        for (Entry<String, ExecutionStatus> entry : statusForExecution.entrySet()) {
            if (status == null || entry.getValue() == status)
                ret.add(entry.getKey());
        }
        return ret;
    }

    public Collection<String> getAllExcecutions() {
        return getByStatus(null);
    }

    public void shutDown() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
    
    public void stopExecution(String executionURI) {        
        Future f = futureList.get(executionURI);
        f.cancel(true);
        setStatus(executionURI, FAILED);
    }

}
