/*
 * Created on Jul 12, 2007
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
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2007 by 
 * The Rector and Visitors of the University of Virginia. 
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 * @author Cortlandt
 */

package edu.virginia.speclab.legacy.diff.document;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Block 
{
	private String id;
	
	private int startOffset;
	private int endOffset;
	
	private String notes;
	private String texttag;
	
	private List currentlyPresent;
	
	//equal when IDs are equal -- allows comparison over different docs
	public boolean equals(Object obj) {
		try{
			Block other = (Block)obj;
			return (this.id.equals(other.id));
		}catch(ClassCastException e)
		{
			return false;
		}
	}
	
	public Block(String id, int startOffset, int endOffset, String notes)
	{
		this.id = id;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.notes = notes;
		currentlyPresent = new LinkedList();
	}
	
	

	public Block(Block otherBlock) 
	{
		this.id = otherBlock.id;
		this.startOffset = otherBlock.startOffset;
		this.endOffset = otherBlock.endOffset;
		this.notes = otherBlock.notes;
		this.texttag = otherBlock.texttag;
		this.currentlyPresent = otherBlock.currentlyPresent;
	}

	public int getEndOffset() {
		return endOffset;
	}

	public int getStartOffset() {
		return startOffset;
	}

	public String getId() {
		return id;
	}


	public void setEndOffset(int endOffset) {
		this.endOffset = endOffset;
	}

	public String toString() {
		return id + " " + startOffset + " " + endOffset + " with notes: " + notes;
	}

	public String getNotes() 
	{
		return notes;
	}

	public String getTextTag() 
	{
		return texttag;
	}

	public void setTexttag(String texttag) {
		this.texttag = texttag;
	}

	public String getCurrentlyPresent() 
	{
		String s = "<html>";
		for(Iterator i = currentlyPresent.iterator();i.hasNext();)
		{
			String docName = (String) i.next();
			s += docName;
			if(i.hasNext())
				s += ", ";
		}
		s+="</html>";
		return s;
	}

	public void setNotes(String notes) 
	{
		this.notes = notes;
	}

	public void addToCurrentlyPresent(String documentName) 
	{
		//add to the front of the list
		if(!currentlyPresent.contains(documentName))
			currentlyPresent.add(0,documentName);
	}
	
	public void removeFromCurrentlyPresent(String documentName)
	{
		currentlyPresent.remove(documentName);
	}
	
	public void clearCurrentlyPresent()
	{
		currentlyPresent.clear();
	}

	public void setStartOffset(int offset) 
	{
		this.startOffset = offset;		
	}

	public void addBoldToCurrentlyPresent(String documentName) 
	{
		addToCurrentlyPresent("<b>" + documentName + "</b>");
		
	}

	public boolean isCurrentlyPresentEmpty() 
	{
		return currentlyPresent.isEmpty();
	}

}
