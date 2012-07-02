/*
 * Created on Feb 8, 2005
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

package edu.virginia.speclab.diff;

import edu.virginia.speclab.diff.document.DocumentModel;
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

//    private int baseDocumentID, witnessDocumentID;
//    private int baseOffset, witnessOffset;
//    private int baseLength, witnessLength;
    private int type;

    private OffsetRange baseRange;
    private OffsetRange witnessRange;
    
    private int distance;
    
    public Difference( DocumentModel baseDocument, DocumentModel witnessDocument, int type )
    {
        this.baseRange = new OffsetRange(baseDocument);
        this.witnessRange = new OffsetRange(witnessDocument);
        this.type = type;
        this.distance = Integer.MAX_VALUE;
    }


    public Difference( int baseDocumentID, int witnessDocumentID, int type )
    {
        this.baseRange = new OffsetRange(baseDocumentID);
        this.witnessRange = new OffsetRange(witnessDocumentID);
        this.type = type;
        this.distance = Integer.MAX_VALUE;
    }
    
    public Difference duplicate()
    {
    	Difference other = new Difference(this.baseRange.getDocument(), this.witnessRange.getDocument(), type);
        other.baseRange = new OffsetRange(this.baseRange);
        other.witnessRange = new OffsetRange(this.witnessRange);
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
        setWitnessTextLength(witnessLength, OffsetRange.Space.ACTIVE);
    }

    public void setWitnessTextLength(int witnessLength, OffsetRange.Space space)
    {
        witnessRange.set(witnessRange.getStartOffset(space), witnessRange.getStartOffset(space) + witnessLength, space);
    }

    /**
     * Sets the number of lines in the base text citation.
     * @param baseLength The baseLength to set.
     */
    public void setBaseTextLength(int baseLength)
    {
        setBaseTextLength(baseLength, OffsetRange.Space.ACTIVE);
    }

    public void setBaseTextLength(int baseLength, OffsetRange.Space space)
    {
       baseRange.set(baseRange.getStartOffset(space), baseRange.getStartOffset(space) + baseLength, space);
    }

    public int getOffset(int offsetType)
    {
        return getOffset(offsetType, OffsetRange.Space.ACTIVE);
    }

    public int getOffset( int offsetType, OffsetRange.Space space )
    {
        if( offsetType == BASE )
        {
            return baseRange.getStartOffset(space);
        }
        else
        {
            return witnessRange.getStartOffset(space);
        }
    }
    
    public int getLength(int offsetType)
    {
        return getLength(offsetType, OffsetRange.Space.ACTIVE);
    }
    
    public int getLength( int offsetType, OffsetRange.Space space )
    {
        if( offsetType == BASE )
        {
            return baseRange.getLength(space);
        }
        else
        {
            return witnessRange.getLength(space);
        }
    }
    
    /**
     * @param baseOffset The baseOffset to set.
     */
    public void setBaseOffset(int baseOffset)
    {
        setBaseOffset(baseOffset, OffsetRange.Space.ACTIVE);
    }

    public void setBaseOffset(int baseOffset, OffsetRange.Space space)
    {
        int adjustment = baseOffset - baseRange.getStartOffset(space);
        baseRange.set(baseOffset, baseRange.getEndOffset(space) + adjustment, space);
    }
	
	/**
     * @param witnessOffset The witnessOffset to set.
     */
    public void setWitnessOffset(int witnessOffset)
    {
        setWitnessOffset(witnessOffset, OffsetRange.Space.ACTIVE);
    }

    public void setWitnessOffset(int witnessOffset, OffsetRange.Space space)
    {
        int adjustment = witnessOffset - witnessRange.getStartOffset(space);
        witnessRange.set(witnessOffset, witnessRange.getEndOffset(space) + adjustment, space);
    }

    public DocumentModel getBaseDocument()
    {
        return baseRange.getDocument();
    }

    public int getBaseDocumentID()
    {
        return baseRange.getDocumentID();
    }

    public DocumentModel getWitnessDocument()
    {
        return witnessRange.getDocument();
    }
    

    public int getWitnessDocumentID()
    {
        return witnessRange.getDocumentID();
    }

    public int getDistance()
    {
        return distance;
    }

    public void setDistance(int distance)
    {
        this.distance = distance;
    }

    public OffsetRange getBaseRange()
    {
        return this.baseRange;
    }

    public OffsetRange getWitnessRange()
    {
        return this.witnessRange;
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
			d.getBaseDocumentID() == this.getBaseDocumentID() &&
		    d.getWitnessDocumentID() == this.getWitnessDocumentID() &&
		    d.getOffset(BASE) == this.getOffset(BASE) &&
		    d.getOffset(WITNESS) == this.getOffset(WITNESS) &&
		    d.getLength(BASE) == this.getLength(BASE) &&
		    d.getLength(WITNESS) == this.getLength(WITNESS) )
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
		if (this.getOffset(BASE) != baseOffset)
			str += "baseOffset: " + this.getOffset(BASE) + "!=" + baseOffset + " ";
		if (this.getLength(BASE) != baseLength)
			str += "baseLength: " + this.getLength(BASE) + "!=" + baseLength + " ";
		if (this.getOffset(WITNESS) != witnessOffset)
			str += "witnessOffset: " + this.getOffset(WITNESS) + "!=" + witnessOffset + " ";
		if (this.getLength(WITNESS) != witnessLength)
			str += "witnessLength: " + this.getLength(WITNESS) + "!=" + witnessLength + " ";
		return str;
	}
	
	public String dump()
	{
		return getTypeName(type) + ": (" + getOffset(BASE) + "," + getLength(BASE) + ") (" + getOffset(WITNESS) + "," + getLength(WITNESS) + ")";
	}
	
	public String dumpContents(String baseText, String witnessText)
	{
		String str = "";
		str += "B: " + getTypeName(getType()) + " " + insertBrackets(baseText, getOffset(BASE), getOffset(BASE) + getLength(BASE)) + "\n";
		str += "W: " + getTypeName(getType()) + " " + insertBrackets(witnessText, getOffset(WITNESS), getOffset(WITNESS) + getLength(WITNESS)) + "\n";
		return str;
	}
	
	public String dumpContentsTruncated(String baseText, String witnessText)
	{
		String str = "";
		str += "B: " + getTypeName(getType()) + " " + Truncate(insertBrackets(baseText, getOffset(BASE), getOffset(BASE) + getLength(BASE)), getOffset(BASE), getOffset(BASE) + getLength(BASE)) + "\n";
		str += "W: " + getTypeName(getType()) + " " + Truncate(insertBrackets(witnessText, getOffset(WITNESS), getOffset(WITNESS) + getLength(WITNESS)), getOffset(WITNESS), getOffset(WITNESS) + getLength(WITNESS)) + "\n";
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
