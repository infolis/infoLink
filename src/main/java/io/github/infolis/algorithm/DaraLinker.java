package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.ExtractionMethod;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.resolve.DaraSolrMatcher;
import io.github.infolis.resolve.FilterDaraJsonResults;
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

	public List<EntityLink> createLinks(List<String> contextURIs) throws IOException {
		List<EntityLink> links = new ArrayList<>();
		for (TextualReference context : getInputDataStoreClient().get(TextualReference.class, contextURIs)) {
			links.addAll(linkContext(context));
		}
		return links;
	}

	private Set<EntityLink> linkContext(TextualReference context) throws IOException {
		Entity study = extractStudy(context);
		log.debug("study: " + study.getName());
		// whitespace create problems for json parsing
		String studyName = study.getName().replaceAll("\\s", "-").trim();
		log.debug("search term: " + studyName);
		DaraSolrMatcher matcher = new DaraSolrMatcher(studyName);
		JsonArray candidates = matcher.query();
		log.debug("number of candidates in dara: " + String.valueOf(candidates.size()));
		JsonArray matchingDatasets = FilterDaraJsonResults.filter(candidates, study);
		Set<EntityLink> links = createStudyLinks(matchingDatasets, study, context);
		return links;
	}

	private Set<Entity> getDaraStudies(JsonArray datasets) throws IllegalArgumentException {
		Set<Entity> daraStudies = new HashSet<>();
		for (JsonObject item : datasets.getValuesAs(JsonObject.class)) {
			log.debug("item: " + item.toString());
			// each dataset in dara should have exactly one DOI
                        //TODO: set the DOI within the instance?
			JsonArray dois = item.getJsonArray("doi");
			String doi = dois.getString(0);
			log.debug("DOI: " + doi);
			// datasets must have at least one title
			// may have more than one title: e.g. translations
			JsonArray titles = item.getJsonArray("title");
			//String[] titleStringArray = new String[titles.size()];                        
                        Entity daraStudy = new Entity();
			
			for (int i = 0; i < titles.size(); i++) { 
                            log.debug("title " + i + ": " + titles.get(i).toString());
                            if(i==0) {
                                daraStudy.setName(titles.get(0).toString());
                            }
                            else {
                                daraStudy.addAlternativeNames(titles.get(i).toString());
                            }				
			}
                        getOutputDataStoreClient().post(Entity.class, daraStudy);
			daraStudies.add(daraStudy);
		}
                
		return daraStudies;
	}

	// TODO: use URIs in StudyLinks for all entities?
	private Set<EntityLink> createStudyLinks(JsonArray datasets, Entity study, TextualReference context) {
		Set<EntityLink> links = new HashSet<>();
		Set<Entity> daraStudies = getDaraStudies(datasets);
		Entity publication =  getInputDataStoreClient().get(Entity.class, context.getMentionsReference());
		for (Entity daraStudy : daraStudies) {
			float confidence = 0; // score, how to add?
			String snippet = context.toPrettyString();
			// where  to  get  that  info  from? // context?
			ExtractionMethod extractionMethod = ExtractionMethod.PATTERN;
			// TODO: implement methods for string and url links, not only doi...
			// TODO: use other titles as well, not only the first one
                        
                        //TODO not really a nice link reason by now
			EntityLink link = new EntityLink(daraStudy.getUri(),publication.getUri(), confidence, snippet + " " +extractionMethod);
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

	private List<String> extractNumericInfo(TextualReference context) {
		List<String> numericInfo = new ArrayList<>();
		for (String string : Arrays.asList(context.getReference(), context.getRightText(), context.getLeftText())) {
			String year = searchComplexNumericInfo(string);
			if (year != null) {
				numericInfo.add(year);
			}
		}
		return numericInfo;
	}

	Entity extractStudy(TextualReference context) {
		List<String> numericInfo = extractNumericInfo(context);
		Entity study = new Entity();
		if (RegexUtils.ignoreStudy(context.getReference())) {
			return study;
		}
		// remove numeric info from study name
		String studyName = context.getReference().replaceAll("[^a-zA-Z]", "");
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
			List<String> contexts = getExecution().getTextualReferences();
			debug(log, "Linking " + String.valueOf(contexts.size()) + " contexts.");
			List<EntityLink> studyLinks = createLinks(contexts);                        
                        List<String> linkURIs = new ArrayList<>();
                        for(EntityLink el : studyLinks) {
                            getOutputDataStoreClient().post(EntityLink.class, el);
                            linkURIs.add(el.getUri());
                        }
			getExecution().setLinks(linkURIs);
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
