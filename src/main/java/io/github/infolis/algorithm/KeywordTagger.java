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


import de.unima.ki.infolis.lohai.IflConcept;
import de.unima.ki.infolis.lohai.IflConceptScheme;
import de.unima.ki.infolis.lohai.IflLanguage;
import io.github.infolis.datastore.DataStoreClient;
import io.github.infolis.datastore.FileResolver;
import java.io.IOException;
import org.semtinel.core.data.api.Concept;
import org.semtinel.core.data.api.ConceptScheme;
import org.semtinel.core.skos.impl.SKOSManager;


/**
 *
 * @author domi
 */
public class KeywordTagger extends BaseAlgorithm {

    public KeywordTagger(DataStoreClient inputDataStoreClient, DataStoreClient outputDataStoreClient, FileResolver inputFileResolver, FileResolver outputFileResolver) {
        super(inputDataStoreClient, outputDataStoreClient, inputFileResolver, outputFileResolver);
    }

    @Override
    public void execute() throws IOException {
        //read thesaurus URL -> variable in execution    
        //need to be skos!
        String thesaurusURL ="file:///C:/Users/domi/InFoLiS2/Verschlagwortung/thesaurus/thesoz.rdf";
        SKOSManager man = new SKOSManager(thesaurusURL);
	ConceptScheme iflCs = new IflConceptScheme(man.getConceptSchemes().get(0), thesaurusURL);
        IflConcept.preferredLangaue = new IflLanguage("en");
        for(Concept c : iflCs.getConcepts()) {
            System.out.println(c.getPrefLabel(new IflLanguage("en")).getText());
        }
        
        //input files come from the execution
        
    }

    @Override
    public void validate() throws IllegalAlgorithmArgumentException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
