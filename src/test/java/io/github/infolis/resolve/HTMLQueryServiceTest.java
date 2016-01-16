package io.github.infolis.resolve;

import io.github.infolis.model.SearchQuery;
import io.github.infolis.resolve.HTMLQueryService;

import java.io.IOException;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kata
 * 
 */
public class HTMLQueryServiceTest extends QueryServiceTest {

	Logger log = LoggerFactory.getLogger(HTMLQueryServiceTest.class);
	
	public static Set<ExpectedOutput> getExpectedOutput() {
		Set<ExpectedOutput> expectedOutput = DaraHTMLQueryServiceTest.getExpectedOutput();
		// add other expected output here to test other sub-classes of HTMLQueryService
		return expectedOutput;
	}

    @Test
    public void testAdaptQuery() throws IOException {
        HTMLQueryService queryService = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show");
        queryService.setMaxNumber(600);
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setQuery("?q=title:Studierendensurvey");
        String query = queryService.adaptQuery(searchQuery);
        Assert.assertEquals("http://www.da-ra.de/dara/study/web_search_show?title=Studierendensurvey&max=600&lang=de", query);
    }

}
