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

package edu.virginia.speclab.legacy.diff.collation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import edu.virginia.speclab.legacy.diff.Difference;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * This class maps each character in the base document to a difference, if there is one, with 
 * the witness document. 
 * @author Nick
 *
 */
public class DifferenceMap
{
    private int source;
    private Difference differenceMap[];
    
    public DifferenceMap( List differenceList, int baseLength, int source )
    {
        if( differenceList != null )
        {
            this.differenceMap = new Difference[baseLength];
            this.source = source;
            
            for( Iterator i = differenceList.iterator(); i.hasNext(); )
            {
                Difference difference = (Difference) i.next();
                
                if( difference.getType() != Difference.MOVE )
                {
                    int start = difference.getOffset(source);
                    int end = start + difference.getLength(source);
                    
                    // difference range must start within document 
                    if( start < baseLength ) 
                    {   
                        // clip highlighting at the end of the document
                        if( end > baseLength ) end = baseLength;
                        
                        // record a zero length entry
                        if( start == end ) differenceMap[start] = difference;
    
                        for( int j = start; j < end; j++ )
                        {
                            differenceMap[j] = difference;    
                        }
                    }
                }
            }
        }
        else
        {
            SimpleLogger.logInfo("No differences found.");
        }
    }
    
    public boolean isEmpty()
    {
        return (differenceMap == null);
    }
    
    /**
     * Reports if there is a non-zero length difference at this offset in the document.
     * @param offset The target offset.
     * @return true if present
     */
    public boolean differencePresent( int offset )
    {
        if( !isEmpty() && inBounds(offset) )
        {
            Difference difference = differenceMap[offset];
            
            // if there is a difference here and it has a length greater than zero
            // then report it present at this offset. 
            if( difference != null && difference.getLength(this.source) > 0 )
            {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean inBounds( int offset )
    {
        if( differenceMap == null ) return false;
        else return (offset >= 0 && offset < differenceMap.length );
    }

    /**
     * Reports all differences between the start and end offsets, inclusive. This includes
     * zero length differences, unlike <code>differencePresent()</code>
     * @param start Start offset
     * @param end End offset
     * @return A <code>HashSet</code> of <code>Difference</code> objects or <code>null</code> if no differences are found.
     */
    public HashSet getDifferences( int start, int end )
    {
        if( !inBounds(start) || !inBounds(end) ) return null;
        
        HashSet differenceSet = new HashSet();
        
        // look for differences on this line and add them to the set            
        for( int offset = start; offset <= end; offset++ )
        {
            Difference difference = differenceMap[offset];
            
            if( difference != null )
            {
                // note: duplicates are filtered out because this container is a set
                differenceSet.add(difference);
            }
        }
        
        // if we found no differences, return null
        if( differenceSet.size() == 0 ) return null;
        else return differenceSet;
    }

    public int getLength()
    {
        if( differenceMap == null ) return 0;
        else return differenceMap.length; 
    }
}
