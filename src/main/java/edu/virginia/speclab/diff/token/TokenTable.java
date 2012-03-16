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
import java.util.ArrayList;
import java.util.List;

import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.util.SimpleLogger;

public class TokenTable
{
    private DocumentModel document;
    private List<Token> tokenList;
    private TokenizerSettings settings;
    
    public TokenTable( DocumentModel document, TokenizerSettings settings )
    {
        this.tokenList = new ArrayList<Token>();
        this.document = document;
        this.settings = settings;
        tokenize();
    }
    
    public static TokenReader getTokenReader( TokenizerSettings settings )
    {
        return new ConfigurableReader(settings);
    }
    
    public void tokenize()
    {       
        TokenReader tokenReader = getTokenReader(settings);       
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
            }

            // empty token references the last position in the document 
            Token endOfDocument = new Token( "", document.getDocumentLength() );
            tokenList.add(endOfDocument);
            
            tokenReader.close();
        }
        catch( IOException e )
        {
            SimpleLogger.logError("Error tokenizing file "+document.getDocumentName());
        }
        
    }
    
    public List<Token> getTokenList()
    {
        return tokenList;
    }

    public TokenizerSettings getSettings()
    {
        return settings;
    }
        
}
