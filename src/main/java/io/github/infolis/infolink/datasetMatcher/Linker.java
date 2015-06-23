package io.github.infolis.infolink.datasetMatcher;

import io.github.infolis.algorithm.BaseAlgorithm;
import io.github.infolis.algorithm.IllegalAlgorithmArgumentException;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.ExtractionMethod;
import io.github.infolis.model.Study;
import io.github.infolis.model.StudyContext;
import io.github.infolis.model.StudyLink;
import io.github.infolis.model.StudyType;
import io.github.infolis.util.LimitedTimeMatcher;
import io.github.infolis.ws.server.InfolisConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Linker extends BaseAlgorithm {
	
	private static final Logger log = LoggerFactory.getLogger(Linker.class);
	
	private static final String enumRegex = "(([,;/&\\\\])|(and)|(und))";
	private static final String yearRegex = "(\\d{4})";
	private static final String yearAbbrRegex = "('\\d\\d)";
	private static final String numberRegex = "(\\d+[.,]?\\d*)"; //this includes yearRegex
	private static final String rangeRegex = "(([-–])|(bis)|(to)|(till)|(until))";
	
	private static final String numericInfoRegex = "(" + yearRegex + "|" + yearAbbrRegex + "|" + numberRegex + ")";
	private static final String enumRangeRegex = "(" + enumRegex + "|" + rangeRegex + ")";
	static final String complexNumericInfoRegex = "(" + numericInfoRegex + "(\\s*" + enumRangeRegex + "\\s*" + numericInfoRegex + ")*)";
	
	private static final long maxTimeMillis = 75000;
	
	public Linker() {	
	}
	
	public Set<StudyLink> createLinks(List<String> contextURIs) throws IOException {
		Set<StudyLink> links = new HashSet<>();
		for (StudyContext context : resolveContextURIs(contextURIs)) {
			links.addAll(linkContext(context));
		}
		return links;
	}
	
	private Set<StudyLink> linkContext(StudyContext context) throws IOException {
		Study study = extractStudy(context);
		log.debug("study: " + study.getName());
		// whitespace create problems for json parsing
		String studyName = study.getName().replaceAll("\\s", "-").trim();
		log.debug("search term: " + studyName);
		DaraSolrMatcher matcher = new DaraSolrMatcher(studyName);
		JsonArray candidates = matcher.query();
		log.debug("number of candidates in dara: " + String.valueOf(candidates.size()));
		JsonArray matchingDatasets = Filter.filter(candidates, study);
		Set<StudyLink> links = createStudyLinks(matchingDatasets, study, context);
		return links;
	}
	
	private class DaraStudy {
		
		String[] title;
		String doi;
		
		DaraStudy(String[] title, String doi) {
			this.title = title;
			this.doi = doi;
		}
	}
	
	private Set<DaraStudy> getDaraStudies(JsonArray datasets) throws IllegalArgumentException {
		HashSet<DaraStudy> daraStudies = new HashSet<>();
		for (JsonObject item : datasets.getValuesAs(JsonObject.class)) {
			log.debug("item: " + item.toString());
			// each dataset in dara should have exactly one DOI
			JsonArray dois = item.getJsonArray("doi");
			String doi = dois.getString(0);
			log.debug("DOI: " + doi);
			// datasets must have at least one title
			// may have more than one title: e.g. translations
			JsonArray titles = item.getJsonArray("title");
			String[] titleStringArray = new String[titles.size()];
			for (int i=0; i<titles.size(); i++) {
				titleStringArray[i] = titles.get(i).toString();
				log.debug("title " + i + ": " + titles.get(i).toString());
			}
			DaraStudy daraStudy = new DaraStudy(titleStringArray, doi);
			daraStudies.add(daraStudy);
		}
		return daraStudies;
	}
	
	// TODO: use URIs in StudyLinks for all entities?
	private Set<StudyLink> createStudyLinks(JsonArray datasets, Study study, StudyContext context){
		Set<StudyLink> links = new HashSet<>();
		Set<DaraStudy> daraStudies = getDaraStudies(datasets);
		String publication = context.getFile();
		for (DaraStudy daraStudy : daraStudies) {
			float confidence = 0; //score, how to add?
			String snippet = context.toPrettyString();
			ExtractionMethod extractionMethod = ExtractionMethod.PATTERN; //where to get that info from? context?
			//TODO: implement methods for string and url links, not only doi...
			//TODO: use other titles as well, not only the first one
			StudyLink link = new StudyLink(publication, daraStudy.title[0], study.getNumber(), study.getName(), daraStudy.doi, StudyType.DOI,
					confidence, snippet, extractionMethod);
			links.add(link);
		}
		return links;
	}
	
	private Set<StudyContext> resolveContextURIs(List<String> contextURIs) {
    	Set<StudyContext> contextSet = new HashSet<>();
    	for (String uri : contextURIs) {
    		contextSet.add(getDataStoreClient().get(StudyContext.class, uri));
    	}
    	return contextSet;
	}
	
	private String search(String string) {
		LimitedTimeMatcher ltm = new LimitedTimeMatcher(Pattern.compile(complexNumericInfoRegex), string, maxTimeMillis, string + "\n" + complexNumericInfoRegex);
		ltm.run();
		// if thread was aborted due to long processing time, matchFound should be false
        if (! ltm.finished()) {
            //TODO: what to do if search was aborted?
            log.error("Search was aborted. TODO");
        }
        while (ltm.finished() && ltm.matched()) {
           return ltm.group();
        }
        return null;
	}
	
	private List<String> extractNumericInfo(StudyContext context) {
		List<String> numericInfo = new ArrayList<String>();
			for (String string : Arrays.asList(context.getTerm(), context.getRightText(), context.getLeftText())) {
				String year = search(string);
				if (year != null) { numericInfo.add(year); }
			}
		return numericInfo;
	}
	
	private boolean ignoreStudy(String studyname) {
	// TODO should work but need to test it
		for (String ignorePattern : InfolisConfig.getIgnoreStudy()) {
			if (studyname.matches(ignorePattern)) {
		    	return true;
		    }
		}
		return false;
	}
        
    private Study extractStudy(StudyContext context) {
    	List<String> numericInfo = extractNumericInfo(context);
        Study study = new Study();
        if (ignoreStudy(context.getTerm())) { return study; }
        // remove numeric info from study name
        String studyName = context.getTerm().replaceAll("[^a-zA-Z]", "");
        study.setName(studyName);
        // 1. prefer mentions found inside of term
        // 2. prefer mentions found in right context
        // 3. accept mentions found in left context
        // TODO: better heuristic for choosing best numeric info item?
        if (numericInfo.size() > 0) { study.setNumber(numericInfo.get(0)); }
        else { study.setNumber(null); }
        return study;
    }

	@Override
	public void execute() throws IOException {
		try {
			//TODO: post links etc.
			List<String> contexts = getExecution().getStudyContexts();
			log.debug("Linking " + String.valueOf(contexts.size()) + " contexts.");
			getExecution().setLinks(createLinks(contexts));
			getExecution().setStatus(ExecutionStatus.FINISHED);
		}
		catch (Exception e) { 
			log.error("Error executing Linker: " + e);
			getExecution().setStatus(ExecutionStatus.FAILED); 
		}
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub
		
	}
}
