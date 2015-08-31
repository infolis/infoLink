package io.github.infolis.algorithm;

import static io.github.infolis.algorithm.DaraLinker.complexNumericInfoRegex;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.Execution;
import io.github.infolis.model.ExecutionStatus;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.model.entity.Instance;
import io.github.infolis.util.LimitedTimeMatcher;
import io.github.infolis.util.RegexUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
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

    public static final String enumRegex = "(([,;/&\\\\])|(and)|(und))";
    public static final String yearRegex = "(\\d{4})";
    public static final String yearAbbrRegex = "('\\d\\d)";
    public static final String numberRegex = "(\\d+[.,]?\\d*)"; // this includes
    // yearRegex
    public static final String rangeRegex = "(([-–])|(bis)|(to)|(till)|(until))";

    public static final String numericInfoRegex = "(" + yearRegex + "|" + yearAbbrRegex + "|" + numberRegex + ")";
    public static final String enumRangeRegex = "(" + enumRegex + "|" + rangeRegex + ")";
    public static final String complexNumericInfoRegex = "(" + numericInfoRegex + "(\\s*" + enumRangeRegex + "\\s*" + numericInfoRegex + ")*)";

    private static final long maxTimeMillis = 75000;
    
    //methods from DataLinker to extract dates etc.
    @Override
    public void execute() throws IOException {

        String tr = getExecution().getTextualReference();
        TextualReference ref = getInputDataStoreClient().get(TextualReference.class, tr);
        
        debug(log, "Start to build query from texteual reference %s", ref);
        
        String query = extractQuery(ref);
        if(query ==null || query.isEmpty()) {
            debug(log, "could not create a query");
            getExecution().setStatus(ExecutionStatus.FAILED);
            return;
        }
        SearchQuery squery = new SearchQuery();
        squery.setQuery(query);        
        getOutputDataStoreClient().post(SearchQuery.class, squery);
        getExecution().setQueryForQueryService(squery.getUri());        
        getExecution().setStatus(ExecutionStatus.FINISHED);
        persistExecution();
    }
    
    /**
     * Extract a query (SOLR based syntax) with information contained in the context
     * like date information or version.
     * 
     * @param ref
     * @return 
     */
    public String extractQuery(TextualReference ref) {
        String finalQuery="?q=";
        if (RegexUtils.ignoreStudy(ref.getTerm())) {
            return null;
        }
        List<String> numericInfo = extractNumericInfo(ref);
        String name = ref.getTerm().replaceAll("[^a-zA-Z]", "");
        
        if(name!=null && !name.isEmpty()) {
            finalQuery += "title:"+name+"&";
        }
        if(numericInfo.size()>0) {
            for(String numInf : numericInfo) {
                finalQuery += "?date:"+numInf+"&";
            }
        }
        finalQuery = finalQuery.substring(0, finalQuery.lastIndexOf("&"));
        
        //TODO: author of publications? other information?
        return finalQuery;
    }
    

    private String searchComplexNumericInfo(String string) {
        LimitedTimeMatcher ltm = new LimitedTimeMatcher(Pattern.compile(complexNumericInfoRegex), string, maxTimeMillis, string + "\n" + complexNumericInfoRegex);
        ltm.run();
        // if thread was aborted due to long processing time, matchFound should
        // be false
        if (!ltm.finished()) {
            // TODO: what to do if search was aborted?
            log.error("Search was aborted. TODO");
        }
        while (ltm.finished() && ltm.matched()) {
            return ltm.group();
        }
        return null;
    }

    private List<String> extractNumericInfo(TextualReference context) {
        List<String> numericInfo = new ArrayList<>();
        for (String string : Arrays.asList(context.getTerm(), context.getRightText(), context.getLeftText())) {
            String year = searchComplexNumericInfo(string);
            if (year != null) {
                numericInfo.add(year);
            }
        }
        return numericInfo;
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
         if (null == getExecution().getTextualReference()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "textualReference", "Required parameter 'textual reference' is missing!");
        } 
    }
}
