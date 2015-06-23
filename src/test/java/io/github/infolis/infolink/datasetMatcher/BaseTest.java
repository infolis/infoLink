package io.github.infolis.infolink.datasetMatcher;

import java.util.List;

import io.github.infolis.infolink.luceneIndexing.InfolisBaseTest;
import io.github.infolis.model.InfolisFile;

public class BaseTest extends InfolisBaseTest {
	
	//@Override
	protected String[] testStrings = {
			"Hallo, please try to find the FOOBAR in this short text snippet. Thank you.",
			"Hallo, please try to find the Studierendensurvey 1990 in this short text snippet. Thank you.",
			"Hallo, please try to find the Studierendensurvey in this short text snippet. Thank you.",
			"Hallo, please try to find the Studierendensurvey '86 in this short text snippet. Thank you.",
			"Hallo, please try to find the 2010 Studierendensurvey in this short text snippet. Thank you.",
			"Hallo, please try to find .the Studierendensurvey. in this short text snippet. Thank you.",
			"Hallo, please try to find the FOOBAR in this short text snippet. Thank you."
	};
	
	@Override
	protected List<InfolisFile> createTestFiles(int nrFiles) throws Exception {
		return createTestFiles(this.testStrings, nrFiles);
	}
	
}