package io.github.infolis.algorithm;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.util.RegexUtils;

import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author kata
 *
 */
public class NameExtractor extends BaseAlgorithm {

    	private static final Logger log = LoggerFactory.getLogger(NameExtractor.class);
	public NameExtractor(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
			FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private InfolisPattern createPatternForName(String name) {
		InfolisPattern namePat = new InfolisPattern();
		namePat.setPatternRegex("(" + Pattern.quote(name) + ")");
		namePat.setLuceneQuery(RegexUtils.normalizeQuery(name, true));
		return namePat;
	}

	private List<InfolisPattern> getPatterns(List<String> names) {
		List<InfolisPattern> patterns = new ArrayList<>();
		for (String name : names) patterns.add(createPatternForName(name));
		return patterns;
	}

	private List<String> getNames(List<String> infolisFileUris) throws IOException {
		List<String> names = new ArrayList<>();
		for (String infolisFileUri : infolisFileUris) {
			debug(log, "Extracting names from " + infolisFileUri);
			String content = IOUtils.toString(getInputFileResolver().openInputStream(infolisFileUri));
			for (String line : content.split(System.getProperty("line.separator"))) {
				if (!line.isEmpty()) names.add(line.trim());
			}
		}
		return names;
	}
	// 1. create patterns from names in list in file (inputFileUri...)
	//    give tags to patterns: just like with goldstandard... ("namelist" + filename)
	// 2. apply InfolisPatternSearcher
	private void extractNames(List<String> patternUris, List<String> inputFileUris) {
		Execution exec = getExecution().createSubExecution(InfolisPatternSearcher.class);
		exec.setLeftContextGroup(1);
		exec.setReferenceGroup(2);
		exec.setRightContextGroup(3);
		exec.setInputFiles(inputFileUris);
		exec.setPatterns(patternUris);
		exec.setIndexDirectory(getExecution().getIndexDirectory());
		exec.setTags(getExecution().getTags());
		exec.instantiateAlgorithm(this).run();
		getExecution().setTextualReferences(exec.getTextualReferences());
	}

	@Override
	public void execute() throws IOException {
    		// TODO name lists need to be given as input files; files names to extract patterns from by tags... not the best solution
        	List<String> names = getNames(getExecution().getInputFiles());

		Execution tagExec = getExecution().createSubExecution(TagSearcher.class);
    		tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    		tagExec.instantiateAlgorithm(this).run();
		List<InfolisPattern> patterns = getPatterns(names);
		List<String> patternUris = getOutputDataStoreClient().post(InfolisPattern.class, patterns);

		extractNames(patternUris, tagExec.getInputFiles());
        	getExecution().setStatus(ExecutionStatus.FINISHED);
	}

	@Override
	public void validate() throws IllegalAlgorithmArgumentException {
		// TODO Auto-generated method stub
		
	}
	
}
