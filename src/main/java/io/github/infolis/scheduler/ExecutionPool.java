//
//package io.github.infolis.scheduler;
//
//import io.github.infolis.model.Execution;
//import io.github.infolis.model.ExecutionStatus;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
///**
// *
// * @author domi
// */
//public class ExecutionPool {
//    
//    private static ExecutionPool instance = null;
//    
//    private ExecutionPool() {}
//    
//   public static ExecutionPool getInstance() {
//      if(instance == null) {
//         instance = new ExecutionPool();
//      }
//      return instance;
//   }
//   
//   Map<Integer, RunnableItem> exeuctions = new LinkedHashMap<>();
//   
//   public RunnableItem getItem(int id) {
//       return exeuctions.get(id);
//   }
//   
//   public int addItem(RunnableItem r) {
//       int number = r.hashCode();
//       exeuctions.put(number, r);
//       return number;
//   }
//   
//   public int addExecuion(Execution e) {
//       RunnableItem r = new RunnableItem();
//       r.setExec(e);
//       int number = r.hashCode();
//       exeuctions.put(number, r);
//       return number;
//   }
//    
//   protected synchronized RunnableItem getNextItem() {
//       for(RunnableItem ri : exeuctions.values()) {
//           if(ri.getExec().getStatus() == ExecutionStatus.PENDING) {               
//               return exeuctions.get(exeuctions.keySet().iterator().next());
//           }
//       }
//       return null;
//   }    
//}
