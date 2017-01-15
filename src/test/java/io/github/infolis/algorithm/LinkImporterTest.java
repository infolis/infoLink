package io.github.infolis.algorithm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.infolis.InfolisBaseTest;
import io.github.infolis.datastore.FileResolverFactory;
import io.github.infolis.model.Execution;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.EntityLink;
import io.github.infolis.model.entity.InfolisFile;
import io.github.infolis.util.SerializationUtils;

/**
 * 
 * @author kata
 *
 */
public class LinkImporterTest extends InfolisBaseTest {
	
	private static final Logger log = LoggerFactory.getLogger(LinkImporterTest.class);
	
	@Test
	public void test() throws IOException {
		File inputDir = new File(getClass().getResource("/linkImporter/").getFile());
		List<String> uris = postFiles(inputDir, "text/json");
		Execution exec = new Execution();
		exec.setInputFiles(uris);
		exec.setAlgorithm(LinkImporter.class);
		exec.instantiateAlgorithm(dataStoreClient, dataStoreClient, FileResolverFactory.local(), fileResolver).run();
		log.debug("links: " + exec.getLinks());
		for (EntityLink link : dataStoreClient.get(EntityLink.class, exec.getLinks())) {
			log.debug("confidence: " + link.getConfidence());
			log.debug("fromEntity: " + link.getFromEntity());
			log.debug("toEntity: " + link.getToEntity());
			log.debug("linkReason: " + link.getLinkReason());
			log.debug("entityRelations: " + link.getEntityRelations()); 
			log.debug("tags: " + link.getTags());
		}
		log.debug("entities: " + exec.getLinkedEntities());
		for (Entity entity : dataStoreClient.get(Entity.class, exec.getLinkedEntities())) {
			log.debug("name: " + entity.getName());
			log.debug("numericInfo: " + entity.getNumericInfo());
			log.debug("versionInfo: " + entity.getVersionInfo());
			log.debug("entityType: " + entity.getEntityType());
			log.debug("identifiers: " + entity.getIdentifiers());
		}
	}
	
	private List<String> postFiles(File dir, String mimetype) throws IOException {
        List<InfolisFile> infolisFiles = new ArrayList<>();
        for (File file : dir.listFiles()) {
            InfolisFile infolisFile = new InfolisFile();
            InputStream inputStream = Files.newInputStream(Paths.get(file.getAbsolutePath()));
            byte[] bytes = IOUtils.toByteArray(inputStream);
            infolisFile.setMd5(SerializationUtils.getHexMd5(bytes));
            infolisFile.setFileName(file.toString());
            infolisFile.setMediaType(mimetype);
            infolisFile.setFileStatus("AVAILABLE");
            infolisFiles.add(infolisFile);
            
            // TODO load ontology and test linking to ontology entities
        }
        return dataStoreClient.post(InfolisFile.class, infolisFiles);
    }
}