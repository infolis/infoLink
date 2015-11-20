package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.BootstrapStrategy;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.InfolisPattern;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.resolve.HTMLQueryService;
import io.github.infolis.resolve.QueryService;
import io.github.infolis.util.SerializationUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.search.BooleanQuery;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author domi
 */
//@Ignore
public class ExampleChecker extends InfolisBaseTest {

    //@Test    
    public void resolveDOI() throws IOException {
        List<String> qServices = postQueryServices();
        String sq = postDoiQuery("10.4232/1.2525");
        List<String> searchRes = searchInRepositories(sq, qServices);

        //TextualReference ref = new TextualReference("10.4232/1.2525", TextualReference.ReferenceType.DOI);
        //dataStoreClient.post(TextualReference.class, ref);
        
        //TODO: create an entity link? to what?
        //List<String> entityLinks = resolve(searchRes, ref.getUri());

    }

    public String postDoiQuery(String q) throws IOException {
        SearchQuery sq = new SearchQuery();
        sq.setQuery(q);
        //sq.setReferenceType(TextualReference.ReferenceType.DOI);
        dataStoreClient.post(SearchQuery.class, sq);
        return sq.getUri();
    }

    public List<String> searchInRepositories(String query, List<String> queryServices) {
        Execution searchRepo = new Execution();
        searchRepo.setAlgorithm(FederatedSearcher.class);
        searchRepo.setSearchQuery(query);
        searchRepo.setQueryServices(queryServices);
        dataStoreClient.post(Execution.class, searchRepo);
        System.out.println("q: " + query + " qs " + queryServices.get(0));
        searchRepo.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        return searchRepo.getSearchResults();
    }

    public List<String> postQueryServices() throws IOException {
        List<String> postedQueryServices = new ArrayList<>();
        QueryService p1 = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show", 0.5);
        dataStoreClient.post(QueryService.class, p1);
        postedQueryServices.add(p1.getUri());
        return postedQueryServices;
    }

    //@Test
    public void checkExamples() throws IOException {

        File pdfDir = new File(getClass().getResource("/examples/pdfs").getFile());
        File txtDir = new File(getClass().getResource("/examples/txts").getFile());
        File patternFile = new File(getClass().getResource("/examples/pattern.txt").getFile());

        learn(pdf2txt(pdfDir));
        //searchSeed("ALLBUS",pdf2txt(pdfDir));
        //searchPattern(learn(pdf2txt(pdfDir)), pdf2txt(pdfDir));

        List<String> pattern = postPattern(patternFile);
        List<String> txt = postTxtFiles(txtDir);
        List<String> contexts = searchPattern(pattern, txt);
        ArrayList<TextualReference> contextList = new ArrayList<>();
        for (String uri : contexts) {
            contextList.add(dataStoreClient.get(TextualReference.class, (uri)));
        }
        for (TextualReference sc : contextList) {
            System.out.println("context: " + sc.toString());
            printFileNameOfContext(sc);
            System.out.println("study: " + sc.getTerm());
        }

    }

    protected void printFileNameOfContext(TextualReference sc) throws BadRequestException, ProcessingException {
        String fileUri = sc.getFile();
        InfolisFile file = dataStoreClient.get(InfolisFile.class, fileUri);
        System.out.println("file: " + file.getFileName());
    }

    public List<String> postPattern(File pattern) throws IOException {
        BufferedReader read = new BufferedReader(new FileReader(pattern));
        String line = read.readLine();
        List<String> postedPattern = new ArrayList<>();
        while (line != null) {
            InfolisPattern p = new InfolisPattern(line);
            dataStoreClient.post(InfolisPattern.class, p);
            postedPattern.add(p.getUri());
            line = read.readLine();
        }
        return postedPattern;
    }

    public List<String> postTxtFiles(File dir) throws IOException {
        List<String> txtFiles = new ArrayList<>();
        for (File f : dir.listFiles()) {

            Path tempFile = Files.createTempFile("infolis-", ".txt");
            InfolisFile inFile = new InfolisFile();

            FileInputStream inputStream = new FileInputStream(f.getAbsolutePath());

            int numberBytes = inputStream.available();
            byte pdfBytes[] = new byte[numberBytes];
            inputStream.read(pdfBytes);

            IOUtils.write(pdfBytes, Files.newOutputStream(tempFile));

            inFile.setFileName(tempFile.toString());
            inFile.setMd5(SerializationUtils.getHexMd5(pdfBytes));
            inFile.setMediaType("text/plain");
            inFile.setFileStatus("AVAILABLE");

            try {
                OutputStream os = fileResolver.openOutputStream(inFile);
                IOUtils.write(pdfBytes, os);
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            dataStoreClient.post(InfolisFile.class, inFile);
            txtFiles.add(inFile.getUri());
        }
        return txtFiles;
    }

    public List<String> searchPattern(List<String> pattern, List<String> input) {
        Execution search = new Execution();
        search.setAlgorithm(RegexSearcher.class);
        search.setPatternUris(pattern);
        search.setInputFiles(input);
        dataStoreClient.post(Execution.class, search);
        Algorithm algo = search.instantiateAlgorithm(dataStoreClient, fileResolver);
        try {
            algo.run();
        } catch (Exception e) {
            e.printStackTrace();
            throw (e);
        }

        ArrayList<TextualReference> contextList = new ArrayList<>();
        for (String uri : search.getTextualReferences()) {
            contextList.add(dataStoreClient.get(TextualReference.class, uri));
        }
        for (TextualReference sc : contextList) {
            System.out.println("context: " + sc.toString());
        }

        return search.getTextualReferences();
    }
    
    private Execution createIndex(List<String> input) throws IOException {
		Execution execution = new Execution();
		execution.setAlgorithm(Indexer.class);
		execution.setInputFiles(input);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
		return execution;
	}

    // TODO: bolandka @domi: I integrated the index generation here but could not test it as all tests are ignored
    //and I don't know which part is supposed to be working and which isn't
    // if you find any problem with searching the index when reactivating this class, please let me know
    public List<String> searchSeed(String seed, List<String> input) throws IOException {
        Execution search = new Execution();
        search.setAlgorithm(SearchTermPosition.class);
        Execution indexerExecution = createIndex(input);
        search.setIndexDirectory(indexerExecution.getOutputDirectory());
        search.setSearchTerm(seed);
        search.setSearchQuery(seed);
        search.setInputFiles(input);
        dataStoreClient.post(Execution.class, search);
        Algorithm algo = search.instantiateAlgorithm(dataStoreClient, fileResolver);
        algo.run();

        ArrayList<TextualReference> contextList = new ArrayList<>();
        for (String uri : search.getTextualReferences()) {
            contextList.add(dataStoreClient.get(TextualReference.class, uri));
        }
        for (TextualReference sc : contextList) {
            System.out.println("context: " + sc.toString());
        }

        return search.getTextualReferences();
    }

    public List<String> pdf2txt(File dir) throws IOException {
        Execution execution = new Execution();

        dataStoreClient.post(Execution.class, execution);

        for (File f : dir.listFiles()) {

            Path tempFile = Files.createTempFile("infolis-", ".pdf");
            InfolisFile inFile = new InfolisFile();

            FileInputStream inputStream = new FileInputStream(f.getAbsolutePath());

            int numberBytes = inputStream.available();
            byte pdfBytes[] = new byte[numberBytes];
            inputStream.read(pdfBytes);

            IOUtils.write(pdfBytes, Files.newOutputStream(tempFile));

            inFile.setFileName(tempFile.toString());
            inFile.setMd5(SerializationUtils.getHexMd5(pdfBytes));
            inFile.setMediaType("application/pdf");
            inFile.setFileStatus("AVAILABLE");

            try {
                OutputStream os = fileResolver.openOutputStream(inFile);
                IOUtils.write(pdfBytes, os);
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            dataStoreClient.post(InfolisFile.class, inFile);
            execution.getInputFiles().add(inFile.getUri());

        }
        execution.setAlgorithm(TextExtractor.class);

        Algorithm algo = execution.instantiateAlgorithm(dataStoreClient, fileResolver);
        algo.run();
        return execution.getOutputFiles();
    }

    public List<String> learn(List<String> input) {
        Execution bootstrapping = new Execution();
        bootstrapping.setAlgorithm(FrequencyBasedBootstrapping.class);

        bootstrapping.getSeeds().add("ALLBUS");
        bootstrapping.setInputFiles(input);
        bootstrapping.setSearchTerm("ALLBUS");
        bootstrapping.setMaxIterations(4);
        bootstrapping.setReliabilityThreshold(0.1);
        bootstrapping.setBootstrapStrategy(BootstrapStrategy.mergeAll);
        dataStoreClient.post(Execution.class, bootstrapping);

        Algorithm algo3 = bootstrapping.instantiateAlgorithm(dataStoreClient, fileResolver);
        algo3.run();

        ArrayList<InfolisPattern> patternList = new ArrayList<>();
        for (String uri : bootstrapping.getPatterns()) {
            patternList.add(dataStoreClient.get(InfolisPattern.class, uri));
        }

        ArrayList<TextualReference> contextList = new ArrayList<>();
        for (String uri : bootstrapping.getTextualReferences()) {
            contextList.add(dataStoreClient.get(TextualReference.class, uri));
        }
        for (TextualReference sc : contextList) {
            System.out.println("context: " + sc.toString());
            printFileNameOfContext(sc);
        }

        for (InfolisPattern p : patternList) {
            System.out.println("pattern: " + p.getPatternRegex());
        }

        return bootstrapping.getPatterns();
    }

}
