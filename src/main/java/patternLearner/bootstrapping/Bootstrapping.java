/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package patternLearner.bootstrapping;

import java.util.Set;
import patternLearner.Learner;

/**
 *
 * @author domi
 */
public abstract class Bootstrapping {
 
    public Learner l;

    public Bootstrapping(Learner l) {
       this.l = l;
    }
        
    public abstract void bootstrap(Set<String> terms, int numIter, int maxIter);
    
}