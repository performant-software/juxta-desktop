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

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.document.TagSet;
import edu.virginia.speclab.diff.token.JuxtaXMLNode;
import edu.virginia.speclab.juxta.author.model.Revision.Type;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author ben
 */
public class JuxtaXMLParserAddDelTagHandler implements JuxtaXMLParserTagHandler {
    private TagSet interestingTags;
    private List<Revision> revisions;

    public JuxtaXMLParserAddDelTagHandler() {
        this.interestingTags = new TagSet();
        this.interestingTags.includeTag("add");
        this.interestingTags.includeTag("del");
        this.interestingTags.includeTag("addSpan");
        this.interestingTags.includeTag("delSpan");
        this.revisions = new LinkedList<Revision>();
    }

    /**
     * NOTES: At this point in processing, we have no document so all
     * offset ranges must be in the ORIGINAL space. xmlNode contains
     * an OffsetRange to the raw xml for the add/delete tag. Problem is
     * that this offset is to the beginning/end of the tag itself - not 
     * the content. If this range is used for the add/del range, the final
     * presentation will be off by 1 character. To make it work, the 
     * offset range stored here must be to the actual text content of the
     * add/del tag. Get this by pulling the raw xml for the tag and determin
     * the offset range for all text inside of the xml markup.
     */
    public void processTag(JuxtaXMLParser xmlParser, JuxtaXMLNode xmlNode) {        
        String name = xmlNode.getName();
        if (name.equals("add") || name.equals("addSpan")) {
            OffsetRange addRange = getContentRange(xmlParser, xmlNode);
            this.revisions.add(new Revision(Type.ADD, addRange));
        } else if (name.equals("del") || name.equals("delSpan")) {
            OffsetRange delRange = getContentRange(xmlParser, xmlNode);
            this.revisions.add(new Revision(Type.DELETE, delRange));
        }
    }

    private OffsetRange getContentRange(JuxtaXMLParser xmlParser, JuxtaXMLNode xmlNode) {
        OffsetRange range = new OffsetRange();
        int start  = xmlNode.getXMLStartOffset();
        int end = xmlNode.getXMLEndOffset();
        String tagTxt = xmlParser.getXMLText().substring(start, end);
        int relTxtStart = tagTxt.indexOf(">")+1;
        int relTxtEnd = tagTxt.lastIndexOf("</");
        int realStart = start+relTxtStart;
        int realEnd = realStart+(relTxtEnd-relTxtStart);
        range.set(realStart, realEnd, OffsetRange.Space.ORIGINAL);
        return range;
    }

    public TagSet getInterestingTagSet() {
        return interestingTags;
    }

    public List<Revision> getRevisions()
    {
        return this.revisions;
    }

}
