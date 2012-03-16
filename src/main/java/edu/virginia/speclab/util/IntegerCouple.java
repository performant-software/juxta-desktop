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
 

package edu.virginia.speclab.util;

/**
 * A useful little class for pairing two integers together as one object, suitable
 * for use as a key in a hash table.
 * @author Nick
 *
 */
public class IntegerCouple
{
    private int a, b;
    private int hashCode;
    
    public IntegerCouple( int a, int b )
    {
        this.a = a;
        this.b = b;
        
        String hash = this.a+","+this.b;
        this.hashCode = hash.hashCode(); 
    }
    
    public int hashCode()
    {                       
        return this.hashCode; 
    }
    
    public boolean equals( IntegerCouple couple )
    {
        if( couple.a == this.a && 
            couple.b == this.b  ) return true;            
        else 
            return false;
    }
    
   public boolean equals(Object obj)
   {
       if( obj instanceof IntegerCouple )
       {
           IntegerCouple otherRange = (IntegerCouple) obj;
           return equals(otherRange);
       }
        
       return false;
   }

    public int getA()
    {
        return a;
    }
    
    
    public int getB()
    {
        return b;
    }
 
}
