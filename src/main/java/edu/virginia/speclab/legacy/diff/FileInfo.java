/*
 * Created on Feb 3, 2005
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

import java.util.ArrayList;

class FileInfo
{
    // The symtab handle of each line. 
    private ArrayList symbolList;

    // Table to keep track of document offsets
    private ArrayList documentOffsets;
    
    // Map of line# to line# in other file
    // ( -1 means don't-know )
    // Allocated AFTER the lines are read.

    private int crossIndex[];

    public FileInfo()
    {
        symbolList = new ArrayList();
        documentOffsets = new ArrayList();
    }
 
    public void addSymbol( Symbol node, int documentOffset )
    {
        symbolList.add(node);
        documentOffsets.add(new Integer(documentOffset));
    }
    
    public int getCrossIndex( int index )
    {
		if( index < 0 || index >= crossIndex.length ) return -1;
		else return crossIndex[index];
    }
    
    public int getDocumentOffset( int index )
    {
        Integer offset = (Integer) documentOffsets.get(index-1); 
        return offset.intValue();
    }
    
    public void setCrossIndex( int index, int value )
    {
        crossIndex[index] = value;
    }
    
    public Symbol getSymbol( int position )
    {
        if( position < 0 || position > getSymbolCount() ) return null;
        
        return (Symbol) symbolList.get(position-1);
    }
    
    public ArrayList getSymbolList()
    {        
        return symbolList;
    }
    
    public int getSymbolCount()
    {
        return symbolList.size();
    }
    
    public void init()
    {
        crossIndex = new int[getSymbolCount() + 2];
        
        // Count pseudolines 
        int max = getSymbolCount() + 2; 
    
        for ( int line = 0; line < max; line++)
        {
            crossIndex[line] = -1;
        }        
    }
}