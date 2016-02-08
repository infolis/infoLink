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
public class InformationExtractor {

    public static List<String> getNumericInfo(String title) {
    	List<String> numericInfo = new ArrayList<>();
        LimitedTimeMatcher ltm = new LimitedTimeMatcher(Pattern.compile(RegexUtils.complexNumericInfoRegex), title, RegexUtils.maxTimeMillis, title + "\n" + RegexUtils.complexNumericInfoRegex);
        ltm.run();
        if (!ltm.finished()) {
            // TODO: what to do if search was aborted?
        }
        while (ltm.finished() && ltm.matched()) {
        	if ("".equals(extractRegex(RegexUtils.doiRegex, ltm.group()))) {
        		numericInfo.add(ltm.group());
        	}
        	ltm.run();
        }
        return numericInfo;
    }
    
    public static String getBestNumericInfo(List<String> numericInfo) {
    	//prefer years to abbreviated years to numbers
    	//prefer position: term to right context to left context
    	for (String numInfo : numericInfo) {
    		Pattern yearPat = Pattern.compile(RegexUtils.yearRegex);
    		Matcher matcher = yearPat.matcher(numInfo);
    		if (matcher.find()) return numInfo;
    	}
    	for (String numInfo : numericInfo) {
    		Pattern yearPat = Pattern.compile(RegexUtils.yearAbbrRegex);
    		Matcher matcher = yearPat.matcher(numInfo);
    		if (matcher.find()) return numInfo;
    	}
    	return numericInfo.get(0);
    }

    // TODO: make priorities adjustable (depends on language: e.g. left context is more useful in English, 
    //right context more useful in German)
    /**
     * Extracts all numeric information and orders them according to confidence level:
     * high confidence: numeric information found in term
     * modest confidence: numeric information found in left context
     * low confidence: numeric information found in right context
     * 
     * @param context
     * @return
     */
    public static List<String> extractNumericInfo(TextualReference context) {
    	List<String> numericInfo = new ArrayList<>();
        for (String string : Arrays.asList(context.getReference(), context.getRightText(), context.getLeftText())) {
        	List<String> numericInfoInString = InformationExtractor.getNumericInfo(string);
            if (!numericInfoInString.isEmpty()) {
                numericInfo.addAll(numericInfoInString);
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
    
    public static String extractDOI(TextualReference ref) {
    	for (String string : Arrays.asList(ref.getReference(), ref.getRightText(), ref.getLeftText())) {
    		String doi = extractRegex(RegexUtils.doiRegex, string);
    		if (!"".equals(doi)) return doi;
    	}
    	return "";
    }
    
    public static String extractRegex(String regex, String string) {
        LimitedTimeMatcher ltm = new LimitedTimeMatcher(Pattern.compile(regex), string, RegexUtils.maxTimeMillis, string + "\n" + regex);
        ltm.run();
        if (!ltm.finished()) {
            // TODO: what to do if search was aborted?
        }
        while (ltm.finished() && ltm.matched()) {
        	return ltm.group();
        }
        return "";
    }
    
    public static String extractURL(TextualReference ref) {
    	for (String string : Arrays.asList(ref.getReference(), ref.getRightText(), ref.getLeftText())) {
    		String url = extractRegex(RegexUtils.urlRegex, string);
    		if (!"".equals(url)) return url;
    	}
    	return "";
    }

}
