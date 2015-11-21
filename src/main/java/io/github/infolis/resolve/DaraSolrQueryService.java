
package io.github.infolis.resolve;

/**
 *
 * @author domi
 */
public class DaraSolrQueryService extends SolrQueryService {

    public DaraSolrQueryService() {
        super("http://www.da-ra.de/solr/dara/",0.5);
    }

}
