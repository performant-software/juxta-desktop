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

import java.io.IOException;
import java.util.LinkedList;

import edu.virginia.speclab.diff.Diff;
import edu.virginia.speclab.diff.DifferenceSet;
import edu.virginia.speclab.diff.MultiPassDiff;
import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.diff.token.Token;
import edu.virginia.speclab.diff.token.TokenReader;
import edu.virginia.speclab.diff.token.TokenTable;
import edu.virginia.speclab.diff.token.TokenizerSettings;
import edu.virginia.speclab.util.SimpleLogger;

public class WhiteSpaceReaderTest
{
    public static void main( String argv[] )
    {
        SimpleLogger.initConsoleLogging();
        SimpleLogger.setLoggingLevel(0);
        
        TokenizerSettings settings = new TokenizerSettings(true,true,true);
        DocumentModel documentA= new DocumentModel("lucene","sample/damozel/1-1881.1stedn.rad.txt","UTF-8");
        DocumentModel documentB = new DocumentModel("lucene2","sample/damozel/1-1847.morgms.rad.txt","UTF-8");
        
        documentA.tokenize(settings);
        documentB.tokenize(settings);
        
	    MultiPassDiff mpd = new MultiPassDiff(documentA,documentB);
        DifferenceSet set = mpd.getDifferenceSet();
        Diff.viewDifferenceList(set);
        
    }

    public static void tokenizeLuceneReader( DocumentModel document )
    {
        LinkedList tokenList = new LinkedList();
        
        TokenReader tokenReader = TokenTable.getTokenReader(new TokenizerSettings(true,true,true));
        tokenReader.openDocument(document);
        
        try
        {
            int nextSymbolOffset = 0;
            String tokenBuffer;        
            while ((tokenBuffer = tokenReader.readSymbol()) != null)
            {
                nextSymbolOffset = tokenReader.getSymbolOffset();
                Token token = new Token(tokenBuffer,nextSymbolOffset);
                tokenList.add(token);
                System.out.println(nextSymbolOffset+"|"+tokenBuffer);
            }
            
            tokenReader.close();
        }
        catch( IOException e )
        {
            SimpleLogger.logError("Error tokenizing file "+document.getDocumentName());
        }        
    }

    public static void tokenizeWordReader( DocumentModel document )
    {
        LinkedList tokenList = new LinkedList();
        
        TokenReader tokenReader = TokenTable.getTokenReader(new TokenizerSettings(true,true,true));
        tokenReader.openDocument(document);
        
        try
        {
            int nextSymbolOffset = 0;
            String tokenBuffer;        
            while ((tokenBuffer = tokenReader.readSymbol()) != null)
            {
                Token token = new Token(tokenBuffer,nextSymbolOffset);
                tokenList.add(token);
                System.out.println(nextSymbolOffset+": "+tokenBuffer);
                nextSymbolOffset = tokenReader.getSymbolOffset();                
            }
            
            tokenReader.close();
        }
        catch( IOException e )
        {
            SimpleLogger.logError("Error tokenizing file "+document.getDocumentName());
        }        
    }
}
