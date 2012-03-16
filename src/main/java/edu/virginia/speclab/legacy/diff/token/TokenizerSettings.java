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
