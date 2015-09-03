package io.github.infolis.infolink.datasetMatcher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import static io.github.infolis.algorithm.MetaDataExtractor.complexNumericInfoRegex;
import io.github.infolis.model.BaseModel;
import io.github.infolis.model.entity.SearchResult;
import io.github.infolis.util.LimitedTimeMatcher;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author domi
 */
//TODO: subclass of BaseModel?
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(as = HTMLQueryService.class)
public abstract class QueryService extends BaseModel {

    public QueryService() {
    }

    private String target = "";

    public QueryService(String target) {
        this.target = target;
    }
    private static final long maxTimeMillis = 75000;
    
    public abstract List<SearchResult> executeQuery(String solrQuery);

    public String getNumericInfo(String title) {
        String num="";
        LimitedTimeMatcher ltm = new LimitedTimeMatcher(Pattern.compile(complexNumericInfoRegex), title, maxTimeMillis, title + "\n" + complexNumericInfoRegex);
        ltm.run();
        while (ltm.finished() && ltm.matched()) {
            num = ltm.group();
        }
        return num;
    }

    /**
     * @return the target
     */
    public String getTarget() {
        return target;
    }
}
