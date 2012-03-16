/*
 * Created on Feb 25, 2005
 *
 */
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

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.token.JuxtaXMLNode;
import edu.virginia.speclab.diff.token.Token;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import edu.virginia.speclab.diff.token.TokenTable;
import edu.virginia.speclab.diff.token.TokenizerSettings;
import edu.virginia.speclab.juxta.author.model.Revision;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * @author Nick
 * 
 */
public class DocumentModel 
{
    protected int ID;
    protected String fileName;
    protected TokenTable tokenTable;
    protected List<LocationMarker> locationMarkerList;
    protected List<Revision> revisions;
    protected List<NoteData> notes;
    protected List<PageBreakData> pageBreaks;
	protected String textEncoding;
    protected String processedText;
    protected OffsetRange activeTextRange;
    protected SourceDocumentModel sourceDocument;


	public DocumentModel( DocumentModel document )
	{
        this.ID = document.ID;        
        this.sourceDocument = document.sourceDocument;
        this.fileName = document.fileName;
        this.processedText = document.processedText;

        this.activeTextRange = document.activeTextRange;
        if (this.activeTextRange != null) {
            this.activeTextRange.resetDocument(this);
        } else {
            this.activeTextRange = new OffsetRange(this, 0, processedText.length(), OffsetRange.Space.PROCESSED);
        }

        setLocationMarkerList(document.locationMarkerList);
        setRevisions(document.getRevisions());
        setNotes(document.getNotes() );
        setPageBreaks( document.getPageBreaks() );
		this.textEncoding = document.textEncoding;
    }
    
    public DocumentModel( SourceDocumentModel source, String fileName, String documentText, String textEncoding )
    {
        this.ID = createID();        
        this.sourceDocument = source; // this must be done before the OffsetRange below is created
        this.fileName = fileName;
        this.processedText = documentText;
        this.activeTextRange = new OffsetRange(this, 0, documentText.length(), OffsetRange.Space.PROCESSED); // all of the processed text
        this.textEncoding = textEncoding;
        initLists();

    }

    public DocumentModel( String fileName, String documentText, String textEncoding )
    {
        this.ID = createID();
        this.fileName = fileName;
        this.processedText = documentText;
        this.sourceDocument = new SourceDocumentModel(documentText, fileName);
        this.activeTextRange = new OffsetRange(this, 0, documentText.length(), OffsetRange.Space.PROCESSED);
        this.textEncoding = textEncoding;
        initLists();

    }

    public DocumentModel( String fileName, String sourceFileName, String documentText, String textEncoding )
    {
        this.ID = createID();
        this.fileName = fileName;
        this.processedText = documentText;
        this.sourceDocument = new SourceDocumentModel(documentText, sourceFileName); 
        this.activeTextRange = new OffsetRange(this, 0, documentText.length(), OffsetRange.Space.PROCESSED);
        this.textEncoding = textEncoding;
        initLists();
    }
    
    private void initLists() {
        setLocationMarkerList(new ArrayList<LocationMarker>());
        setRevisions(new ArrayList<Revision>());
        setNotes(new ArrayList<NoteData>());
        setPageBreaks( new ArrayList<PageBreakData>() );
    }


    // Make a new document model that is a copy of this one, except that it restricts the
    // active range to a subset of the old active range
    public DocumentModel( DocumentModel other, int substringStart, int substringEnd )
    {
        this.ID = other.ID;
        this.textEncoding = other.textEncoding;
        this.processedText = other.processedText;
        this.sourceDocument = other.sourceDocument;
        
        this.activeTextRange = new OffsetRange(other, substringStart, substringEnd, OffsetRange.Space.ACTIVE);
        this.activeTextRange.resetDocument(this);
    }
    
    /**
     * Update this doc with the guts of another
     * @param document
     */
    public void update( DocumentModel other )
    {      
        this.sourceDocument = other.getSourceDocument();
        this.processedText = other.getAllProcessedText();
        this.activeTextRange = other.getActiveTextRange();

        setLocationMarkerList( other.getLocationMarkerList() );
        setRevisions( other.getRevisions() );
        setNotes( other.getNotes() );
    }
    
    public boolean isXML()
    {
        return this.sourceDocument.isXml();
    }

    public void setActiveRange(OffsetRange newActiveRange)
    {
        OffsetRange copy = new OffsetRange(newActiveRange);
        copy.resetDocument(this);
        this.activeTextRange = copy;
    }

    public SourceDocumentModel getSourceDocument()
    {
        return this.sourceDocument;
    }

    public void setSourceDocument(SourceDocumentModel source)
    {
        this.sourceDocument = source;
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
        // We need to see which JuxtaXMLNodes cover each token, 
        // and figure out what tags are notable up the tree, add all of these
        // to the token's notable tag set
        Iterator<Token> tokenIter = tokenTable.getTokenList().iterator();
        JuxtaXMLNode root = this.getSourceDocument().getXMLRoot();
        while(tokenIter.hasNext())
        {
            Token token = tokenIter.next();
            // for each character offset in the token, find the JuxtaXMLNode that
            // covers it
            OffsetRange tokenRange = new OffsetRange(this, token.getOffset(), token.getOffset() + token.getToken().length(), OffsetRange.Space.ACTIVE);
            int i = tokenRange.getStartOffset(OffsetRange.Space.PROCESSED);
            int endOffset = tokenRange.getEndOffset(OffsetRange.Space.PROCESSED);
            JuxtaXMLNode node = null;

            while(i < endOffset)
            {
                if (node == null)
                    node = root.getNodeForOffset(i);

                if (node != null)
                {
                    token.addNotableTags(node.getNotableTags());
                    // iterate forward until node changes or we reach the end of the token
                    while(i < endOffset && node != null && node == node.getNodeForOffset(i)) { ++i; }

                    // reset node by diving into itself for the current offset.
                    // this will do one of two things:
                    //   1. return null, because the node no longer comprises this offset, in which case on the
                    //      next iteration, we will dive through the root again
                    //   2. return a subnode of node that contains the offset
                    node = node.getNodeForOffset(i);

                    // in either case, it's likely that our call to token.addNotableTags() will produce a lot
                    // of redundent tagging, but they will be de-duped and I don't think the extra
                    // cost is too extravagent. There are ways to correct this if necessary.
                }
            }
        }
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
        for( Iterator<LocationMarker> i = locationMarkerList.iterator(); i.hasNext(); )
        {
            LocationMarker nextMarker = i.next();
            int nextEndPoint = nextMarker.getEndOffset(OffsetRange.Space.ACTIVE);
            int nextStartPoint = nextMarker.getStartOffset(OffsetRange.Space.ACTIVE);

            if(( offset >= nextStartPoint ) && (offset <= nextEndPoint))
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
                
        for( LocationMarker nextMarker : this.locationMarkerList )
        {
            if( offset >= nextMarker.getStartOffset(OffsetRange.Space.ACTIVE) )
            {
            	if(offset == nextMarker.getStartOffset(OffsetRange.Space.ACTIVE))
            	{
            		locationMarker = nextMarker;
            	}
            }
            else break;
        }
        
        return locationMarker;    
    }
    
    public List<Image> getImageList()
    {
        List<Image> imageList = new LinkedList<Image>();
        for( LocationMarker marker : this.locationMarkerList )
        {            
            Image image = marker.getImage();   
            if( image != null )  {
                imageList.add(image);
            }
        }
        return imageList;
    }
       
    public int getDocumentLength()
    {
        return getDocumentText().length();
    }

    public OffsetRange getActiveTextRange()
    {
        return this.activeTextRange;
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
       int pos = getDocumentText().toLowerCase().indexOf( searchTxt.toLowerCase(), startPos );
       
       if (pos == -1 && wrap ) 
       {
            // wrap and search from start
            SimpleLogger.logInfo("Not found; wrapping");
            pos = getDocumentText().toLowerCase().indexOf( searchTxt.toLowerCase(), 0 );
       }
       
       return pos;
    }
    
    public String getSubString( int offset, int length )
    {
        return getDocumentText().substring( offset, offset+length );
    }

    /**
     * @return Returns the documentText.
     */
    public String getDocumentText()
    {
        if (activeTextRange != null) return activeTextRange.getText(OffsetRange.Space.PROCESSED);
        else return processedText;
    }

    public String getAllProcessedText()
    {
        return processedText;
    }

    public void setProcessedText(String text)
    {
        processedText = text;
        // If the text is reset, the active text range is no longer any good, so reset it
        // to be the whole text.
        activeTextRange = new OffsetRange(this, 0, text.length(), OffsetRange.Space.PROCESSED);
    }

    public List<Token> getTokenList()
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

    public List<LocationMarker> getLocationMarkerList()
    {
        return getLocationMarkerSubset( activeTextRange.getStartOffset(OffsetRange.Space.ACTIVE), activeTextRange.getEndOffset(OffsetRange.Space.ACTIVE));
    }

    public List<LocationMarker> getLocationMarkerSubset( int startOffset, int endOffset ) 
    {
        return getOffsetRangeSubset( locationMarkerList, startOffset, endOffset );
    }

    public List<LocationMarker> getOffsetRangeSubset( List<LocationMarker> list, int startOffset, int endOffset ) {
    	List<LocationMarker> subsetList = new LinkedList<LocationMarker>();
    	for( LocationMarker range : list ) {
    		int markerStart = range.getStartOffset(OffsetRange.Space.ACTIVE);
    		int markerEnd = range.getEndOffset(OffsetRange.Space.ACTIVE);
    		
    		// contains the start point
    		if( markerStart >= startOffset && 
    			startOffset < markerEnd ) {
    			subsetList.add(range);
    		}     		
    		// contains the end point
    		else if( markerStart >= endOffset &&
    				 endOffset < markerEnd ) {
    			subsetList.add(range);
    		}     		
    		// contains points in between start and end
    		else if( markerStart > startOffset && 
    				 markerEnd   < endOffset ) {
    			subsetList.add(range);
    		}
    	}
    
    	return subsetList;
    }
    
    public TokenizerSettings getTokenizerSettings()
    {
        if(tokenTable!=null) return tokenTable.getSettings();
        else return null;
    }


	public void setLocationMarkerList(List<LocationMarker> locationMarkerList) {
		this.locationMarkerList = locationMarkerList;
        for ( LocationMarker marker : locationMarkerList) {
            marker.resetDocument(this);
        }
	}

    public void setNotes(List<NoteData> notes) {
        this.notes = notes;
        for (NoteData note : this.notes ) {
            note.getNoteRange().resetDocument(this);
            OffsetRange anchor = note.getAnchorRange();
            if ( anchor != null ) {
                note.getAnchorRange().resetDocument(this);
            }
        }
    }
    
    public List<NoteData> getNotes() {
        return this.notes;
    }
    
    public final List<PageBreakData> getPageBreaks() {
        return this.pageBreaks;
    }
    
    public void setPageBreaks( List<PageBreakData> breaks ) {
        this.pageBreaks = breaks;
        for (PageBreakData data : this.pageBreaks ) {
            data.getRange().resetDocument(this);
        }
    }


    public List<Revision> getRevisions()
    {
        return this.revisions;
    }

    public void setRevisions(List<Revision> revisions)
    {
        this.revisions = revisions;
        for (Revision revision : this.revisions ) {
            revision.getOffsetRange().resetDocument(this);
        }
    }

	public String getEncoding() {
		return textEncoding;
	}
    
    //TODO factor this out
    public String getDocumentName() {
        return "";
    }

    public void setID(int ID)
    {
        this.ID = ID;
    }

}
