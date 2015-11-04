//package io.github.infolis.scheduler;
//
//import io.github.infolis.model.ExecutionStatus;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ThreadPoolExecutor;
//
///**
// *
// * @author domi
// */
//public class Scheduler {
//
//    public void schedule() {
//        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
//        ExecutionPool exePool = ExecutionPool.getInstance();
//        while (true) {
//            RunnableItem ri = exePool.getNextItem();
//            ri.getExec().setStatus(ExecutionStatus.STARTED);
//            executor.execute(ri);
//        }
//    }
//}
