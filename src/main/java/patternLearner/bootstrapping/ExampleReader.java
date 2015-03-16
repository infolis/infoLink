package patternLearner.bootstrapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Class for parsing InfoLink reference extraction context files.
 *
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
public class ExampleReader {

    private File examples;
    private ContextHandler handler;

    /**
     * Class constructor specifying the XML file containing training examples.
     *
     * @param examples	XML file containing training examples (= output of
     * InfoLink reference extraction)
     */
    public ExampleReader(File examples) {
        this.examples = examples;
        handler = readExamples();
    }

    /**
     * Returns this handler's set of contexts (surrounding words of all assumed
     * dataset references found by InfoLink reference extraction).
     *
     * @return	set of contexts
     */
    public Set<String[]> getContexts() {
        return handler.contextSet;
    }

    /**
     * Returns this handler's set of document filenames (documents where assumed
     * dataset references were found by InfoLink reference extraction).
     *
     * @return	set of document names
     */
    public Set<String> getDocuments() {
        return handler.documentSet;
    }

    /**
     * Returns this handler's <emph>termMap</emph>.
     *
     * @return	map having string representations of terms (assumed dataset
     * references) as keys and a list of corresponding Term instances as values
     */
    public Map<String, Term> getTermMap() {
        return handler.termMap;
    }

    /**
     * Returns this handler's <emph>documentMap</emph>.
     *
     * @return	map having document names as keys and sets of assumed dataset
     * references and contexts as values
     */
    public Map<String, Set<String[]>> getDocumentMap() {
        return handler.documentMap;
    }

    /**
     * Returns a Collection of <emph>Term</emph> instances representing terms
     * (assumed dataset names), their surrounding words (= contexts) and the
     * names of the documents they are found in.
     *
     * @return	a Collection of <emph>Term</emph> instances representing terms
     * (assumed dataset names), their surrounding words (= contexts) and the
     * names of the documents they are found in
     */
    public Collection<Term> getContextsForTerms() {
        Map<String, Term> termMap = handler.termMap;
        return termMap.values();
    }


    /**
     * Parses <emph>this examples</emph> using an XMLReader and a ContentHandler
     * instance and returns the ContentHandler instance for further use.
     *
     * @return	the ContentHandler instance used for parsing <emph>this
     * examples</emph> and storing all information
     */
    private ContextHandler readExamples() {
        try {
            // create XML reader
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();

            // path to XML file
            FileReader reader = new FileReader(examples);
            InputSource inputSource = new InputSource(reader);

            // optionally specify DTD
            // inputSource.setSystemId("X:\\examples.dtd");

            // set ContentHandler
            ContextHandler newHandler = new ContextHandler();
            xmlReader.setContentHandler(newHandler);

            // start parsing
            xmlReader.parse(inputSource);
            return newHandler;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Filename: " + this.examples.getAbsolutePath());
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Filename: " + this.examples.getAbsolutePath());
            System.exit(1);
        } catch (SAXException e) {
            e.printStackTrace();
            System.out.println("Filename: " + this.examples.getAbsolutePath());
            System.exit(1);
        }
        return new ContextHandler();
    }

    /**
     * ContentHandler class to parse InfoLink context XML files.
     * <emph>ContextHandler</emph> instances have the following public fields:
     * <ul>
     * <li><emph>documentSet</emph>: names of text documents where assumed
     * dataset references were found by InfoLink reference extraction</li>
     * <li><emph>termMap</emph>: map having string representations of terms
     * (assumed dataset references) as keys and a list of corresponding Term
     * instances as values</li>
     * <li><emph>contextSet</emph>: assumed dataset references and their
     * surrounding words (= contexts) that were extracted by InfoLink reference
     * extraction</li>
     * <li><emph>documentMap</emph>: map having document names as keys and sets
     * of assumed dataset references and contexts as values</li>
     * </ul>
     *
     * @author katarina.boland@gesis.org
     * @version 2014-01-27
     *
     */
    private class ContextHandler implements ContentHandler {

        public Set<String> documentSet = new HashSet();
        public Map<String, Term> termMap = new HashMap();
        public Set<String[]> contextSet = new HashSet();
        public Map<String, Set<String[]>> documentMap = new HashMap();
        private Term newTerm;
        private String doc;
        private String currentValue;
        private String[] leftNrightContext;
        private String[] completeContext;

        /**
         * Buffers read characters for further processing.
         */
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            currentValue += new String(ch, start, length);
        }

        /**
         * Is called when parser arrives at any start tag - prepares saving of
         * content.
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            currentValue = new String();
            if (localName.equals("context")) {
                String termString = atts.getValue("term");
                this.doc = atts.getValue("document");

                documentSet.add(doc);

                if (termMap.containsKey(termString) == false) {
                    newTerm = new Term(termString);
                    newTerm.addDocument(doc);
                    termMap.put(termString, newTerm);
                } else {
                    newTerm = termMap.get(termString);
                    newTerm.addDocument(doc);
                }

                leftNrightContext = new String[2];
                completeContext = new String[3];
                completeContext[1] = termString;
            }
        }

        /**
         * Is called when parser arrives at any end tag - collects and saves all
         * read content.
         */
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (localName.equals("leftContext")) {
                leftNrightContext[0] = currentValue;
                completeContext[0] = currentValue;
            }
            if (localName.equals("rightContext")) {
                leftNrightContext[1] = currentValue;
                completeContext[2] = currentValue;
            }
            if (localName.equals("context")) {
                newTerm.addContext(leftNrightContext);
                contextSet.add(leftNrightContext);

                if (documentMap.containsKey(this.doc) == false) {
                    Set<String[]> docContexts = new HashSet<>();
                    docContexts.add(completeContext);
                    documentMap.put(this.doc, docContexts);
                } else {
                    Set<String[]> docContexts = documentMap.get(this.doc);
                    docContexts.add(completeContext);
                    documentMap.put(this.doc, docContexts);
                }
            }
        }

        @Override
        public void setDocumentLocator(Locator lctr) {
        }

        @Override
        public void startDocument() throws SAXException {
        }

        @Override
        public void endDocument() throws SAXException {
        }

        @Override
        public void startPrefixMapping(String string, String string1) throws SAXException {
        }

        @Override
        public void endPrefixMapping(String string) throws SAXException {
        }

        @Override
        public void ignorableWhitespace(char[] chars, int i, int i1) throws SAXException {
        }

        @Override
        public void processingInstruction(String string, String string1) throws SAXException {
        }

        @Override
        public void skippedEntity(String string) throws SAXException {
        }
    }

    /**
     * Main method reading the specified context file.
     *
     * @param args	args[0]: path of the context file
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: ExampleReader <path of the context file>");
            System.exit(1);
        }
        ExampleReader learner = new ExampleReader(new File(args[0]));
        Map<String, Term> termMap = learner.getTermMap();

        Collection<Term> terms = termMap.values();
        Iterator<Term> termIter = terms.iterator();
        while (termIter.hasNext()) {
            Term newTerm = termIter.next();
            Set<String[]> contexts = newTerm.contexts;
            Set<String> documents = newTerm.documents;
        }
    }
}
