/*
 * Created on Feb 3, 2005
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