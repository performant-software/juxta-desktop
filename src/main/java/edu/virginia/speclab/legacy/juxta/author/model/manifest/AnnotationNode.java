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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.legacy.diff.Difference;
import edu.virginia.speclab.legacy.juxta.author.model.Annotation;

public class AnnotationNode
{
	private int differenceType;
	private int baseDocumentID, witnessDocumentID;
	private int baseOffset, baseLength;
	private int witnessOffset, witnessLength;
	private String notes;
	private String baseQuote;
	private String witnessQuote;
    private boolean includeImage;
    
    public AnnotationNode( Node annotationNode ) throws ReportedException
    {
       loadAnnotationNode(annotationNode);
    }
    
    public AnnotationNode( Annotation annotation )
    {
		Difference difference = annotation.getDifference();
		
		this.differenceType = difference.getType();
		this.baseDocumentID = difference.getBaseDocumentID();
		this.witnessDocumentID = difference.getWitnessDocumentID();
		this.baseLength = difference.getLength(Difference.BASE);
		this.witnessLength = difference.getLength(Difference.WITNESS);
		this.baseOffset = difference.getOffset(Difference.BASE);
		this.witnessOffset = difference.getOffset(Difference.WITNESS);
		this.notes = annotation.getNotes();
		this.includeImage = annotation.includeImage();
    }
    
    private void loadDifferenceNode( Node differenceNode ) throws ReportedException
    {
        NamedNodeMap attributes = differenceNode.getAttributes();

        // Parse the type 
        Node nameNode = attributes.getNamedItem("type");
        if (nameNode != null)
        {
            setDifferenceType(nameNode.getNodeValue());
        }
        else
        {
           throw new ReportedException("Unable to load file, format is incorrect.",
                                       "Difference element missing required attribute \"type\".");
        }

        // Gather data from child elements
        NodeList differenceChildren = differenceNode.getChildNodes();

        if (differenceChildren != null)
        {
           for (int i = 0; i < differenceChildren.getLength(); i++)
           {
              Node currentNode = differenceChildren.item(i);

              if ( currentNode.getNodeType() == Node.ELEMENT_NODE &&
                   currentNode.getNodeName().equals("base")           )
              {
                  loadDocumentNode(currentNode,Difference.BASE);
              }
              
              if ( currentNode.getNodeType() == Node.ELEMENT_NODE &&
                   currentNode.getNodeName().equals("witness")        )
              {
                  loadDocumentNode(currentNode,Difference.WITNESS);
              }
           }
        }
    }

    private void loadDocumentNode(Node documentNode, int documentType ) throws ReportedException
    {
        NamedNodeMap attributes = documentNode.getAttributes();
        
        String elementName = documentType==Difference.BASE ? "Base" : "Witness";
        
        Node docIDNode = attributes.getNamedItem("docid");
        if (docIDNode != null)
        {
            if( documentType == Difference.BASE )
                baseDocumentID = Integer.parseInt(docIDNode.getNodeValue());
            else
                witnessDocumentID = Integer.parseInt(docIDNode.getNodeValue());
        }
        else
        {
            throw new ReportedException("Unable to load file, format is incorrect.",
                                    elementName + " element missing required attribute \"docid\".");
        }
        
        Node offsetNode = attributes.getNamedItem("offset");
        if (offsetNode != null)
        {
            if( documentType == Difference.BASE )
                baseOffset = Integer.parseInt(offsetNode.getNodeValue());
            else
                witnessOffset = Integer.parseInt(offsetNode.getNodeValue());
        }
        else
        {
           throw new ReportedException("Unable to load file, format is incorrect.",
                                     elementName + " element missing required attribute \"offset\".");           
        }
 
        Node lengthNode = attributes.getNamedItem("length");
        if (lengthNode != null)
        {
            if( documentType == Difference.BASE )
                baseLength = Integer.parseInt(lengthNode.getNodeValue());
            else
                witnessLength = Integer.parseInt(lengthNode.getNodeValue());
        }
        else
        {           
           throw new ReportedException("Unable to load file, format is incorrect.",
                                     elementName + " element missing required attribute \"length\".");         
        }
        
    }

    private void loadAnnotationNode(Node annotationNode) throws ReportedException
    {
       boolean foundDifference = false;

       // Gather data from child elements
       NodeList annotationChildren = annotationNode.getChildNodes();

       if (annotationChildren != null)
       {
          for (int i = 0; i < annotationChildren.getLength(); i++)
          {
             Node currentNode = annotationChildren.item(i);

             if (!foundDifference &&
                 currentNode.getNodeType() == Node.ELEMENT_NODE &&
                 currentNode.getNodeName().equals("difference")    )
             {
                 loadDifferenceNode(currentNode);
                 foundDifference = true;
             }
         
             if (currentNode.getNodeType() == Node.ELEMENT_NODE &&
                 currentNode.getNodeName().equals("notes")           )
             {                   
                   Node textNode = currentNode.getFirstChild();
                   if ( textNode != null && textNode.getNodeType() == Node.TEXT_NODE)
                      notes = textNode.getNodeValue();
             }   
             
             if (currentNode.getNodeType() == Node.ELEMENT_NODE &&
                     currentNode.getNodeName().equals("image")           )
                 {                   
                       Node textNode = currentNode.getFirstChild();
                       if ( textNode != null && textNode.getNodeType() == Node.TEXT_NODE)
                          if( textNode.getNodeValue().equals(Boolean.toString(true)) )
                              includeImage = true;
                          else 
                              includeImage = false;
                 }   
          }
       }

       // Take exception if we are missing any required elements. 

       if (foundDifference == false)
       {
          ReportedException e =
             new ReportedException("Unable to load file, format is incorrect.","Annotation missing required element: <difference>");
          throw e;
       }
    }
	
	public int getBaseDocumentID() 
	{
		return baseDocumentID;
	}	

	public int getBaseLength() 
	{
		return baseLength;
	}
	
	public int getBaseOffset() 
	{
		return baseOffset;
	}	

	public int getDifferenceType() 
	{
		return differenceType;
	}
    
    private void setDifferenceType( String type )
    {
        if( type.equals("change") )
        {
            differenceType = Difference.CHANGE;            
        }
        else if( type.equals("delete") )
        {
            differenceType = Difference.DELETE;            
        }
        else if( type.equals("insert") )
        {
            differenceType = Difference.INSERT;            
        }
        else if( type.equals("move") )
        {
            differenceType = Difference.MOVE;            
        }
    }
	
	public String getDifferenceTypeString()
	{
		if( differenceType == Difference.CHANGE )
		{
			return "change";
		}
		else if( differenceType == Difference.DELETE )
		{
			return "delete";
		}
		else if( differenceType == Difference.INSERT )
		{
			return "insert";
		}
		else if( differenceType == Difference.MOVE )
		{
			return "move";
		}
		
		return null;		
	}

	public String getNotes() 
	{
        if( notes == null ) return "";
		else return notes;
	}	

	public int getWitnessDocumentID() 
	{
		return witnessDocumentID;
	}
	

	public int getWitnessLength() 
	{
		return witnessLength;
	}
	
	public int getWitnessOffset() 
	{
		return witnessOffset;
	}

	public String getBaseQuote()
	{
		return baseQuote;
	}
	
	public String getWitnessQuote()
	{
		return witnessQuote;
	}

    public boolean includeImage()
    {        
        return includeImage;
    }

}
