/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Educational Community License (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.opensource.org/licenses/ecl1.txt">
 * http://www.opensource.org/licenses/ecl1.txt.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2006 by 
 * The Rector and Visitors of the University of Virginia. 
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package edu.virginia.speclab.legacy.juxta.author.model.manifest;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.legacy.diff.token.TokenizerSettings;
import edu.virginia.speclab.legacy.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.legacy.juxta.author.model.JuxtaSession_1_3_1;

public class ComparisonSetNode
{
    private TokenizerSettings settings;
    private String name;
    private LinkedList comparandList;

    /**
     * Create a new Comparison object.
     */
    public ComparisonSetNode(Node comparisonNode)
            throws ReportedException
    {
        comparandList = new LinkedList();
        loadComparisonNode(comparisonNode);
    }

    public ComparisonSetNode(JuxtaSession_1_3_1 session)
    {
        settings = session.getComparisonSet().getTokenizerSettings();
        comparandList = new LinkedList();
        name = session.getComparisonSet().getName();        
        LinkedList documentList = session.getDocumentManager().getDocumentList();
        
        for( Iterator i = documentList.iterator(); i.hasNext(); )
        {
            JuxtaDocument document = (JuxtaDocument) i.next();
            ComparandNode comparand = new ComparandNode(document);
            comparandList.add(comparand);
        }
    }

    /**
     * Load the comparison data from the &ltcomparison&gt element.
     * 
     * @param comparisonNode
     *            The DOM Node of the comparison element.
     * @throws JuxtaFileParsingException
     *             If the comparison element is malformed or not a comparison
     *             element.
     */
    private void loadComparisonNode(Node comparisonNode)
            throws ReportedException
    {
        // Parse the id
        NamedNodeMap attributes = comparisonNode.getAttributes();
        Node idNode = attributes.getNamedItem("name");
        if (idNode != null)
        {
            name = idNode.getNodeValue();
        } 
        else
        {
            ReportedException e = new ReportedException(
                    "Unable to load file, format is incorrect.",
                    "Comparison element missing required attribute \"name\".");
            throw e;
        }
        
 
        TokenizerSettings defaultSettings = TokenizerSettings.getDefaultSettings();

        // use default settings for optional attributes not found
        boolean filterCase = defaultSettings.filterCase();
        boolean filterPunctuation = defaultSettings.filterPunctuation();
        boolean filterWhitespace = defaultSettings.filterWhitespace();
        
        // Parse the case filter
        Node filterCaseNode = attributes.getNamedItem("filter-case");
        if (filterCaseNode != null)
        {
            filterCase = filterCaseNode.getNodeValue().equals("false") ? false : true;
        } 

        // Parse the whitespace filter
        Node filterWhitespaceNode = attributes.getNamedItem("filter-whitespace");
        if (filterWhitespaceNode != null)
        {
            filterWhitespace = filterWhitespaceNode.getNodeValue().equals("false") ? false : true;
        } 

        // Parse the punctuation filter
        Node filterPunctuationNode = attributes.getNamedItem("filter-punctuation");
        if (filterPunctuationNode != null)
        {
            filterPunctuation = filterPunctuationNode.getNodeValue().equals("false") ? false : true;
        } 
        
        settings = new TokenizerSettings(filterCase,filterPunctuation,filterWhitespace);

        // Traverse the child elements looking for comparisons and commentary
        NodeList progressionChildren = comparisonNode.getChildNodes();

        if (progressionChildren != null)
        {
            for (int i = 0; i < progressionChildren.getLength(); i++)
            {
                Node currentNode = progressionChildren.item(i);

                if (currentNode.getNodeType() == Node.ELEMENT_NODE
                        && currentNode.getNodeName().equals("comparand"))
                {
                    ComparandNode c = new ComparandNode(currentNode);
                    comparandList.add(c);
                } 
            }
        }
    }
    
    public LinkedList createDocumentEntryList( File basePath, File cachePath ) throws ReportedException
    {
        LinkedList documentEntryList = new LinkedList();
        
        for( Iterator i = comparandList.iterator(); i.hasNext(); )
        {
            ComparandNode comparand = (ComparandNode) i.next();
            
            DocumentEntry entry = new DocumentEntry(comparand,basePath,cachePath);
            documentEntryList.add(entry);
        }
        
        return documentEntryList;
    }

    public String getName()
    {
        return name;
    }

    public LinkedList getComparandList()
    {
        return comparandList;
    }

    public TokenizerSettings getSettings()
    {
        return settings;
    }
    
}
