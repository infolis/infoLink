package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.infolink.datasetMatcher.FilterDaraJsonResults;
import io.github.infolis.infolink.datasetMatcher.DaraSolrMatcher;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.ExtractionMethod;
import io.github.infolis.model.Instance;
import io.github.infolis.model.StudyContext;
import io.github.infolis.model.StudyLink;
import io.github.infolis.model.StudyType;
import io.github.infolis.util.LimitedTimeMatcher;
import io.github.infolis.util.RegexUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaraLinker extends BaseAlgorithm {

	public DaraLinker(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}

	private static final Logger log = LoggerFactory.getLogger(DaraLinker.class);

	public static final String enumRegex = "(([,;/&\\\\])|(and)|(und))";
	public static final String yearRegex = "(\\d{4})";
	public static final String yearAbbrRegex = "('\\d\\d)";
	public static final String numberRegex = "(\\d+[.,]?\\d*)"; // this includes
	// yearRegex
	public static final String rangeRegex = "(([-–])|(bis)|(to)|(till)|(until))";

	public static final String numericInfoRegex = "(" + yearRegex + "|" + yearAbbrRegex + "|" + numberRegex + ")";
	public static final String enumRangeRegex = "(" + enumRegex + "|" + rangeRegex + ")";
	public static final String complexNumericInfoRegex = "(" + numericInfoRegex + "(\\s*" + enumRangeRegex + "\\s*" + numericInfoRegex + ")*)";

	private static final long maxTimeMillis = 75000;

	public Set<StudyLink> createLinks(List<String> contextURIs) throws IOException {
		Set<StudyLink> links = new HashSet<>();
		for (StudyContext context : getInputDataStoreClient().get(StudyContext.class, contextURIs)) {
			links.addAll(linkContext(context));
		}
		return links;
	}

	private Set<StudyLink> linkContext(StudyContext context) throws IOException {
		Instance study = extractStudy(context);
		log.debug("study: " + study.getName());
		// whitespace create problems for json parsing
		String studyName = study.getName().replaceAll("\\s", "-").trim();
		log.debug("search term: " + studyName);
		DaraSolrMatcher matcher = new DaraSolrMatcher(studyName);
		JsonArray candidates = matcher.query();
		log.debug("number of candidates in dara: " + String.valueOf(candidates.size()));
		JsonArray matchingDatasets = FilterDaraJsonResults.filter(candidates, study);
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
			for (int i = 0; i < titles.size(); i++) {
				titleStringArray[i] = titles.get(i).toString();
				log.debug("title " + i + ": " + titles.get(i).toString());
			}
			DaraStudy daraStudy = new DaraStudy(titleStringArray, doi);
			daraStudies.add(daraStudy);
		}
		return daraStudies;
	}

	// TODO: use URIs in StudyLinks for all entities?
	private Set<StudyLink> createStudyLinks(JsonArray datasets, Instance study, StudyContext context) {
		Set<StudyLink> links = new HashSet<>();
		Set<DaraStudy> daraStudies = getDaraStudies(datasets);
		String publication = context.getFile();
		for (DaraStudy daraStudy : daraStudies) {
			float confidence = 0; // score, how to add?
			String snippet = context.toPrettyString();
			// where  to  get  that  info  from? // context?
			ExtractionMethod extractionMethod = ExtractionMethod.PATTERN;
			// TODO: implement methods for string and url links, not only doi...
			// TODO: use other titles as well, not only the first one
			StudyLink link = new StudyLink(publication, daraStudy.title[0], study.getNumber(), study.getName(), daraStudy.doi, StudyType.DOI,
					confidence, snippet, extractionMethod);
			links.add(link);
		}
		return links;
	}

	private String searchComplexNumericInfo(String string) {
		LimitedTimeMatcher ltm = new LimitedTimeMatcher(Pattern.compile(complexNumericInfoRegex), string, maxTimeMillis, string + "\n" + complexNumericInfoRegex);
		ltm.run();
		// if thread was aborted due to long processing time, matchFound should
		// be false
		if (!ltm.finished()) {
			// TODO: what to do if search was aborted?
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
			String year = searchComplexNumericInfo(string);
			if (year != null) {
				numericInfo.add(year);
			}
		}
		return numericInfo;
	}

	Instance extractStudy(StudyContext context) {
		List<String> numericInfo = extractNumericInfo(context);
		Instance study = new Instance();
		if (RegexUtils.ignoreStudy(context.getTerm())) {
			return study;
		}
		// remove numeric info from study name
		String studyName = context.getTerm().replaceAll("[^a-zA-Z]", "");
		study.setName(studyName);
		// 1. prefer mentions found inside of term
		// 2. prefer mentions found in right context
		// 3. accept mentions found in left context
		// TODO: better heuristic for choosing best numeric info item?
		if (numericInfo.size() > 0) {
			study.setNumber(numericInfo.get(0));
		}
		else {
			study.setNumber(null);
		}
		return study;
	}

	@Override
	public void execute() throws IOException {
		try {
			// TODO: post links etc.
			List<String> contexts = getExecution().getStudyContexts();
			debug(log, "Linking " + String.valueOf(contexts.size()) + " contexts.");
			Set<StudyLink> studyLinks = createLinks(contexts);
			getExecution().setLinks(studyLinks);
			getExecution().setStatus(ExecutionStatus.FINISHED);
		} catch (Exception e) {
			log.error("Error executing Linker: " + e);
			e.printStackTrace();
			getExecution().setStatus(ExecutionStatus.FAILED);
		}
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub

	}
}
