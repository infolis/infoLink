package io.github.infolis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.MediaType;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.entity.InfolisFile;

/**
 *
 * This algorithm searches for a set of given patterns, then executes the 
 * ReferenceLinker algorithm to create EntityLinks from the resulting textualReferences.
 *
 * Used algorithms: InfolisPatternSearcher - ReferenceLinker
 *
 * @author domi
 * @author kata
 *
 */
public class SearchPatternsAndCreateLinks extends BaseAlgorithm {

    public SearchPatternsAndCreateLinks(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(SearchPatternsAndCreateLinks.class);

    @Override
    public void execute() throws IOException {

    	Execution tagExec = getExecution().createSubExecution(TagSearcher.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.getInfolisPatternTags().addAll(getExecution().getInfolisPatternTags());
    	tagExec.instantiateAlgorithm(this).run();
    	getExecution().getPatterns().addAll(tagExec.getPatterns());
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
    	
        List<String> textualRefs = searchPatterns(getExecution().getPatterns(), getExecution().getInputFiles());

        List<String> createdLinks = createLinks(textualRefs);
        
        debug(log, "Created links: " + createdLinks);
        getExecution().setLinks(createdLinks);
        getExecution().setStatus(ExecutionStatus.FINISHED);
    }
    
    private List<String> searchPatterns(List<String> patterns, List<String> input) {
    	debug(log, "Executing InfolisPatternSearcher with patterns " + patterns);
    	Execution search = getExecution().createSubExecution(InfolisPatternSearcher.class);
        search.setPatterns(patterns);
        search.setInputFiles(input);
        search.setPhraseSlop(getExecution().getPhraseSlop());
        search.setUpperCaseConstraint(getExecution().isUpperCaseConstraint());
        search.setIndexDirectory(getExecution().getIndexDirectory());
        getOutputDataStoreClient().post(Execution.class, search);
        search.instantiateAlgorithm(this).run();
        updateProgress(1, 2);
    	debug(log, "Done executing InfolisPatternSearcher, found textualReferences: " + search.getTextualReferences());
        return search.getTextualReferences();
    }
    
    protected List<String> createLinks(List<String> textualRefs) {
    	Execution exec = new Execution();
    	if (null != getExecution().getQueryServices()) {
    		exec.setQueryServices(getExecution().getQueryServices());
    	}
    	if (null != getExecution().getQueryServiceClasses()) {
    		exec.setQueryServiceClasses(getExecution().getQueryServiceClasses());
    	}
    	exec.setTextualReferences(textualRefs);
    	exec.setSearchResultLinkerClass(getExecution().getSearchResultLinkerClass());
    	exec.setAlgorithm(ReferenceLinker.class);
    	exec.instantiateAlgorithm(this).run();
    	updateProgress(2, 2);
    	debug(log, "Done executing ReferenceLinker, created entityLinks: " + exec.getLinks());
    	return exec.getLinks();
    }
    
    
    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        Execution exec = this.getExecution();
		if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) &&
        		(null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())){
            throw new IllegalArgumentException("Must set at least one inputFile!");
        }
        if ((null == exec.getPatterns() || exec.getPatterns().isEmpty()) &&
        		(null == exec.getInfolisPatternTags() || exec.getInfolisPatternTags().isEmpty()))
        {
            throw new IllegalArgumentException("No patterns given.");
        }
        boolean queryServiceSet = false;
        if (null != exec.getQueryServiceClasses() && !exec.getQueryServiceClasses().isEmpty()) {
            queryServiceSet = true;
        }
		if (null != exec.getQueryServices() && !exec.getQueryServices().isEmpty()) {
            queryServiceSet = true;
		}
		if (!queryServiceSet) {
            throw new IllegalAlgorithmArgumentException(getClass(), "queryService", "Required parameter 'query services' is missing!");
        }
		if (null == exec.getSearchResultLinkerClass()) {
			throw new IllegalAlgorithmArgumentException(getClass(), "searchResultLinkerClass", "Required parameter 'SearchResultLinkerClass' is missing!");
		}
		if (null == exec.isTokenize()) {
			warn(log, "tokenize parameter unspecified. Setting to true for SearchPatternsAndCreateLinks"); 
			exec.setTokenize(true);
		}
    }
}
