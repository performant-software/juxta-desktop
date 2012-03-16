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

public class LocationMarker 
{
    private String id;
    private int offset,length,number;
    private String name, locationType;
    private Image image;
    
    public LocationMarker( String id, String name, String type, int number, Image image, int offset )
    {
        this.id = id;
        this.name = name;
        this.number = number;
        this.locationType = (type!=null)?type:"";
        this.offset = offset;
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
        if(( name == null ) || (name == ""))
        {
            return locationType+ " " + Integer.toString(number);    
        }
        else
        {
            return name;
        }
        
    }    

    public int getOffset()
    {
        return offset;
    }

    public int getLength()
    {
        return length;
    }
    

    public void setLength(int length)
    {
        this.length = length;
    }

	public String getLocationType() {
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

	public void setOffset(int offset) {
		this.offset = offset;
	}
    
}
