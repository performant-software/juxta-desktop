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
 
package edu.virginia.speclab.diff.document;

// TODO provide implementation of DocumentWriter for stand alone diff package
public class DocumentWriter 
{
	 public static String escapeText( String s )
	    {
	        if( s.indexOf('&') != -1 || s.indexOf('<') != -1 || s.indexOf('>') != -1 )
	        {
	            StringBuffer result = new StringBuffer(s.length()+4);
	            
	            for( int i=0; i < s.length(); i++ )
	            {
	                char c = s.charAt(i);
	                if( c == '&' ) result.append("&amp;");
	                else if( c == '<' ) result.append("&lt;");
	                else if( c == '>' ) result.append("&gt;");
	                else result.append(c);
	            }
	            
	            return result.toString();
	        }
	        else
	        {
	            return s;
	        }        
	    }

	 public static void escapeText( char c, StringBuffer buffer )
	 {
        if( c == '&' ) 
        {
			buffer.append("&amp;");
        }
        else if( c == '<' )
		{
			buffer.append("&lt;");
		}	
        else if( c == '>' )
		{
			buffer.append("&gt;");                
		}
        else
		{
			buffer.append(c);	
		}		
	 }
	 

}
