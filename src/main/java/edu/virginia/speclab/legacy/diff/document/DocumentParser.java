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

package edu.virginia.speclab.legacy.diff.document;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class DocumentParser extends DefaultHandler
{        
    private LinkedList locationMarkerList;
    private LinkedList emphasisSpanList;
    private LinkedList blockList;
    
    private String documentText;

    // counter marker for numbering 
    private MarkerNumberCounter counter;
    
    // stores location for which we haven't yet found the end
    private HashMap openLocationMarkerSet;
    
    // keeps a buffer of the text read so far
    private StringBuffer documentTextBuffer;
    
    // the current position in the file, minus element tags
    private int currentOffset;
    
    // the start of the currently open span
    private int currentSpanStartOffset;
    private File baseDirectory;

    public DocumentParser()
    {
        locationMarkerList = new LinkedList();
        emphasisSpanList = new LinkedList();
		blockList = new LinkedList();
		
        counter = new MarkerNumberCounter(1);
        documentTextBuffer = new StringBuffer();
        openLocationMarkerSet = new HashMap();
        currentSpanStartOffset = -1;
    }
    
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        for( int i=start; i<start+length; i++ )
        {
            documentTextBuffer.append(ch[i]);
            currentOffset++;
        }
    }
    
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if( qName.equals("m_s") )
        {
            String name = attributes.getValue("name");
            String id = attributes.getValue("id");
            String type = attributes.getValue("type");
            String imageFileName = attributes.getValue("img");
            String numString = attributes.getValue("n");
            
            Image image = null; 
            
            if( imageFileName != null && baseDirectory != null )
            {
                File imageFile = new File( baseDirectory.getParentFile() + "/images/" + imageFileName);
                image = new Image(imageFile);
            }
            
            int number = 0;
            if( numString != null ) 
            {
                // parse the marker number if there is one
                number = Integer.parseInt(numString);
                counter.setLastNumber(type,number);
            }
            else
            {
                // if there isn't use counter to count up from the last encounter type
                number = counter.getNumber(type);
            }
            
            LocationMarker marker = new LocationMarker(id,name,type,number,image,currentOffset);
            openLocationMarkerSet.put(id,marker);
        }

        if( qName.equals("m_e") )
        {
            String id = attributes.getValue("refid");                
            LocationMarker marker = (LocationMarker) openLocationMarkerSet.get(id);
            int length = currentOffset - marker.getOffset();
            marker.setLength(length);
            locationMarkerList.add(marker);
        }
        
        if( qName.equals("block") )
        {
        	String id = attributes.getValue("id");
        	String notes = "";
        	try
        	{
        		notes = attributes.getValue("notes");
        	} catch(Exception e) {/* leave as blank */}
        	
        	Block block = null;
        	
        	if(blockList.isEmpty())
        	{
        		block = new Block(id,currentOffset,-1,notes);
        	}
        	else
        	{
        		Block previousBlock = (Block) blockList.getLast();
        		previousBlock.setEndOffset(currentOffset-1);
        		block = new Block(id,currentOffset,-1,notes);
        	}
        	blockList.add(block);
        }
        	

        if( qName.equals("em") )
        {
            if( currentSpanStartOffset == -1 )
            {
                currentSpanStartOffset = currentOffset;                
            }
            else
            {
                //TODO emphasis parsing error
            }
        }
                
        super.startElement(uri, localName, qName, attributes);
    }
    
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if( qName.equals("text") )
        {
        	if(!blockList.isEmpty())
        	{
	        	// end the final block
	        	Block previousBlock = (Block)blockList.getLast();
	        	previousBlock.setEndOffset(currentOffset-1);
        	}
        	
            // assign the final document text to an immutable string and de-reference the buffer
            documentText = documentTextBuffer.toString();                
        }
        else if( qName.equals("em") )
        {
            if( currentSpanStartOffset != -1 )
            {
                int length = currentOffset - currentSpanStartOffset;
                
                if( length > 0 )
                {
                    EmphasisSpan span = new EmphasisSpan(currentSpanStartOffset,length);
                    emphasisSpanList.add(span);
                }
            }
            else
            {
                //TODO emphasis parsing error
            }            
        }

        super.endElement(uri, localName, qName);
    }
	
    public String getDocumentText()
	{
	    return documentText;
	}

	public LinkedList getEmphasisSpanList()
	{
	    return emphasisSpanList;
	}

	public LinkedList getBlockList() 
	{
		return blockList;
	}

	public LinkedList getLocationMarkerList()
	{
	    return locationMarkerList;
	}

	// Counts continous runs of markers of the same type, starting from the given start number
    // The count can be overridden by supplying the last type and number encountered.
    private class MarkerNumberCounter
    {
        private String lastType;
        private int lastNumber, startNumber;
        
        public MarkerNumberCounter( int startNumber )
        {
            this.startNumber = startNumber;
        }
        
        public int getNumber( String type )
        {
            if( type.equals(lastType) )
            {   
                lastType = type;
                return ++lastNumber;
            }
            else
            {
                lastType = type;
                lastNumber = startNumber;
                return lastNumber;
            }
        }
        
        public void setLastNumber( String type, int number )
        {
            lastType = type;
            lastNumber = number;
        }
                
    }

    public void setBaseDirectory(File baseDirectory)
    {
        this.baseDirectory = baseDirectory;        
    }

}
