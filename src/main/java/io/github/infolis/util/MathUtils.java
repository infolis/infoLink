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
     * Computes the point-wise mutual information of two entities given their
     * probabilities. see http://www.aclweb.org/anthology/P06-1#page=153
     *
     * Note: if x and y occur together less frequently than one would expect by chance,
     * the score will be negative. If x and y occur together exactly as frequently as
     * expected by chance, the score will be 0. If x and y never occur together,
     * the value will either be infinity, -infinity or NaN.
     *
     * @param p_xy	probability P(x,y), i.e. probability of x and y occurring jointly in the corpus
     * @param p_x probability P(x), i.e. probability of x occurring in the corpus
     * @param p_y	probability P(y), i.e. probability of y occurring in the corpus
     * @return	the point-wise mutual information score of x and y. Note: if x and y do not occur
     * together in the data, the value will be infinity, -infinity or NaN.
     */
    public static double pmi(double p_xy, double p_x, double p_y) {
    	double p_x_y = p_x * p_y;
        return log2(p_xy / p_x_y);
    }

}
