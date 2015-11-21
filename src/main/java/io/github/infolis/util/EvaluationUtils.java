package io.github.infolis.util;

import java.util.Collection;

import org.apache.commons.collections4.CollectionUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 *
 */
public class EvaluationUtils {

	//private static final Logger log = LoggerFactory.getLogger(EvaluationUtils.class);

	public static double getPrecision(Collection<?> relevant, Collection<?> retrieved) {
		Collection<?> intersect = CollectionUtils.intersection(relevant, retrieved);
		return intersect.size() / (double) retrieved.size();
	}

	public static double getRecall(Collection<?> relevant, Collection<?> retrieved) {
		Collection<?> intersect = CollectionUtils.intersection(relevant, retrieved);
		return intersect.size() / (double) relevant.size();
	}

	public static double getF1Measure(double precision, double recall) {
		return ((precision * recall) / (precision + recall)) * 2;
	}
}