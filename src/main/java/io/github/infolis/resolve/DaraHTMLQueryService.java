
package io.github.infolis.resolve;

/**
 *
 * @author domi
 */
public class DaraHTMLQueryService extends HTMLQueryService {

    public DaraHTMLQueryService() {
        super("http://www.da-ra.de/dara/search/search_result", 0.5);
    }

}
