package io.github.infolis.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.MediaType;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.BootstrapStrategy;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.SerializationUtils;

/**
 * 
 * @author kata
 *
 */
public class LearnPatternsAndCreateLinks extends BaseAlgorithm {
	
	public LearnPatternsAndCreateLinks(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient,
            		FileResolver inputFileResolver, FileResolver outputFileResolver) {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private static final Logger log = LoggerFactory.getLogger(LearnPatternsAndCreateLinks.class);
	
	@Override
	public void execute() {
		Execution tagExec = getExecution().createSubExecution(TagSearcher.class);
		tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
		tagExec.instantiateAlgorithm(this).run();
		getExecution().getInputFiles().addAll(tagExec.getInputFiles());
		
		List<String> toTextExtract = new ArrayList<>();
    	List<String> toTokenize = new ArrayList<>();
    	List<String> toBibExtract = new ArrayList<>();
    	
    	for (InfolisFile file : getInputDataStoreClient().get(
    			InfolisFile.class, getExecution().getInputFiles())) {
    		if (file.getMediaType().equals(MediaType.PDF.toString())) {
    			toTextExtract.add(file.getUri());
    			getExecution().getInputFiles().remove(file.getUri());
    		}
    	}
    	
    	if (!toTextExtract.isEmpty()) {
    		Execution textExtract = getExecution().createSubExecution(TextExtractor.class);
    		textExtract.setTokenize(getExecution().isTokenize());
    		textExtract.setRemoveBib(getExecution().isRemoveBib());
    		textExtract.setTags(getExecution().getTags());
    		textExtract.instantiateAlgorithm(this).run();
    		getExecution().getInputFiles().addAll(textExtract.getOutputFiles());
    	}
    	
    	if (getExecution().isTokenize() || getExecution().isRemoveBib()) {
	    	for (InfolisFile file : getInputDataStoreClient().get(
	    			InfolisFile.class, getExecution().getInputFiles())) {
	    		// if input file isn't tokenized, apply tokenizer
	    		// TODO tokenizer parameters also relevant...
	    		if (getExecution().isTokenize()) {
		    		if (!file.getTags().contains(Tokenizer.getExecutionTags().get(0))) {
		    			toTokenize.add(file.getUri());
		    			getExecution().getInputFiles().remove(file.getUri());
		    		}
	    		}
	    		// removing bibliographies is optional
	    		// if it is to be performed, check whether input files are stripped of 
	    		// their bibliography sections already
	    		if (getExecution().isRemoveBib()) {
		    		if (!file.getTags().contains(BibliographyExtractor.getExecutionTags().get(0))) {
		    			toBibExtract.add(file.getUri());
		    			getExecution().getInputFiles().remove(file.getUri());
		    		}
	    		}
	    	}
	
	    	if (getExecution().isRemoveBib() && !toBibExtract.isEmpty()) {
	    		Execution bibRemoverExec = getExecution().createSubExecution(BibliographyExtractor.class);
	    		bibRemoverExec.setTags(getExecution().getTags());
	    		for (String uri : toBibExtract) {
	    			bibRemoverExec.setInputFiles(Arrays.asList(uri));
	    			bibRemoverExec.instantiateAlgorithm(this).run();
	    			debug(log, "Removed bibliographies of input file: " + uri);
	    			if (!toTokenize.contains(uri)) {
	    				getExecution().getInputFiles().add(bibRemoverExec.getOutputFiles().get(0));
	    			}
	    			else {
	    				toTokenize.remove(uri);
	    				toTokenize.add(bibRemoverExec.getOutputFiles().get(0));
	    			}
	    		}
	    	}
	    	
	    	if (getExecution().isTokenize() && !toTokenize.isEmpty()) {
		    	Execution tokenizerExec = getExecution().createSubExecution(TokenizerStanford.class);
		    	tokenizerExec.setTags(getExecution().getTags());
		    	tokenizerExec.setTokenizeNLs(getExecution().getTokenizeNLs());
		    	tokenizerExec.setPtb3Escaping(getExecution().getPtb3Escaping());
		    	tokenizerExec.setInputFiles(toTokenize);
		    	tokenizerExec.instantiateAlgorithm(this).run();
		    	debug(log, "Tokenized input with parameters tokenizeNLs=" + tokenizerExec.getTokenizeNLs() + " ptb3Escaping=" + tokenizerExec.getPtb3Escaping());
		    	getExecution().getInputFiles().addAll(tokenizerExec.getOutputFiles());
	    	}
    	}
    	
		try {
			debug(log, "Step1: Learning patterns and extracting textual references...");
			Execution learnExec = learn();
			updateProgress(1, 2);
			debug(log, "Step 2: Creating links...");
			Execution linkingExec = createLinks(learnExec);
			updateProgress(2, 2);
			getExecution().setTextualReferences(linkingExec.getTextualReferences());
			getExecution().setLinks(linkingExec.getLinks());
			debug(log, "Done. Returning {} textual references and {} entity links", 
					getExecution().getTextualReferences().size(),
					getExecution().getLinks().size());
			log.debug(SerializationUtils.toCsv(getExecution().getLinks(), getOutputDataStoreClient()));
			getExecution().setStatus(ExecutionStatus.FINISHED);
		}
		catch (Exception e) {
			error(log, "Execution threw an Exception: {}", e);
			getExecution().setStatus(ExecutionStatus.FAILED);
		}
	}
	
	private Execution createLinks(Execution learnExec) {
		Execution linkExec = new Execution();
		linkExec.setSearchResultLinkerClass(getExecution().getSearchResultLinkerClass());
		linkExec.setAlgorithm(ReferenceLinker.class);
		linkExec.setInputFiles(getExecution().getInputFiles());
		linkExec.setTags(getExecution().getTags());
		
		linkExec.setTextualReferences(learnExec.getTextualReferences());
		if (null != getExecution().getQueryServiceClasses()) {
			linkExec.setQueryServiceClasses(getExecution().getQueryServiceClasses());
		}
		if (null != getExecution().getQueryServices()) {
			linkExec.setQueryServices(getExecution().getQueryServices());
		}
		linkExec.instantiateAlgorithm(this).run();
		return linkExec;
	}
	
	private Execution learn() {
		Execution learnExec = new Execution();
		learnExec.setTags(getExecution().getTags());
		learnExec.setInputFiles(getExecution().getInputFiles());
		learnExec.setBootstrapStrategy(getExecution().getBootstrapStrategy());
		if (getExecution().getBootstrapStrategy().equals(BootstrapStrategy.reliability)){
			learnExec.setAlgorithm(ReliabilityBasedBootstrapping.class);
		}
		else learnExec.setAlgorithm(FrequencyBasedBootstrapping.class);
		learnExec.setStartPage(getExecution().getStartPage());
		learnExec.setRemoveBib(getExecution().isRemoveBib());
		learnExec.setTokenize(getExecution().isTokenize());
		learnExec.setTokenizeNLs(getExecution().getTokenizeNLs());
		learnExec.setPtb3Escaping(getExecution().getPtb3Escaping());
		learnExec.setPhraseSlop(getExecution().getPhraseSlop());
		learnExec.setSeeds(getExecution().getSeeds());
		learnExec.setUpperCaseConstraint(getExecution().isUpperCaseConstraint());
		learnExec.setReliabilityThreshold(getExecution().getReliabilityThreshold());
		learnExec.setMaxIterations(getExecution().getMaxIterations());
		
		learnExec.instantiateAlgorithm(this).run();
		return learnExec;
	}
	
	@Override
	public void validate() {
		// TODO: Validator with validations for each parameter to choose from
		// all non-optional fields must be given...
		Execution exec = this.getExecution();
		if (null == exec.isTokenize()) {
			warn(log, "tokenize parameter unspecified. Setting to true for LearnPatternsAndCreateLinks"); 
			exec.setTokenize(true);
		}
	}
}
