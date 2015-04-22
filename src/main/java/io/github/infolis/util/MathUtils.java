package io.github.infolis.util;

public class MathUtils {

    /**
     * Computes the logarithm (base 2) for a given value
     *
     * @param x	the value for which the log2 value is to be computed
     * @return	the logarithm (base 2) for the given value
     */
    public static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    /**
     * Computes the point-wise mutual information of two strings given their
     * probabilities. see http://www.aclweb.org/anthology/P06-1#page=153
     *
     * @param p_xy	probability P(x,y), i.e. ...
     * @param p_x probability P(x), i.e. ...
     * @param p_y	probability P(y), i.e. ...
     * @return
     */
    public static double pmi(double p_xy, double p_x, double p_y) {
        return log2(p_xy / (p_x * p_y));
    }


}
