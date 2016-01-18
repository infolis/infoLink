package io.github.infolis.resolve;

import io.github.infolis.algorithm.DaraLinker;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.util.NumericInformationExtractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
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
     * Computes score based on correspondence of numbers in textual references and 
     * search results. Considers ranges, abbreviated years and exact matches.
     *
     * @param reference
     * @param targetCandidate
     * @return
     */
    public static double computeScoreBasedOnNumbers(TextualReference reference, SearchResult targetCandidate) {
        List<String> textRefNumInfoList = NumericInformationExtractor.extractNumericInfoFromTextRef(reference);
        if(targetCandidate.getNumericInformation() == null || targetCandidate.getNumericInformation().isEmpty()) {
        	targetCandidate.setNumericInformation(NumericInformationExtractor.extractNumbersFromString(targetCandidate.getTitles().get(0)));
        }
        List<String> targetCandidateNumInfoList = targetCandidate.getNumericInformation();
        for (String textRefNumInfo : textRefNumInfoList) {
            for (String targetCandidateNumInfo : targetCandidateNumInfoList) {
                if (numericInfoMatches(textRefNumInfo, targetCandidateNumInfo)) {
                    return 1.0;
                }
            }
        }
        return 0.0;
    }
    
    private static boolean containsYear(String number) {
		return Pattern.matches(".*?" + DaraLinker.yearRegex + ".*?", number);
	}

	private static boolean containsAbbreviatedYear(String number) {
		return Pattern.matches("[\\D]*?" + "('?\\d\\d)" + "[^\\d\\.]*?", number);
	}

	private static boolean containsEnum(String number) {
		return Pattern.matches(".*?\\d\\s*" + DaraLinker.enumRegex+ "\\s*\\d.*?", number);
	}

	private static boolean containsRange(String number) {
		return Pattern.matches(".*?\\d\\s*" + DaraLinker.rangeRegex+ "\\s*\\d.*?", number);
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

	// call method for every enumerated value, one match is sufficient
	private static boolean enumMatches(List<String> enumInfo, String info2) {
		for (String enumeratedNumber : enumInfo) {
			if (numericInfoMatches(enumeratedNumber, info2)) { return true; }
		}
		return false;
	}

	private static boolean rangeMatches(List<String> numericInfo1, List<String> numericInfo2, boolean containsRange_numericInfo2, boolean containsYear_numericInfo2, boolean containsAbbrYear_numericInfo2) {
		// ranges may contain abbreviated years
		String[] variants1a = getFullYearVariants(numericInfo1.get(0));
		String[] variants1b = getFullYearVariants(numericInfo1.get(1));

		List<String> numericInfo1a = Arrays.asList(variants1a[0], variants1b[0]);
		List<String> numericInfo1b = Arrays.asList(variants1a[1], variants1b[1]);

		if (containsRange_numericInfo2) {
			// ranges may contain abbreviated years
			String[] variants2a = getFullYearVariants(numericInfo2.get(0));
			String[] variants2b = getFullYearVariants(numericInfo2.get(1));
			List<String> numericInfo2a = Arrays.asList(variants2a[0], variants2b[0]);
			List<String> numericInfo2b = Arrays.asList(variants2a[1], variants2b[1]);

			return overlap(numericInfo1a, numericInfo2a)
					|| overlap(numericInfo1b, numericInfo2b) ;
		}
		// year must be inside of range
		if (containsYear_numericInfo2) {
			return inRange(numericInfo1a, numericInfo2.get(0))
					|| inRange(numericInfo1b, numericInfo2.get(0));
		}
		// modified value must be inside of range
		if (containsAbbrYear_numericInfo2) {
			return inRange(numericInfo1a, "19" + numericInfo2.get(0))
					|| inRange(numericInfo1b, "19" + numericInfo2.get(0))
					|| inRange(numericInfo1a, "20" + numericInfo2.get(0))
					|| inRange(numericInfo1b, "20" + numericInfo2.get(0));
		}
		else { return inRange(numericInfo1a, numericInfo2.get(0))
				|| inRange(numericInfo1b, numericInfo2.get(0));
		}

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

	//TODO: also return kind of match (overlap vs exact match...?)
	protected static boolean numericInfoMatches(String numericInfo, String string) {
		// for study references without any specified years / numbers, accept all candidates
		// TODO: match to higher order entity according to dataset ontology (study, not dataset)
		if (numericInfo == null || string == null) return true;
		List<String> numericInfo1 = extractNumbers(numericInfo);
		List<String> numericInfo2 = extractNumbers(string);
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
			// continue if range does not match - maybe parts do
			if (rangeMatches(numericInfo1, numericInfo2, containsRange_numericInfo2, containsYear_numericInfo2, containsAbbrYear_numericInfo2)) {
				return true ;
			}
		}

		if (containsRange_numericInfo2) {
			log.debug("Range match for: " + string + " <-> " + numericInfo + "?");
			// continue if range does not match - maybe parts do
			if (rangeMatches(numericInfo2, numericInfo1, containsRange_numericInfo1, containsYear_numericInfo1, containsAbbrYear_numericInfo1)) {
				return true;
			}
		}

		// extracted numeric info contains a year
		if (containsYear_numericInfo1) {
			log.debug("Year match for: " + numericInfo + " <-> " + string + "?");
			return yearsMatch(numericInfo1, numericInfo2, containsYear_numericInfo2, containsAbbrYear_numericInfo2);
		}

		// extracted numeric info contains a year
		if (containsYear_numericInfo2) {
			log.debug("Year match for: " + string + " <-> " + numericInfo + "?");
			return yearsMatch(numericInfo2, numericInfo1, containsYear_numericInfo1, containsAbbrYear_numericInfo1);
		}

		if (containsAbbrYear_numericInfo1) {
			log.debug("Abbreviated year match for: " + numericInfo + " <-> " + string + "?");
			return abbreviatedYearsMatch(numericInfo1, numericInfo2, containsAbbrYear_numericInfo2);
		}

		if (containsAbbrYear_numericInfo2) {
			log.debug("Abbreviated year match for: " + string + " <-> " + numericInfo + "?");
			return abbreviatedYearsMatch(numericInfo2, numericInfo1, containsAbbrYear_numericInfo1);
		}
		else {
			log.debug("Number match for: " + numericInfo + " <-> " + string + "?");
			return floatsMatch(numericInfo1, numericInfo2);
		}
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
	 * 4 cases may occur:
	 * 1. period b is entirely covered: e.g. 1991-1999 in 1990-2000
	 * 2. period b is partly covered 1): e.g. 1980-1999 in 1990-2000
	 * 3. period b is partly covered 2): e.g. 1991-2013 in 1990-2000
	 * 4. period a is entirely covered: e.g. 1980-2013 in 1990-2000
	 *
	 * @param range1
	 * @param range2
	 * @return
	 */
	static boolean overlap(List<String> range1, List<String> range2) {
		try {
			float[] info1 = toFloatArray(range1);
			float[] info2 = toFloatArray(range2);
			float year1a = info1[0]; float year1b = info1[1];
			float year2a = info2[0]; float year2b = info2[1];
			// probably not a range after all (e.g. Ausländer in Deutschland 2000 - 2. Welle)
			if (year1a > year1b || year2a > year2b)  { return false; }
			boolean case1 = ((year2a >= year1a) && (year2b <= year1b));
			boolean case2 = ((year2a < year1a) && (year2b <= year1b) && (year2b >= year1a));
			boolean case3 = ((year2a >= year1a) && (year2b > year1b) && (year2a <= year1b));
			boolean case4 = ((year2a <= year1a) && (year2b >= year1b));
			return (case1 || case2 || case3 || case4);
		}
		catch (NumberFormatException nfe) { log.debug(nfe.getMessage()); return false; }
	}

	private static List<String> extractNumbers (String string) {
		Pattern p = Pattern.compile(DaraLinker.numberRegex);
		Matcher matcher = p.matcher(string);
		List<String> numericInfo = new ArrayList<String>();
		while (matcher.find()) {
			// remove "." and "," if not followed by any number (1. -> 1; 1.0 -> 1.0)
	        numericInfo.add(matcher.group().replaceAll("[.,](?!\\d)", ""));
	    }
		return numericInfo;
	}
}