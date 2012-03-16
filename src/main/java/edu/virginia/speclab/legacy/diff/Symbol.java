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

class Symbol
{ 
    private static final int freshnode = 0, bothonce = 3;
    
    private int symbolBaseIndex;
    private int linestate;
    private String line;

    /**
     * Construct a new symbol table node and fill in its fields.
     * 
     * @param string
     *            A line of the text file
     */
    public Symbol(String pline)
    {
        linestate = freshnode;
        // linenum field is not always valid 
        line = pline;        
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
    public String getLine()
    {
        return line;
    }
    
    public int getSymbolLength()
    {
        return line.length();
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

