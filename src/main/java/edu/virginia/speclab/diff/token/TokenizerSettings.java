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

public class TokenizerSettings
{
    private boolean filterCase;
    private boolean filterPunctuation;
    private boolean filterWhitespace;
    
    public TokenizerSettings( boolean filterCase, boolean filterPunctuation, boolean filterWhitespace )
    {
        this.filterCase = filterCase;
        this.filterPunctuation = filterPunctuation;
        this.filterWhitespace = filterWhitespace;         
    }
    
    public static TokenizerSettings getDefaultSettings()
    {
        return new TokenizerSettings(true,true,true);
    }

    public boolean filterCase()
    {
        return filterCase;
    }
    
    public boolean filterPunctuation()
    {
        return filterPunctuation;
    }
    
    public boolean filterWhitespace()
    {
        return filterWhitespace;
    }
    
    public boolean equals( Object that )
    {
        if( that instanceof TokenizerSettings == false ) return false;
        TokenizerSettings other = (TokenizerSettings) that;
        
        if( other.filterCase == filterCase &&
            other.filterPunctuation == filterPunctuation && 
            other.filterWhitespace == filterWhitespace ) return true;
        else            
            return false;
    }
}
