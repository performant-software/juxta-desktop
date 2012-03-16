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

package edu.virginia.speclab.legacy.juxta.author.model;

import java.util.Iterator;
import java.util.LinkedList;

import edu.virginia.speclab.legacy.diff.document.DocumentModel;
import edu.virginia.speclab.legacy.diff.document.Image;
import edu.virginia.speclab.legacy.diff.document.LocationMarker;
import edu.virginia.speclab.legacy.juxta.author.model.manifest.BiblioData;

public class JuxtaDocument extends DocumentModel 
{
	private BiblioData biblioData;
    private LinkedList listeners;
    private int fragmentStart, fragmentEnd;
    private boolean fragment;
    private FragmentData oldFragment;
	
	public JuxtaDocument( DocumentModel document, BiblioData biblioData  )
	{
		super(document);
		this.listeners = new LinkedList();
		this.biblioData = biblioData;
		if( this.biblioData == null )
		{
			this.biblioData = BiblioData.createNew();
		}
		fragment = false;
	}
	
	public boolean isFragment()
	{
		return fragment;
	}
	
	public int getFragmentStart()
	{
		return fragmentStart;
	}
	
	public int getFragmentEnd()
	{
		return fragmentEnd;
	}
	
	public void setFragment(int start, int end, boolean exists)
	{
		if(exists)
		{
			int length = getDocumentText().length();
			this.fragmentStart = (start > length)? length : start;
			this.fragmentEnd = (end > length)? length : end;
		}
		
		this.fragment = exists;
		fireDocumentNameChanged(biblioData.getShortTitle());
	}
	
	public JuxtaDocumentFragment getFragment()
	{
		if(!fragment) return null;
		JuxtaDocumentFragment fragmentDoc = new JuxtaDocumentFragment(this,null);
		fragmentDoc.setDocumentText(this.documentText.substring(fragmentStart, fragmentEnd));
		return fragmentDoc;
	}
	
	public JuxtaDocumentFragment getBlock(int startOffset, int endOffset)
	{
		JuxtaDocumentFragment blockDoc = new JuxtaDocumentFragment(this,null);
		blockDoc.setDocumentText(this.documentText.substring(startOffset, endOffset));
		return blockDoc;
	}
	
	public void storeOldFragment()
	{
		oldFragment = new FragmentData(fragmentStart,fragmentEnd,fragment);
	}
	
	public void resetFragment()
	{
		fragmentStart = oldFragment.getStartOffset();
		fragmentEnd = oldFragment.getEndOffset();
		fragment = oldFragment.getExists();
	}
	
	private class FragmentData
	{
		private int startOffset, endOffset;
		boolean exists;
		public FragmentData(int fragmentStart, int fragmentEnd, boolean fragment)
		{
			startOffset = fragmentStart;
			endOffset = fragmentEnd;
			exists = fragment;
		}
		public int getStartOffset() {
			return startOffset;
		}
		
		public int getEndOffset() {
			return endOffset;
		}
		
		public boolean getExists()
		{
			return exists;
		}
	}
    
    public void addJuxtaDocumentListener( JuxtaDocumentListener listener )
    {        
        listeners.add(listener);        
    }
    
    public void removeJuxtaDocumentListener( JuxtaDocumentListener listener )
    {
        listeners.remove(listener);
    }
    
    private void fireDocumentNameChanged( String name )
    {
        for( Iterator i = listeners.iterator(); i.hasNext(); )
        {
            JuxtaDocumentListener listener = (JuxtaDocumentListener) i.next();
            listener.nameChanged(name);
        }
    }
	
	public JuxtaDocument( JuxtaDocument document, String docName )
	{
		super(document);
		this.listeners = new LinkedList();
		this.biblioData = document.biblioData;
		setDocumentName(docName);
	}

    public JuxtaDocument( JuxtaDocument document, String docName, String filename )
    {
        super(document);
		this.listeners = new LinkedList();
        this.biblioData = document.biblioData;
        this.fileName = filename;
		setDocumentName(docName);
    }

	public JuxtaDocument( JuxtaDocument document, int id )
	{
		super(document,id);
		this.listeners = new LinkedList();
		this.biblioData = document.biblioData;
	}

	public JuxtaDocument(JuxtaDocument document, int documentID, int fragmentStart, int fragmentEnd) {
		super(document,documentID);
		this.listeners = new LinkedList();
		this.biblioData = document.biblioData;
		if(fragmentStart != -1)
			setFragment(fragmentStart, fragmentEnd, true);
		else
			setFragment(-1,-1,false);
	}

	public BiblioData getBiblioData() 
	{	
		return biblioData;
	}
	
	public void setBiblioData(BiblioData biblioData) 
	{
        if( biblioData != null )
        {
            if( this.biblioData != null )
            {
                if( !this.biblioData.getShortTitle().equals(biblioData.getShortTitle()) ) 
                    fireDocumentNameChanged(biblioData.getShortTitle());
            }
            
            if( this.biblioData == null )
            {
                fireDocumentNameChanged(biblioData.getShortTitle());
            }
        }
        
		this.biblioData = biblioData;
	}
    
    public Image getImageAt( int centerOffset)
    {
        LocationMarker locationMarker = null;

        for( Iterator i = locationMarkerList.iterator(); i.hasNext(); )
        {
            LocationMarker nextMarker = (LocationMarker) i.next();
            
            if( nextMarker.hasImage() )
            {
                int startOffset = nextMarker.getOffset(); 
                int endOffset =  startOffset + nextMarker.getLength();  
                if( centerOffset >= startOffset && centerOffset < endOffset )
                {
                    locationMarker = nextMarker;
                    break;
                }
            }            
        }
                
        if( locationMarker != null )
        {
            return locationMarker.getImage();    
        }
        else return null;
    }

    public String getDocumentName()
    {
        return biblioData.getShortTitle();
    }
    
    public void setDocumentName( String name )
    {
    	System.out.println("Document Name set to " + name);
        BiblioData newData = new BiblioData( biblioData.getTitle(), 
                                             name,
                                             biblioData.getAuthor(),
                                             biblioData.getEditor(),
                                             biblioData.getSource(),
                                             biblioData.getDate(),
                                             biblioData.getNotes() );
        this.biblioData = newData;
        
        fireDocumentNameChanged(name);
    }
    
    public String toString()
    {
        return getDocumentName();
    }
	
    public String getFragmentStartMarker()
    {
        LocationMarker locationMarker = getExactLocationMarker(fragmentStart);
        if(locationMarker==null)
        	return "text offset:" + fragmentStart;
        else
        	return locationMarker.getLocationName();
    }
    
    public String getFragmentEndMarker()
    {
        LocationMarker locationMarker = getExactLocationMarker(fragmentEnd);
        if(locationMarker==null)
        	return "text offset:" + fragmentEnd;
        else
        	return locationMarker.getLocationName();
    }

	public boolean containsOnlyWhitespace(int start, int end) 
	{
		String s = documentText.substring(start, end);
		for(int i = 0; i<s.length();i++)
		{
			if(!Character.isWhitespace(s.charAt(i)))
				return false;
		}
		return true;
	}

}
