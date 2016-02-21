package io.github.infolis.infolink.querying;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import io.github.infolis.algorithm.SearchResultLinker;
import io.github.infolis.algorithm.MultiMatchesLinker;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.infolink.querying.QueryService.QueryField;
import io.github.infolis.infolink.querying.QueryServiceTest.ExpectedOutput;

/**
 * 
 * @author kata
 *
 */
public class DaraHTMLQueryServiceTest {
	
	public static Set<ExpectedOutput> getExpectedOutput() {
		QueryService queryService = new DaraHTMLQueryService();
		queryService.setMaxNumber(15);
		Entity entity = new Entity();
		entity.setName("Studierendensurvey");
		Class<? extends SearchResultLinker> searchResultLinkerClass = MultiMatchesLinker.class;
		Set<ExpectedOutput> expectedOutput = new HashSet<ExpectedOutput>();
		Map<String, String> expectedDoiTitleMap = new HashMap<String, String>();
		expectedDoiTitleMap.put("10.4232/1.4263", "Studiensituation und studentische Orientierungen 2006/07 (Studierenden-Survey)");
		expectedDoiTitleMap.put("10.4232/1.4344", "Studiensituation und studentische Orientierungen 2003/04 (Studierenden-Survey)");
		expectedDoiTitleMap.put("10.4232/1.11059", "Studiensituation und studentische Orientierungen 2009/10 (Studierenden-Survey)");
		expectedDoiTitleMap.put("10.4232/1.1884", "Studiensituation und studentische Orientierungen 1982/83 (Studierenden-Survey)");
		expectedDoiTitleMap.put("10.4232/1.1885", "Studiensituation und studentische Orientierungen 1984/85 (Studierenden-Survey)");
		expectedDoiTitleMap.put("10.4232/1.3130", "Studiensituation und studentische Orientierungen 1992/93 (Studierenden-Survey)");
		expectedDoiTitleMap.put("10.4232/1.3131", "Studiensituation und studentische Orientierungen 1994/95 (Studierenden-Survey)");
		expectedDoiTitleMap.put("10.4232/1.2416", "Studiensituation und studentische Orientierungen 1986/87 (Studierenden-Survey)");
		expectedDoiTitleMap.put("10.4232/1.2417", "Studiensituation und studentische Orientierungen 1989/90 (Studierenden-Survey)");
		expectedDoiTitleMap.put("10.4232/1.5126", "Studiensituation und studentische Orientierungen 2012/13 (Studierenden-Survey)");
		expectedDoiTitleMap.put("10.4232/1.4208", "Studiensituation und studentische Orientierungen 2000/01 (Studierenden-Survey)");
		expectedDoiTitleMap.put("10.4232/1.3511", "Studiensituation und studentische Orientierungen 1997/98 (Studierenden-Survey)");
		ExpectedOutput output = new ExpectedOutput(queryService, entity, searchResultLinkerClass, expectedDoiTitleMap);
		expectedOutput.add(output);
		return expectedOutput;
	}
	
	 @Test
	    public void testCreateQuery() throws IOException {
	        QueryService queryService = new DaraHTMLQueryService();
	        queryService.setMaxNumber(600);
	        Set<QueryField> queryStrategy = new HashSet<>();
	        queryStrategy.add(QueryField.title);
	        queryService.setQueryStrategy(queryStrategy);
	        Entity entity = new Entity();
	        entity.setName("Studierendensurvey");
	        Assert.assertEquals(new URL("http://www.da-ra.de/dara/search/search_result?q=title:Studierendensurvey+resourceType:2&lang=en&mdlang=de&max=600"), queryService.createQuery(entity));
	    }
}