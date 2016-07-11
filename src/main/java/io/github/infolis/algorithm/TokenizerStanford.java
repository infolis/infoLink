package io.github.infolis.algorithm;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.WordToSentenceProcessor;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;

/**
 *  
 * @author kata
 *
 */
public class TokenizerStanford extends Tokenizer {

	private final static List<String> compoundMarkers = Arrays.asList("(-)", "(–)", "(/)");
	
	public TokenizerStanford (DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) throws IOException {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private static final Logger log = LoggerFactory.getLogger(TokenizerStanford.class);
	
	public List<String> getTokenizedSentences(String text) {
		Reader reader = new StringReader(text);
		return tokenize(reader, getExecution().getTokenizeNLs(), getExecution().getPtb3Escaping());
	}
	
	public List<String> getTokenizedSentences(File file) {
		return tokenize(file.getAbsolutePath(), getExecution().getTokenizeNLs(), getExecution().getPtb3Escaping());
	}
	
	public static List<String> tokenize(String filename, boolean tokenizeNLs, boolean ptb3Escaping) {
		DocumentPreprocessor dp = new DocumentPreprocessor(filename);
		return applyPTBTokenizer(dp, tokenizeNLs, ptb3Escaping);
	}
	
	public static List<String> tokenize(Reader reader, boolean tokenizeNLs, boolean ptb3Escaping) {
		DocumentPreprocessor dp = new DocumentPreprocessor(reader);
		return applyPTBTokenizer(dp, tokenizeNLs, ptb3Escaping);
	}
	
	// splitCompounds has to be applied afterwards if desired
	public static List<List<CoreLabel>> getInvertibleSentences(String text, boolean tokenizeNLs, boolean ptb3Escaping) {
		List<CoreLabel> tokens = new ArrayList<CoreLabel>();
		PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(text), new CoreLabelTokenFactory(), "tokenizeNLs=" + tokenizeNLs + ",ptb3Escaping=" + ptb3Escaping + ",asciiQuotes=true,invertible=true");
		while (tokenizer.hasNext()) {
		    tokens.add(tokenizer.next());
		}
		List<List<CoreLabel>> sentences = new WordToSentenceProcessor<CoreLabel>().process(tokens);
		return sentences;
	}
	
	private static List<String> applyPTBTokenizer(DocumentPreprocessor dp, boolean tokenizeNLs, boolean ptb3Escaping) {
		PTBTokenizerFactory<Word> tf = PTBTokenizer.PTBTokenizerFactory.newWordTokenizerFactory("tokenizeNLs=" + tokenizeNLs + ",ptb3Escaping=" + ptb3Escaping + ",asciiQuotes=true");
		dp.setTokenizerFactory(tf);
		List<String> sentences = new ArrayList<>();
		for (List<HasWord> wordList : dp) {
			String sentence = "";
			for (HasWord word : wordList) {
				sentence += " " + splitCompounds(word.word());
			}
			sentences.add(sentence);
		}
		return sentences;
	}
	
	public static String splitCompounds(String text) {
		return text.replaceAll("(?<=\\S)(" + String.join("|", compoundMarkers) + ")(?=\\S)", " $1 ");
	}
	
	
	@Override
	//TODO
	public void validate() {
		//getExecution().getTokenizeNLs()
		//getExecution().getPtb3Escaping()
	}

}