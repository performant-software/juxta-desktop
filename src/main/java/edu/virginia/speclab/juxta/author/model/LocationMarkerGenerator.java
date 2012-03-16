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
 

package edu.virginia.speclab.juxta.author.model;

import java.util.LinkedList;
import java.util.List;

import edu.virginia.speclab.diff.document.LocationMarker;

public class LocationMarkerGenerator
{
    public static List<LocationMarker> generateNewLineMarkers( String documentText )
    {
        List<LocationMarker> locationMarkers = new LinkedList<LocationMarker>();

        int lineNumber = 1;
        boolean newLine = true;
        for( int x = 0; x < documentText.length(); x++ )
        {
            char c = documentText.charAt(x);
            
            if( c == '\n' )
            {
                newLine = true;
            }
            else if( newLine )
            {                
                LocationMarker marker = new LocationMarker(Integer.toHexString(x),"","",lineNumber++,null,x);
                int end = documentText.indexOf('\n', x);
                if (end > 0)
                	marker.setEndOffset(end);
                locationMarkers.add(marker);
                newLine = false;
            }
        }
        
        return locationMarkers;        
    }
}
