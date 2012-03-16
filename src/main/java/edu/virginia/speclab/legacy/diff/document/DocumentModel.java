/*
 * Created on Feb 25, 2005
 *
 */
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

import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import edu.virginia.speclab.legacy.diff.token.TokenTable;
import edu.virginia.speclab.legacy.diff.token.TokenizerSettings;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * @author Nick
 * 
 */
public class DocumentModel 
{
    protected int ID;
    protected String fileName;
    protected String documentText;
    protected TokenTable tokenTable;
    protected List emphasisSpanList, locationMarkerList;
    private List blockList;
	protected String textEncoding;

	public DocumentModel( DocumentModel document )
	{
        this.ID = document.ID;        
        this.fileName = document.fileName;
        this.documentText = document.documentText;
        this.emphasisSpanList = document.emphasisSpanList;
        this.locationMarkerList = document.locationMarkerList;
        this.blockList = document.blockList;
		this.textEncoding = document.textEncoding;
	}

	public DocumentModel( DocumentModel document, int id )
	{
        this.ID = id;        
        this.fileName = document.fileName;
        this.documentText = document.documentText;  
        this.emphasisSpanList = document.emphasisSpanList;
        this.locationMarkerList = document.locationMarkerList;
        this.blockList = document.blockList;
		this.textEncoding = document.textEncoding;
	}
    
    public DocumentModel( String fileName, String documentText, String textEncoding )
    {
        this.ID = createID();        
        this.fileName = fileName;
        this.documentText = documentText; 
		this.textEncoding = textEncoding;
    }
    
    /**
     * De-references the token table, allowing the memory to be reclaimed by the garbage
     * collector.
     */
    public void releaseTokenTable()
    {
        tokenTable = null;
    }

    /**
     * Computes the token table for this text using the provided tokenizer settings.
     * @param settings 
     */
    public void tokenize( TokenizerSettings settings )
    {
        tokenTable = new TokenTable( this, settings );        
    }
	
    protected int createID()
    {
        return UUID.randomUUID().hashCode(); 
    }
    
    public LocationMarker getLocationMarker( int offset )
    {
        if(( locationMarkerList == null ) || (locationMarkerList.size() == 0)) return null;
        
        int highest = 0;
        LocationMarker highestMarker = null;
        for( Iterator i = locationMarkerList.iterator(); i.hasNext(); )
        {
            LocationMarker nextMarker = (LocationMarker) i.next();
            int nextEndPoint = nextMarker.getOffset()+nextMarker.getLength();
            if(( offset >= nextMarker.getOffset() ) && (offset <= nextEndPoint))
            {
                return nextMarker;
            }
            if (highest < nextEndPoint)
            {
            	highest = nextEndPoint;
            	highestMarker = nextMarker;
            }
        }
        
        // if the location is past the end, then use the highest
        if ((offset > highest) && (highestMarker != null))
        	return highestMarker;
        return null;    
    }

    public LocationMarker getExactLocationMarker( int offset )
    {
        if( locationMarkerList == null ) return null;
        
        LocationMarker locationMarker = null;
                
        for( Iterator i = locationMarkerList.iterator(); i.hasNext(); )
        {
            LocationMarker nextMarker = (LocationMarker) i.next();
            
            if( offset >= nextMarker.getOffset() )
            {
            	if(offset == nextMarker.getOffset())
            	{
            		locationMarker = nextMarker;
            	}
            }
            else break;
        }
        
        return locationMarker;    
    }
    
    public List getImageList()
    {
        LinkedList imageList = new LinkedList();
        
        for( Iterator i = locationMarkerList.iterator(); i.hasNext(); )
        {
            LocationMarker marker = (LocationMarker) i.next();
            
            Image image = marker.getImage();
            
            if( image != null ) 
                imageList.add(image);
        }

        return imageList;
    }
       
    public int getDocumentLength()
    {
        if( documentText == null ) return 0;
        else return documentText.length();
    }
    
    /**
     * Search the document for the first occurance of searchTxt
     * starting from startPos
     * @param startPos Start position for the search
     * @param searchTxt Text to look for
     * @return the position of the found text or -1 of not found
     */
    public int search(int startPos, String searchTxt, boolean wrap)
    {
       if( searchTxt == null ) return -1;
       
       if (startPos < 0)
       {
          SimpleLogger.logInfo("Adjusting search start to 0");
          startPos = 0;        
       }
       
       // perform the search 
       int pos = documentText.toLowerCase().indexOf( searchTxt.toLowerCase(), startPos );         
       
       if (pos == -1 && wrap ) 
       {
            // wrap and search from start
            SimpleLogger.logInfo("Not found; wrapping");
            pos = documentText.toLowerCase().indexOf( searchTxt.toLowerCase(), 0 );
       }
       
       return pos;
    }
    
    public String getSubString( int offset, int length )
    {
        return documentText.substring( offset, offset+length );
    }

    /**
     * @return Returns the documentText.
     */
    public String getDocumentText()
    {
        return documentText;
    }

    public ArrayList getTokenList()
    {
        if( tokenTable == null ) return null;            
        return tokenTable.getTokenList();
    }


    public String getFileName()
    {
        return fileName;
    }

    public int getID()
    {
        return ID;
    }

    public List getEmphasisSpanList()
    {
        return emphasisSpanList;
    }

    public List getLocationMarkerList()
    {
        return locationMarkerList;
    }
    
    public List getLocationMarkerSubset( int startOffset, int endOffset ) {  	
    	LinkedList subsetList = new LinkedList();
    	for( Iterator i = locationMarkerList.iterator(); i.hasNext(); ) {
    		LocationMarker locationMarker = (LocationMarker) i.next();
    		int markerStart = locationMarker.getOffset();
    		int markerEnd = locationMarker.getOffset() + locationMarker.getLength();
    		
    		// contains the start point
    		if( markerStart >= startOffset && 
    			startOffset < markerEnd ) {
    			subsetList.add(locationMarker);
    		}     		
    		// contains the end point
    		else if( markerStart >= endOffset &&
    				 endOffset < markerEnd ) {
    			subsetList.add(locationMarker);
    		}     		
    		// contains points in between start and end
    		else if( markerStart > startOffset && 
    				 markerEnd   < endOffset      ) {
    			subsetList.add(locationMarker);
    		}
    	}
    
    	return subsetList;
    }
    
	public List getBlockList() 
	{
		return blockList;
	}		
	
	/**
	 * Inserts the block into the list, and adjusts the offsets of the adjacent blocks
	 * correctly
	 * @param block
	 */
	
//	public void addToBlockList(Block blockToAdd)
//	{
//		sortBlockListByOffset();
//		//if the blocklist is empty, put the new block in, ignore everything else
//		if(blockList.isEmpty())
//		{
//			blockToAdd.setEndOffset(documentText.length());
//			blockList.add(blockToAdd);
//			return;
//		}
//		
//		for(Iterator i = blockList.iterator(); i.hasNext();)
//		{
//			Block currentBlock = (Block) i.next();
//			if(currentBlock.getStartOffset() > blockToAdd.getStartOffset())
//			{
//				if(!blockList.contains(blockToAdd))
//				{
//					if(blockList.indexOf(currentBlock) != 0)
//					{
//						Block previousBlock = (Block) blockList.get(blockList.indexOf(currentBlock) - 1);
//						previousBlock.setEndOffset(blockToAdd.getEndOffset());
//					}
//					blockToAdd.setEndOffset(currentBlock.getStartOffset());
//					blockList.add(blockList.indexOf(currentBlock), blockToAdd);
//				}
//				return;
//			}
//		}
//		if(!blockList.contains(blockToAdd))
//		{
//			Block lastBlock = (Block) blockList.get(blockList.size() - 1);
//			lastBlock.setEndOffset(blockToAdd.getStartOffset());
//			blockToAdd.setEndOffset(documentText.length()-1);
//			blockList.add(blockToAdd);
//		}
//	}
	
	/**
	 * this removal makes sure that when a passage marker is removed, 
	 * the offset of the previous passage extends all the way down
	 * @param block
	 */
//	public void removeFromBlockList(Block block)
//	{
//		sortBlockListByOffset();
//		
//		int index = blockList.indexOf(block);
//		if(index == -1) return;
//		int endOffset;
//		//get rid of the block
//		block = (Block) blockList.remove(index);
//		
//		if(index>0)
//		{
//			endOffset=block.getEndOffset();
//			Block adjustedBlock = (Block) blockList.get(index-1);
//			adjustedBlock.setEndOffset(endOffset);
//		}
//	}
    
//    public String getBlockOrder()
//    {
//    	String order = "";
//    	for(Iterator i = blockList.iterator();i.hasNext();)
//    	{
//    		Block b = (Block)i.next();
//    		order += b.getId() + " ";
//    	}
//    	return order;
//    }
    
    public void dumpLocationMarkerList()
    {
        SimpleLogger.logInfo("Location Markers for "+fileName);
        
        if( locationMarkerList == null )
        {
            SimpleLogger.logInfo("Location Marker list empty!");
            return;
        }        
        
        for( Iterator i = locationMarkerList.iterator(); i.hasNext(); )
        {
            LocationMarker marker = (LocationMarker) i.next();
            int start = marker.getOffset();
            int end = start + marker.getLength();
            SimpleLogger.logInfo(marker.getLocationName()+" range: "+start+"-"+end);            
        }
    }
    
    public TokenizerSettings getTokenizerSettings()
    {
        if(tokenTable!=null) return tokenTable.getSettings();
        else return null;
    }

	public void setEmphasisSpanList(List emphasisSpanList) {
		this.emphasisSpanList = emphasisSpanList;
	}
	

	public void setLocationMarkerList(List locationMarkerList) {
		this.locationMarkerList = locationMarkerList;
	}
	

	public void setBlockList(List blockList) {
		this.blockList = blockList;
	}

	public String getEncoding() {
		return textEncoding;
	}
    
    //TODO factor this out
    public String getDocumentName() {
        return "";
    }
    
//    private void sortBlockListByOffset()
//    {
//    	Collections.sort(blockList, (Comparator) new OffsetComparator() );
//    }
    
//	public Block getSurroundingBlock(int offset) 
//	{
//		sortBlockListByOffset();
//		Block target = null;
//		for(Iterator i = blockList.iterator(); i.hasNext();)
//		{
//			Block block = (Block) i.next();
//			if(block.getStartOffset() > offset)
//				break;
//			else
//				target = block;
//		}
//		return target;
//	}

//	private class OffsetComparator implements Comparator
//    {
//
//		public int compare(Object o1, Object o2) {
//			try{
//				Block block1 = (Block) o1;
//				Block block2 = (Block) o2;
//				int comparisonValue = block1.getStartOffset() - block2.getStartOffset();
//				if(comparisonValue != 0)
//					return comparisonValue;
//				else
//					return block1.getEndOffset() - block2.getEndOffset();
//			} catch (ClassCastException e)
//			{
//				e.printStackTrace();
//				return 0;
//			}
//		}
//    	
//    };
}
