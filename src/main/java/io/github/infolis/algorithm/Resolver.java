package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.datasetMatcher.QueryService;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.util.NumericInformationExtractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class Resolver extends BaseAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(Resolver.class);

    public Resolver(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    /**
     * Computes whether the numbers mentioned in the textual reference fit to
     * the numbers found in a search result. Considers ranges, abbreviated years
     * etc.
     *
     * @param textRef
     * @param result
     * @return
     */
    public double computeScorebasedOnNumbers(TextualReference textRef, SearchResult result) {
        List<String> numericInfosRef = NumericInformationExtractor.extractNumericInfoFromTextRef(textRef);
        List<String> numericInfoSearch = result.getNumericInformation();
        for (String ref : numericInfosRef) {
            for (String search : numericInfoSearch) {
                if (numericInfoMatches(ref, search)) {
                    return 1.0;
                }
            }
        }
        return 0.0;
    }

    protected boolean numericInfoMatches(String numericInfoRef, String numericInfoSearch) {
        // for study references without any specified years / numbers, accept all candidates
        // TODO: match to higher order entity according to dataset ontology (study, not dataset)

        if (numericInfoRef == null || numericInfoSearch == null) {
            return true;
        }

        List<String> numericInfo1 = NumericInformationExtractor.extractNumbersFromString(numericInfoRef);
        List<String> numericInfo2 = NumericInformationExtractor.extractNumbersFromString(numericInfoSearch);

        boolean containsRange_numericInfo1 = containsRange(numericInfoRef);
        boolean containsRange_numericInfo2 = containsRange(numericInfoSearch);
        boolean containsEnum_numericInfo1 = containsEnum(numericInfoRef);
        boolean containsEnum_numericInfo2 = containsEnum(numericInfoSearch);
        boolean containsYear_numericInfo1 = containsYear(numericInfoRef);
        boolean containsYear_numericInfo2 = containsYear(numericInfoSearch);
        boolean containsAbbrYear_numericInfo1 = containsAbbreviatedYear(numericInfoRef);
        boolean containsAbbrYear_numericInfo2 = containsAbbreviatedYear(numericInfoSearch);

        if (containsEnum_numericInfo1) {
            log.debug("Enum match for: " + numericInfoRef + " <-> " + numericInfoSearch + "?");
            return enumMatches(numericInfo1, numericInfoSearch);
        }
        if (containsEnum_numericInfo2) {
            log.debug("Enum match for: " + numericInfoSearch + " <-> " + numericInfoRef + "?");
            return enumMatches(numericInfo2, numericInfoRef);
        }
        // extracted numeric information is a range specification
        if (containsRange_numericInfo1) {
            log.debug("Range match for: " + numericInfoRef + " <-> " + numericInfoSearch + "?");
            // continue if range does not match - maybe parts do
            if (rangeMatches(numericInfo1, numericInfo2, containsRange_numericInfo2, containsYear_numericInfo2, containsAbbrYear_numericInfo2)) {
                return true;
            }
        }

        if (containsRange_numericInfo2) {
            log.debug("Range match for: " + numericInfoSearch + " <-> " + numericInfoRef + "?");
            // continue if range does not match - maybe parts do
            if (rangeMatches(numericInfo2, numericInfo1, containsRange_numericInfo1, containsYear_numericInfo1, containsAbbrYear_numericInfo1)) {
                return true;
            }
        }

        // extracted numeric info contains a year
        if (containsYear_numericInfo1) {
            log.debug("Year match for: " + numericInfoRef + " <-> " + numericInfoSearch + "?");
            return yearsMatch(numericInfo1, numericInfo2, containsYear_numericInfo2, containsAbbrYear_numericInfo2);
        }

        // extracted numeric info contains a year
        if (containsYear_numericInfo2) {
            log.debug("Year match for: " + numericInfoSearch + " <-> " + numericInfoRef + "?");
            return yearsMatch(numericInfo2, numericInfo1, containsYear_numericInfo1, containsAbbrYear_numericInfo1);
        }

        if (containsAbbrYear_numericInfo1) {
            log.debug("Abbreviated year match for: " + numericInfoRef + " <-> " + numericInfoSearch + "?");
            return abbreviatedYearsMatch(numericInfo1, numericInfo2, containsAbbrYear_numericInfo2);
        }

        if (containsAbbrYear_numericInfo2) {
            log.debug("Abbreviated year match for: " + numericInfoSearch + " <-> " + numericInfoRef + "?");
            return abbreviatedYearsMatch(numericInfo2, numericInfo1, containsAbbrYear_numericInfo1);
        } else {
            log.debug("Number match for: " + numericInfoRef + " <-> " + numericInfoSearch + "?");
            return floatsMatch(numericInfo1, numericInfo2);
        }
    }

    private static float[] toFloatArray(List<String> numberList) {
        float[] res = new float[numberList.size()];
        for (int i = 0; i < numberList.size(); i++) {
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
        } catch (NumberFormatException nfe) {
            log.debug(nfe.getMessage());
            return false;
        }
    }

    /**
     * Checks whether given value lies inside of range1.
     *
     * @param range1
     * @param value
     * @return
     */
    static boolean inRange(float[] range1, float value) {
        float year1a = range1[0];
        float year1b = range1[1];
        // probably not a range after all (e.g. Ausländer in Deutschland 2000 - 2. Welle)
        if (year1a > year1b) {
            return false;
        }
        return (value >= year1a && value <= year1b);
    }

    /**
     * Return true if both ranges overlap = Check whether period a entirely or
     * partly covers period b. Period a: year1a - year1b; period b: year2a -
     * year2b. 4 cases may occur: 1. period b is entirely covered: e.g.
     * 1991-1999 in 1990-2000 2. period b is partly covered 1): e.g. 1980-1999
     * in 1990-2000 3. period b is partly covered 2): e.g. 1991-2013 in
     * 1990-2000 4. period a is entirely covered: e.g. 1980-2013 in 1990-2000
     *
     * @param range1
     * @param range2
     * @return
     */
    static boolean overlap(List<String> range1, List<String> range2) {
        try {
            float[] info1 = toFloatArray(range1);
            float[] info2 = toFloatArray(range2);
            float year1a = info1[0];
            float year1b = info1[1];
            float year2a = info2[0];
            float year2b = info2[1];
            // probably not a range after all (e.g. Ausländer in Deutschland 2000 - 2. Welle)
            if (year1a > year1b || year2a > year2b) {
                return false;
            }
            boolean case1 = ((year2a >= year1a) && (year2b <= year1b));
            boolean case2 = ((year2a < year1a) && (year2b <= year1b) && (year2b >= year1a));
            boolean case3 = ((year2a >= year1a) && (year2b > year1b) && (year2a <= year1b));
            boolean case4 = ((year2a <= year1a) && (year2b >= year1b));
            return (case1 || case2 || case3 || case4);
        } catch (NumberFormatException nfe) {
            log.debug(nfe.getMessage());
            return false;
        }
    }

    private static boolean containsYear(String number) {
        return Pattern.matches(".*?" + NumericInformationExtractor.yearRegex + ".*?", number);
    }

    private static boolean containsAbbreviatedYear(String number) {
        return Pattern.matches("[\\D]*?" + "('?\\d\\d)" + "[^\\d\\.]*?", number);
    }

    private static boolean containsEnum(String number) {
        return Pattern.matches(".*?\\d\\s*" + NumericInformationExtractor.enumRegex + "\\s*\\d.*?", number);
    }

    private static boolean containsRange(String number) {
        return Pattern.matches(".*?\\d\\s*" + NumericInformationExtractor.rangeRegex + "\\s*\\d.*?", number);
    }

    private static String[] getFullYearVariants(String extractedNumber) {
        String number1a, number1b = extractedNumber;
        if (containsAbbreviatedYear(extractedNumber)) {
            number1a = "19" + extractedNumber;
            number1b = "20" + extractedNumber;
        } else {
            number1a = number1b = extractedNumber;
        }
        return new String[]{number1a, number1b};
    }

    // call method for every enumerated value, one match is sufficient
    private boolean enumMatches(List<String> enumInfo, String info2) {
        for (String enumeratedNumber : enumInfo) {
            if (numericInfoMatches(enumeratedNumber, info2)) {
                return true;
            }
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
                    || overlap(numericInfo1b, numericInfo2b);
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
        } else {
            return inRange(numericInfo1a, numericInfo2.get(0))
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
                        } else {
                            log.debug("No year match: " + year + " <-> " + year2);
                        }
                    }
                }
            }
            return false;
        } // candidate numeric info contains a year as well or is some number
        else {
            for (String year : numericInfo1) {
                for (String year2 : numericInfo2) {
                    if (year.equals(year2)) {
                        log.debug("Years match: " + year + " <-> " + year2);
                        return true;
                    } else {
                        log.debug("No year match: " + year + " <-> " + year2);
                    }
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
                    if (abbreviatedYear.equals(abbreviatedYear2)) {
                        return true;
                    }
                }
            }
            return false;
        } // info2 is some float number. Compare, because 90 may be an abbreviated year or a number and 90 == 90.0
        else {
            for (String info2 : numericInfo2) {
                float number2 = Float.parseFloat(info2);
                for (String abbreviatedYear : numericInfo1) {
                    float number1 = Float.parseFloat(abbreviatedYear);
                    if (Math.abs(number1 - number2) < 0.00001) {
                        log.debug("Equal: " + number1 + " <-> " + number2);
                        return true;
                    }
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
                if (number1 == number2) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void execute() throws IOException {
        List<String> searchResultURIs = getExecution().getSearchResults();
        List<SearchResult> results = getInputDataStoreClient().get(SearchResult.class, searchResultURIs);

        String textRefURI = getExecution().getTextualReferences().get(0);
        TextualReference textRef = getInputDataStoreClient().get(TextualReference.class, textRefURI);
        //check which search results fit 
        Map<SearchResult, Double> resultValues = new HashMap<>();
        int counter = 0;
        for (SearchResult r : results) {
            counter++;
            double confidenceValue = 0.0;
            //a fitting number counts twice than for example the list index
            confidenceValue += 2 * computeScorebasedOnNumbers(textRef, r);
            //reliability of the used query service
            confidenceValue += getInputDataStoreClient().get(QueryService.class, r.getQueryService()).getReliability();
            confidenceValue += 1 - ((double) r.getListIndex() / (double) results.get(results.size() - 1).getListIndex());
            resultValues.put(r, confidenceValue);
            updateProgress(counter, results.size());

        }
        //determine the best search result for the textual reference
        SearchResult bestSearchResult = null;
        double bestConfidence = -1.0;
        for (SearchResult sr : resultValues.keySet()) {
            if (resultValues.get(sr) > bestConfidence) {
                bestConfidence = resultValues.get(sr);
                bestSearchResult = sr;
            }
        }
        //create and post the instance representing the search result
        Entity referencedInstance = new Entity();
        referencedInstance.setTags(getExecution().getTags());
        referencedInstance.setIdentifier(bestSearchResult.getIdentifier());
        referencedInstance.setName(bestSearchResult.getTitles().get(0));
        referencedInstance.setNumber(bestSearchResult.getNumericInformation().get(0));
        getOutputDataStoreClient().post(Entity.class, referencedInstance);
        //TODO: how to define the link reason?
        String linkReason = textRefURI;
        //genretate the link
        System.out.println("textref: " + textRef.getTerm() + " -- " + textRef.getMentionsReference());
        System.out.println("file: " + getInputDataStoreClient().get(Entity.class, textRef.getMentionsReference()).getInfolisFile());
        EntityLink el = new EntityLink(referencedInstance, getInputDataStoreClient().get(Entity.class, textRef.getMentionsReference()), bestConfidence, linkReason);
        //TODO should EntityLink have tags?
        getOutputDataStoreClient().post(EntityLink.class, el);
        List<String> allLinks = new ArrayList<>();
        allLinks.add(el.getUri());
        //set the final link as output of this algorithm
        getExecution().setLinks(allLinks);
        getExecution().setStatus(ExecutionStatus.FINISHED);
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        if (null == getExecution().getSearchResults()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "searchResults", "Required parameter 'search results' is missing!");
        }
        if (null == getExecution().getTextualReferences() || getExecution().getTextualReferences().isEmpty()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "textualReferences", "Required parameter 'textual reference' is missing!");
        }
    }

}
