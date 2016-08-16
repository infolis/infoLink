package io.github.infolis.infolink.querying;

import io.github.infolis.algorithm.SearchResultLinker;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.util.InformationExtractor;
import io.github.infolis.util.RegexUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author kata
 *
 */
public class SearchResultScorer {
	
	private final static Logger log = LoggerFactory.getLogger(SearchResultScorer.class);
	
	/**
     * Computes score and temporal relations based on correspondence of numbers in entities and 
     * search results. Considers ranges, abbreviated years and exact matches.
     *
     * @param reference
     * @param targetCandidate
     * @return
     */
    public static SearchResultLinker.CandidateTargetEntity computeScoreBasedOnNumbers(
    		Entity entity, SearchResult targetCandidate) {
    	SearchResultLinker.CandidateTargetEntity candidate = new SearchResultLinker.CandidateTargetEntity();
        candidate.setSearchResult(targetCandidate);
        
        List<String> textRefNumInfoList = entity.getNumericInfo();
        if(targetCandidate.getNumericInformation() == null || targetCandidate.getNumericInformation().isEmpty()) {
        	targetCandidate.setNumericInformation(InformationExtractor.extractNumbers(targetCandidate.getTitles().get(0)));
        }
        
        List<String> targetCandidateNumInfoList = targetCandidate.getNumericInformation();
        
        if (targetCandidate.getNumericInformation().isEmpty()) {
        	candidate.setScore(0.5);
        	if (textRefNumInfoList.isEmpty()) candidate.addEntityRelation(EntityLink.EntityRelation.same_as_temporal);
        	else candidate.addEntityRelation(EntityLink.EntityRelation.part_of_temporal);
        	return candidate;
        }
        if (textRefNumInfoList.isEmpty()) {
        	candidate.setScore(0.7);
        	candidate.addEntityRelation(EntityLink.EntityRelation.superset_of_temporal);
        	return candidate;
        }
        for (String textRefNumInfo : textRefNumInfoList) {
            for (String targetCandidateNumInfo : targetCandidateNumInfoList) {
            	Set<EntityLink.EntityRelation> entityRelations = numericInfoMatches(textRefNumInfo, targetCandidateNumInfo);
                if (null != entityRelations) {
                	candidate.setScore(1.0);
                	candidate.setEntityRelations(entityRelations);
                	return candidate;
                	
                }
            }
        }
        candidate.setScore(0.0);
        return candidate;
    }
	
	/**
     * Computes score and temporal relations based on correspondence of numbers in textual references and 
     * search results. Considers ranges, abbreviated years and exact matches.
     *
     * @param reference
     * @param targetCandidate
     * @return
     */
    public static SearchResultLinker.CandidateTargetEntity computeScoreBasedOnNumbers(TextualReference reference, SearchResult targetCandidate) {
    	SearchResultLinker.CandidateTargetEntity candidate = new SearchResultLinker.CandidateTargetEntity();
        candidate.setSearchResult(targetCandidate);
        
        List<String> textRefNumInfoList = InformationExtractor.extractNumericInfo(reference);
        if(targetCandidate.getNumericInformation() == null || targetCandidate.getNumericInformation().isEmpty()) {
        	targetCandidate.setNumericInformation(InformationExtractor.extractNumbers(targetCandidate.getTitles().get(0)));
        }
        List<String> targetCandidateNumInfoList = targetCandidate.getNumericInformation();
        
        if (targetCandidate.getNumericInformation().isEmpty()) {
        	candidate.setScore(0.5);
        	if (textRefNumInfoList.isEmpty()) candidate.addEntityRelation(EntityLink.EntityRelation.same_as_temporal);
        	else candidate.addEntityRelation(EntityLink.EntityRelation.part_of_temporal);
        	return candidate;
        }
        if (textRefNumInfoList.isEmpty()) {
        	candidate.setScore(0.7);
        	candidate.addEntityRelation(EntityLink.EntityRelation.superset_of_temporal);
        	return candidate;
        }
        
        for (String textRefNumInfo : textRefNumInfoList) {
            for (String targetCandidateNumInfo : targetCandidateNumInfoList) {
            	Set<EntityLink.EntityRelation> entityRelations = numericInfoMatches(textRefNumInfo, targetCandidateNumInfo);
                if (null != entityRelations) {
                	candidate.setScore(1.0);
                	candidate.setEntityRelations(entityRelations);
                	return candidate;
                	
                }
            }
        }
        candidate.setScore(0.0);
        return candidate;
    }
    
    private static boolean containsYear(String number) {
		return Pattern.matches(".*?" + RegexUtils.yearRegex + ".*?", number);
	}

	private static boolean containsAbbreviatedYear(String number) {
		return Pattern.matches("[\\D]*?" + "('?\\d\\d)" + "[^\\d\\.]*?", number);
	}

	private static boolean containsEnum(String number) {
		return Pattern.matches(".*?\\d\\s*" + RegexUtils.enumRegex+ "\\s*\\d.*?", number);
	}

	private static boolean containsRange(String number) {
		return Pattern.matches(".*?\\d\\s*" + RegexUtils.rangeRegex+ "\\s*\\d.*?", number);
	}

	private static String[] getFullYearVariants(String extractedNumber) {
		String number1a, number1b = extractedNumber;
		if (containsAbbreviatedYear(extractedNumber)) {
			number1a = "19" + extractedNumber;
			number1b = "20" + extractedNumber;
		}
		else { number1a = number1b = extractedNumber; }
		return new String[]{number1a, number1b};
	}

	// call method for every enumerated value, one match is sufficient <- ?
	// TODO if not all enumerated values have a match, superset relation should be added...
	private static Set<EntityLink.EntityRelation> enumMatches(List<String> enumInfo, String info2) {
		Set<EntityLink.EntityRelation> relations = new HashSet<>();
		for (String info : enumInfo) {
			for (String enumeratedNumber : info.split(RegexUtils.enumRegex)) {
				log.debug(String.format("computing score for enum part \"%s\"", enumeratedNumber));
				Set<EntityLink.EntityRelation> rels = numericInfoMatches(enumeratedNumber, info2);
				if (null != rels) {
					relations.addAll(rels);
					// remove same_as_temporal: if it is part of an enumeration, 
					// it's just a part
					relations.remove(EntityLink.EntityRelation.same_as_temporal);
					relations.add(EntityLink.EntityRelation.part_of_temporal);
					return relations;
				}
			}
			//return relations;
		}
		return null;
	}

	private static Set<EntityLink.EntityRelation> rangeMatches(
			List<String> numericInfo1, List<String> numericInfo2, 
			boolean containsRange_numericInfo2, boolean containsYear_numericInfo2, 
			boolean containsAbbrYear_numericInfo2, boolean invert) {
		// ranges may contain abbreviated years
		String[] variants1a = getFullYearVariants(numericInfo1.get(0));
		String[] variants1b = getFullYearVariants(numericInfo1.get(1));

		List<String> numericInfo1a = Arrays.asList(variants1a[0], variants1b[0]);
		List<String> numericInfo1b = Arrays.asList(variants1a[1], variants1b[1]);

		Set<EntityLink.EntityRelation> relations = new HashSet<>();
		Set<EntityLink.EntityRelation> relations1;
		Set<EntityLink.EntityRelation> relations2;
		
		if (containsRange_numericInfo2) {
			log.debug("2nd argument also contains range, computing overlap");
			// ranges may contain abbreviated years
			String[] variants2a = getFullYearVariants(numericInfo2.get(0));
			String[] variants2b = getFullYearVariants(numericInfo2.get(1));
			List<String> numericInfo2a = Arrays.asList(variants2a[0], variants2b[0]);
			List<String> numericInfo2b = Arrays.asList(variants2a[1], variants2b[1]);

			
			relations1 = overlap(numericInfo1a, numericInfo2a, invert); 
			relations2 = overlap(numericInfo1b, numericInfo2b, invert);
			if (null != relations1) relations.addAll(relations1);
			if (null != relations2) relations.addAll(relations2);
			if (!relations.isEmpty()) return relations;
			else return null;
		}
		// year must be inside of range
		if (containsYear_numericInfo2) {
			if (inRange(numericInfo1a, numericInfo2.get(0))
					|| inRange(numericInfo1b, numericInfo2.get(0))) {
				if (invert) relations.add(EntityLink.EntityRelation.part_of_temporal);
				else relations.add(EntityLink.EntityRelation.superset_of_temporal);
				return relations;
			}
		}
		// modified value must be inside of range
		if (containsAbbrYear_numericInfo2) {
			if (inRange(numericInfo1a, "19" + numericInfo2.get(0))
					|| inRange(numericInfo1b, "19" + numericInfo2.get(0))
					|| inRange(numericInfo1a, "20" + numericInfo2.get(0))
					|| inRange(numericInfo1b, "20" + numericInfo2.get(0))) {
				if (invert) relations.add(EntityLink.EntityRelation.part_of_temporal);
				else relations.add(EntityLink.EntityRelation.superset_of_temporal);
				return relations;
			}
		}
		else { 
			if (inRange(numericInfo1a, numericInfo2.get(0))
				|| inRange(numericInfo1b, numericInfo2.get(0))) {
				if (invert) relations.add(EntityLink.EntityRelation.part_of_temporal);
				else relations.add(EntityLink.EntityRelation.superset_of_temporal);
				return relations;
			}
		}
		return null;
	}

	public static boolean yearsMatch(List<String> numericInfo1, List<String> numericInfo2, boolean containsYear_numericInfo2, boolean containsAbbrYear_numericInfo2) {

		if (containsAbbrYear_numericInfo2) {
			for (String year : numericInfo1) {
				for (String abbrYear2 : numericInfo2) {
					for (String year2 : getFullYearVariants(abbrYear2)) {
						if (year.equals(year2)) {
							log.debug("Years match: " + year + " <-> " + year2);
							return true;
						}
						else { log.debug("No year match: " + year + " <-> " + year2); }
					}
				}
			}
			return false;
		}

		// candidate numeric info contains a year as well or is some number
		else {
			for (String year : numericInfo1) {
				for (String year2 : numericInfo2) {
					if (year.equals(year2)) {
						log.debug("Years match: " + year + " <-> " + year2);
						return true;
					}
					else { log.debug("No year match: " + year + " <-> " + year2); }
				}
			}
			return false;
		}
	}

	public static boolean abbreviatedYearsMatch(List<String> numericInfo1, List<String> numericInfo2, boolean containsAbbrYear_numericInfo2) {
		// modified year must match modified year
		if (containsAbbrYear_numericInfo2) {
			for (String abbreviatedYear : numericInfo1) {
				for (String abbreviatedYear2 : numericInfo2) {
					if (abbreviatedYear.equals(abbreviatedYear2)) { return true; }
				}
			}
			return false;
		}
		// info2 is some float number. Compare, because 90 may be an abbreviated year or a number and 90 == 90.0
		else {
			for (String info2 : numericInfo2) {
				float number2 = Float.parseFloat(info2);
				for (String abbreviatedYear : numericInfo1) {
					float number1 = Float.parseFloat(abbreviatedYear);
					if (Math.abs(number1 - number2) < 0.00001) {
						log.debug("Equal: " + number1 + " <-> " +  number2); return true; }
				}
			}
			return false;
		}
	}

	private static boolean floatsMatch(List<String> numericInfo1, List<String> numericInfo2) {
		for (String info1 : numericInfo1) {
			float number1 = Float.parseFloat(info1);
			for (String info2 : numericInfo2) {
				float number2 = Float.parseFloat(info2);
				if (number1 == number2) { return true; }
			}
		}
		return false;
	}

	protected static Set<EntityLink.EntityRelation> numericInfoMatches(String numericInfo, String string) {
		// for study references without any specified years / numbers, accept all candidates
		// TODO: match to higher order entity according to dataset ontology (study, not dataset)
		Set<EntityLink.EntityRelation> relations = new HashSet<>();
		if (numericInfo == null || string == null) {
			relations.add(EntityLink.EntityRelation.superset_of_temporal);
			return relations;
		}
		List<String> numericInfo1 = InformationExtractor.extractNumbers(numericInfo);
		List<String> numericInfo2 = InformationExtractor.extractNumbers(string);
		boolean containsRange_numericInfo1 = containsRange(numericInfo);
		boolean containsRange_numericInfo2 = containsRange(string);
		boolean containsEnum_numericInfo1 = containsEnum(numericInfo);
		boolean containsEnum_numericInfo2 = containsEnum(string);
		boolean containsYear_numericInfo1 = containsYear(numericInfo);
		boolean containsYear_numericInfo2 = containsYear(string);
		boolean containsAbbrYear_numericInfo1 = containsAbbreviatedYear(numericInfo);
		boolean containsAbbrYear_numericInfo2 = containsAbbreviatedYear(string);

		if (containsEnum_numericInfo1) {
			log.debug("Enum match for: " + numericInfo + " <-> " + string + "?");
			return enumMatches(numericInfo1, string);
		}
		if (containsEnum_numericInfo2) {
			log.debug("Enum match for: " + string + " <-> " + numericInfo + "?");
			return enumMatches(numericInfo2, numericInfo);
		}
		// extracted numeric information is a range specification
		if (containsRange_numericInfo1) {
			log.debug("Range match for: " + numericInfo + " <-> " + string + "?");
			log.debug("first argument is range");
			// continue if range does not match - maybe parts do
			Set<EntityLink.EntityRelation> rels = rangeMatches(numericInfo1, numericInfo2, 
					containsRange_numericInfo2, containsYear_numericInfo2, 
					containsAbbrYear_numericInfo2, false);//false
			// if empty, it is not a valid range; continue searching
			if (null == rels) ;
			else if (!rels.isEmpty()) return rels;
		}

		if (containsRange_numericInfo2) {
			log.debug("Range match for: " + numericInfo + " <-> " + string + "?");
			log.debug("second argument is range");
			// continue if range does not match - maybe parts do
			Set<EntityLink.EntityRelation> rels = rangeMatches(numericInfo2, numericInfo1, 
					containsRange_numericInfo1, containsYear_numericInfo1, 
					containsAbbrYear_numericInfo1, true);//true
			// if empty, it is not a valid range; continue searching
			if (null == rels) ;
			else if (!rels.isEmpty()) return rels;
		}

		// extracted numeric info contains a year
		if (containsYear_numericInfo1) {
			log.debug("Year match for: " + numericInfo + " <-> " + string + "?");
			if (yearsMatch(numericInfo1, numericInfo2, 
					containsYear_numericInfo2, containsAbbrYear_numericInfo2)) {
				relations.add(EntityLink.EntityRelation.same_as_temporal);
				return relations;
			}
		}

		// extracted numeric info contains a year
		if (containsYear_numericInfo2) {
			log.debug("Year match for: " + string + " <-> " + numericInfo + "?");
			if (yearsMatch(numericInfo2, numericInfo1, containsYear_numericInfo1, containsAbbrYear_numericInfo1)) {
				relations.add(EntityLink.EntityRelation.same_as_temporal);
				return relations;
			}
		}

		if (containsAbbrYear_numericInfo1) {
			log.debug("Abbreviated year match for: " + numericInfo + " <-> " + string + "?");
			if (abbreviatedYearsMatch(numericInfo1, numericInfo2, containsAbbrYear_numericInfo2)) {
				relations.add(EntityLink.EntityRelation.same_as_temporal);
				return relations;
			}
		}

		if (containsAbbrYear_numericInfo2) {
			log.debug("Abbreviated year match for: " + string + " <-> " + numericInfo + "?");
			if (abbreviatedYearsMatch(numericInfo2, numericInfo1, containsAbbrYear_numericInfo1)) {
				relations.add(EntityLink.EntityRelation.same_as_temporal);
				return relations;
			}
		}
		else {
			log.debug("Number match for: " + numericInfo + " <-> " + string + "?");
			if (floatsMatch(numericInfo1, numericInfo2)) {
				relations.add(EntityLink.EntityRelation.same_as_temporal);
				return relations;
			}
		}
		return null;
	}

	private static float[] toFloatArray(List<String> numberList) {
		float[] res = new float[numberList.size()];
		for (int i = 0; i< numberList.size(); i++) {
			res[i] = Float.parseFloat(numberList.get(i));
		}
		return res;
	}

	/**
	 * Checks whether given value lies inside of range1.
	 *
	 * @param range1
	 * @param value
	 * @return
	 */
	static boolean inRange(List<String> range1, String value) {
		try {
			float[] info1 = toFloatArray(range1);
			float year2 = Float.parseFloat(value);
			return (inRange(info1, year2));
		}
		catch (NumberFormatException nfe) { log.debug(nfe.getMessage()); return false; }
	}

	/**
	 * Checks whether given value lies inside of range1.
	 *
	 * @param range1
	 * @param value
	 * @return
	 */
	static boolean inRange(float[] range1, float value) {
		float year1a = range1[0]; float year1b = range1[1];
		// probably not a range after all (e.g. Ausländer in Deutschland 2000 - 2. Welle)
		if (year1a > year1b)  { return false; }
		return (value >= year1a && value <= year1b);
	}

	/**
	 * Return true if both ranges overlap = Check whether period a entirely or partly covers period b.
	 * Period a: year1a - year1b; period b: year2a - year2b.
	 * 5 cases may occur:
	 * 0. period a and b are equal: e.g. 1991-1999 and 1991-1999
	 * 1. period b is entirely covered: e.g. 1991-1999 in 1990-2000
	 * 2. period b is partly covered 1): e.g. 1980-1999 in 1990-2000
	 * 3. period b is partly covered 2): e.g. 1991-2013 in 1990-2000
	 * 4. period a is entirely covered: e.g. 1980-2013 in 1990-2000
	 *
	 * @param range1
	 * @param range2
	 * @param invert
	 * @return
	 */
	static Set<EntityLink.EntityRelation> overlap(List<String> range1, 
			List<String> range2, boolean invert) {
		log.debug(String.format("computing overlap between '%s' and '%s'. Invert: %s", range1, range2, invert));
		Set<EntityLink.EntityRelation> relations = new HashSet<>();
		try {
			float[] info1 = toFloatArray(range1);
			float[] info2 = toFloatArray(range2);
			float year1a = info1[0]; float year1b = info1[1];
			float year2a = info2[0]; float year2b = info2[1];
			// probably not a range after all (e.g. Ausländer in Deutschland 2000 - 2. Welle)
			if (year1a > year1b || year2a > year2b)  return relations;
			// case 0
			if ((year1a == year2a) && (year1b == year2b)) {
				relations.add(EntityLink.EntityRelation.same_as_temporal);
				log.debug("returning " + relations);
				return relations;
			}
			// case 1
			if ((year2a >= year1a) && (year2b <= year1b)) {
				if (invert) relations.add(EntityLink.EntityRelation.part_of_temporal);
				else relations.add(EntityLink.EntityRelation.superset_of_temporal);
				log.debug("returning " + relations);
				return relations;
			}
			// case 2
			if ((year2a < year1a) && (year2b <= year1b) && (year2b >= year1a)) {
				if (year2b != year1b) relations.add(EntityLink.EntityRelation.superset_of_temporal);
				relations.add(EntityLink.EntityRelation.part_of_temporal);
				log.debug("returning " + relations);
				return relations;
			}
			// case 3
			if ((year2a >= year1a) && (year2b > year1b) && (year2a <= year1b)) {
				if (year2a != year1a) relations.add(EntityLink.EntityRelation.superset_of_temporal);
				relations.add(EntityLink.EntityRelation.part_of_temporal);
				log.debug("returning " + relations);
				return relations;
			}
			// case 4
			if ((year2a <= year1a) && (year2b >= year1b)) {
				if (invert) relations.add(EntityLink.EntityRelation.superset_of_temporal);
				else relations.add(EntityLink.EntityRelation.part_of_temporal);
				log.debug("returning " + relations);
				return relations;
			}
			return null;
		}
		catch (NumberFormatException nfe) { log.debug(nfe.getMessage()); return null; }
	}

}