/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.entity.Entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 *
 */
public class InfolisPatternSearcher extends BaseAlgorithm {

    public InfolisPatternSearcher(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(InfolisPatternSearcher.class);
    
    private List<InfolisPattern> getInfolisPatterns(Collection<String> patternUris) {
    	List<InfolisPattern> patterns = new ArrayList<>();
    	for (String uri : patternUris) {
    		patterns.add(getInputDataStoreClient().get(InfolisPattern.class, uri));
    	}
    	return patterns;
    }
    
    private List<String> getTextRefsForLuceneQueries(
    		List<String> patternUris, DataStoreClient client) {
        Execution exec = getExecution().createSubExecution(LuceneSearcher.class);
        exec.setIndexDirectory(getExecution().getIndexDirectory());
        exec.setPhraseSlop(getExecution().getPhraseSlop());
        exec.setAllowLeadingWildcards(getExecution().isAllowLeadingWildcards());
        exec.setMaxClauseCount(getExecution().getMaxClauseCount());
        exec.setPatterns(patternUris);
        exec.setInputFiles(getExecution().getInputFiles());
    	// LuceneSearcher posts textual references but they are temporary
        exec.instantiateAlgorithm(this.getInputDataStoreClient(), client,
        		this.getInputFileResolver(), this.getOutputFileResolver()).run();
    	return exec.getTextualReferences();
    }
    
    private static String getReference(String text, String regex) {
    	Pattern p = Pattern.compile(regex);
    	Matcher m = p.matcher(text);
    	if (m.find()) return m.group(1);
    	return "";
    }

    private static boolean satisfiesUpperCaseConstraint(String string) {
    	// do not treat -RRB-, -LRB- and *NL* tokens as uppercase words
    	return !(string.replaceAll("-RRB-", "").replaceAll("-LRB-", "")
    			.replaceAll("\\*NL\\*", "").toLowerCase()
    			.equals(string.replaceAll("-RRB-", "")
    					.replaceAll("-LRB-", "")
    					.replaceAll("\\*NL\\*", "")));
    }
    
    /**
     * Retrieves contexts for InfolisPatterns using LuceneSearcher and validates them using 
     * the patterns' regular expressions. Validation is necessary because 
     * <ul>
     * <li>lucene queries in the InfolisPatterns may have wildcards for words that must match 
     * a regular expression, e.g. consist of digits only</li>
     * <li>finding named entities consisting of more than one word is enabled using lucene's 
     * phraseSlop parameter. This fuzzy matching may cause text snippets to match that are 
     * not supposed to match</li>
     * <li>lucene's Highlighters perform approximate matching of queries and text. Highlighted 
     * snippets may not always truely contain a match</li>
     * </ul>
     * @param patterns
     * @return
     */
    private List<String> getContextsForPatterns(List<String> patternUris) {
        int counter = 0, size = patternUris.size();
        log.debug("number of patterns to search for: " + size);
        DataStoreClient tempClient = this.getTempDataStoreClient();
    	// for all patterns, retrieve documents in which they occur (using lucene)
        List<String> tempPatternUris = tempClient.post(InfolisPattern.class, getInfolisPatterns(patternUris));
    	List<String> textRefsForPatterns = getTextRefsForLuceneQueries(
    			tempPatternUris, tempClient);
    	List<String> validatedTextualReferences = new ArrayList<>();
    	// open each reference once and validate with the corresponding regular expression
    	for (String textRefUri : textRefsForPatterns) {
    		TextualReference textRef = tempClient.get(TextualReference.class, textRefUri);
    		InfolisPattern pattern = tempClient.get(InfolisPattern.class, textRef.getPattern());
    		log.debug("pattern: " + pattern.getPatternRegex());
    		log.debug("candidate textual reference: " + textRef.getLeftText());
    		String referencedTerm = getReference(textRef.getLeftText(), pattern.getPatternRegex());
    		// textual reference does not match regex
	    	if ("".equals(referencedTerm)) {
	    		log.debug("Textual reference does not match regex: " + pattern.getPatternRegex());
	    		log.debug("Textual reference: " + textRef.getLeftText());
	    		continue;
	    	}
	    	if ((getExecution().isUpperCaseConstraint() && 
	    			!satisfiesUpperCaseConstraint(referencedTerm))) {
	    		log.debug("Referenced term does not satisfy uppercase-constraint \"" + 
	    				referencedTerm + "\"");
	    		continue;
	    	}
	    	// if referencedTerm contains no characters: ignore
            // TODO: not accurate - include accents etc in match... \p{M}?
            if (referencedTerm.matches("\\P{L}+")) {
                log.debug("Invalid referenced term \"" + referencedTerm + "\"");
                continue;
            }
            Entity e = tempClient.get(Entity.class, textRef.getMentionsReference());
            getOutputDataStoreClient().post(Entity.class, e);
            getOutputDataStoreClient().post(InfolisPattern.class, pattern);
            try {
              	TextualReference validatedTextRef = LuceneSearcher.getContext(referencedTerm, textRef.getLeftText(), textRef.getFile(), pattern.getUri(), e.getUri());
               	getOutputDataStoreClient().post(TextualReference.class, validatedTextRef);
                validatedTextualReferences.add(validatedTextRef.getUri());
                log.debug("added textual reference " + validatedTextRef);
            } catch (StringIndexOutOfBoundsException sioobe) { 
	        	log.warn(sioobe.getMessage());
	        	log.warn("(this is not an error if term is the first or last word in the input)");
	        	log.warn("\"" + referencedTerm + "\" in \"" + textRef.getLeftText() + "\"");
	        }
    		counter++;
    		updateProgress(counter, size);
    	}
    	tempClient.clear();
        return validatedTextualReferences;
    }

    Execution createIndex() throws IOException {
		Execution execution = getExecution().createSubExecution(Indexer.class);
		execution.setInputFiles(getExecution().getInputFiles());
        getOutputDataStoreClient().post(Execution.class, execution);
        execution.instantiateAlgorithm(this).run();
		return execution;
	}
    
    @Override
    public void execute() throws IOException {
    	Execution tagExec = new Execution();
    	tagExec.setAlgorithm(TagSearcher.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.getInfolisPatternTags().addAll(getExecution().getInfolisPatternTags());
    	tagExec.instantiateAlgorithm(this).run();
    	getExecution().getPatterns().addAll(tagExec.getPatterns());
    	getExecution().getInputFiles().addAll(tagExec.getInputFiles());
    	
    	if (null == getExecution().getIndexDirectory() || getExecution().getIndexDirectory().isEmpty()) {
    		debug(log, "No index directory specified, indexing on demand");
    		Execution indexerExecution = createIndex();
    		getExecution().setIndexDirectory(indexerExecution.getOutputDirectory());
    	}
    	log.debug("started");
        getExecution().setTextualReferences(getContextsForPatterns(getExecution().getPatterns()));
        log.debug("No. contexts found: {}", getExecution().getTextualReferences().size());
        getExecution().setStatus(ExecutionStatus.FINISHED);
    }

    @Override
    public void validate() {
    	Execution exec = this.getExecution();
		if ((null == exec.getInputFiles() || exec.getInputFiles().isEmpty()) && 
    		(null == exec.getInfolisFileTags() || exec.getInfolisFileTags().isEmpty())) {
            throw new IllegalArgumentException("Must set at least one inputFile!");
        }
        if ((null == exec.getPatterns() || exec.getPatterns().isEmpty()) && 
        		(null == exec.getInfolisPatternTags() || exec.getInfolisPatternTags().isEmpty())) {
            throw new IllegalArgumentException("No patterns given.");
        }
    }
}
