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

package edu.virginia.speclab.legacy.diff.token;

import java.io.IOException;
import java.util.ArrayList;

import edu.virginia.speclab.legacy.diff.document.DocumentModel;
import edu.virginia.speclab.util.SimpleLogger;

public class TokenTable
{
    private DocumentModel document;
    private ArrayList tokenList;
    private TokenizerSettings settings;
    
    public TokenTable( DocumentModel document, TokenizerSettings settings )
    {
        this.tokenList = new ArrayList();
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
    
    public ArrayList getTokenList()
    {
        return tokenList;
    }

    public TokenizerSettings getSettings()
    {
        return settings;
    }
        
}
