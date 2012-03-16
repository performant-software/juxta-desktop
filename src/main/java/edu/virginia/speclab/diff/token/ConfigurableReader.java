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
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

import edu.virginia.speclab.diff.document.DocumentModel;

/**
 * This <code>TokenReader</code> implementation parses tokens based on the settings 
 * passed in the <code>TokenizerSettings</code> object.
 * @author Nick
 *
 */
public class ConfigurableReader implements TokenReader
{
    private TokenizerSettings tokenizerSettings;

    private TokenStream tokenStream;

    private Token token;

    public ConfigurableReader(TokenizerSettings settings)
    {
        this.tokenizerSettings = settings;
    }

    public void openDocument(DocumentModel model)
    {
        Analyzer analyzer = new ConfigurableAnalyzer(tokenizerSettings);
        tokenStream = analyzer.tokenStream("contents", new StringReader(model
                .getDocumentText()));
    }

    public void openString( String text )
    {
        Analyzer analyzer = new ConfigurableAnalyzer(tokenizerSettings);
        tokenStream = analyzer.tokenStream("contents", new StringReader(text));
    }

    public int getSymbolOffset()
    {
        if (token == null)
            return -1;
        else
            return token.startOffset();
    }

    public String readSymbol() throws IOException
    {
        token = tokenStream.next();
        if (token == null)
            return null;
        else
        {
            return token.termText();
        }
    }

    public void close() throws IOException
    {
        tokenStream.close();
    }

    private class ConfigurableTokenizer extends Tokenizer
    {        
        private TokenizerSettings settings;
        private int offset = 0, bufferIndex = 0, dataLen = 0;
        private final char[] buffer = new char[MAX_WORD_LEN];
        private final char[] ioBuffer = new char[IO_BUFFER_SIZE];

        private static final int MAX_WORD_LEN = 255;
        private static final int IO_BUFFER_SIZE = 1024;
        
        public ConfigurableTokenizer(Reader reader, TokenizerSettings settings)
        {
            super(reader);
            this.settings = settings;
        }

        /** Returns the next token in the stream, or null at EOS. */
        public final Token next() throws IOException
        {
            int length = 0;
            int start = offset;

            while (true)
            {
                final char c;

                offset++;
                if (bufferIndex >= dataLen)
                {
                    dataLen = input.read(ioBuffer);
                    bufferIndex = 0;
                }

                if (dataLen == -1)
                {
                    if (length > 0)
                        break;
                    else
                        return null;
                } else
                    c = ioBuffer[bufferIndex++];
                
                if( !settings.filterWhitespace() && Character.isWhitespace(c) && length == 0 ) 
                {
                    // start of token
                    start = offset - 1;

                    buffer[length++] = normalize(c); // buffer it,
                    // normalized

                    if (length == MAX_WORD_LEN) // buffer overflow!
                    break;                    
                } 
                else if (isTokenChar(c))
                { // if it's a token char

                    if (length == 0) // start of token
                        start = offset - 1;

                    buffer[length++] = normalize(c); // buffer it,
                                                        // normalized

                    if (length == MAX_WORD_LEN) // buffer overflow!
                        break;

                } 
                else if (length > 0) // at non-Letter w/ chars
                    break; // return 'em
            }

            return new Token(new String(buffer, 0, length), start, start
                    + length);
        }

//        private Token digestWhiteSpace()
//        {
//            int length = 0, start = offset-1;
//            
//            int position = start;
//            char c = ioBuffer[position++];
//            while( Character.isWhitespace(c) )
//            {                
//                buffer[length++]  = c;
//                c = ioBuffer[position++];
//            } 
//
//            return new Token(new String(buffer, 0, length), start, start
//                    + length);
//        }

        private char normalize(char c)
        {
            if (tokenizerSettings.filterCase() && Character.isLetter(c))
            {
                return Character.toLowerCase(c);
            } else
                return c;
        }

        private boolean isTokenChar(char c)
        {
            if (Character.isWhitespace(c))
                return false;

            if (settings.filterPunctuation())
            {
                if (Character.isLetter(c) || Character.isDigit(c))
                    return true;
                else
                    return false;
            }

            return true;
        }
    }

    private class ConfigurableAnalyzer extends Analyzer
    {
        private TokenizerSettings settings;

        public ConfigurableAnalyzer(TokenizerSettings settings)
        {
            this.settings = settings;
        }

        public TokenStream tokenStream(String fieldName, Reader reader)
        {
            return new ConfigurableTokenizer(reader, settings);
        }
    }
}
