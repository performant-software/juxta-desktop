/*
 *  Copyright 2002-2011 The Rector and Visitors of the
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

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.OffsetRange.Space;
import edu.virginia.speclab.diff.document.NoteData;
import edu.virginia.speclab.diff.document.TagSet;
import edu.virginia.speclab.diff.token.JuxtaXMLNode;

public class JuxtaXMLParserNoteTagHandler implements JuxtaXMLParserTagHandler {
    private List<NoteData> notes;
    private TagSet interestingTags;

    public JuxtaXMLParserNoteTagHandler() {
        this.notes = new ArrayList<NoteData>();
        this.interestingTags = new TagSet();
        this.interestingTags.includeTag("note");
    }

    public void processTag(JuxtaXMLParser xmlParser, JuxtaXMLNode xmlNode) {
        String name = xmlNode.getName();
        if (name.equals("note")) {
            OffsetRange noteRange = getContentRange(xmlParser, xmlNode);
            String tgt = xmlNode.getAttribute("target");
            String type = xmlNode.getAttribute("type");
            NoteData note = new NoteData(type);
            note.setNoteContentRange(noteRange);
            
            // if the note is targted to a specific element, tgt will be
            // non-null and contain the xml:id if that element
            if ( tgt != null ) {
                note.setTargetID(tgt);
                note.setAnchorRange( xmlParser.getTagOffsetRange(tgt));
            } else {
                // this note is note tied to an element. it just sits in the document
                // margin at the offset where the note tag resides
                OffsetRange anchor = new OffsetRange();
                int pos = noteRange.getStartOffset(Space.ORIGINAL);
                anchor.set(pos, pos, Space.ORIGINAL);
                note.setAnchorRange(anchor);
            }
            this.notes.add(note);
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

    public List<NoteData> getNotes() {
        return this.notes;
    }


}
