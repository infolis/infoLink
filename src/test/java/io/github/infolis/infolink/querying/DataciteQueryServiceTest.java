package io.github.infolis.infolink.querying;

import io.github.infolis.model.entity.Entity;
import io.github.infolis.infolink.querying.QueryService.QueryField;
import io.github.infolis.infolink.querying.QueryServiceTest.ExpectedOutput;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author kata
 *
 */
public class DataciteQueryServiceTest {
	
	// TODO
	//public static Set<ExpectedOutput> getExpectedOutput() {	}
	
	@Test
    public void testCreateTitleQuery() throws MalformedURLException {
        QueryService queryService = new DataciteQueryService();
        Entity entity = new Entity();
        entity.setName("Studierendensurvey");
        Set<QueryField> queryStrategy = new HashSet<>();
        queryStrategy.add(QueryField.title);
        queryService.setQueryStrategy(queryStrategy);
        Assert.assertEquals(new URL("https://api.datacite.org/works/?query=title:\"Studierendensurvey\"%20AND%20type:\"dataset\"&start=0&rows=1000&sort=score&order=desc"), queryService.createQuery(entity));
    }
	
	@Test
    public void testCreateNumInTitleQuery() throws MalformedURLException {
        QueryService queryService = new DataciteQueryService();
        Entity entity = new Entity();
        entity.setName("Studierendensurvey");
        Set<QueryField> queryStrategy = new HashSet<>();
        queryStrategy.add(QueryField.numericInfoInTitle);
        queryService.setQueryStrategy(queryStrategy);
        Assert.assertEquals(new URL("https://api.datacite.org/works/?query=title:\"Studierendensurvey\"%20AND%20type:\"dataset\"&start=0&rows=1000&sort=score&order=desc"), queryService.createQuery(entity));
    }
	
}