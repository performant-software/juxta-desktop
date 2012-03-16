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

import edu.virginia.speclab.diff.token.Token;

class Symbol
{ 
    private static final int freshnode = 0, bothonce = 3;
    
    private int symbolBaseIndex;
    private int linestate;
    private Token token;

    /**
     * Construct a new symbol table node and fill in its fields.
     * 
     * @param string
     *            A line of the text file
     */
    public Symbol(Token token)
    {
        linestate = freshnode;
        // linenum field is not always valid 
        this.token = token;
    }

    /**
     * symbolIsUnique Arg is a ptr previously returned by addSymbol.
     * -------------- Returns true if the line was added to the symbol table
     * exactly once with inoldfile true, and exactly once with inoldfile
     * false.
     */
    public boolean symbolIsUnique()
    {        
        return (linestate == bothonce);
    }

    /**
     * @return Returns the index in base file symbol array where this node is found.
     */
    public int getBaseIndex()
    {
        return symbolBaseIndex;
    }
    
    /**
     * @param index Set the index in the symbol array where this node is found.
     */
    public void setBaseIndex(int index)
    {
        this.symbolBaseIndex = index;
    }
    
    /**
     * @return Returns the line.
     */
    public String getTokenText()
    {
        return token.getToken();
    }
    
    public int getSymbolLength()
    {
        return token.getToken().length();
    }
    
    /**
     * @return Returns the linestate.
     */
    public int getLineState()
    {
        return linestate;
    }
    /**
     * @param linestate The linestate to set.
     */
    public void setLineState(int linestate)
    {
        this.linestate = linestate;
    }
}

