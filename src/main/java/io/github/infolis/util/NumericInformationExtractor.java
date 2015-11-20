package io.github.infolis.util;

import io.github.infolis.model.TextualReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author domi
 */
public class NumericInformationExtractor {

    private static final long maxTimeMillis = 75000;

    public static final String enumRegex = "(([,;/&\\\\])|(and)|(und))";
    public static final String yearRegex = "(\\d{4})";
    public static final String yearAbbrRegex = "('\\d\\d)";
    public static final String numberRegex = "(\\d+[.,]?\\d*)"; // this includes
    // yearRegex
    public static final String rangeRegex = "(([-–])|(bis)|(to)|(till)|(until))";

    public static final String numericInfoRegex = "(" + yearRegex + "|" + yearAbbrRegex + "|" + numberRegex + ")";
    public static final String enumRangeRegex = "(" + enumRegex + "|" + rangeRegex + ")";
    public static final String complexNumericInfoRegex = "(" + numericInfoRegex + "(\\s*" + enumRangeRegex + "\\s*" + numericInfoRegex + ")*)";

    public static String getNumericInfo(String title) {
        LimitedTimeMatcher ltm = new LimitedTimeMatcher(Pattern.compile(complexNumericInfoRegex), title, maxTimeMillis, title + "\n" + complexNumericInfoRegex);
        ltm.run();
        if (!ltm.finished()) {
            // TODO: what to do if search was aborted?
        }
        while (ltm.finished() && ltm.matched()) {
            return ltm.group();
        }
        return null;
    }

    public static List<String> extractNumericInfoFromTextRef(TextualReference context) {
        List<String> numericInfo = new ArrayList<>();
        for (String string : Arrays.asList(context.getReference(), context.getRightText(), context.getLeftText())) {
            String year = NumericInformationExtractor.getNumericInfo(string);
            if (year != null) {
                numericInfo.add(year);
            }
        }
        return numericInfo;
    }

    public static List<String> extractNumbersFromString(String string) {
        Pattern p = Pattern.compile(numberRegex);
        Matcher matcher = p.matcher(string);
        List<String> numericInfo = new ArrayList<>();
        while (matcher.find()) {
            // remove "." and "," if not followed by any number (1. -> 1; 1.0 -> 1.0)
            numericInfo.add(matcher.group().replaceAll("[.,](?!\\d)", ""));
        }
        return numericInfo;
    }

}
