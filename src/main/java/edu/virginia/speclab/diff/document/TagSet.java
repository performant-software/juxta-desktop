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

import edu.virginia.speclab.diff.token.JuxtaXMLNode;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author ben
 */
public class TagSet {

    private Set<String> tags;

    public TagSet() {
        tags = new HashSet<String>();
    }

    public boolean isEmpty() {
        return tags.isEmpty();
    }

    public void includeTag(String tag) {
        tags.add(tag);
    }

    public void excludeTag(String tag) {
        tags.remove(tag);
    }

    public boolean contains(String tag) {
        return tags.contains(tag);
    }

    public String toXML(String name) {
        StringBuffer result = new StringBuffer();
        Iterator<String> iterator = tags.iterator();
        result.append("<tagset name=\"" + name + "\">\n");
        while (iterator.hasNext()) {
            String tag = iterator.next();
            result.append("\t<tag name=\"" + tag + "\" />\n");
        }

        result.append("</tagset>\n");
        return result.toString();
    }

    public Set<String> getCollection() {
        return tags;
    }

    public static TagSet fromJuxtaXMLNode(JuxtaXMLNode xml) {
        TagSet result = new TagSet();
        for (int i = 0; i < xml.getChildCount(); i++) {
            String name = xml.getChildAt(i).getAttribute("name");
            // Directly add the tags rather than figure out which
            // of "includeTag" or "excludeTag" should be called
            result.tags.add(name);
        }
        return result;
    }
}
