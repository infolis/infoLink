package io.github.infolis.resolve;

import io.github.infolis.resolve.QueryServiceTest.ExpectedOutput;

import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author kata
 *
 */
public class DaraSolrQueryServiceTest {
	
	public static Set<ExpectedOutput> getExpectedOutput() {
		SolrQueryService queryService = new DaraSolrQueryService();
		// equal results must be retrieved when submitting queries via solr and submitting them via web interface
		Set<ExpectedOutput> expectedOutputHtml = DaraHTMLQueryServiceTest.getExpectedOutput();
		Set<ExpectedOutput> expectedOutput = new HashSet<ExpectedOutput>();
		for (ExpectedOutput outputHtml : expectedOutputHtml) {
			ExpectedOutput output = new ExpectedOutput(queryService, outputHtml.getEntity(), outputHtml.getSearchResulterRankerClass(), outputHtml.getDoiTitleMap());
			expectedOutput.add(output);
		}
		return expectedOutput;
	}
}