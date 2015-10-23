package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.infolink.datasetMatcher.HTMLQueryService;
import io.github.infolis.infolink.datasetMatcher.QueryService;
import io.github.infolis.infolink.datasetMatcher.SolrQueryService;
import io.github.infolis.model.Execution;
import io.github.infolis.model.SearchQuery;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author domi
 */
public class HTMLQueryServiceTest extends InfolisBaseTest {
    
    @Ignore
    @Test
    public void searchWeb() throws IOException {
        QueryService qs = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show",0.5);              
        dataStoreClient.post(QueryService.class, qs);
        List<String> qsList = new ArrayList<>();
        qsList.add(qs.getUri());
        
        Execution execution = new Execution();
        execution.setAlgorithm(FederatedSearcher.class);
        SearchQuery searchQuery = postTitelQuery("?q=title:ALLBUS");
        execution.setSearchQuery(searchQuery.getUri());
        execution.setQueryServices(qsList);
        execution.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        
        List<String> sr= execution.getSearchResults();
        List<SearchResult> result = dataStoreClient.get(SearchResult.class, sr);
        
        //be careful, could change from time to time
        Assert.assertEquals("Arbeitsorientierung (ALLBUS)", result.get(0).getTitles().get(0));
        Assert.assertEquals("10.6102/zis13", result.get(0).getIdentifier());
        Assert.assertEquals("http://www.da-ra.de/dara/study/web_search_show", dataStoreClient.get(QueryService.class,result.get(0).getQueryService()).getTarget());
        Assert.assertEquals(0, result.get(0).getListIndex());

        Assert.assertEquals("German Social Survey (ALLBUS), 1998", result.get(6).getTitles().get(0));
        Assert.assertEquals("10.3886/ICPSR02779.v1", result.get(6).getIdentifier());
        Assert.assertEquals("http://www.da-ra.de/dara/study/web_search_show", dataStoreClient.get(QueryService.class,result.get(0).getQueryService()).getTarget());
        Assert.assertEquals(6, result.get(6).getListIndex());
        Assert.assertEquals("1998", result.get(6).getNumericInformation().get(0)); 
    }
    
    public SearchQuery postTitelQuery(String q) throws IOException {
        SearchQuery sq = new SearchQuery();
        sq.setQuery(q);
        dataStoreClient.post(SearchQuery.class, sq);
        return sq;
    }
    
    public SearchQuery postDoiQuery(String q) throws IOException {
        SearchQuery sq = new SearchQuery();
        sq.setQuery(q);
        dataStoreClient.post(SearchQuery.class, sq);
        return sq;
    }
    
    @Ignore
    @Test
    public void testDoiQueryExecution() throws IOException {
        HTMLQueryService qs = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show");
        qs.setMaxNumber(10);
        SearchQuery sq = postDoiQuery("?q=doi:10.4232/1.2525");
        String query = qs.adaptQuery(sq);
        Assert.assertEquals("http://www.da-ra.de/dara/study/web_search_show?doi=10.4232/1.2525&max=10&lang=de", query);
        List<SearchResult> sr = qs.executeQuery(sq);
        for(SearchResult s : sr) {
            Assert.assertEquals("Flash Eurobarometer 35", s.getTitles().get(0)); 
        }     
    }    
    
    
    @Test
    public void testQueryAdaption() throws IOException {
        HTMLQueryService qs = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show");
        qs.setMaxNumber(600);
        SearchQuery sq = postTitelQuery("?q=title:ALLBUS");
        String query = qs.adaptQuery(sq);
        Assert.assertEquals("http://www.da-ra.de/dara/study/web_search_show?title=ALLBUS&max=600&lang=de", query);
    }

    @Ignore
    @Test
    public void testQueryExecution() throws IOException {
        HTMLQueryService qs = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show");
        qs.setMaxNumber(10);
        SearchQuery sq = postTitelQuery("?q=title:ALLBUS");
        List<SearchResult> sr = qs.executeQuery(sq);
        
        //be careful, could change from time to time
        Assert.assertEquals("Arbeitsorientierung (ALLBUS)", sr.get(0).getTitles().get(0));
        Assert.assertEquals("10.6102/zis13", sr.get(0).getIdentifier());
        Assert.assertEquals(0, sr.get(0).getListIndex());

        Assert.assertEquals("German Social Survey (ALLBUS), 1998", sr.get(6).getTitles().get(0));
        Assert.assertEquals("10.3886/ICPSR02779.v1", sr.get(6).getIdentifier());
        Assert.assertEquals(6, sr.get(6).getListIndex());
        Assert.assertEquals("1998", sr.get(6).getNumericInformation().get(0));
    }
}
