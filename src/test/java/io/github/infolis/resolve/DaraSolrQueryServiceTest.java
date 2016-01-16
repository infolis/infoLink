package io.github.infolis.resolve;

import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author kata
 *
 */
public class DaraSolrQueryServiceTest extends QueryServiceTest {
	
	public static Set<ExpectedOutput> getExpectedOutput() {
		SolrQueryService queryService = new DaraSolrQueryService();
		// equals results must be retrieved when submitting queries via solr and submitting them via web interface
		Set<ExpectedOutput> expectedOutputHtml = DaraHTMLQueryServiceTest.getExpectedOutput();
		Set<ExpectedOutput> expectedOutput = new HashSet<ExpectedOutput>();
		for (ExpectedOutput outputHtml : expectedOutputHtml) {
			ExpectedOutput output = new ExpectedOutput(queryService, outputHtml.getSearchQuery(), outputHtml.getDoiTitleMap());
			expectedOutput.add(output);
		}
		return expectedOutput;
	}
}