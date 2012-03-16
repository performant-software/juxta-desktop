/*
 * Created on Mar 8, 2005
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

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import edu.virginia.speclab.legacy.diff.document.DocumentModel;
import edu.virginia.speclab.legacy.diff.token.Token;

/**
 * Creating a <code>SymbolTable</code> is the first step in the comparing two tokenized documents.
 * The symbol table hashes the tokens from both documents and determines which symbols are 
 * uniques to which documents, if at all. This information is then used by the <code>Correlator</code>.   
 *  
 * @author Nick
 * 
 */
class SymbolTable
{        
    private DocumentModel baseDocument, witnessDocument;
    private int numberOfSymbols;
    
    private FileInfo oldinfo,newinfo;
    private Hashtable symbolTable;
    
    private static final int freshnode = 0, oldonce = 1, newonce = 2,
    bothonce = 3, other = 4;
    
    public SymbolTable( DocumentModel oldFile, DocumentModel newFile )   
    {
        this.baseDocument = oldFile;
        this.witnessDocument = newFile;
        
        symbolTable = new Hashtable();
        
        try
        {
            oldinfo = inputScan( oldFile, true );
            newinfo = inputScan( newFile, false );
        } 
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Saves line into the symbol table. Returns the Symbol for that unique line. If inoldfile
     * nonzero, then linenum is remembered.
     */
    private Symbol addSymbol(String line, boolean inOldFile, int symbolIndex )
    {
        // find the node in the tree
        Symbol symbol = matchSymbol(line);
        
        if (symbol.getLineState() == freshnode)
        {
            symbol.setLineState(inOldFile ? oldonce : newonce);
        } 
        else
        {
            if ((symbol.getLineState() == oldonce && !inOldFile) ||                    
                (symbol.getLineState() == newonce && inOldFile)     )
            {
                symbol.setLineState(bothonce);
            }
            else
            {
                symbol.setLineState(other);
            }
        }
        
        if (inOldFile)
        {
            symbol.setBaseIndex(symbolIndex);
        }
        
        return symbol;
    }
    
    private FileInfo inputScan( DocumentModel document, boolean oldFile ) throws IOException
    {
        FileInfo fileInfo = new FileInfo();
                
        List tokenList = document.getTokenList();
        
        for( Iterator i = tokenList.iterator(); i.hasNext(); )
        {
            Token token = (Token) i.next();            
            int symbolIndex = fileInfo.getSymbolCount()+1;            
            Symbol node = addSymbol(token.getToken(), oldFile, symbolIndex );
            fileInfo.addSymbol(node, token.getOffset() );
        }        
        
        return fileInfo;
    }

    /**
     * matchsymbol Searches tree for a match to the line.
     * 
     * @param String
     *            pline, a line of text If node's linestate == freshnode,
     *            then created the node.
     */
    private Symbol matchSymbol(String pline)
    {
        Integer hashcode = new Integer(pline.hashCode());
        Symbol symbol = (Symbol) symbolTable.get(hashcode);
        
        if( symbol == null )
        {
            symbol = createSymbol(pline);
            symbolTable.put(hashcode,symbol);
        }
        
        return symbol;
    }
    
    private Symbol createSymbol( String line )
    {
        numberOfSymbols++;
        return new Symbol(line);
    }

    /**
     * @return Returns the newinfo.
     */
    public FileInfo getNewInfo()
    {
        return newinfo;
    }
    
    public int getNumberOfSymbols()
    {
        return numberOfSymbols;
    }
    
    /**
     * @return Returns the oldinfo.
     */
    public FileInfo getOldInfo()
    {
        return oldinfo;
    }

    public DocumentModel getBaseDocument()
    {
        return baseDocument;
    }
    

    public DocumentModel getWitnessDocument()
    {
        return witnessDocument;
    }
    
}
