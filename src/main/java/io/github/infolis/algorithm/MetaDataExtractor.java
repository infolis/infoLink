package io.github.infolis.algorithm;

import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.MetaDataExtractingStrategy;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.TextualReference;
import io.github.infolis.util.NumericInformationExtractor;
import io.github.infolis.util.RegexUtils;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author domi
 */
public class MetaDataExtractor extends BaseAlgorithm {

    public MetaDataExtractor(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    private static final Logger log = LoggerFactory.getLogger(MetaDataExtractor.class);

    //methods from DataLinker to extract dates etc.
    @Override
    public void execute() throws IOException {

        String tr = getExecution().getTextualReferences().get(0);
        MetaDataExtractingStrategy strat = getExecution().getMetaDataExtractingStrategy();
        
        TextualReference ref = getInputDataStoreClient().get(TextualReference.class, tr);

        debug(log, "Start to build query from textual reference %s", ref);

        String query = extractQuery(ref,strat);
        if (query == null || query.isEmpty()) {
            debug(log, "could not create a query");
            getExecution().setStatus(ExecutionStatus.FAILED);
            return;
        }
        SearchQuery squery = new SearchQuery();
        squery.setQuery(query);
        getOutputDataStoreClient().post(SearchQuery.class, squery);
        getExecution().setSearchQuery(squery.getUri());
        getExecution().setStatus(ExecutionStatus.FINISHED);
        persistExecution();
    }

    /**
     * Extract a query (SOLR based syntax) with information contained in the
     * context like date information or version.
     *
     * @param ref
     * @param strat
     * @return
     */
    public String extractQuery(TextualReference ref, MetaDataExtractingStrategy strat) {
        String finalQuery = "?q=";
        String name;
        if (RegexUtils.ignoreStudy(ref.getReference())) {
            return null;
        }
        switch (strat) {
            case title:
                List<String> numericInfo = NumericInformationExtractor.extractNumericInfoFromTextRef(ref);
                name = ref.getReference().replaceAll("[^a-zA-Z]", "");

                if (name != null && !name.isEmpty()) {
                    finalQuery += "title:" + name + "&";
                }
                if (numericInfo.size() > 0) {
                    for (String numInf : numericInfo) {
                        finalQuery += "?date:" + numInf + "&";
                    }
                }
                finalQuery = finalQuery.substring(0, finalQuery.lastIndexOf("&"));
                break;
            case doi:
                name = ref.getReference();
                finalQuery += "doi:" + name;
                break;
			case bibliography:
				break;
			case urn:
				break;
			default:
				break;
        }
        //TODO: author of publications? other information?
        return finalQuery;
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        if (null == getExecution().getTextualReferences() || getExecution().getTextualReferences().isEmpty()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "textualReference", "Required parameter 'textual reference' is missing!");
        }
    }
}
