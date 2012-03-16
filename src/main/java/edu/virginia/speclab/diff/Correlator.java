/*
 * Created on Mar 8, 2005
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
import edu.virginia.speclab.util.SimpleLogger;

/**
 * <code>Correlator</code> takes a <code>SymbolTable</code> and uses the information about 
 * individual symbols to combine them into blocks of symbols which share a common disposition
 * relative to the other file being compared. The results are then used by the 
 * <code>DifferenceCollector</code> class.   
 *  
 * @author Nick
 */
class Correlator
{
    // blocklen is the info about found blocks. It will be set to 0, except at
    // the line#s where blocks start in the old file. At these places it will be
    // set to the # of lines in the block. The array declarations are to 
    // MAXLINECOUNT+2 so that we can have two extra lines (pseudolines) at line# 0 
    // and line# MAXLINECOUNT+1 (or less).
    private int blocklen[];

    // Keeps track of information about file1 and file2 
    private SymbolTable symbolTable;
    
    // block len > any possible real block len 
    private static final int UNREAL = Integer.MAX_VALUE;
    
    public Correlator( SymbolTable symbolTable )
    {
        this.symbolTable = symbolTable;
        
        init();
        scan();
    }
    
    private void init()
    {
        FileInfo oldinfo = symbolTable.getOldInfo();
        FileInfo newinfo = symbolTable.getNewInfo();

        // records the length of blocks of matches at the start point of the block
        // +1 for the beginning marker and +1 for the end marker
        blocklen = new int[oldinfo.getSymbolCount() + 2];
        
        // clear the "other" tables
        oldinfo.init();
        newinfo.init();
    }
    
    /*
     * scanafter Expects both files in symtab, and oldinfo and newinfo valid.
     * Expects the "other" arrays contain positive #s to indicate lines that are
     * unique in both files. For each such pair of places, scans past in each
     * file. Contiguous groups of lines that match non-uniquely are taken to be
     * good-enough matches, and so marked in "other". Assumes each other[0] is
     * 0.
     */
    private void scanAfter()
    {
        FileInfo oldinfo = symbolTable.getOldInfo();
        FileInfo newinfo = symbolTable.getNewInfo();

        int oldline, newline;
    
        for (newline = 0; newline <= newinfo.getSymbolCount(); newline++)
        {
            oldline = newinfo.getCrossIndex(newline);
            if (oldline >= 0)
            { /* is unique in old & new */
                for (;;)
                { /* scan after there in both files */
                    if (++oldline > oldinfo.getSymbolCount())
                        break;
                    if (oldinfo.getCrossIndex(oldline) >= 0)
                        break;
                    if (++newline > newinfo.getSymbolCount())
                        break;
                    if (newinfo.getCrossIndex(newline) >= 0)
                        break;
    
                    /*
                     * oldline & newline exist, and aren't already matched
                     */
    
                    if (newinfo.getSymbol(newline) != oldinfo.getSymbol(oldline) )
                        break; // not same
    
                    recordMatch(oldline,newline);
                }
            }
        }
    }

    /**
     * scanbefore As scanafter, except scans towards file fronts. Assumes the
     * off-end lines have been marked as a match.
     */
    private void scanBefore()
    {
        FileInfo oldinfo = symbolTable.getOldInfo();
        FileInfo newinfo = symbolTable.getNewInfo();

        int oldline, newline;
    
        for (newline = newinfo.getSymbolCount() + 1; newline > 0; newline--)
        {
            oldline = newinfo.getCrossIndex(newline);
            if (oldline >= 0)
            { /* unique in each */
                for (;;)
                {
                    if (--oldline <= 0)
                        break;
                    if (oldinfo.getCrossIndex(oldline) >= 0)
                        break;
                    if (--newline <= 0)
                        break;
                    if (newinfo.getCrossIndex(newline) >= 0)
                        break;
    
                    /*
                     * oldline and newline exist, and aren't marked yet
                     */
    
                    if (newinfo.getSymbol(newline) != oldinfo.getSymbol(oldline) )
                        break; // not same
    
                    recordMatch(oldline,newline);
                }
            }
        }
    }

    /**
     * scanblocks - Finds the beginnings and lengths of blocks of matches. Sets
     * the blocklen array (see definition). Expects oldinfo valid.
     */
    private void scanBlocks()
    {
        FileInfo oldinfo = symbolTable.getOldInfo();

        int frontOfBlock = 0; 
        int newlast = -1; // newline's value during prev. iteration
    
        blocklen[oldinfo.getSymbolCount() + 1] = UNREAL; // starts a mythical blk
    
        for (int oldline = 1; oldline <= oldinfo.getSymbolCount(); oldline++)
        {
            int newline = oldinfo.getCrossIndex(oldline);
            
            // no match: not in block
            if (newline < 0)
            {                 
                frontOfBlock = 0; 
            }            
            // match
            else
            { 
                // new block 
                if (frontOfBlock == 0 || newline != (newlast + 1) )    
                {
                    frontOfBlock = oldline;
                }

                // add line to count
                ++blocklen[frontOfBlock];
            }
            
            newlast = newline;
        }
    }

    /*
     * scanunique Scans for lines which are used exactly once in each file.
     * Expects both files in symtab, and oldinfo and newinfo valid. The
     * appropriate "other" array entries are set to the line# in the other file.
     * Claims pseudo-lines at 0 and XXXinfo.maxLine+1 are unique.
     */
    private void scanUnique()
    {
        FileInfo oldinfo = symbolTable.getOldInfo();
        FileInfo newinfo = symbolTable.getNewInfo();

        int oldline, newline;
        Symbol psymbol;
    
        for (newline = 1; newline <= newinfo.getSymbolCount(); newline++)
        {
            psymbol = newinfo.getSymbol(newline);
            if (psymbol.symbolIsUnique())
            { // 1 use in each file
                oldline = psymbol.getBaseIndex();
                recordMatch(oldline,newline); // record 1-1 map
            }
        }

        // link beginning of the files, before the first symbol
        recordMatch(0,0);
        
        // link the end of the files, after the last symbol
        int newLastLine = newinfo.getSymbolCount()+1;
        int oldLastLine = oldinfo.getSymbolCount()+1;

        recordMatch(oldLastLine,newLastLine);
    }
    
    private void recordMatch( int oldLine, int newLine )
    {
        FileInfo oldinfo = symbolTable.getOldInfo();
        FileInfo newinfo = symbolTable.getNewInfo();

        newinfo.setCrossIndex(newLine,oldLine);
        oldinfo.setCrossIndex(oldLine,newLine);
    }

    /*
     * transform Analyzes the file differences and leaves its findings in the
     * global arrays oldinfo.other, newinfo.other, and blocklen. Expects both
     * files in symtab. Expects valid "maxLine" and "symbol" in oldinfo and
     * newinfo.
     */
    private void scan()
    {    
        // scan for lines used once in both files
        scanUnique();
        
        // scan past sure-matches for non-unique blocks
        scanAfter(); 
        
        // scan backwards from sure-matches
        scanBefore();
        
        // find the fronts and lengths of blocks
        scanBlocks();  
    }
    
    public int getBlockSize( int index )
    {
        if( index >= 0 && index < blocklen.length )
        {
            return blocklen[index];
        }
        else
        {
            SimpleLogger.logError("Accessed invalid block index: "+index);
            return -1;
        }
    }
    
    public int getNumberOfSymbols()
    {
        return symbolTable.getNumberOfSymbols();
    }
    
    public FileInfo getOldInfo()
    {
        return symbolTable.getOldInfo();
    }
    
    public FileInfo getNewInfo()
    {
        return symbolTable.getNewInfo();
    }

    public int getBlockLength()
    {
        return blocklen.length;
    }

    public DocumentModel getBaseDocument()
    {
        if( symbolTable != null )         
            return symbolTable.getBaseDocument();
        else
            return null;
    }
    
    public DocumentModel getWitnessDocument()
    {
        if( symbolTable != null )
            return symbolTable.getWitnessDocument();
        else
            return null;
    }
    
   
}
