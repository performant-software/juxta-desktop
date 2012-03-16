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
 

package edu.virginia.speclab.juxta.author.model;

import edu.virginia.speclab.diff.Difference;

public class Annotation 
{
	private Difference difference;		
	private String notes;
    private boolean includeImage;
    private boolean fromOldVersion;
    
    public Annotation( Annotation annotation )
    {
        this.difference = annotation.difference;
        this.notes = annotation.notes;
        this.includeImage = annotation.includeImage;
        this.fromOldVersion = annotation.fromOldVersion;
    }
		
	public Annotation( Difference difference )
	{
		this.difference = difference;
	}

    public void setFromOldVersion(boolean value)
    {
        this.fromOldVersion = value;
    }

    public boolean isFromOldVersion()
    {
        return fromOldVersion;
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
