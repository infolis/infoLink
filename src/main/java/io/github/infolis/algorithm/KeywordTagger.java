/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.infolis.algorithm;

import de.unima.ki.infolis.fastjoin.core.FastJoinIndexer;
import de.unima.ki.infolis.fastjoin.core.Settings;
import de.unima.ki.infolis.fastjoin.util.ConceptWithScore;
import de.unima.ki.infolis.lohai.IflAnnotationSource;
import de.unima.ki.infolis.lohai.IflConcept;
import de.unima.ki.infolis.lohai.IflConceptScheme;
import de.unima.ki.infolis.lohai.IflLanguage;
import de.unima.ki.infolis.lohai.IflRecord;
import de.unima.ki.infolis.lohai.IflRecordSet;
import io.github.infolis.InfolisConfig;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import io.github.infolis.model.entity.Entity;
import io.github.infolis.model.entity.Keyword;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.semtinel.core.data.api.AnnotationSource;
import org.semtinel.core.data.api.ConceptScheme;
import org.semtinel.core.skos.impl.SKOSManager;

/**
 *
 * @author domi
 * 
 * Algorithm to detect keywords for entities. All keywords come from a 
 * given thesaurus. The keyword detection can either use the abstract of an 
 * entity or exploit existing links.
 * 
 */
public class KeywordTagger extends BaseAlgorithm {

    public KeywordTagger(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    @Override
    public void execute() throws IOException {        
        
        String thesaurusURL = getExecution().getThesaurus();
        SKOSManager man = new SKOSManager(thesaurusURL);
        ConceptScheme iflCs = new IflConceptScheme(man.getConceptSchemes().get(0), thesaurusURL);
        IflConcept.preferredLangaue = new IflLanguage(getExecution().getAbstractLanguage());
        
        AnnotationSource target = new IflAnnotationSource(iflCs);

        IflRecordSet rs = new IflRecordSet();
        
        List<Entity> entitiesForKeyowrdDetection = getInputDataStoreClient().get(Entity.class, getExecution().getEntitiesForKeywordTagging());
        
        for (Entity ent : entitiesForKeyowrdDetection) {
            IflRecord r = new IflRecord();
            r.setAbstractText(ent.getAbstractText());
            // TODO more than one identifier for IflRecord?
            r.setIdentifier(ent.getIdentifiers().get(0));
            r.setOrigin(ent.getUri());
            //if there are any given subject headings/original keywords
            r.setSubjectHeadings(null);
            rs.addRecord(r);
        }
        FastJoinIndexer indexer = new FastJoinIndexer(IflConcept.preferredLangaue, target, iflCs, rs);
        //depends on the indexing strategy!
        Settings.USE_SUBJECTHEADINGS = false;
        Settings.USE_TITLE = false;
        Settings.FASTJOIN_EXE_WIN = InfolisConfig.getFastJoinLocation();
        Settings.TOP_K = 5;
        Settings.TOP_K_UNNORMALIZED = 0;
        
        Map<IflRecord, HashSet<ConceptWithScore>> results = indexer.index();

        List<Keyword> keywords = new ArrayList<>();
        
        for (IflRecord rec : results.keySet()) {
            HashSet<ConceptWithScore> set = results.get(rec);
            for (ConceptWithScore con : set) {
                Keyword keyword = new Keyword();
                keyword.setThesaurusURI(con.getConcept().getURI());
                keyword.setReferredEntity(rec.getIdentifier());
                keyword.setThesaurusLabel(con.getConcept().getPrefLabel(IflConcept.preferredLangaue).getText());
                keyword.setConfidenceScore(con.getScore());
                keywords.add(keyword);                
            }
        }
        List<String> keyWordURIs = new ArrayList<>();
        getOutputDataStoreClient().post(Keyword.class, keywords);
        for(Keyword k : keywords) {
            keyWordURIs.add(k.getUri());
        }
        getExecution().setKeyWords(keyWordURIs);
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        if (null == getExecution().getEntitiesForKeywordTagging()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "entitiesFotKeywordTagging", "Required parameter 'entities for keyword tagging' is missing!");
        }
       if (null == getExecution().getThesaurus()) {
            throw new IllegalAlgorithmArgumentException(getClass(), "thesaurus", "Required parameter 'thesaurus' is missing!");
        }
    }

}
