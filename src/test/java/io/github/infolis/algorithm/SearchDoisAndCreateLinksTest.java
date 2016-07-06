package io.github.infolis.algorithm;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.model.Execution;
import io.github.infolis.model.TextualReference;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.infolink.querying.DaraHTMLQueryService;
import io.github.infolis.infolink.querying.QueryService;

/**
 * 
 * @author kata
 *
 */
public class SearchDoisAndCreateLinksTest extends InfolisBaseTest {
	
	private final String[] testString = {
			"Version 1.0.0, 21.03.2013 erste Archiv-Version doi:10.4232/1.11692. Änderungen in dieser Version. 2013-11-21, Fehler in Antwortskala für V749 OVERALL ..."
			};
	private List<String> uris = new ArrayList<>();
	
	public SearchDoisAndCreateLinksTest() throws Exception {
		List<InfolisFile> inputFiles = createTestTextFiles(1, testString);
		for (InfolisFile file : inputFiles) {
            uris.add(file.getUri());
		}
	}
	

	private static final Logger log = LoggerFactory.getLogger(SearchDoisAndCreateLinksTest.class);

    @Test
    public void testSearchDoisAndCreateLinks() throws IOException {
        List<String> qServices = postQueryServices();
        Execution e = new Execution();
        e.setAlgorithm(SearchDoisAndCreateLinks.class);
        e.setInputFiles(uris);
        e.setQueryServices(qServices);
        dataStoreClient.post(Execution.class, e);
        e.instantiateAlgorithm(dataStoreClient, fileResolver).run();
        for (String ref : e.getTextualReferences()) {
        	log.debug("created textual reference: " + ref);
        }

        List<EntityLink> createdLinks = dataStoreClient.get(EntityLink.class, e.getLinks());
        assertEquals(1, createdLinks.size());

        for (EntityLink el : createdLinks) {
        	log.debug("created link from " + el.getFromEntity() + " to " + el.getToEntity());
        	Entity targetEntity = dataStoreClient.get(Entity.class, el.getToEntity());
        	assertEquals("German General Social Survey - ALLBUS 2010", targetEntity.getName());
        	assertEquals("10.4232/1.11692", (targetEntity.getIdentifier()));
        	TextualReference textRef = dataStoreClient.get(TextualReference.class, el.getLinkReason());
        	assertEquals("infolisFile_1", textRef.getFile());
        }
    }

    public List<String> postQueryServices() throws IOException {
        List<String> postedQueryServices = new ArrayList<>();
        QueryService p1 = new DaraHTMLQueryService();
        p1.setMaxNumber(10);
        dataStoreClient.post(QueryService.class, p1);
        postedQueryServices.add(p1.getUri());
        return postedQueryServices;
    }

}
