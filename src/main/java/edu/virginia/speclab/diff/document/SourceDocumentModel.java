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
 
package edu.virginia.speclab.diff.document;

import edu.virginia.speclab.diff.token.JuxtaXMLNode;
import java.util.Set;

/**
 *
 * @author ben
 */
public class SourceDocumentModel {
    protected String filename;
    protected JuxtaXMLNode rootXMLNode;
    protected String rawXML;
    protected OffsetMap offsetMap;
    protected TagSet notableTags, excludedTags, newlineTags;
    protected Set<String> elementsEncountered;
    protected boolean isXml;

    public SourceDocumentModel(String flatText, String filename)
    {
        fillOutModelFromFlatText(flatText);
        this.filename = filename;
        this.isXml = false;
    }

    public SourceDocumentModel(String filename, String sourceText, JuxtaXMLNode rootParseNode, OffsetMap offsetMap, boolean isXml)
    {
        this.filename = filename;
        this.setXMLRoot(rootParseNode);
        this.setRawXMLContent(sourceText);
        this.offsetMap = offsetMap;
        this.isXml = isXml;
    }
    
    public SourceDocumentModel(SourceDocumentModel other)
    {
        this.rawXML = other.rawXML;
        this.rootXMLNode = other.rootXMLNode;
        this.offsetMap = other.offsetMap;
        this.isXml = other.isXml;
    }
    
    public boolean isXml() {
        return this.isXml;
    }

    /**
     * Sets the root of the JuxtaXMLNode tree that corresponds to this document
     */
    public void setXMLRoot(JuxtaXMLNode root)
    {
        this.rootXMLNode = root;
    }

    public JuxtaXMLNode getXMLRoot()
    {
        return this.rootXMLNode;
    }

    /**
     * Sets the raw XML content of the document for later reference
     */
    public void setRawXMLContent(String rawXML)
    {
        this.rawXML = rawXML;
    }

    public String getRawXMLContent()
    {
        return this.rawXML;
    }

    public OffsetMap getOffsetMap()
    {
        return offsetMap;
    }

    public void setOffsetMap(OffsetMap map)
    {
        this.offsetMap = map;
    }

    private void fillOutModelFromFlatText(String flatText)
    {
        rawXML = flatText;
        rootXMLNode = new JuxtaXMLNode("root", 0, flatText.length(), null);
        offsetMap = new OffsetMap(rawXML.length());
        for (int i = 0; i <= rawXML.length(); i++)
        {
            offsetMap.mapSourceToTarget(i, i);
            offsetMap.mapTargetToSource(i, i);
        }
    }

    public String getFileName()
    {
        return this.filename;
    }

    public TagSet getNotableTagSet()
    {
        return this.notableTags;
    }

    public void setNotableTagSet(TagSet notable)
    {
        this.notableTags = notable;
    }


    public TagSet getExcludedTagSet()
    {
        return this.excludedTags;
    }

    public void setExcludedTagSet(TagSet excluded)
    {
        this.excludedTags = excluded;
    }

    public Set<String> getElementsEncountered()
    {
        return this.elementsEncountered;
    }

    public void setElementsEncountered(Set<String> elementsEncountered)
    {
        this.elementsEncountered = elementsEncountered;
    }

    public TagSet getNewlineTagSet()
    {
        return this.newlineTags;
    }

    public void setNewlineTagSet(TagSet newlineTags)
    {
        this.newlineTags = newlineTags;
    }

}
