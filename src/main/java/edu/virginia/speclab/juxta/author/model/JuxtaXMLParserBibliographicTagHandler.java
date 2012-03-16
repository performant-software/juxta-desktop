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

import edu.virginia.speclab.diff.document.TagSet;
import edu.virginia.speclab.diff.token.JuxtaXMLNode;
import edu.virginia.speclab.juxta.author.model.manifest.BiblioData;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author ben
 */
public class JuxtaXMLParserBibliographicTagHandler implements JuxtaXMLParserTagHandler {
    private String _title;
    private String _shortTitle;
    private String _author;
    private String _editor;
    private String _source;
    private String _date;
    private String _notes;
    private Date   _sortDate;
    private TagSet _interestingTagSet;

    
    public JuxtaXMLParserBibliographicTagHandler()
    {
        _title = "";
        _shortTitle = "";
        _author = "";
        _editor = "";
        _source = "";
        _date = "";
        _notes = "";
        _sortDate = new Date();

        _interestingTagSet = new TagSet();
        _interestingTagSet.includeTag("bibliographic");
        _interestingTagSet.includeTag("teiHeader");
    }


    public void processTag(JuxtaXMLParser xmlParser, JuxtaXMLNode xmlNode)
    {
        processText(xmlParser.getXMLText().substring(xmlNode.getXMLStartOffset(), xmlNode.getXMLEndOffset()), xmlNode.getName());
    }


    public void processText(String xml, String name)
    {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false); // ignore the horrible issues of namespacing
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            if (name.equals("bibliographic"))
            {
                processBibliodataTag(doc);
            }
            else if (name.equals("teiHeader"))
            {
                processTEIHeaderTag(doc);
            }

        } catch (SAXException ex) {
            Logger.getLogger(JuxtaXMLParserBibliographicTagHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JuxtaXMLParserBibliographicTagHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(JuxtaXMLParserBibliographicTagHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(JuxtaXMLParserBibliographicTagHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void processTEIHeaderTag(Document doc) throws XPathExpressionException
    {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        _title = xpath.evaluate("/teiHeader/fileDesc[1]/titleStmt[1]/title[1]/text()", doc);
        _shortTitle = ""; // no TEI tag maps to this
        _author = xpath.evaluate("/teiHeader/fileDesc[1]/titleStmt[1]/author[1]/text()", doc);
        _editor = xpath.evaluate("/teiHeader/fileDesc[1]/titleStmt[1]/editor[1]/text()", doc);
        _source = ""; // the TEI data is far too complicated for us to parse here simply
        _date = xpath.evaluate("/teiHeader/fileDesc[1]/publicationStmt[1]/date[1]/text()", doc);

        // Collect all of the <note> elements inside /teiHeader/fileDesc/notesStmt
        // and concatenate them
        _notes = "";
        NodeList notes = (NodeList)xpath.evaluate("/teiHeader/fileDesc[1]/notesStmt[1]/note", doc, XPathConstants.NODESET);
        for (int i = 0; i < notes.getLength(); i++)
        {
            Node notesNode = notes.item(i);
            _notes = _notes.concat(notesNode.getTextContent() + "\n");
        }
        _sortDate = null;
    }


    private void processBibliodataTag(Document doc) throws XPathExpressionException
    {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        _title = xpath.evaluate("/bibliographic/title[1]/text()", doc);
        _shortTitle = xpath.evaluate("/bibliographic/short-title[1]/text()", doc);
        _author = xpath.evaluate("/bibliographic/author[1]/text()", doc);
        _editor = xpath.evaluate("/bibliographic/editor[1]/text()", doc);
        _source = xpath.evaluate("/bibliographic/source[1]/text()", doc);
        _date = xpath.evaluate("/bibliographic/date[1]/text()", doc);
        _notes = xpath.evaluate("/bibliographic/notes[1]/text()", doc);

        String sortDateString = xpath.evaluate("/bibliographic/sort-date[1]/text()", doc);
        try
        {
            _sortDate = new Date(Long.parseLong(sortDateString));
        } catch (NumberFormatException e)
        {
            _sortDate = null;
        }
    }



    public BiblioData getBiblioData()
    {
        return new BiblioData(_title, _shortTitle, _author, _editor, _source, _date, _notes, _sortDate);
    }

    public TagSet getInterestingTagSet() {
        return _interestingTagSet;
    }

}
