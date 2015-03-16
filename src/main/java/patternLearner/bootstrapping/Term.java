/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package patternLearner.bootstrapping;

import java.util.HashSet;
import java.util.Set;

/**
 * Class for representing terms along with their contexts and the filenames they
 * are found in. Term instances have the following fields:
 *
 * <ul>
 * <li><emph>string</emph>: string representation of the term</li>
 * <li><emph>contexts</emph>: set of the surrounding words for each occurrence
 * of term</li>
 * <li><emph>documents</emph>: set of documents where term is found</li>
 * </ul>
 *
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
public class Term {

    public String string;
    public Set<String[]> contexts;
    public Set<String> documents;

    /**
     * Class constructor specifying the string representation of the term to
     * represent as <emph>Term</emph> instance
     *
     * @param string	term to represent as <emph>Term</emph> instance
     */
    public Term(String string) {
        this.string = string;
        contexts = new HashSet();
        documents = new HashSet();
    }

    /**
     * Adds a context to this list of contexts.
     *
     * @param context	new context to add to this list of contexts
     */
    public void addContext(String[] context) {
        contexts.add(context);
    }

    /**
     * Adds a document (filename) to this list of documents (filenames).
     *
     * @param document	new document (filename) to add to this list of documents
     * (filenames)
     */
    public void addDocument(String document) {
        documents.add(document);
    }

    /**
     * Overrides the toString method: this string representation consists of the
     * string representation of this term, the string representation of this
     * contexts and the string representation of this documents separated by
     * whitespace.
     */
    @Override
    public String toString() {
        return string + "\n" + contexts + "\n" + documents;
    }
}
