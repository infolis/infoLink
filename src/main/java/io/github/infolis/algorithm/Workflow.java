package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.DataStoreClientFactory;
import io.github.infolis.datastore.DataStoreStrategy;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.model.BootstrapStrategy;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisPattern;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 *
 * @author domi
 */
public class Workflow {

    //Variables, methods from the InfolisBaseTest...
    protected DataStoreClient dataStoreClient = DataStoreClientFactory.create(DataStoreStrategy.TEMPORARY);
    protected FileResolver fileResolver = FileResolverFactory.create(DataStoreStrategy.TEMPORARY);


    public List<String> applyPatternAndResolveRefs(List<String> pattern, List<String> input, List<String> queryServices) {
        System.out.println("in");
        List<String> createdLinks = new ArrayList<>();
        List<String> textRef = searchPattern(pattern, input);
        for (String s : textRef) {
            String searchQuery = extractMetaData(s);
            List<String> searchRes = searchInRepositories(searchQuery, queryServices);
            List<String> entityLinks = resolve(searchRes, s);
            createdLinks.addAll(entityLinks);
        }
        return createdLinks;
    }
    
    public List<String> resolve(List<String> searchResults, String textRef) {
        Execution resolve = new Execution();
        resolve.setAlgorithm(Resolver.class);
        resolve.setSearchResults(searchResults);
        List<String> textRefs = Arrays.asList(textRef);
        resolve.setTextualReferences(textRefs);
        dataStoreClient.post(Execution.class, resolve);
        resolve.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        return resolve.getLinks();
    }

    public List<String> searchInRepositories(String query, List<String> queryServices) {
        Execution searchRepo = new Execution();
        searchRepo.setAlgorithm(FederatedSearcher.class);
        searchRepo.setSearchQuery(query);
        searchRepo.setQueryServices(queryServices);
        dataStoreClient.post(Execution.class, searchRepo);
        searchRepo.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        return searchRepo.getSearchResults();
    }

    public String extractMetaData(String textualReference) {
        Execution extract = new Execution();
        extract.setAlgorithm(MetaDataExtractor.class);
        List<String> textRefs = Arrays.asList(textualReference);
        extract.setTextualReferences(textRefs);
        dataStoreClient.post(Execution.class, extract);
        extract.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        return extract.getSearchQuery();
    }

    public List<String> searchSeed(String seed, List<String> input) {
        Execution search = new Execution();
        search.setAlgorithm(SearchTermPosition.class);
        search.setSearchTerm(seed);
        search.setSearchQuery(seed);
        search.setInputFiles(input);
        dataStoreClient.post(Execution.class, search);
        search.instantiateAlgorithm(dataStoreClient, fileResolver).run();

        ArrayList<TextualReference> contextList = new ArrayList<>();
        for (String uri : search.getTextualReferences()) {
            contextList.add(dataStoreClient.get(TextualReference.class, uri));
        }
        for (TextualReference sc : contextList) {
            System.out.println("context: " + sc.toString());
        }

        return search.getTextualReferences();
    }

    public List<String> searchPattern(List<String> pattern, List<String> input) {
        Execution search = new Execution();
        search.setAlgorithm(PatternApplier.class);
        search.setPatternUris(pattern);
        search.setInputFiles(input);
        dataStoreClient.post(Execution.class, search);
        search.instantiateAlgorithm(dataStoreClient, fileResolver).run();

        ArrayList<TextualReference> contextList = new ArrayList<>();
        for (String uri : search.getTextualReferences()) {
            contextList.add(dataStoreClient.get(TextualReference.class, uri));
        }
        for (TextualReference sc : contextList) {
            System.out.println("context: " + sc.toString());
        }
        return search.getTextualReferences();
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
            //printFileNameOfContext(sc);
        }

        for (InfolisPattern p : patternList) {
            System.out.println("pattern: " + p.getPatternRegex());
        }

        return bootstrapping.getPatterns();
    }

}
