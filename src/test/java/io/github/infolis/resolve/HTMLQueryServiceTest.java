package io.github.infolis.resolve;

import io.github.infolis.model.entity.Entity;
import io.github.infolis.resolve.HTMLQueryService;
import io.github.infolis.resolve.QueryService.QueryField;
import io.github.infolis.resolve.QueryServiceTest.ExpectedOutput;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
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
public class HTMLQueryServiceTest {

	Logger log = LoggerFactory.getLogger(HTMLQueryServiceTest.class);
	
	public static Set<ExpectedOutput> getExpectedOutput() {
		Set<ExpectedOutput> expectedOutput = DaraHTMLQueryServiceTest.getExpectedOutput();
		// add other expected output here to test other sub-classes of HTMLQueryService
		return expectedOutput;
	}

    @Test
    public void testCreateQuery() throws IOException {
        HTMLQueryService queryService = new HTMLQueryService("http://www.da-ra.de/dara/study/web_search_show");
        queryService.setMaxNumber(600);
        Set<QueryField> queryStrategy = new HashSet<>();
        queryStrategy.add(QueryField.title);
        queryService.setQueryStrategy(queryStrategy);
        Entity entity = new Entity();
        entity.setName("Studierendensurvey");
        Assert.assertEquals(new URL("http://www.da-ra.de/dara/study/web_search_show?title=Studierendensurvey&max=600&lang=de"), queryService.createQuery(entity));
    }

}
