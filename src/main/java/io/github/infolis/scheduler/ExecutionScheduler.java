package io.github.infolis.scheduler;

import io.github.infolis.algorithm.Algorithm;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 
 * Class to run executions over a thredpool to allow .
 * 
 *
 * @author domi
 */
public class ExecutionScheduler {

    private static ExecutionScheduler instance = null;
    private static ThreadPoolExecutor executor;
    private final List<Execution> failedExecutions = new ArrayList();
    private final List<Execution> completedExecutions = new ArrayList();
    private final List<Execution> runningExecutions = new ArrayList();
    private final List<Execution> openExecutions = new ArrayList();

private ExecutionScheduler() {}

    public void execute(final Algorithm r) {
        synchronized (openExecutions) {
            getOpenExecutions().add(r.getExecution());
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                getOpenExecutions().remove(r.getExecution());
                getRunningExecutions().add(r.getExecution());
                
                try {
                    r.run();
                } catch(Exception e) { 
                    getRunningExecutions().remove(r.getExecution());
                    getFailedExecutions().add(r.getExecution());
                }
                
                getRunningExecutions().remove(r.getExecution());
                
                if(r.getExecution().getStatus()==ExecutionStatus.FINISHED) {
                    getCompletedExecutions().add(r.getExecution());
                } else {
                    getFailedExecutions().add(r.getExecution());
                }
//                try {
//                socket.write(r.getExecution().getUri());
//                } catch(IOException io) {
//                    //TODO: log
//                }
            }
        });
    }
    
    public ExecutionStatus getStatus(Execution e) {
        return e.getStatus();
    }

    public static ExecutionScheduler getInstance() {
        if (instance == null) {
            instance = new ExecutionScheduler();
            executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        }
        return instance;
    }

    /**
     * @return the failedExecutions
     */
    public List<Execution> getFailedExecutions() {
        return failedExecutions;
    }

    /**
     * @return the completedExecutions
     */
    public List<Execution> getCompletedExecutions() {
        return completedExecutions;
    }

    /**
     * @return the runningExecutions
     */
    public List<Execution> getRunningExecutions() {
        return runningExecutions;
    }

    /**
     * @return the openExecutions
     */
    public List<Execution> getOpenExecutions() {
        return openExecutions;
    }

    public void shutDown() throws InterruptedException {        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
    
}
