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

import edu.virginia.speclab.diff.document.TagSet;
import edu.virginia.speclab.diff.token.JuxtaXMLNode;

/**
 * 
 * @author ben
 */
public final class JuxtaXMLParserDocumentPointerTagHandler implements JuxtaXMLParserTagHandler {

    private TagSet interestingTagSet;
    private String filename;
    private String parseTemplate;
    private String acceptedRevsions;

    public JuxtaXMLParserDocumentPointerTagHandler() {
        this.interestingTagSet = new TagSet();
        this.interestingTagSet.includeTag("juxta-doc-reference");
        this.parseTemplate = "";
        this.acceptedRevsions = "";
    }

    public void processTag(JuxtaXMLParser xmlParser, JuxtaXMLNode xmlNode) {
        this.filename = xmlNode.getAttribute("filename");

        for (int i = 0; i < xmlNode.getChildCount(); i++) {
            JuxtaXMLNode child = xmlNode.getChildAt(i);
            
            if ( child.getName().equals("parseTemplate")) {
                
                this.parseTemplate = extractTagContent( xmlParser,
                    child.getXMLStartOffset(), 
                    child.getXMLEndOffset() );     
                
            } else if ( child.getName().equals("acceptedRevisions") ) { 
                
                this.acceptedRevsions = extractTagContent( xmlParser, 
                    child.getXMLStartOffset(), 
                    child.getXMLEndOffset() );    
                
            }
        }
    }
    
    private String extractTagContent( JuxtaXMLParser xmlParser, int tagStart, int tagEnd ) {
        // grab the full tag text
        String data = xmlParser.getXMLText().substring(tagStart, tagEnd);
    
        // the actual value is between the first '> 'and  next '</'
        int start = data.indexOf(">");
        int end = data.indexOf("</", start);  
        return data.substring(start+1, end).trim();   
    }
    
    public final String getAcceptedRevsisons() {
        return this.acceptedRevsions;
    }
    
    public final String getParseTemplate() {
        return this.parseTemplate;
    }

    public final TagSet getInterestingTagSet() {
        return this.interestingTagSet;
    }

    public final String getReferencedFilename() {
        return this.filename;
    }
}
