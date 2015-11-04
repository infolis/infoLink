
package io.github.infolis.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *
 * @author domi
 */
public class ExecutionExecutor {
    
    private static ExecutionExecutor instance = null;
    private static ThreadPoolExecutor executor;   

    /**
     * @return the executor
     */
    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    /**
     * @param aExecutor the executor to set
     */
    public void setExecutor(ThreadPoolExecutor aExecutor) {
        executor = aExecutor;
    }
    
    private ExecutionExecutor() {}
    
   public static ExecutionExecutor getInstance() {
      if(instance == null) {          
         instance = new ExecutionExecutor();
         instance.setExecutor((ThreadPoolExecutor) Executors.newFixedThreadPool(2)); 
      }        
      return instance;
   }
     
    
}