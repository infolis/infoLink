package io.github.infolis.infolink.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;

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
	}
	
	public void addAnnotatedReferences(int n) {
		this.numAnnotatedReferences += n; 
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
	}
	
	public void addInflatedReference(String foundReference, String annotatedReference) {
		this.annoPartOfRef.add(Arrays.asList(annotatedReference, foundReference));
	}
	
	public void addOverlap(String foundReference, String annotatedReference) {
		this.refPartOfAnno.add(Arrays.asList(annotatedReference, foundReference));
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
	}

}