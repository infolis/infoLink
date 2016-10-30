package io.github.infolis.infolink.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import io.github.infolis.util.EvaluationUtils;


/**
 * 
 * @author kata
 *
 */
public class Agreement {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Agreement.class);
			
	List<String> exactMatchesRefToAnno = new ArrayList<>();
	List<String> noMatchesRefToAnno = new ArrayList<>();
	// algorithm found incomplete reference
	List<List<String>> refPartOfAnno = new ArrayList<>();
	// algorithm found reference but included unrelated surrounding words
	List<List<String>> annoPartOfRef = new ArrayList<>();
	List<List<String>> refAndAnnoOverlap = new ArrayList<>();
	
	int numFoundReferences = 0;
	int numAnnotatedReferences = 0;
	List<String> annotatedReferences = new ArrayList<>();
	
	ListMultimap<String, String> annoTypeMap = ArrayListMultimap.create();
	double typeRecall;
	double typeRecallExact;
	double summedTypeRecall;
	double summedTypeRecallExact;
	int summedRecallTypes = 0;
	int summedRecallTypesExact = 0;
	
	public Agreement(int numFoundReferences, int numAnnotatedReferences) {
		this.numFoundReferences = numFoundReferences;
		this.numAnnotatedReferences = numAnnotatedReferences;
	}
	
	public Agreement() { }
	
	public void update(Agreement agreement) {
		this.exactMatchesRefToAnno.addAll(agreement.exactMatchesRefToAnno);
		this.noMatchesRefToAnno.addAll(agreement.noMatchesRefToAnno);
		this.refPartOfAnno.addAll(agreement.refPartOfAnno);
		this.annoPartOfRef.addAll(agreement.annoPartOfRef);
		this.refAndAnnoOverlap.addAll(agreement.refAndAnnoOverlap);
		this.numFoundReferences += agreement.numFoundReferences;
		this.numAnnotatedReferences += agreement.numAnnotatedReferences;
		this.annotatedReferences.addAll(agreement.annotatedReferences);
		/*try {
			if (!Double.isNaN(agreement.typeRecall)) {
				this.summedTypeRecall += agreement.typeRecall;
				this.summedRecallTypes++;
			}
			if (!Double.isNaN(agreement.typeRecallExact)) {
				this.summedTypeRecallExact += agreement.typeRecallExact;
				this.summedRecallTypesExact++;
			}
		} catch (NullPointerException npe) {*/
			agreement.getRecallForTypes();
			if (!Double.isNaN(agreement.typeRecall)) {
				this.summedTypeRecall += agreement.typeRecall;
				this.summedRecallTypes++;
			}
			if (!Double.isNaN(agreement.typeRecallExact)) {
				this.summedTypeRecallExact += agreement.typeRecallExact;
				this.summedRecallTypesExact++;
			}
		//}
	}
	
	public void setAnnotatedReferences(List<String> annotatedReferences) {
		this.annotatedReferences = annotatedReferences;
	}
	
	public int getNumAnnotatedReferences() {
		return this.numAnnotatedReferences;
	}
	
	public void addExactMatch(String foundReference) {
		this.exactMatchesRefToAnno.add(foundReference);
	}
	
	public void addNoMatch(String foundReference) {
		this.noMatchesRefToAnno.add(foundReference);
	}
	
	public void addIncompleteReference(String foundReference, String annotatedReference) {
		this.refPartOfAnno.add(Arrays.asList(annotatedReference, foundReference));
		annoTypeMap.put(annotatedReference, foundReference);
	}
	
	public void addInflatedReference(String foundReference, String annotatedReference) {
		this.annoPartOfRef.add(Arrays.asList(annotatedReference, foundReference));
		annoTypeMap.put(annotatedReference, foundReference);
	}
	
	public void addOverlap(String foundReference, String annotatedReference) {
		this.refAndAnnoOverlap.add(Arrays.asList(annotatedReference, foundReference));
		annoTypeMap.put(annotatedReference, foundReference);
	}

	// the current lists count matches per token; count them per type here
	// e.g. if "PIAAC" has been found once, it does not need to be found twice
	private void getRecallForTypes() {
		Set<String> annotatedTypes = new HashSet<String>(this.annotatedReferences);
		List<String> exactMatches = new ArrayList<>();
		List<String> inexactMatches = new ArrayList<>();
		List<String> noMatches  = new ArrayList<>();
		
		for (String anno : annotatedTypes) {
			if (this.exactMatchesRefToAnno.contains(anno)) exactMatches.add(anno);
			else if (annoTypeMap.containsKey(anno)) inexactMatches.add(anno);
			else noMatches.add(anno);
		}
		this.typeRecall = getRecall(exactMatches.size() + inexactMatches.size(), annotatedTypes.size());
		this.typeRecallExact = getRecall(exactMatches.size(), annotatedTypes.size());
	}
	
	public void logStats() {
		log.debug(String.format("%s of %s (%s%%) (%s) references have an exact match in the gold standard", 
				exactMatchesRefToAnno.size(), numFoundReferences, 
				(exactMatchesRefToAnno.size() / (double)numFoundReferences) * 100,
				exactMatchesRefToAnno));
		log.debug(String.format("%s of %s (%s%%) (%s) references do not have any exact or inexact match in the gold standard", 
				noMatchesRefToAnno.size(), numFoundReferences,
				(noMatchesRefToAnno.size() / (double)numFoundReferences) * 100,
				noMatchesRefToAnno));
		log.debug(String.format("%s of %s (%s%%) (%s) references include annotation but also additional surrounding words", 
				annoPartOfRef.size(), numFoundReferences, 
				(annoPartOfRef.size() / (double)numFoundReferences) * 100,
				annoPartOfRef));
		log.debug(String.format("%s of %s (%s%%) (%s) references are a substring of the annotated reference", 
				refPartOfAnno.size(), numFoundReferences,
				(refPartOfAnno.size() / (double)numFoundReferences) * 100,
				refPartOfAnno));
		log.debug(String.format("%s of %s (%s%%) (%s) references and annotations are not substrings but overlap", 
				refAndAnnoOverlap.size(), numFoundReferences,
				(refAndAnnoOverlap.size() / (double)numFoundReferences) * 100,
				refAndAnnoOverlap));
		
		List foundReferences = new ArrayList();
		foundReferences.addAll(exactMatchesRefToAnno);
		foundReferences.addAll(annoPartOfRef);
		foundReferences.addAll(refPartOfAnno);
		foundReferences.addAll(refAndAnnoOverlap);
		
		log.debug(String.format("%s of %s (%s%%) annotated entites were found by the algorithm:\n%s", 
				foundReferences.size(),
				numAnnotatedReferences,
				(foundReferences.size() / (double)numAnnotatedReferences) * 100,
				foundReferences));
		
		// per token
		double precision = getPrecision(foundReferences.size(), numFoundReferences);
		double recall = getRecall(foundReferences.size(), numAnnotatedReferences);
		double f1 = getF1(precision, recall);
		log.debug("Precision: {}", precision);
		log.debug("Recall: {}", recall);
		log.debug("F1: {}", f1);
		
		// per token
		double precisionExact = getPrecision(this.exactMatchesRefToAnno.size(), numFoundReferences);
		double recallExact = getRecall(this.exactMatchesRefToAnno.size(), numAnnotatedReferences);
		double f1Exact = getF1(precisionExact, recallExact);
		log.debug("Precision (exact match only): {}", precisionExact);
		log.debug("Recall (exact match only): {}", recallExact);
		log.debug("F1: {}", f1Exact);
		
		// per type 
		// count statistics for types per file, do not aggregate this over multiple files!
		if (this.summedRecallTypes == 0 && this.summedRecallTypesExact == 0) {
			getRecallForTypes();
			double typesF1 = getF1(precision, this.typeRecall);
			double typesF1Exact = getF1(precisionExact, typeRecallExact);
			log.debug("Recall for types: {}", this.typeRecall);
			log.debug("F1 for types: {}", typesF1);
			log.debug("Recall for types (exact match only): {}", this.typeRecallExact);
			log.debug("F1 for types (exact match only): {}", typesF1Exact);
		} else {
			double cumulatedTypeRecall = this.summedTypeRecall / this.summedRecallTypes;
			double cumulatedTypeRecallExact = this.summedTypeRecallExact / this.summedRecallTypesExact;
			double cumulatedTypesF1 = getF1(precision, cumulatedTypeRecall);
			double cumulatedTypesF1Exact = getF1(precisionExact, cumulatedTypeRecallExact);
			log.debug("Recall for types: {}", cumulatedTypeRecall);
			log.debug("F1 for types: {}", cumulatedTypesF1);
			log.debug("Recall for types (exact match only): {}", cumulatedTypeRecallExact);
			log.debug("F1 for types (exact match only): {}", cumulatedTypesF1Exact);
		}
		
	}
	
	public double getPrecision(int correct, int retrieved) {
		return correct / (double)retrieved;
	}
	
	public double getRecall(int correct, int relevant) {
		return correct / (double)relevant;
	}
	
	public double getF1(double precision, double recall) {
		if (recall == 0.0) return 0.0;
		return EvaluationUtils.getF1Measure(precision, recall);
	}

}