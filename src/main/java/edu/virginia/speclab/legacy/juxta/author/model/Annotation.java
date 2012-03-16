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

import edu.virginia.speclab.legacy.diff.Difference;

public class Annotation 
{
	private Difference difference;		
	private String notes;
    private boolean includeImage;
    
    public Annotation( Annotation annotation )
    {
        this.difference = annotation.difference;
        this.notes = annotation.notes;
        this.includeImage = annotation.includeImage;
    }
		
	public Annotation( Difference difference )
	{
		this.difference = difference;
	}
	
	public JuxtaDocument getBaseDocument( DocumentManager documentManager )
	{
		if( documentManager != null && difference != null )
		{
			return documentManager.lookupDocument(difference.getBaseDocumentID());	
		}
		else
		{
			return null;
		}
	}

	public JuxtaDocument getWitnessDocument( DocumentManager documentManager )
	{
		if( documentManager != null && difference != null )
		{
			return documentManager.lookupDocument(difference.getWitnessDocumentID());	
		}
		else
		{
			return null;
		}
	}
	
	public boolean isMarked()
	{
		return (notes != null);
	}

	public Difference getDifference()
	{
		return difference;
	}

	public String getNotes() 
    {
		return notes;
	}
	
	public void setNotes(String notes) 
	{
		this.notes = notes;
	}

    public boolean includeImage()
    {
        return includeImage;
    }
    
    public void setIncludeImage(boolean includeImage)
    {
        this.includeImage = includeImage;
    }
    

}
