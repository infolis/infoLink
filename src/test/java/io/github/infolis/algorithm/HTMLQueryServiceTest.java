package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.infolink.datasetMatcher.HTMLQueryService;
import io.github.infolis.infolink.datasetMatcher.QueryService;
import io.github.infolis.infolink.datasetMatcher.SolrQueryService;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.SearchResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 *
 * @author domi
 */
public class HTMLQueryServiceTest extends InfolisBaseTest {
    
    @Test
    public void searchWeb() {
        QueryService qs = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show",0.5);              
        dataStoreClient.post(QueryService.class, qs);
        List<String> qsList = new ArrayList<>();
        qsList.add(qs.getUri());
        
        Execution execution = new Execution();
        execution.setAlgorithm(FederatedSearcher.class);
        execution.setSearchQuery("?q=title:ALLBUS");
        execution.setQueryServices(qsList);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        
        List<String> sr= execution.getSearchResults();
        List<SearchResult> result = dataStoreClient.get(SearchResult.class, sr);
        
        //be careful, could change from time to time
        Assert.assertEquals("Arbeitsorientierung (ALLBUS)", result.get(0).getTitles().get(0));
        Assert.assertEquals("10.6102/zis13", result.get(0).getIdentifier());
        Assert.assertEquals("http://www.da-ra.de/dara/study/web_search_show", result.get(0).getTags().get(0));
        Assert.assertEquals(0, result.get(0).getListIndex());

        Assert.assertEquals("German Social Survey (ALLBUS), 1998", result.get(6).getTitles().get(0));
        Assert.assertEquals("10.3886/ICPSR02779.v1", result.get(6).getIdentifier());
        Assert.assertEquals("http://www.da-ra.de/dara/study/web_search_show", result.get(6).getTags().get(0));
        Assert.assertEquals(6, result.get(6).getListIndex());
        Assert.assertEquals("1998", result.get(6).getNumericInformation().get(0)); 
    }
    
    @Test
    public void testQueryAdaption() {
        HTMLQueryService qs = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show");
        qs.setMaxNumber(600);
        String query = qs.adaptQuery("?q=title:ALLBUS");
        Assert.assertEquals("http://www.da-ra.de/dara/study/web_search_show?title=ALLBUS&max=600&lang=de", query);
    }

    @Test
    public void testQueryExecution() {
        HTMLQueryService qs = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show");
        qs.setMaxNumber(10);
        List<SearchResult> sr = qs.executeQuery("?q=title:ALLBUS");
        
        //be careful, could change from time to time
        Assert.assertEquals("Arbeitsorientierung (ALLBUS)", sr.get(0).getTitles().get(0));
        Assert.assertEquals("10.6102/zis13", sr.get(0).getIdentifier());
        Assert.assertEquals("http://www.da-ra.de/dara/study/web_search_show", sr.get(0).getTags().get(0));
        Assert.assertEquals(0, sr.get(0).getListIndex());

        Assert.assertEquals("German Social Survey (ALLBUS), 1998", sr.get(6).getTitles().get(0));
        Assert.assertEquals("10.3886/ICPSR02779.v1", sr.get(6).getIdentifier());
        Assert.assertEquals("http://www.da-ra.de/dara/study/web_search_show", sr.get(6).getTags().get(0));
        Assert.assertEquals(6, sr.get(6).getListIndex());
        Assert.assertEquals("1998", sr.get(6).getNumericInformation().get(0));
    }
}
