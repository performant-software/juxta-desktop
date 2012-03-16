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
 

package edu.virginia.speclab.juxta.author.model.manifest;

import edu.virginia.speclab.diff.OffsetRange.Space;
import java.io.File;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;

public class ComparandNode
{
    private int documentID;
    private String file;
    private int activeRangeStart;
    private int activeRangeEnd;
    
    /**
     * Create an empty <code>Comparand</code> object.
     *
     */
    public ComparandNode( Node comparandNode ) throws ReportedException
    {
       loadComparandNode(comparandNode);
    }
    
    public ComparandNode( JuxtaDocument document )
    {
        documentID = document.getID();
        file = document.getFileName() == null ? "" : document.getFileName();
        activeRangeStart = document.getActiveTextRange().getStartOffset(Space.ORIGINAL);
        activeRangeEnd = document.getActiveTextRange().getEndOffset(Space.ORIGINAL);
    }

    /**
     * Load the comparand data from a &ltcomparand&gt element.
     * 
     * @param comparandNode The DOM Node of the comparand element.
     * @throws JuxtaFileParsingException If the comparand element is malformed or not
     * a comparand element.
     */
    private void loadComparandNode(Node comparandNode) throws ReportedException
    {
       boolean foundFile = false;

       NamedNodeMap attributes = comparandNode.getAttributes();

       //   Parse the doc id        
       Node docid = attributes.getNamedItem("docid");
       if (docid != null)
       {
          documentID = Integer.parseInt(docid.getNodeValue());           
       }
       //	parse the fragment offsets
       Node fragmentStart = attributes.getNamedItem("activeRangeStart");
       if (fragmentStart != null)
       {
    	   this.activeRangeStart = Integer.parseInt(fragmentStart.getNodeValue());
       }
       else
       {
    	   this.activeRangeStart = -1;
       }
       Node fragmentEnd = attributes.getNamedItem("activeRangeEnd");
       if (fragmentEnd != null)
       {
    	   this.activeRangeEnd = Integer.parseInt(fragmentEnd.getNodeValue());
       }
       else
       {
    	   this.activeRangeEnd = -1;
       }
       
       // Gather data from child elements
       NodeList comparandChildren = comparandNode.getChildNodes();

       if (comparandChildren != null)
       {
          for (int i = 0; i < comparandChildren.getLength(); i++)
          {
             Node currentNode = comparandChildren.item(i);

             if (currentNode.getNodeType() == Node.ELEMENT_NODE)
             {
                if (currentNode.getNodeName().equals("file"))
                {
                   foundFile = true;
                   Node textNode = currentNode.getFirstChild();
                   if (textNode.getNodeType() == Node.TEXT_NODE)
                      file = textNode.getNodeValue();
                }
             }
          }
       }

       // Take exception if we are missing any required elements. 

       if (foundFile == false)
       {
          ReportedException e =
             new ReportedException("Unable to load file, format is incorrect.","Comparand missing required element: <file>");
          throw e;
       }
    }

    public String getFile()
    {
        return file;
    }

    public String getFileName()
    {
    	File absFile = new File(file);
    	return absFile.getName();
    }

    public int getDocumentID()
    {
        return documentID;
    }

	public int getActiveRangeStart() {
		return activeRangeStart;
	}

	public int getActiveRangeEnd() {
		return activeRangeEnd;
	}
}
