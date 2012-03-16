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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.legacy.juxta.author.model.JuxtaDocument;

public class ComparandNode
{
    private int documentID;
    private String file;
    private int fragmentStart;
    private int fragmentEnd;
    
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
        fragmentStart = -1;
        fragmentEnd = -1;
        if(document.isFragment())
        {
        	fragmentStart = document.getFragmentStart();
        	fragmentEnd = document.getFragmentEnd();
        }
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
       Node fragmentStart = attributes.getNamedItem("fragstart");
       if (fragmentStart != null)
       {
    	   this.fragmentStart = Integer.parseInt(fragmentStart.getNodeValue());
       }
       else
       {
    	   this.fragmentStart = -1;
       }
       Node fragmentEnd = attributes.getNamedItem("fragend");
       if (fragmentEnd != null)
       {
    	   this.fragmentEnd = Integer.parseInt(fragmentEnd.getNodeValue());
       }
       else
       {
    	   this.fragmentEnd = -1;
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

	public int getFragmentStart() {
		return fragmentStart;
	}

	public int getFragmentEnd() {
		return fragmentEnd;
	}
}
