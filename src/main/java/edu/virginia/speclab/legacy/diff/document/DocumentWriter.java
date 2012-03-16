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

package edu.virginia.speclab.legacy.diff.document;

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
