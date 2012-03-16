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

import edu.virginia.speclab.diff.DiffAlgorithm;
import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * @author Nick
 * 
 *  Reads symbols out of the specified file or string. 
 */
public class LineReader implements TokenReader
{
    private boolean done;
    private char source[];
    private int readPosition, sourceLength;        
    
    public void openDocument( DocumentModel model )
    {        
        String string = model.getDocumentText();
        this.source = string.toCharArray();
        sourceLength = string.length();
        readPosition = 0;
        done = false;
    }

    public String readSymbol() throws IOException
    {        
        if( done ) return null;
        
        StringBuffer buffer = new StringBuffer();
        
        while( readPosition < sourceLength )
        {                
            char c = source[readPosition++];
            
            if( lineEndingCharacter(c) )
            {
                digestLineEnding();
                break;
            }
            else
            {
                buffer.append(c);
            }
        } 
        
        // we've consumed the entire buffer
        if( readPosition >= sourceLength )
        {
            done = true;
        }
        
        String result = buffer.toString();
        
        SimpleLogger.logInfo( "line reader parsed line:"+result, DiffAlgorithm.VERBOSE_LOGGING);
        
        return result;               
    }
    
    private void digestLineEnding()
    {
        if( readPosition >= sourceLength ) return;
            
        char firstChar = source[readPosition];
        
        if( firstChar == '\n' )
        {
            readPosition++;            
        }
        else if( firstChar == '\r' )
        {
            readPosition++;
            
            if( readPosition+1 < sourceLength )
            {
                char secondChar = source[readPosition+1];

                if( secondChar == '\n' )
                {
                    readPosition++;                    
                }
            }
        }
    }
    
    private boolean lineEndingCharacter( char c )
    {
        if( c == '\n' || c == '\r' ) return true;
        else return false;
    }
    
    public void close() throws IOException
    {
        done = true;
    }

    public int getSymbolOffset()
    {
        return this.readPosition;   
    }
    

}
