/*
 * Created on Feb 2, 2005
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
 * This class takes a correlated pair of files (represented with a <code>Correlator</code> object
 * and generates a set of differences in the form of a <code>DifferenceSet</code> object.   
 *  
 * @author Nick
 */
class DifferenceCollector
{
    private static final int idle = 0, delete = 1, insert = 2, changefrom = 5, changeto = 6;

    private int collectStatus;
    private boolean anyCollected;
    private int baseIndex, witnessIndex; // line numbers in old & new file
    
    private Correlator correlator;
    
    private DifferenceSet differenceSet;
    private Difference currentDifference;

    // used for calculating the length of a difference
    private LengthMarker baseLengthMarker, witnessLengthMarker;

    public DifferenceCollector( Correlator correlator )
    {
        this.correlator = correlator;
        collectDifferences();
    }
    
    /*
     * newconsume Part of printout. Have run out of old file. Print the rest of
     * the new file, as inserts and/or moves.
     */
    private void consumeWitness()
    {
        SimpleLogger.logInfo("Running through witness text to end.", DiffAlgorithm.VERBOSE_LOGGING );

        FileInfo newinfo = correlator.getNewInfo();

        // In this mode, we assume we have already exhausted the base file and all is left is 
        // the witness file
        for (;;)
        {
            // if we are done with the witness file, break out
            if (witnessIndex > newinfo.getSymbolCount())
            {
                break; 
            }
            // if there is not cross reference from witness to base...
            if (newinfo.getCrossIndex(witnessIndex) < 0)
            {
                // if we are in the middle of recording a change, continue
                if( collectStatus == changefrom || collectStatus == changeto )
                {
                    collectChangeTo();
                }
                // otherwise, it must be an insert
                else
                {
                    collectInsert();
                }
            }
            // if there is a cross reference to the base file, the block must have moved from 
            // earlier in the base file
            else
            {
            	SimpleLogger.logInfo("consumeWitness collectMove() reached.",DiffAlgorithm.VERBOSE_LOGGING);
                collectMove();  
//                System.out.println(currentDifference.dump() + "consumeWitness collectMove() reached.");
                //break;
            }
        }
    }

    /**
     * skipnew Part of printout. Expects printnewline is at start of a new block
     * that has already been announced as a move. Skips over the new block.
     */
    private void skipWitness()
    {
        SimpleLogger.logInfo("Skipping block in witness text.", DiffAlgorithm.VERBOSE_LOGGING);

        FileInfo newinfo = correlator.getNewInfo();
        
        int oldline;
        collectStatus = idle;
        for (;;)
        {
            if (++witnessIndex > newinfo.getSymbolCount())
                break; /* end of file */
            oldline = newinfo.getCrossIndex(witnessIndex);
            if (oldline < 0)
                break; /* end of block */
            if (correlator.getBlockSize(oldline) != 0)
                break; /* start of another */
        }
    }
    
    /**
     * skipold Part of printout. Expects printoldline at start of an old block
     * that has already been announced as a move. Skips over the old block.
     */
    private void skipBase()
    {
        SimpleLogger.logInfo("Skipping block in base file.", DiffAlgorithm.VERBOSE_LOGGING);

        FileInfo oldinfo = correlator.getOldInfo();

        collectStatus = idle;
        for (;;)
        {
            if (++baseIndex > oldinfo.getSymbolCount())
                break; /* end of file */
            if (oldinfo.getCrossIndex(baseIndex) < 0)
                break; /* end of block */
            if (correlator.getBlockSize(baseIndex) != 0)
                break; /* start of another */
        }
    }

    /**
     * oldconsume Part of printout. Have run out of new file. Process the rest
     * of the old file, printing any parts which were deletes or moves.
     */
    private void consumeBase()
    {
        SimpleLogger.logInfo("Running through base text to end.", DiffAlgorithm.VERBOSE_LOGGING);

        FileInfo oldinfo = correlator.getOldInfo();

        // assume we have exhausted the witness file and go through the rest of the base file
        for (;;)
        {
            // done with base file
            if (baseIndex > oldinfo.getSymbolCount())
            {
                break; /* end of file */
            }
            
            // if this is no cross reference from base to witness, must be a delete 
            if ( oldinfo.getCrossIndex(baseIndex) < 0 )
            {
                collectDelete();
            }            
            else // if it hasn't been processed, this block must have been moved from here in the witness            else
            {
                collectMove();
//                System.out.println(currentDifference.dump() + "consumeBase collectMove() reached.");
            }
        }
    }

    private void collectDifferences()
    {
      FileInfo oldinfo = correlator.getOldInfo();
      FileInfo newinfo = correlator.getNewInfo();

      SimpleLogger.logInfo("Collecting differences.", DiffAlgorithm.VERBOSE_LOGGING);
      
      // Collection of differences
      differenceSet = new DifferenceSet();
      
      differenceSet.setBaseDocument(correlator.getBaseDocument());
      differenceSet.setWitnessDocument(correlator.getWitnessDocument());      
      differenceSet.setNumberOfSymbols(correlator.getNumberOfSymbols());
      
        // These length markers are used to measure the length of runs in their respective text streams
        baseLengthMarker = new LengthMarker(oldinfo);
        witnessLengthMarker = new LengthMarker(newinfo);

        collectStatus = idle;
        anyCollected = false;
        for (baseIndex = witnessIndex = 1;;)
        {            
            // if we are at the end of the base file, change modes and consume the rest of the witness file
            if (baseIndex > oldinfo.getSymbolCount())
            {
                consumeWitness();
                break;
            }
            
            // if we are at the end of the witness file, change modes and consume the rest of base
            if (witnessIndex > newinfo.getSymbolCount())
            {
                consumeBase();
                break;
            }
            
            // If there is no cross reference here from witness to base...
            if (newinfo.getCrossIndex(witnessIndex) < 0)
            {
                // If there is no cross reference from base to witness either, this text must have changed.
                if (oldinfo.getCrossIndex(baseIndex) < 0)
                {
                    collectChangeFrom();
                }
                // There is a cross reference from base to witness
                else
                {
                    // We are in the midst of recording a change, continue
                    if( collectStatus == changefrom || collectStatus == changeto )
                    {
                        collectChangeTo();
                    }
                    // If this isn't a change, then this is a one way reference from base to witness
                    // which is an insert.
                    else
                    {
                        collectInsert();    
                    }
                }
                    
            } 
            // This is a reference from witness to base, but not vice versa, this is a delete
            else if (oldinfo.getCrossIndex(baseIndex) < 0)
            {
                collectDelete();
            }
            
            // Otherwise.. we can deduce that there is a reference from witness->base and base->witness
            
            // The references are to the same symbol, exact match, skip it.
            else if (oldinfo.getCrossIndex(baseIndex) == witnessIndex)
            {
                skipSame();
            }
            // Otherwise, the references must relate two different symbols, must be a "move".
            else
            {
                collectMove();
            }
        }
        
        // close the last difference that was added to the set.
        if( currentDifference != null )
        {
            addDifference(currentDifference);
        }
        
        // Log total number of differences
        if (anyCollected == true)
        {
            SimpleLogger.logInfo("Collected "+differenceSet.getDifferenceList().size()+" differences.", DiffAlgorithm.VERBOSE_LOGGING);
        }
        else
        {
            SimpleLogger.logInfo("Texts are identical, no differences found.", DiffAlgorithm.VERBOSE_LOGGING);
        }
    }

    /**
     * showchange Part of printout. Expects printnewline is an insertion.
     * Expects printoldline is a deletion.
     */
    private void collectChangeFrom()
    {
        SimpleLogger.logInfo("Collecting CHANGE from base text.", DiffAlgorithm.VERBOSE_LOGGING);
        logAction();
        
        if (collectStatus != changefrom)
        {
            collectStatus = changefrom;
            nextDifference(Difference.CHANGE);
            currentDifference.setBaseOffset(baseLengthMarker.lookupOffset(baseIndex));
        }

        // take this symbol into account when calculating length of this run of symbols
        baseLengthMarker.addSymbol(baseIndex);
        
        anyCollected = true;
        baseIndex++;
    }
    
  
    /**
     * showsame Part of printout. Expects printnewline and printoldline at start
     * of two blocks that aren't to be displayed.
     */
    private void skipSame()
    {
        FileInfo newinfo = correlator.getNewInfo();
        
        logAction();

        int count;
        collectStatus = idle;
        if (newinfo.getCrossIndex(witnessIndex) != baseIndex)
        {
            SimpleLogger.logError("BUG IN LINE REFERENCING");            
        }
        count = correlator.getBlockSize(baseIndex);
        
        baseIndex += count;
        witnessIndex += count;
        
        SimpleLogger.logInfo("Skipping "+count+" symbols in identitical text area.", DiffAlgorithm.VERBOSE_LOGGING);
    }
    
	private void collectMove()
    {
		SimpleLogger.logInfo("Collecting MOVE.", DiffAlgorithm.VERBOSE_LOGGING);

        logAction();

        FileInfo newinfo = correlator.getNewInfo();
        //FileInfo oldinfo = correlator.getOldInfo();
        
        int block = newinfo.getCrossIndex(witnessIndex);
        int anotherBlock = baseIndex;

        int blockSize = correlator.getBlockSize(block);
        int anotherBlockSize = correlator.getBlockSize(anotherBlock);
        
        // if the text is present in the witness, then it is an INSERT or a CHANGE
        if( anotherBlockSize >= blockSize )
        {
            SimpleLogger.logInfo("Collecting moving block as INSERT.", DiffAlgorithm.VERBOSE_LOGGING);
            collectInsert();    
        }
        // otherwise, if there is no text in the witness, then it is a DELETE
        else 
        {            
            SimpleLogger.logInfo("Collecting moving block as DELETE.", DiffAlgorithm.VERBOSE_LOGGING);
            collectDelete();
        }
    }
    
    private void logAction()
    {
//        FileInfo newinfo = correlator.getNewInfo();
//        FileInfo oldinfo = correlator.getOldInfo();

        int baseOffset = baseLengthMarker.lookupOffset(baseIndex);
        int witnessOffset = witnessLengthMarker.lookupOffset(witnessIndex);
//        int xBaseOffset = witnessLengthMarker.lookupOffset(oldinfo.getCrossIndex(baseIndex));
//        int xWitnessOffset = baseLengthMarker.lookupOffset(newinfo.getCrossIndex(witnessIndex));

//        SimpleLogger.logInfo("base offset = "+baseOffset, DiffAlgorithm.VERBOSE_LOGGING);
//        SimpleLogger.logInfo("witness offset = "+witnessOffset, DiffAlgorithm.VERBOSE_LOGGING);
        SimpleLogger.logInfo("base text = "+getTextSnippet(baseOffset,20,Difference.BASE), DiffAlgorithm.VERBOSE_LOGGING);
        SimpleLogger.logInfo("witness text = "+getTextSnippet(witnessOffset,20,Difference.WITNESS), DiffAlgorithm.VERBOSE_LOGGING);
//        SimpleLogger.logInfo("cross base offset = "+xBaseOffset, DiffAlgorithm.VERBOSE_LOGGING);
//        SimpleLogger.logInfo("cross witness offset = "+xWitnessOffset, DiffAlgorithm.VERBOSE_LOGGING);
    }

	// pulls a snippet out of the document. if the length requested 
	// overruns the end of the document, it is clipped to produce the longest
	// possible string
    private String getTextSnippet(int offset, int length, int textType) 
	{
		DocumentModel document;
		
		if( textType == Difference.BASE )
			document = correlator.getBaseDocument();		
		else
			document = correlator.getWitnessDocument();
		
		// clip overflow
		int overflow = offset+length - document.getDocumentLength();		
		if( overflow > 0 ) length -= overflow;
		
		return document.getSubString(offset,length);
	}

//	private void collectMoveOld()
//    {
//        SimpleLogger.logInfo("Examining MOVE block.", DiffAlgorithm.VERBOSE_LOGGING);
//
//        FileInfo newinfo = correlator.getNewInfo();
//
//        int block = newinfo.getCrossIndex(witnessIndex);
//        int anotherBlock = baseIndex;
//
//        int blockSize = correlator.getBlockSize(block);
//        int anotherBlockSize = correlator.getBlockSize(anotherBlock);
//        
//        if ( blockSize < 0 )
//        {
//            SimpleLogger.logInfo("MOVE block already processed.", DiffAlgorithm.VERBOSE_LOGGING);
//            
//            // already processed
//            skipWitness();
//        }             
//        // if block is the smaller of the two blocks, we'll use it
//        else if (anotherBlockSize >= blockSize)
//        {
//            SimpleLogger.logInfo("Collecting MOVE.", DiffAlgorithm.VERBOSE_LOGGING);
//            
//            // assume new's block moved.
//            nextDifference(Difference.MOVE);
//            
//            // mark block as collected
//            blockCollectedFlags[block] = true; 
//
//            // record offsets for base and witness
//            currentDifference.setBaseOffset(baseLengthMarker.lookupOffset(block));
//            currentDifference.setWitnessOffset(witnessLengthMarker.lookupOffset(witnessIndex));
//
//            // look up the offsets for each symbol in the block and update the length markers
//            for( int i = 0; i < blockSize; i++ )
//            {
//                baseLengthMarker.addSymbol(block+i);
//                witnessLengthMarker.addSymbol(witnessIndex+i);
//            }
//
//            // skip past the block
//            witnessIndex += blockSize;
//
//            anyCollected = true;
//            collectStatus = idle;
//        } 
//        else
//        {
//            // assume old's block moved 
//            SimpleLogger.logInfo("Skipping MOVE block.", DiffAlgorithm.VERBOSE_LOGGING);
//            skipBase(); 
//        }
//    }
    
    /**
     * showdelete Part of printout. Expects printoldline is at a deletion.
     */
    private void collectDelete()
    {
        SimpleLogger.logInfo("Collecting DELETE.", DiffAlgorithm.VERBOSE_LOGGING);
        
        logAction();

        if (collectStatus != delete)
        {
            collectStatus = delete;
            nextDifference(Difference.DELETE);
            currentDifference.setBaseOffset(baseLengthMarker.lookupOffset(baseIndex));
            currentDifference.setWitnessOffset(witnessLengthMarker.lookupOffset(witnessIndex));
        }
        
        baseLengthMarker.addSymbol(baseIndex);
        
        anyCollected = true;
        baseIndex++;        
    }


    /*
     * showinsert Part of printout. Expects printnewline is at an insertion.
     */
    private void collectInsert()
    {
        SimpleLogger.logInfo("Collecting INSERT.", DiffAlgorithm.VERBOSE_LOGGING);
        
        logAction();

        if (collectStatus != insert)
        {
            collectStatus = insert;
            nextDifference(Difference.INSERT);
            currentDifference.setBaseOffset(baseLengthMarker.lookupOffset(baseIndex));
            currentDifference.setWitnessOffset(witnessLengthMarker.lookupOffset(witnessIndex));
        }
        
        witnessLengthMarker.addSymbol(witnessIndex);
        
        anyCollected = true;
        witnessIndex++;
    }
    
    /*
     * showinsert Part of printout. Expects printnewline is at an insertion.
     */
    private void collectChangeTo()
    {
        SimpleLogger.logInfo("Collecting CHANGE to.", DiffAlgorithm.VERBOSE_LOGGING);
        logAction();
        
        if (collectStatus == changefrom )
        {
            currentDifference.setWitnessOffset(witnessLengthMarker.lookupOffset(witnessIndex));
            collectStatus = changeto;
        }
        
        witnessLengthMarker.addSymbol(witnessIndex);
        
        anyCollected = true;
        witnessIndex++;
    }
    
    private void nextDifference( int type )
    {
        if( currentDifference != null )
        {
            SimpleLogger.logInfo("Storing collected difference.", DiffAlgorithm.VERBOSE_LOGGING);
            addDifference( currentDifference );
        }
        
        currentDifference = new Difference(differenceSet.getBaseDocument(),
                                           differenceSet.getWitnessDocument(),
                                           type);
    }

    private void addDifference( Difference difference )
    {
        // obtain the length of each run of symbols for each doc
        int baseLength = baseLengthMarker.getLength();
        int witnessLength = witnessLengthMarker.getLength();
        
        // store these values in difference object
        difference.setBaseTextLength(baseLength);
        difference.setWitnessTextLength(witnessLength);
        
        // record the change distance score
        if( difference.getType() == Difference.CHANGE )
        {
            TRStringDistance tr = new TRStringDistance(correlator.getBaseDocument(),
                                                       correlator.getWitnessDocument(),
                                                       difference );
            difference.setDistance(tr.getDistance());
        }
        
        // add the object to the difference set
        differenceSet.addDifference(currentDifference);
        
        // reset the markers
        baseLengthMarker.reset();
        witnessLengthMarker.reset();
    }
    
    /**
     * @return Returns the differenceSet.
     */
    public DifferenceSet getDifferenceSet()
    {
        return differenceSet;
    }
    
    
    // measures the length of a text run in the file from the beginning of the first symbol 
    // added to the end of the last symbol added. Can be reused by calling reset().
    private class LengthMarker
    {
        private FileInfo info; 
        private int offsetStart;
        private int offsetEnd;
                        
        public LengthMarker( FileInfo info )
        {            
            this.info = info;
            reset();
        }
        
        public void addSymbol( int symbolIndex )
        {        
            Symbol symbol = info.getSymbol(symbolIndex);
            
            if( symbol != null )
            {
                int offset = info.getDocumentOffset(symbolIndex);
                
                if( offsetStart == -1 )
                {
                    offsetStart = offset;
                    offsetEnd = offset + symbol.getSymbolLength();
                }
                else
                {            
                    offsetEnd = offset + symbol.getSymbolLength();
                }
            }
            else
            {
                SimpleLogger.logError("Symbol index invalid on add: "+symbolIndex);
            }
        }

        // for a given symbol index, get the offset into the document
        public int lookupOffset( int symbolIndex )
        {        
            if( symbolIndex > info.getSymbolCount() )
            {
                symbolIndex = info.getSymbolCount();
            }
            
            Symbol symbol = info.getSymbol(symbolIndex);
            if( symbol != null )
            {
                return info.getDocumentOffset(symbolIndex);
            }
            else
            {
                //SimpleLogger.logError("Symbol index invalid on lookup: "+symbolIndex);
                return -1;
            }
        }
        
        public int getLength()
        {
            int length = 0;
            
            if( this.offsetStart != -1 ) 
            {
                length = offsetEnd - offsetStart;
            }
            
            return length;
        }
        
        public void reset()
        {
            offsetStart = -1;
            offsetEnd = -1;
        }

    }
    
}
