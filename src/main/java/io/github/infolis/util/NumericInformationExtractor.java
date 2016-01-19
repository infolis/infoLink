package io.github.infolis.util;

import io.github.infolis.model.TextualReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author kata
 * 
 */
public class NumericInformationExtractor {

    public static String getNumericInfo(String title) {
        LimitedTimeMatcher ltm = new LimitedTimeMatcher(Pattern.compile(RegexUtils.complexNumericInfoRegex), title, RegexUtils.maxTimeMillis, title + "\n" + RegexUtils.complexNumericInfoRegex);
        ltm.run();
        if (!ltm.finished()) {
            // TODO: what to do if search was aborted?
        }
        while (ltm.finished() && ltm.matched()) {
            return ltm.group();
        }
        return null;
    }

    public static List<String> extractNumericInfo(TextualReference context) {
        List<String> numericInfo = new ArrayList<>();
        // 1. prefer mentions found inside of term
     	// 2. prefer mentions found in right context
     	// 3. accept mentions found in left context
     	// TODO: better heuristic for choosing best numeric info item? Adjustable depending on language?
        for (String string : Arrays.asList(context.getReference(), context.getRightText(), context.getLeftText())) {
            String year = NumericInformationExtractor.getNumericInfo(string);
            if (year != null) {
                numericInfo.add(year);
            }
        }
        return numericInfo;
    }

    public static List<String> extractNumbers(String string) {
        Pattern p = Pattern.compile(RegexUtils.numberRegex);
        Matcher matcher = p.matcher(string);
        List<String> numericInfo = new ArrayList<>();
        while (matcher.find()) {
            // remove "." and "," if not followed by any number (1. -> 1; 1.0 -> 1.0)
            numericInfo.add(matcher.group().replaceAll("[.,](?!\\d)", ""));
        }
        return numericInfo;
    }

}
