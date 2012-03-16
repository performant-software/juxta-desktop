/*
 *  Copyright 2002-2010 The Rector and Visitors of the
 *                      University of Virginia. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package edu.virginia.speclab.juxta.author.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import com.Ostermiller.util.StringTokenizer;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.document.LocationMarker;
import edu.virginia.speclab.diff.document.NoteData;
import edu.virginia.speclab.diff.document.OffsetMap;
import edu.virginia.speclab.diff.document.PageBreakData;
import edu.virginia.speclab.diff.document.TagSet;
import edu.virginia.speclab.diff.token.JuxtaXMLNode;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.manifest.BiblioData;

/**
 * Normally juxta reads in either an XML file with a very limited set of tags 
 * (call this a juxta xml file) and simply parses out the raw text, making note 
 * of some line number annotations with data given in some of the tags.  
 * 
 * What we want to be able to do is read an arbitrary XML file with an 
 * arbitrary set of tags and do something meaningful with it.  This parser
 * attempts to do that. It is based around a SAX parsing strategy. It builds 
 * a simple tree of XML nodes as it parses through the document. The nodes do 
 * not contain the text components of the document.  As the document is parsed,
 * it builds a flattened string of the text elements encountered, and annotates 
 * the nodes in the tree it is building with these offsets. It also produces a 
 * flat wad of XML text that can be referred to later via another pair of offsets
 * contained in each node. The resulting tree provides an interface for mapping 
 * text offsets in the flattened, parsed text output back to nodes inside the 
 * tree, which use the structure of the tree to form simple XPaths into the 
 * originating document.  The nodes also contain a pair of offsets into the 
 * raw XML produced by reading the file, so that the raw file can be displayed
 * with the appropriate tag highlighted for the user for any particular token.
 *
 * We also build a bidirectional character-for-character map between the
 * XML source and the processed raw text.
 *
 * This class also handles submission of plaintext, and treats malformed XML
 * documents as plaintext. (In fact, that's how it knows: if the SAX parser
 * throws an exception, it treats it as plaintext.)
 *
 * @author ben
 */
public class JuxtaXMLParser extends DefaultHandler implements LexicalHandler {
    public enum DocumentType {
        PLAINTEXT, XML, UNKNOWN
    };


    private DocumentType docType;
    private File file;
    private Charset encodingCharSet;
    private CharsetDecoder decoder;
    private String juxtaVersion;
    private SAXParser parser;
    private StringBuffer flattenedText;
    private JuxtaXMLNode root;
    private JuxtaXMLNode current;
    private int flatTextPosition, xmlTextPosition;
    private TagSet excludeElements;
    private TagSet notableElements;
    private TagSet newlineElements;
    private Set<String> elementsEncountered;
    private Locator locator;
    private List<Integer> lineToOffsetMap;
    private String rawXMLText;
    private int lastXMLPosition;
    private OffsetMap offsetMap;
    private boolean insideEntity;
    private boolean insideWhitespaceRun;
    private int rawEntityLength;
    private int charactersInsideEntity;
    private List<JuxtaXMLParserTagHandler> tagHandlers;
    
    private List<Integer> revisionsToAccept;
    private int currentRevisionIndex;

    private JuxtaXMLParserBibliographicTagHandler biblioTagHandler;
    private JuxtaXMLParserDocumentPointerTagHandler docPointerHandler;
    private JuxtaXMLParserMilestoneTagHandler milestoneTagHandler;
    private JuxtaXMLParserAddDelTagHandler addDelTagHandler;
    private JuxtaXMLParserNoteTagHandler noteTagHandler;
    private JuxtaXMLPageBreakTagHandler pageBreakHandler;
    private HashMap<String, OffsetRange> idOffsetMap;
    
    public JuxtaXMLParser(File file, String juxtaVersion, Charset encodingCharSet) throws FileNotFoundException {
        this.docType = DocumentType.UNKNOWN;
        this.juxtaVersion = juxtaVersion;
        this.encodingCharSet = encodingCharSet;
        this.decoder = this.encodingCharSet.newDecoder();
        this.decoder.onMalformedInput(CodingErrorAction.REPLACE);
        this.decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        this.idOffsetMap = new HashMap<String, OffsetRange>();
        this.file = file;
        
        this.revisionsToAccept = new ArrayList<Integer>();
        this.currentRevisionIndex = 0;
        
        root = current = null;
        flattenedText = new StringBuffer();
        flatTextPosition = -1;
        xmlTextPosition = -1;
        lastXMLPosition = 0;
        excludeElements = new TagSet();
        notableElements = new TagSet();
        newlineElements = new TagSet();

        elementsEncountered = new HashSet<String>();
        lineToOffsetMap = new ArrayList<Integer>();
        insideEntity = false;
        insideWhitespaceRun = false;

        // install our default special-case tag handlers
        tagHandlers = new LinkedList<JuxtaXMLParserTagHandler>();
        biblioTagHandler = new JuxtaXMLParserBibliographicTagHandler();
        addTagHandler(biblioTagHandler);  
        docPointerHandler = new JuxtaXMLParserDocumentPointerTagHandler();
        addTagHandler(docPointerHandler);   
        milestoneTagHandler = new JuxtaXMLParserMilestoneTagHandler();
        addTagHandler(milestoneTagHandler);
    }
    
    public void setRevisionsToAccept( String revisionsList ) {
        this.revisionsToAccept.clear();
        StringTokenizer st = new StringTokenizer( revisionsList, ",");
        while (st.hasMoreTokens() ) {
            this.revisionsToAccept.add( Integer.parseInt(st.nextToken()));
        }
    }
    
    private boolean isDocumentRevised() {
        return (this.revisionsToAccept.size() > 0);
    }
    
    public OffsetRange getTagOffsetRange( final String tagId ) {
        return this.idOffsetMap.get(tagId);
    }
    
    public List<NoteData> getNotes() {
        if ( this.noteTagHandler == null ) {
            return new ArrayList<NoteData>();
        }
        return this.noteTagHandler.getNotes();
    }

    public List<Revision> getRevisions() {
        if ( addDelTagHandler == null ) {
            return new ArrayList<Revision>();
        }
        return addDelTagHandler.getRevisions();
    }
    
    public List<PageBreakData> getPageBreaks() {
        if ( this.pageBreakHandler != null ) {
            return this.pageBreakHandler.getPageBreaks();
        }
        return new ArrayList<PageBreakData>();
    }

    public DocumentType getDocumentType() {
        return docType;
    }

    public void setDocumentType(DocumentType type) {
        docType = type;
    }

    public String getJuxtaVersion() {
        return juxtaVersion;
    }


    public void addTagHandler(JuxtaXMLParserTagHandler handler) {
        if (handler == null)
            return;
        tagHandlers.add(handler);
    }

    public JuxtaXMLParserDocumentPointerTagHandler getDocumentPointer() {
        return docPointerHandler;
    }

    private InputStream stripUtf8Bom(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(new BufferedInputStream(inputStream), 3);
        byte[] bom = new byte[3];
        if (pushbackInputStream.read(bom) != -1) {
            if (!(bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF)) {
                pushbackInputStream.unread(bom);
            }
        }
        return pushbackInputStream;
    }
    
    /**
     * read doc into lines. determine character offset map for each line.
     * collapse lines into one raw xml string with new lines.
     * @throws ReportedException
     */
    private void firstPassReadFile() throws ReportedException {
        try {
            FileInputStream fis = new FileInputStream(this.file);
            InputStreamReader is = new InputStreamReader(stripUtf8Bom(fis), this.decoder);
            List<String> lines = IOUtils.readLines(is);
            StringBuffer rawXMLTextBuffer = new StringBuffer();
            int offset = 0;
            lineToOffsetMap.add(new Integer(-1));
            for (String line : lines) {
                // Strip out marcon decorators; they break the text components
                line = line.replaceAll("[~\u0304]", "");
                int length = line.length();
                lineToOffsetMap.add(new Integer(offset + length));
                offset += length + 1;
                rawXMLTextBuffer.append(line).append("\n");
            }
            rawXMLText = rawXMLTextBuffer.toString();
        } catch (IOException e) {
            throw new ReportedException(e, "Problem with I/O on file: " + file);
        }
    }

    /**
     * Return the absolute position in the rawXMLText String given by a
     * row number and column number reported (presumably) by the Location object
     * while parsing the file.
     *
     * @param line The line number of the location, 1-based
     * @param col The column number of the location, 1-based
     * @return the absolute character offset
     */
    private int mapLineAndColumnToOffset(int line, int col) {
        if (line == -1 || col == -1)
            return -1;

        return lineToOffsetMap.get(line - 1).intValue() + col;
    }

    public File getBaseDirectory() {
        return file.getParentFile();
    }

    public String getSourceFileName() {
        return file.getName();
    }

    public OffsetMap getOffsetMap() {
        return offsetMap;
    }

    public void parse() throws ReportedException {
        firstPassReadFile();
        offsetMap = new OffsetMap(rawXMLText.length());
        
        // Setup special tag handling to cover behavior for
        // add, del, note and pb tags
        setupCustomTagHandling();

        
        try {
            if (file.getName().endsWith("xml")) {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                parser = factory.newSAXParser();

                XMLReader xmlReader = parser.getXMLReader();
                xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", this);

                // ignore external DTDs
                xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

                parser.parse(new InputSource(new StringReader(rawXMLText)), this);
                if (getDocumentType() == DocumentType.UNKNOWN) {
                    // If we made it here and we don't know what the document type is,
                    // it's just an XML document
                    setDocumentType(DocumentType.XML);
                }
                // Uncomment these lines for some verbose debugging of the XML parsing
                // and the resulting processed output.
                //                getRootNode().debugPrint();
                //                try {
                //                    printDebuggingInfo();
                //                } catch (ReportedException ex) {
                                    //Logger.getLogger(JuxtaXMLParser.class.getName()).log(Level.SEVERE, null, ex);
                //                }
            } else {
                processAsPlaintext();
            }
        } catch (ParserConfigurationException ex) {
            throw new ReportedException(ex, "Problem with our parser configuration.");
        } catch (SAXParseException ex) {
            throw new ReportedException(ex, "XML Parsing Error at column " + ex.getColumnNumber() + ", line "
                + ex.getLineNumber() + " :\n" + ex.getLocalizedMessage());
        } catch (SAXException ex) {
            throw new ReportedException(ex, "SAX Exception: " + ex.getLocalizedMessage());
        } catch (IOException ex) {
            throw new ReportedException(ex, ex.getLocalizedMessage());
        }
    }

    private void setupCustomTagHandling() {
        
        this.addDelTagHandler = new JuxtaXMLParserAddDelTagHandler();
        this.addTagHandler(addDelTagHandler);
        this.currentRevisionIndex = 0;
        
        // The REVISED version will start off by including all
        // revision tags. The subset that is actually accepted
        // will be handled later based on the revisionsToAcceptList.
        if ( isDocumentRevised() ) {
            this.excludeElements.excludeTag( "del" );
            this.excludeElements.excludeTag( "delSpan" );
            this.excludeElements.excludeTag( "add" );
            this.excludeElements.excludeTag( "addSpan" );
        } else {
            // The ORIGINAL version will include text that
            // was marked as deleted, but not text added
            this.excludeElements.excludeTag( "del" );
            this.excludeElements.excludeTag( "delSpan" );
            this.excludeElements.includeTag( "add" );
            this.excludeElements.includeTag( "addSpan" );
        }
        
        if ( this.excludeElements.contains("note") == false ) {
            this.noteTagHandler = new JuxtaXMLParserNoteTagHandler();
            addTagHandler(this.noteTagHandler);
            this.excludeElements.includeTag("note");
        }
        
        if ( this.excludeElements.contains("pb") == false) {
            this.pageBreakHandler = new JuxtaXMLPageBreakTagHandler();
            addTagHandler(this.pageBreakHandler);
            this.excludeElements.includeTag("pb");
        }   
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    public void setExcludedTagSet(TagSet excluded) {
        if (excluded == null)
            excludeElements = new TagSet();
        else
            excludeElements = excluded;
    }

    public void setNotableTagSet(TagSet notable) {
        if (notable == null)
            notableElements = new TagSet();
        else
            notableElements = notable;
    }

    public void setNewlineTagSet(TagSet newlines) {
        if (newlines == null)
            newlineElements = new TagSet();
        else
            newlineElements = newlines;
    }

    public TagSet getNewlineTagSet() {
        return newlineElements;
    }

    public BiblioData getBiblioData() {
        return biblioTagHandler.getBiblioData();
    }

    public String getFlattenedText() {
        // +++ consider caching the flattened text String once the processing is
        // complete
        return flattenedText.toString();
    }

    public String getXMLText() {
        return rawXMLText;
    }

    public JuxtaXMLNode getRootNode() {
        return root;
    }

    public List<LocationMarker> getLocationMarkerList() {
        if (getDocumentType() == DocumentType.PLAINTEXT) {
            return LocationMarkerGenerator.generateNewLineMarkers(rawXMLText);
        }
        return milestoneTagHandler.getLocationMarkerList();
    }

    public Set<String> getElementsEncountered() {
        return elementsEncountered;
    }

    @Override
    public void startDocument() throws SAXException {
        // no-op
    }

    @Override
    public void endDocument() throws SAXException {
        // Allow for indexes at the end of the buffer to (at length()) to correctly map
        offsetMap.mapTargetToSource(flatTextPosition + 1, xmlTextPosition + 1);
        offsetMap.mapSourceToTarget(xmlTextPosition + 1, flatTextPosition + 1);
    }

    @Override
    public void startElement(String uri, String localName, String qname, Attributes attr) {
        if (root == null) {
            // this is the root node. exciting.
            root = new JuxtaXMLNode(qname, flatTextPosition + 1);
            current = root;
        } else {
            JuxtaXMLNode newNode = new JuxtaXMLNode(qname, flatTextPosition + 1);
            current.addChild(newNode);
            current = newNode;
        }
        
        // When dealing with revised modes, only keep revison
        // tags that we have been explicitly told to. Note the nify
        // inverted logic...
        if ( isDocumentRevised() && isRevisionTag(qname) ) {
            if ( this.revisionsToAccept.contains( Integer.valueOf(this.currentRevisionIndex)) ) {
                if ( this.current.getName().equals("del") || this.current.getName().equals("delSpan")) {
                    current.setExcluded(true);
                }
            } else {
                if ( this.current.getName().equals("add") || this.current.getName().equals("addSpan")) {
                    current.setExcluded(true);
                }
            }
        }

        // set the start offset of the current node to the LAST xml position we read
        // then reset lastXMLPosition to be the current position given by locator.
        //
        // When these callbacks are called, the locator element points to the line and column number
        // of the current position, which is usually just after the thing that was just processed.
        // So for instance in this case, the locator points to the end of the opening tag.
        // The LAST time we read the location pointer and shoved it into lastXMLPosition corresponds
        // to either 1) the beginning of the document or 2) the end of the last end tag or character read.
        // This means that it already points to the offset we want to record, with the caveat that
        // the root tag will have offsets that also encompass the <?xml?> directive and anything that comes
        // between the beginning of the file and the open root tag, since we aren't ever infomed of an
        // event that happens when the root tag is getting ready to be opened. 
        current.setXMLStartOffset(lastXMLPosition);
        int openTagStartOffset = lastXMLPosition;
        int openTagEndOffset = lastXMLPosition = mapLineAndColumnToOffset(locator.getLineNumber(),
            locator.getColumnNumber());

        // Keep track of all of the element names that we see in the text.
        elementsEncountered.add(qname);

        // If our parent node is an excluded node, then
        // we ourselves are excluded.  When we called addChild our excluded
        // flag was set to be whatever the parent node's flag was set to.
        // If we AREN'T set as excluded from that event, then we check
        // to see if the current element name is set to be excluded
        if (!current.isExcluded()) {
            current.setExcluded(excludeElements.contains(qname));
        }

        // Check to see if we are an element to be noted
        current.setIsNotable(notableElements.contains(qname));

        // are we a newline tag?
        if (!current.isExcluded() && newlineElements.contains(qname)) {
            insertCharacterFromSource('\n');
            openTagStartOffset++;
        }

        for (int i = openTagStartOffset; i < openTagEndOffset; i++)
            skipCharacterFromSource();

        for (int i = 0; i < attr.getLength(); ++i) {
            String name = attr.getQName(i);
            String value = attr.getValue(i);
            current.setAttribute(name, value);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qname) {
        int closeTagStartOffset = lastXMLPosition;
        lastXMLPosition = mapLineAndColumnToOffset(locator.getLineNumber(), locator.getColumnNumber());
        int closeTagEndOffset = lastXMLPosition;

        // Is this an empty tag?
        if (current.isEmptyTag() && current.isNotable()) {
            // insert a non-standard doc character to represent its presence
            insertInventedFlatTextCharacter('\u25CA'); // that's a "lozenge", see: http://www.fileformat.info/info/unicode/char/25ca/index.htm
        }

        current.setXMLEndOffset(lastXMLPosition);
        current.setEndOffset(flatTextPosition + 1); // plus one, because the end offset should be one past the last character included in our scheme

        for (int i = closeTagStartOffset; i < closeTagEndOffset; i++) {
            skipCharacterFromSource();
        }
        
        // track all non-excluded tags with xml:id attributes
        // in a map. This map can be used to look up ranges for
        // target nodes for tags that use them: example = note.
        if (this.current.isExcluded() == false) {
            String id = this.current.getAttribute("xml:id");
            if ( id != null) {
                this.idOffsetMap.put(id, getContentRange(this.current) );
            }
        }

        // Do we have any handlers for this tag?
        Iterator<JuxtaXMLParserTagHandler> it = tagHandlers.iterator();
        while (it.hasNext()) {
            JuxtaXMLParserTagHandler tagHandler = it.next();
            if (tagHandler.getInterestingTagSet().contains(qname)) {
                tagHandler.processTag(this, current);
            }
        }
        current = current.getParent();
        if ( isRevisionTag(qname) ) {
            this.revisionsToAccept.remove( Integer.valueOf(this.currentRevisionIndex) );
            this.currentRevisionIndex++;
        }
    }
    
    /**
     * Grab the full XML content of the specified node and find the
     * raw offsets to the node content (the text after the first '>'
     * and before the last '</'
     * 
     * @param xmlNode
     * @return
     */
    private OffsetRange getContentRange(JuxtaXMLNode xmlNode) {
        OffsetRange range = new OffsetRange();
        int start  = xmlNode.getXMLStartOffset();
        int end = xmlNode.getXMLEndOffset();
        String tagTxt = getXMLText().substring(start, end);
        int relTxtStart = tagTxt.indexOf(">")+1;
        int relTxtEnd = tagTxt.lastIndexOf("</");
        int realStart = start+relTxtStart;
        int realEnd = realStart+(relTxtEnd-relTxtStart);
        range.set(realStart, realEnd, OffsetRange.Space.ORIGINAL);
        return range;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        int mappedOffset = mapLineAndColumnToOffset(locator.getLineNumber(), locator.getColumnNumber());
        int lastLastXMLPosition = lastXMLPosition;
        if (insideEntity)
            // Let endEntity know how many translated characters
            // resulted from the entity. I've never see this
            // be anything other than 1, but I'm not going to assume that
            // it won't ever be 0 or 2, for instance.
            // In this case, endEndity() is going to handle modifying the offset
            // based on how large the entity is (in terms of absolute characters in
            // the source XML).
            charactersInsideEntity = length;
        else
            lastXMLPosition = mappedOffset;

        current.setIsEmptyTag(false);

        for (int i = start; i < start + length; i++) {
            // Skip text inside excluded tags, and compress whitespace
            char character = ch[i];
            boolean isWhitespace = Character.isWhitespace(character);
            char characterFromSourceDocument = rawXMLText.charAt(lastLastXMLPosition + (i - start));

            if (!isWhitespace || !insideWhitespaceRun) {
                if (isWhitespace) {
                    // normalize all whitespace to a space characer
                    character = ' ';
                }

                // It's not clear that the !insideEntity part is necessary here
                if (characterFromSourceDocument == '&' && !insideEntity) {
                    // we need to advance quickly through all of the characters that make up this
                    // NCR. The XML Parser has already turned this into a
                    // real unicode character for us, but will NOT trigger startEntity/endEntity
                    // because it's a real jerk. So we have to do it ourselves. I quote:
                    //
                    // "Note also that the boundaries of character references (which are not really entities anyway) are not reported."
                    // THANKS FOR THAT LESSON IN PEDANTISM, XML PARSER
                    int j = 0;
                    while (rawXMLText.charAt(lastLastXMLPosition + i - start + j++) != ';') {
                        skipCharacterFromSource();
                    }
                }

                if (current.isExcluded()) {
                    skipCharacterFromSource();
                } else {
                    insertCharacterFromSource(character);
                }
            } else {
                skipCharacterFromSource();
            }

            if (!current.isExcluded()) {
                insideWhitespaceRun = isWhitespace;
            }
        }
    }
    
    private boolean isRevisionTag( String name ) {
        return (name.equals("add") || name.equals("addSpan") || 
                name.equals("del") || name.equals("delSpan") );
    }

    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        // no-op
    }

    public void endDTD() throws SAXException {
        // no-op
    }

    public void startEntity(String name) throws SAXException {
        insideEntity = true;

        // this is a hack, but the locator gets confused on custom entities and won't tell us where
        // we are in the file, and this should work... I think. The two characters are the & and ; that
        // flank the entity name.
        rawEntityLength = name.length() + 2;
    }

    public void endEntity(String name) throws SAXException {
        insideEntity = false;

        for (int i = charactersInsideEntity; i < rawEntityLength; i++)
            skipCharacterFromSource();

        lastXMLPosition = lastXMLPosition + rawEntityLength;
    }

    public void startCDATA() throws SAXException {
        // no-op (for now)
    }

    public void endCDATA() throws SAXException {
        // no-op (for now)
    }

    public void comment(char[] ch, int start, int length) throws SAXException {
        int startXMLPosition = lastXMLPosition;
        int endXMLPosition = lastXMLPosition = mapLineAndColumnToOffset(locator.getLineNumber(),
            locator.getColumnNumber());
        for (int i = startXMLPosition; i < endXMLPosition; i++)
            skipCharacterFromSource();
    }

    // Prints out (to STDOUT) a huge table, showing the offset mapping between the
    // source XML and the flattened text.  When the mapping is working correctly,
    // you should be able to see that in this output.  Also this
    // output is useful when compared with the offsets reported by the
    // JuxtaXMLNode#debugPrint method.
    public void printDebuggingInfo() throws ReportedException {

        int lastFlatTextPosition = -1;
        int remappedIndex = 0;

        for (int i = 0; i <= rawXMLText.length(); i++) {

            char xmlChar, flatChar, revMappedXMLChar;
            int targetOffset = offsetMap.getTargetOffset(i);

            if (targetOffset == -1)
                flatChar = '-';
            else if (targetOffset >= flattenedText.length())
                flatChar = '-';
            else
                flatChar = flattenedText.charAt(targetOffset);

            if (i >= rawXMLText.length())
                xmlChar = '-';
            else
                xmlChar = rawXMLText.charAt(i);

            revMappedXMLChar = ' ';
            remappedIndex = -2;

            if (lastFlatTextPosition == targetOffset)
                flatChar = ' ';
            else if (targetOffset >= 0) {
                remappedIndex = offsetMap.getSourceOffset(targetOffset);
                revMappedXMLChar = rawXMLText.charAt(remappedIndex);
            }

            lastFlatTextPosition = targetOffset;

            if (xmlChar == '\n')
                xmlChar = '*';
            if (flatChar == '\n')
                flatChar = '*';
            if (revMappedXMLChar == '\n')
                revMappedXMLChar = '*';

            System.out.println("" + i + "\t" + xmlChar + "\t" + targetOffset + "\t" + flatChar + "\t" + remappedIndex
                + "\t" + revMappedXMLChar);
        }
    }

    private void processAsPlaintext() {
        // Might want to consume whitespace in here at some point too
        // but since it's plaintext we'll assume the whitespace is
        // meaningful.
        root = new JuxtaXMLNode("", 0, rawXMLText.length(), null);
        for (int i = 0; i <= rawXMLText.length(); i++) {
            offsetMap.mapSourceToTarget(i, i);
            offsetMap.mapTargetToSource(i, i);
        }
        flattenedText.append(rawXMLText);
        this.setDocumentType(DocumentType.PLAINTEXT);
    }

    // Call this to add a character to the flat text that does not exist 
    // in the source text
    private void insertInventedFlatTextCharacter(char character) {
        flattenedText.append(character);

        // increase the flat text position and map the current xml position to it
        flatTextPosition++;
        offsetMap.mapTargetToSource(flatTextPosition, xmlTextPosition);
    }

    private void insertCharacterFromSource(char character) {
        flattenedText.append(character);

        flatTextPosition++;
        xmlTextPosition++;
        offsetMap.mapTargetToSource(flatTextPosition, xmlTextPosition);
        offsetMap.mapSourceToTarget(xmlTextPosition, flatTextPosition);
    }

    private void skipCharacterFromSource() {
        xmlTextPosition++;
        offsetMap.mapSourceToTarget(xmlTextPosition, Math.max(0, flatTextPosition)); // protect against -1 being included at the beginning of the root tag
    }

    // Handy method for walking up a Document tree from a Node object contained within it
    // that was fetched via an XPath lookup.  This way we can take any sort of XPath
    // construct and turn it into a simple index-based xpath that the rest of our
    // system can understand without reparsing the document into a DOM model.
    static public String getIndexBasedXPathForGeneralXPath(String xpathString, String xml) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setNamespaceAware(false); // ignore the horrible issues of namespacing
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
            Node root = doc.getFirstChild();
            XPathExpression expr = xpath.compile(xpathString);
            Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
            return nodeToSimpleXPath(node, root);
        } catch (SAXException ex) {
        } catch (IOException ex) {
        } catch (XPathExpressionException ex) {
        } catch (ParserConfigurationException ex) {
        }
        return null;
    }

    private static String nodeToSimpleXPath(Node node, Node root) {
        // recursion ahoy
        if (node == null)
            return "";

        // need to get my index by looping through previous siblings for nodes with the same name as me
        Node sibling = node.getPreviousSibling();
        int index = 1;
        while (sibling != null) {
            if (sibling.getNodeType() == node.getNodeType() && sibling.getNodeName().equals(node.getNodeName()))
                index++;
            sibling = sibling.getPreviousSibling();
        }

        String path = "/" + node.getNodeName();
        if (!node.isSameNode(root))
            path = path + "[" + index + "]";
        else
            return path;

        // If we've picked anything other than an element node, ignore it, and walk up
        // the tree for other things. We get this when someone asks for /blah/blah/text() or something,
        // which we don't support.
        if (node.getNodeType() != Node.ELEMENT_NODE)
            path = "";

        return (nodeToSimpleXPath(node.getParentNode(), root) + path);
    }

}
