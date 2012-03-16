/*
 * Created on Mar 10, 2005
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
 
package edu.virginia.speclab.diff.token;

import java.io.IOException;
import java.text.BreakIterator;
import java.util.Locale;

import edu.virginia.speclab.diff.DiffAlgorithm;
import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * @author Nick
 * 
 * This symbol reader parses out words.
 */
public class WordReader implements TokenReader
{
    private BreakIterator boundary; 
    private String buffer;
    private int currentBoundary, nextBoundary;
    private boolean done;
    
    public WordReader()
    {
        boundary = BreakIterator.getWordInstance(Locale.US);        
        done = false;
    }
    
    public void openDocument( DocumentModel model )
    {
        buffer = model.getDocumentText();

        boundary.setText(buffer.toString());
        currentBoundary = boundary.first();
    }

    /* (non-Javadoc)
     * @see edu.virginia.speclab.juxta.diff.algorithm.SymbolReader#getSymbolOffset()
     */
    public int getSymbolOffset()
    {
        return currentBoundary;
    }

    /* (non-Javadoc)
     * @see edu.virginia.speclab.juxta.diff.algorithm.SymbolReader#readSymbol()
     */
    public String readSymbol() throws IOException
    {
        String symbol = null;

        if( !done )
        {
            nextBoundary = boundary.next();
            
            if( nextBoundary != BreakIterator.DONE )
            {
                symbol = buffer.substring(currentBoundary,nextBoundary);
            }
            else
            {
                done = true;
                symbol = buffer.substring(currentBoundary,buffer.length());
            }
            
            currentBoundary = nextBoundary;
        }
        
        SimpleLogger.logInfo("symbol: "+symbol, DiffAlgorithm.VERBOSE_LOGGING);
        return symbol;
    }

    /* (non-Javadoc)
     * @see edu.virginia.speclab.juxta.diff.algorithm.SymbolReader#close()
     */
    public void close() throws IOException 
    {
        done = true;
    }

}
