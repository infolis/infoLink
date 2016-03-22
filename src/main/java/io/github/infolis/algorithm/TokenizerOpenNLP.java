package io.github.infolis.algorithm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;

/**
 * Tokenizer for different languages (using OpenNLP tokenizer).
 * 
 * @author kata
 *
 */
public class TokenizerOpenNLP extends Tokenizer {
	
	// TODO path to model as Execution param
	// TODO download script for models...
	public TokenizerOpenNLP (DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) throws IOException {
		super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
		initialize("src/main/resources/de-token.bin", "src/main/resources/de-sent.bin");
	}
	
	private opennlp.tools.tokenize.Tokenizer tokenizer;
	private SentenceDetectorME sentenizer;
	private static final Logger log = LoggerFactory.getLogger(TokenizerOpenNLP.class);
	
	
	public void initialize(String modelPathTokenize, String modelPathSentenize) throws InvalidFormatException, IOException {
		InputStream modelInTokenize = new FileInputStream(modelPathTokenize);
		TokenizerModel modelTokenize = new TokenizerModel(modelInTokenize);
		tokenizer = new TokenizerME(modelTokenize);
		
		InputStream modelInSentenize = new FileInputStream(modelPathSentenize);
		SentenceModel modelSentenize = new SentenceModel(modelInSentenize);
		sentenizer = new SentenceDetectorME(modelSentenize);
	}
	
	public String tokenize(String input) throws InvalidFormatException, IOException {
		return String.join(" ", this.tokenizer.tokenize(input));
	}
	
	public String[] sentenize(String input) throws InvalidFormatException, IOException  {
		return this.sentenizer.sentDetect(input);
	}
	
	public List<String> getTokenizedSentences(String filename) throws InvalidFormatException, IOException {
		String[] sentences = sentenize(FileUtils.readFileToString(new File(filename)));
		List<String> tokenizedSentences = new ArrayList<>();
		for (String sentence : sentences) {
			tokenizedSentences.add(tokenize(sentence));
		}
		return tokenizedSentences;
	}
	
	@Override
	//TODO
	public void validate() {
		
	}
}