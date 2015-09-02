//package io.github.infolis.algorithm;
//
//import io.github.infolis.datastore.DataStoreClient;
//import io.github.infolis.datastore.FileResolver;
//import io.github.infolis.model.Execution;
//import io.github.infolis.model.ExecutionStatus;
//import io.github.infolis.model.entity.InfolisFile;
//import io.github.infolis.model.Execution.Strategy;
//import io.github.infolis.util.SerializationUtils;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.apache.commons.io.FileUtils;
//import org.apache.lucene.queryParser.ParseException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//
///**
// * Class for finding references to scientific datasets in publications using a
// * minimum supervised iterative pattern induction approach. For a description of
// * the basic algorithm see
// * <emph>Boland, Katarina; Ritze, Dominique; Eckert, Kai; Mathiak, Brigitte
// * (2012): Identifying references to datasets in publications. In: Zaphiris,
// * Panayiotis; Buchanan, George; Rasmussen, Edie; Loizides, Fernando (Hrsg.):
// * Proceedings of the Second International Conference on Theory and Practice of
// * Digital Libraries (TDPL 2012), Paphos, Cyprus, September 23-27, 2012. Lecture
// * notes in computer science, 7489, Berlin: Springer, S. 150-161. </emph>. Note
// * that some features are not described in this publication.
// *
// * @author kata
// *
// */
//// TODO kba: Improper Algorithm implementation
//// TODO kb: changed to extending BaseAlgorithm for now
//// TODO add optional conversion to pdf
//public class Learner extends BaseAlgorithm {
//
//    Logger log = LoggerFactory.getLogger(Learner.class);
//
//    /**
//     * Class constructor specifying the constraints for patterns.
//     *
//     * @param constraint_NP	if set, references are only accepted if assumed
//     * dataset name occurs in nominal phrase
//     * @param constraint_upperCase	if set, references are only accepted if
//     * dataset name has at least one upper case character
//     *
//     */
//    public Learner(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) throws IOException {
//    	super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
//    }
//
//    
//    /**
//     * Get filenames of all documents listed in given path (non-recursive).
//     * 
//     * @param path root directory of documents
//     */
//    private List<String> getDocumentNames(File path) {
//    	List<String> corpus = new ArrayList<>();
//    	for (String filename : path.list()) {
//    		corpus.add(path + File.separator + filename);
//    		log.debug("added to corpus: " + path + File.separator + filename);
//    	}
//    	return corpus;
//    } 
//    
//    private List<String> createInfolisFiles(List<String> filenames) throws IOException
//    {
//    	List<String> uris = new ArrayList<>();
//    	for (String filename : filenames) {
//    		String content = FileUtils.readFileToString(new File(filename));
//    		//byte[] content = IOUtils.toByteArray(getClass().getResourceAsStream(filename));
//    		InfolisFile file = new InfolisFile();
//	    	file.setMd5(SerializationUtils.getHexMd5(content));
//			file.setFileName(filename);
//			file.setFileStatus("AVAILABLE");
//			file.setMediaType("text/plain");
//			//file.setMediaType("application/pdf");
//			this.getInputDataStoreClient().post(InfolisFile.class, file);
//	        uris.add(file.getUri());
//	        log.debug("new uri for filename \"" + filename + "\": " + file.getUri());
//    	}
//    	return uris;
//   	}
//    //TODO check
//    private List<String> getTextDocuments(List<String> uris) {
//    	Execution execution = new Execution();
//    	execution.setInputFiles(uris);
//		execution.setAlgorithm(TextExtractorAlgorithm.class);
//		this.getInputDataStoreClient().post(Execution.class, execution);
//		Algorithm algo = execution.instantiateAlgorithm(this.getInputDataStoreClient(), this.getOutputDataStoreClient(), this.getInputFileResolver(), this.getOutputFileResolver());
//		algo.run();
//		log.debug("{}", execution.getOutputFiles());
//    	return execution.getOutputFiles();
//    }
//    
//    //TODO call textExtractor first in case pdf text extraction is desired
//    private List<String> getInputCorpus(File path) throws IOException {
//    	//return getTextDocuments(createInfolisFiles(getDocumentNames(path)));
//    	return createInfolisFiles(getDocumentNames(path));
//    }
//
//    /**
//     * Generates extraction patterns using an iterative bootstrapping approach.
//     *
//     * <ol>
//     * <li>searches for seeds in the specified corpus and extracts the
//     * surrounding words as contexts</li>
//     * <li>analyzes contexts and generates extraction patterns</li>
//     * <li>applies extraction patterns on corpus to extract new seeds</li>
//     * <li>continues with 1) until maximum number of iterations is reached</li>
//     * <li>outputs found seeds, contexts and extraction patterns</li>
//     * </ol>
//     *
//     * Method for assessing pattern validity is frequency- or reliability-based.
//     *
//     **/
//    private List<String> bootstrap() throws IOException, ParseException {
//        Execution e = new Execution();
//        e.setBootstrapStrategy(this.getExecution().getBootstrapStrategy());
//        if (this.getExecution().getBootstrapStrategy().equals(Strategy.reliability)) {
//        	e.setAlgorithm(ReliabilityBasedBootstrapping.class); 
//        }
//        else { e.setAlgorithm(FrequencyBasedBootstrapping.class); }
//        e.setInputFiles(this.getExecution().getInputFiles());
//        e.setTerms(this.getExecution().getTerms());
//        e.setThreshold(this.getExecution().getThreshold());
//        e.setMaxIterations(this.getExecution().getMaxIterations());
//        e.setUpperCaseConstraint(this.getExecution().isUpperCaseConstraint());
//        Algorithm algorithm = e.instantiateAlgorithm(getInputDataStoreClient(), getOutputDataStoreClient(), getInputFileResolver(), getOutputFileResolver());
//        algorithm.run();
//        log.debug("input files: " + this.getExecution().getInputFiles());
//        return e.getStudyContexts();
//    }
//
//
//    @Override
//    public void execute() {
//    	try {
//    		//TODO more than one root directory may be given as input corpus...
//        	getExecution().setInputFiles(getInputCorpus(new File(getExecution().getFirstInputFile())));
//    		List<String> contextURIs = bootstrap();
//    		getExecution().setStudyContexts(contextURIs);
//			getExecution().setStatus(ExecutionStatus.FINISHED);
//		} catch (Exception e) {
//			log.error("Error executing Learner: " + e);
//			e.printStackTrace();
//			getExecution().setStatus(ExecutionStatus.FAILED);
//		}
//    }
//
//    @Override
//    public void validate() {
//        // TODO Auto-generated method stub
//
//    }
//
//}
//
