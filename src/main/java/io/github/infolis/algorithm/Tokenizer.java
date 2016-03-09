package io.github.infolis.algorithm;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.SerializationUtils;

/**
 * 
 * @author kata
 *
 */
public class Tokenizer extends BaseAlgorithm {

	public Tokenizer (DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) throws IOException {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private static final Logger log = LoggerFactory.getLogger(Tokenizer.class);
	
	public static List<String> getTokenizedSentences(String filename) {
		DocumentPreprocessor dp = new DocumentPreprocessor(filename);
		//PTBTokenizerFactory<Word> tf = PTBTokenizer.PTBTokenizerFactory.newWordTokenizerFactory("tokenizeNLs=true, ptb3Escaping=true");
		PTBTokenizerFactory<Word> tf = PTBTokenizer.PTBTokenizerFactory.newWordTokenizerFactory("tokenizeNLs=false, ptb3Escaping=false");
		dp.setTokenizerFactory(tf);
		List<String> sentences = new ArrayList<>();
		for (List<HasWord> wordList : dp) {
			String sentence = "";
			for (HasWord word : wordList) {
				sentence += " " + word.word();
			}
			sentences.add(sentence);
		}
		return sentences;
	}
	
	public static void printCoreLabels(String filename) throws FileNotFoundException {
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(new FileReader(filename), new CoreLabelTokenFactory(), "invertible=true, tokenizeNLs=true, ptb3Escaping=true");
		List<CoreLabel> labels = ptbt.tokenize();
		System.out.println(labels);
	}
	
	public String createInfolisFile(String filename, List<String> tokenizedSentences, Set<String> tags) throws IOException {
		InfolisFile infolisFile = new InfolisFile();
		String outFileName = SerializationUtils.changeFileExtension(filename, "tokenized");
        if (null != getExecution().getOutputDirectory()) {
            outFileName = SerializationUtils.changeBaseDir(outFileName, getExecution().getOutputDirectory());
        }
        String asText = "";
        for (String sentence : tokenizedSentences) {
        	asText += "\n" + sentence;
        }
        infolisFile.setFileName(outFileName);
        infolisFile.setMediaType("text/plain");
		infolisFile.setTags(tags);
		infolisFile.setMd5(SerializationUtils.getHexMd5(asText));
        infolisFile.setFileStatus("AVAILABLE");
        
        try (OutputStream outStream = getOutputFileResolver().openOutputStream(infolisFile)) {
            try {
                IOUtils.write(asText, outStream);
            } catch (IOException e) {
                warn(log, "Error copying text to output stream: " + e);
                throw e;
            }
        } catch (IOException e) {
            warn(log, "Error opening output stream to text file: " + e);
            throw e;
        }
        
        getOutputDataStoreClient().post(InfolisFile.class, infolisFile);
        return infolisFile.getUri();
	}
	
	@Override
	public void execute() throws IOException {
		Execution tagExec = getExecution().createSubExecution(TagSearcher.class);
    	tagExec.getInfolisFileTags().addAll(getExecution().getInfolisFileTags());
    	tagExec.instantiateAlgorithm(this).run();
    	getExecution().getInputFiles().addAll(tagExec.getInputFiles());
    	
    	for (String inputFileURI : getExecution().getInputFiles()) {
    		InfolisFile infolisFile = getInputDataStoreClient().get(InfolisFile.class, inputFileURI);
    		List<String> tokenizedSentences = getTokenizedSentences(infolisFile.getFileName());
    		Set<String> tagsToSet = getExecution().getTags();
    		tagsToSet.addAll(infolisFile.getTags());
    		String outputFileURI = createInfolisFile(infolisFile.getFileName(), tokenizedSentences, tagsToSet);
    		getExecution().getOutputFiles().add(outputFileURI);
    	}
	}
	
	@Override
	//TODO
	public void validate() {
		
	}

}