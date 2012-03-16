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

import java.util.TreeSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class Token
{
    private char[] token;
    private int offset;
    private Set<String> notableTags;
    
    public Token( String token, int offset )
    {
        this.token = token.toCharArray(); 
        this.offset = offset;
        this.notableTags = new TreeSet<String>();
    }

    public int getOffset()
    {
        return offset;
    }
    
    public String getToken()
    {
        return new String(token);
    }

    public void addNotableTags(Set<String> notableTags)
    {
        this.notableTags.addAll(notableTags);
    }
    
    @Override
    // Use information in the xmlNode to build a hash
    // In this way, the same strings held in different
    // tags will be marked as different by the algorithm.
    //
    // This isn't necessarily the way we'll want to do it
    // officially, but it demonstrates using the XML to
    // differentiate two otherwise identical tokens.
    public int hashCode()
    {
        String stringToHash = "";

        Iterator<String> it = notableTags.iterator();
        while(it.hasNext())
        {
            String next = it.next();
            stringToHash += next + "///";
        }
        stringToHash += getToken();
        return stringToHash.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Token other = (Token) obj;
        if (!Arrays.equals(this.token, other.token)) {
            return false;
        }
        if (this.offset != other.offset) {
            return false;
        }
        return true;
    }
}
