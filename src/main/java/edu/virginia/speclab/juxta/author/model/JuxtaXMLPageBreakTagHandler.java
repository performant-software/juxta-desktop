/*
 *  Copyright 2002-2012 The Rector and Visitors of the
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

import java.util.ArrayList;
import java.util.List;

import edu.virginia.speclab.diff.document.PageBreakData;
import edu.virginia.speclab.diff.document.TagSet;
import edu.virginia.speclab.diff.token.JuxtaXMLNode;

/**
 * collect all of the pb tag data encountered during parse
 * 
 * @author loufoster
 *
 */
public class JuxtaXMLPageBreakTagHandler implements JuxtaXMLParserTagHandler {

    private List<PageBreakData> pageBreaks;
    private TagSet interestingTags;
    
    public JuxtaXMLPageBreakTagHandler() {
        this.pageBreaks = new ArrayList<PageBreakData>();
        this.interestingTags = new TagSet();
        this.interestingTags.includeTag("pb");
    }
    
    public void processTag(JuxtaXMLParser xmlParser, JuxtaXMLNode xmlNode) {
        String name = xmlNode.getName();
        if (name.equals("pb")) {
            // get the n attribute which is the page break label
            String label = xmlNode.getAttribute("n");
            if ( label != null ) {
                int start  = xmlNode.getXMLStartOffset()+1; 
                int end = xmlNode.getXMLEndOffset();
                //System.err.println("PARSED "+label+" ("+start+", "+end+")");
                this.pageBreaks.add( new PageBreakData(start,end,label) );
            }
        }
    }

    public TagSet getInterestingTagSet() {
        return this.interestingTags;
    }
    
    public List<PageBreakData> getPageBreaks() {
        return this.pageBreaks;
    }
}
