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

import edu.virginia.speclab.diff.document.Image;
import edu.virginia.speclab.diff.document.LocationMarker;
import edu.virginia.speclab.diff.document.TagSet;
import edu.virginia.speclab.diff.token.JuxtaXMLNode;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ben
 */
public class JuxtaXMLParserMilestoneTagHandler implements JuxtaXMLParserTagHandler {
    private MarkerNumberCounter _counter;
    private Map<String,LocationMarker> _openLocationMarkerSet;
    private List<LocationMarker> _locationMarkerList;
    private TagSet _interestingTagSet;

    public JuxtaXMLParserMilestoneTagHandler()
    {
        _counter = new MarkerNumberCounter();
        _openLocationMarkerSet = new HashMap<String,LocationMarker>();
        _locationMarkerList = new LinkedList<LocationMarker>();
        _interestingTagSet = new TagSet();
        _interestingTagSet.includeTag("m_e");
        _interestingTagSet.includeTag("m_s");
        _interestingTagSet.includeTag("lg");
        _interestingTagSet.includeTag("l");
    }

    public List<LocationMarker> getLocationMarkerList()
    {
        return _locationMarkerList;
    }

    public void processTag(JuxtaXMLParser xmlParser, JuxtaXMLNode xmlNode) {
        if( xmlNode.getName().equals("m_s") )
        {
            String name = xmlNode.getAttribute("name");
            String id = xmlNode.getAttribute("id");
            String type = xmlNode.getAttribute("type");
            String imageFileName = xmlNode.getAttribute("img");
            String numString = xmlNode.getAttribute("n");

            Image image = null;

            if( imageFileName != null && xmlParser.getBaseDirectory() != null )
            {   
            	String juxtaFileVersion = xmlParser.getJuxtaVersion();            	
            	String imageDirectory;
            	
            	// in version 1.3.1 image directory was in a different place relative to the source xml file.
            	if( juxtaFileVersion == "1.3.1" ) {
            		imageDirectory = xmlParser.getBaseDirectory() + File.separator + "images";
            	} else {            		
            		imageDirectory = xmlParser.getBaseDirectory() + File.separator + ".." +  File.separator + "images";
            	}
            	
                File imageFile = new File( imageDirectory + File.separator + imageFileName);
                image = new Image(imageFile);
            }

            int number = 0;
            if( numString != null )
            {
                // parse the marker number if there is one
                number = Integer.parseInt(numString);
                _counter.setLastNumber(type,number);
            }
            else
            {
                // if there isn't use counter to count up from the last encounter type
                number = _counter.getNumber(type);
            }

            LocationMarker marker = new LocationMarker(id,name,type,number,image,xmlNode.getXMLEndOffset());
            
            _openLocationMarkerSet.put(id,marker);
        }
        else if( xmlNode.getName().equals("m_e") )
        {
            String id = xmlNode.getAttribute("refid");
            LocationMarker marker = _openLocationMarkerSet.get(id);
            marker.setEndOffset(xmlNode.getXMLEndOffset());
            _locationMarkerList.add(marker);
        }
        else if( xmlNode.getName().equals("lg") || xmlNode.getName().equals("l") )
        {
            // Does it have an @n attribute? if so, we'll use it as a marker.
            String n = xmlNode.getAttribute("n");
            int number = 0;
            try {
                if (n != null)
                    number = Integer.parseInt(n);
            } catch (NumberFormatException e) {
                // someone put something that isn't a number into their @n attribute,
                // no op here
            }

            LocationMarker marker = new LocationMarker("", xmlNode.getName(), xmlNode.getName(), number, null, xmlNode.getXMLStartOffset());
            marker.setEndOffset(xmlNode.getXMLEndOffset());
            _locationMarkerList.add(marker);
        }
    }

    public TagSet getInterestingTagSet() {
        return _interestingTagSet;
    }

    	// Counts continous runs of markers of the same type, starting from the given start number
    // The count can be overridden by supplying the last type and number encountered.
    private class MarkerNumberCounter
    {
        private String lastType;
        private int lastNumber, startNumber;

        public MarkerNumberCounter()
        {
            this.startNumber = 1;
        }

        public int getNumber( String type )
        {
            if( type.equals(lastType) )
            {
                lastType = type;
                return ++lastNumber;
            }
            else
            {
                lastType = type;
                lastNumber = startNumber;
                return lastNumber;
            }
        }

        public void setLastNumber( String type, int number )
        {
            lastType = type;
            lastNumber = number;
        }

    }


}
