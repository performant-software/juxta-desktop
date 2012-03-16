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

public class LocationMarker extends OffsetRange
{
    private String id;
    private int number;
    private String name;
    private String locationType;
    private Image image;
    
    public LocationMarker( String id, String name, String type, int number, Image image, int offset )
    {
        super();
        this.id = id;
        this.name = name;
        this.number = number;
        this.locationType = (type!=null)?type:"";
        this._startOffset = offset;
        this.image = image;
    }
    
    public boolean hasImage()
    {
        return (image!=null);
    }

    public Image getImage()
    {
        return image;
    }    

    public String getLocationName()
    {
        if(( name == null ) || (name.equals("")))
        {
            return locationType+ " " + Integer.toString(number);    
        }
        else
        {
            return name;
        }
        
    }    

    public void setEndOffset(int endOffset)
    {
        this._endOffset = endOffset;
    }
    
    public String getLocationType()
    {
		return locationType;
	}

    public int getNumber()
    {
        return number;
    }

    public String getID()
    {
        return id;
    }
}
