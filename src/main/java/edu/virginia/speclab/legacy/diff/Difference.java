/*
 * Created on Feb 8, 2005
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

package edu.virginia.speclab.legacy.diff;

import java.io.Serializable;

/**
 * @author Nick
 * 
 * A difference between a base text and a given witness text.
 */
public class Difference implements Serializable
{
    public static final int BASE = 0;
    public static final int WITNESS = 1;
    public static final int NONE = 0, DELETE = 1, INSERT = 2, CHANGE = 6, MOVE = 7;

    private int baseDocumentID, witnessDocumentID;    
    private int baseOffset, witnessOffset;
    private int baseLength, witnessLength;
    private int type;
    
    private int distance;
        
    public Difference( int baseDocumentID, int witnessDocumentID, int type )
    {
        this.baseDocumentID = baseDocumentID;
        this.witnessDocumentID = witnessDocumentID;
        this.type = type;
        this.distance = Integer.MAX_VALUE;
    }
    
    public Difference duplicate()
    {
    	Difference other = new Difference(baseDocumentID, witnessDocumentID, type);
    	other.baseOffset = baseOffset;
    	other.baseLength = baseLength;
    	other.witnessOffset = witnessOffset;
    	other.witnessLength = witnessLength;
    	other.distance = distance;
    	return other;
    }
    /**
     * Returns the code for the type of difference. 
     * @return Returns the type.
     */
    public int getType()
    {
        return type;
    }
    
    public void setType(int type)
    {
    	this.type = type;
    }

    public static String getTypeName(int type)
    {
    	switch (type)
    	{
    	case 0: return "NONE";
    	case 1: return "DELETE";
    	case 2: return "INSERT";
    	case 6: return "CHANGE";
    	case 7: return "MOVE";
    	default: return "UNKNOWN TYPE (" + Integer.toString(type) + ")";
    	}
    }

    public static int getTypeValue(String str)
    {
    	if (str.equals("NONE")) return NONE;
    	if (str.equals("DELETE")) return DELETE;
    	if (str.equals("INSERT")) return INSERT;
    	if (str.equals("CHANGE")) return CHANGE;
    	if (str.equals("MOVE")) return MOVE;
    	return NONE;
    }

    /**
     * Sets the number of lines in the witness citation.
     * @param witnessLength The witnessLength to set.
     */
    public void setWitnessTextLength(int witnessLength)
    {
        this.witnessLength = witnessLength;
    }
       
    /**
     * Sets the number of lines in the base text citation.
     * @param baseLength The baseLength to set.
     */
    public void setBaseTextLength(int baseLength)
    {
        this.baseLength = baseLength;
    }
    
    public int getOffset( int offsetType )
    {
        if( offsetType == BASE )
        {
            return baseOffset;
        }
        else
        {
            return witnessOffset;
        }
    }
    
    public int getLength( int offsetType )
    {
        if( offsetType == BASE )
        {
            return baseLength;            
        }
        else
        {
            return witnessLength;
        }
    }
    
    /**
     * @param baseOffset The baseOffset to set.
     */
    public void setBaseOffset(int baseOffset)
    {
        this.baseOffset = baseOffset;
    }
	
	/**
     * @param witnessOffset The witnessOffset to set.
     */
    public void setWitnessOffset(int witnessOffset)
    {
        this.witnessOffset = witnessOffset;
    }

    public int getBaseDocumentID()
    {
        return baseDocumentID;
    }
    

    public int getWitnessDocumentID()
    {
        return witnessDocumentID;
    }

    public int getDistance()
    {
        return distance;
    }

    public void setDistance(int distance)
    {
        this.distance = distance;
    }
	
	/**
	 * Compares for semantic equivalence without the side effects of 
	 * overriding equals().
	 * @param d the difference to compare to this difference.
	 * @return true if they are the same, false otherwise.
	 */
	public boolean same( Difference d )
	{
		if( d.type == this.type &&
			d.baseDocumentID == this.baseDocumentID &&
		    d.witnessDocumentID == this.witnessDocumentID &&
		    d.baseOffset == this.baseOffset &&
		    d.witnessOffset == this.witnessOffset &&		     
		    d.baseLength == this.baseLength && 
		    d.witnessLength == this.witnessLength )
		{
			return true;
		}
		
		return false;
	}

	public String testContents(int type, int baseOffset, int baseLength, int witnessOffset, int witnessLength)
	{
		String str = "";
		if (this.type != type)
			str += "Type: " + getTypeName(this.type) + "!=" + getTypeName(type) + " ";
		if (this.baseOffset != baseOffset)
			str += "baseOffset: " + this.baseOffset + "!=" + baseOffset + " ";
		if (this.baseLength != baseLength)
			str += "baseLength: " + this.baseLength + "!=" + baseLength + " ";
		if (this.witnessOffset != witnessOffset)
			str += "witnessOffset: " + this.witnessOffset + "!=" + witnessOffset + " ";
		if (this.witnessLength != witnessLength)
			str += "witnessLength: " + this.witnessLength + "!=" + witnessLength + " ";
		return str;
	}
	
	public String dump()
	{
		return getTypeName(type) + ": (" + baseOffset + "," + baseLength + ") (" + witnessOffset + "," + witnessLength + ")";
	}
	
	public String dumpContents(String baseText, String witnessText)
	{
		String str = "";
		str += "B: " + getTypeName(getType()) + " " + insertBrackets(baseText, baseOffset, baseOffset + baseLength) + "\n"; 
		str += "W: " + getTypeName(getType()) + " " + insertBrackets(witnessText, witnessOffset, witnessOffset + witnessLength) + "\n"; 
		return str;
	}
	
	public String dumpContentsTruncated(String baseText, String witnessText)
	{
		String str = "";
		str += "B: " + getTypeName(getType()) + " " + Truncate(insertBrackets(baseText, baseOffset, baseOffset + baseLength), baseOffset, baseOffset + baseLength) + "\n"; 
		str += "W: " + getTypeName(getType()) + " " + Truncate(insertBrackets(witnessText, witnessOffset, witnessOffset + witnessLength), witnessOffset, witnessOffset + witnessLength) + "\n"; 
		return str;
	}
	
	private String Truncate(String strSrc, int iStart, int iEnd)
	{
		int first = iStart - 10;
		if (first < 0) first = 0;
		int last = iEnd + 10; 
		if (last >= strSrc.length())
			last = strSrc.length()-1;
		return "[" + iStart + "," + iEnd + "] " + strSrc.substring(first, last);
	}
	
	private String insertBrackets(String strSrc, int iStart, int iEnd)
	{
		try
		{
			if (iStart == iEnd)
			{
				String str = strSrc.substring(0, iStart) + "[" + "]" + strSrc.substring(iEnd); 
				return str.replaceAll("\n", "/");
			}
			else
			{
				String str = strSrc.substring(0, iStart) + "[" + strSrc.substring(iStart, iEnd) + "]" + strSrc.substring(iEnd); 
				return str.replaceAll("\n", "/");
			}
		}
		catch (Exception e)
		{
			return "Error:" + iStart + " " + iEnd + " " + strSrc;
		}
	}
}
