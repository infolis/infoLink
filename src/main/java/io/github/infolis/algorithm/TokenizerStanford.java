package io.github.infolis.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;

/**
 *  
 * @author kata
 *
 */
public class TokenizerStanford extends Tokenizer {

	public TokenizerStanford (DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) throws IOException {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
	}
	
	private static final Logger log = LoggerFactory.getLogger(TokenizerStanford.class);
	
	public List<String> getTokenizedSentences(String filename) {
		return tokenize(filename, getExecution().getTokenizeNLs(), getExecution().getPtb3Escaping());
	}
	
	public static List<String> tokenize(String filename, boolean tokenizeNLs, boolean ptb3Escaping) {
		DocumentPreprocessor dp = new DocumentPreprocessor(filename);
		PTBTokenizerFactory<Word> tf = PTBTokenizer.PTBTokenizerFactory.newWordTokenizerFactory("tokenizeNLs=" + tokenizeNLs + ", ptb3Escaping=" + ptb3Escaping + "asciiQuotes=true");
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
	
	@Override
	//TODO
	public void validate() {
		//getExecution().getTokenizeNLs()
		//getExecution().getPtb3Escaping()
	}

}