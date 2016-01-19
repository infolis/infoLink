
package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.TextualReference;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import io.github.infolis.model.MetaDataExtractingStrategy;

/**
 *
 * @author domi
 * @author kata
 */
public class MetaDataExtractorTest extends InfolisBaseTest {

    TextualReference[] testContexts = {
        new TextualReference("In this snippet, the reference", "ALLBUS 2000", "is to be extracted as", "document", "pattern","ref"),
        new TextualReference("the reference to the 2000", "ALLBUS", "is to be extracted as", "document", "pattern","ref"),
        new TextualReference("In this snippet, the reference", "ALLBUS", "2000 is to be extracted", "document", "pattern","ref"),
        new TextualReference("In this snippet, the reference", "Eurobarometer 56.1", "is to be extracted as", "document", "pattern","ref"),
        new TextualReference("the reference to the 56.1", "Eurobarometer", "is to be extracted as", "document", "pattern","ref"),
        new TextualReference("In this snippet, the reference", "Eurobarometer", "56.1 is to be extracted", "document", "pattern","ref"),
        new TextualReference("In this snippet, the reference", "Eurobarometer 56.1 2000", "is to be extracted as", "document", "pattern","ref"),
        new TextualReference("reference to the 56.1 2000", "Eurobarometer", "is to be extracted as", "document", "pattern","ref"),
        new TextualReference("In this snippet, the reference", "Eurobarometer", "56.1 2000 is to be", "document", "pattern","ref"),
        new TextualReference("In this snippet, the reference", "ALLBUS 1996/08", "is to be extracted as", "document", "pattern","ref"),
        new TextualReference("the reference to the 1982   -   1983", "ALLBUS", "is to be extracted as", "document", "pattern","ref"),
        new TextualReference("In this snippet, the reference", "ALLBUS", "85/01 is to be extracted", "document", "pattern","ref"),
        new TextualReference("the reference to the 1982 till 1983", "ALLBUS", "is to be extracted as", "document", "pattern","ref"),
        new TextualReference("the reference to the 1982 to 1983", "ALLBUS", "is to be extracted as", "document", "pattern","ref"),
        new TextualReference("the reference to the 1982 bis 1983", "ALLBUS", "is to be extracted as", "document", "pattern","ref"),
        new TextualReference("the reference to the 1982 und 1983", "ALLBUS", "is to be extracted as", "document", "pattern","ref"),
        new TextualReference("the reference to the 2nd wave of the", "2000 Eurobarometer", "56.1 is to be extracted as", "document", "pattern","ref"),
        new TextualReference("the reference to the 2nd wave of the", "Eurobarometer", "2000 is to be extracted as", "document", "pattern","ref"),
        new TextualReference("the reference to the 2nd wave of the", "10.4232/1.2525", "2000 is to be extracted as", "document", "pattern","ref")
    };

    @Test
    public void testMetaDataExtractor() {
        MetaDataExtractor mde = new MetaDataExtractor(dataStoreClient, dataStoreClient, fileResolver, fileResolver);
        assertEquals("?q=title:ALLBUS&?date:2000", mde.extractQuery(testContexts[0],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:ALLBUS&?date:2000", mde.extractQuery(testContexts[1],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:ALLBUS&?date:2000", mde.extractQuery(testContexts[2],MetaDataExtractingStrategy.title));

        assertEquals("?q=title:Eurobarometer&?date:56.1", mde.extractQuery(testContexts[3],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:Eurobarometer&?date:56.1", mde.extractQuery(testContexts[4],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:Eurobarometer&?date:56.1", mde.extractQuery(testContexts[5],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:Eurobarometer&?date:56.1", mde.extractQuery(testContexts[6],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:Eurobarometer&?date:56.1", mde.extractQuery(testContexts[7],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:Eurobarometer&?date:56.1", mde.extractQuery(testContexts[8],MetaDataExtractingStrategy.title));

        assertEquals("?q=title:ALLBUS&?date:1996/08", mde.extractQuery(testContexts[9],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:ALLBUS&?date:1982   -   1983", mde.extractQuery(testContexts[10],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:ALLBUS&?date:85/01", mde.extractQuery(testContexts[11],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:ALLBUS&?date:1982 till 1983", mde.extractQuery(testContexts[12],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:ALLBUS&?date:1982 to 1983", mde.extractQuery(testContexts[13],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:ALLBUS&?date:1982 bis 1983", mde.extractQuery(testContexts[14],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:ALLBUS&?date:1982 und 1983", mde.extractQuery(testContexts[15],MetaDataExtractingStrategy.title));

        assertEquals("?q=title:Eurobarometer&?date:2000&?date:56.1&?date:2", mde.extractQuery(testContexts[16],MetaDataExtractingStrategy.title));
        assertEquals("?q=title:Eurobarometer&?date:2000&?date:2", mde.extractQuery(testContexts[17],MetaDataExtractingStrategy.title));

        assertEquals("?q=doi:10.4232/1.2525", mde.extractQuery(testContexts[18],MetaDataExtractingStrategy.doi));

    }
}
