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
 
package edu.virginia.speclab.diff.token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author ben
 */
public class JuxtaXMLNode
{
    private String name;
    private int startOffset, endOffset;
    private int xmlStartOffset, xmlEndOffset;
    private int index;
    private List<JuxtaXMLNode> children;
    private JuxtaXMLNode parent;
    private Map<String,String> attributes;
    private boolean isExcludedElement;
    private boolean isNotableElement;
    private boolean isEmptyTag;

    public JuxtaXMLNode(String name)
    {
        this(name, 0, 0, null);
    }

    public JuxtaXMLNode(String name, int offset)
    {
        this(name, offset, offset, null);
    }

    public JuxtaXMLNode(String name, int startOffset, int endOffset, JuxtaXMLNode parent)
    {
        this.name = name;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.parent = parent;
        this.children = new ArrayList<JuxtaXMLNode>();
        this.attributes = new HashMap<String, String>();
        this.index = 1;
        this.isExcludedElement = false;
        this.isNotableElement = false;
        this.isEmptyTag = true;
        this.xmlStartOffset = this.xmlEndOffset = 0;
    }

    public JuxtaXMLNode getParent()
    {
        return parent;
    }

    private void setParent(JuxtaXMLNode parent)
    {
        this.parent = parent;
    }

    public int getChildCount()
    {
        return children.size();
    }

    public void addChild(JuxtaXMLNode child)
    {
        if (child == null) return;
        child.setParent(this);
        if (this.isExcludedElement)
            child.setExcluded(true);

        // need to iterate (backwards) through the children
        // to find the last one with the same element name as the
        // new child.  We need that element's index so we can give
        // our new element an appropriate index. (The indexes are used
        // for xpath generation.)
        ListIterator<JuxtaXMLNode> lit = children.listIterator(children.size());
        while(lit.hasPrevious())
        {
            JuxtaXMLNode previous = lit.previous();
            if (previous.name.equals(child.name))
            {
                child.index = previous.index + 1;
                break;
            }
        }
        children.add(child);
    }

    public void setIsEmptyTag(boolean value)
    {
        this.isEmptyTag = value;
    }

    public boolean isEmptyTag()
    {
        return this.isEmptyTag;
    }

    public JuxtaXMLNode getChildAt(int index)
    {
        return children.get(index);
    }

    public void setAttribute(String name, String value)
    {
        attributes.put(name, value);
    }

    public String getAttribute(String name)
    {
        return attributes.get(name);
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    public void setEndOffset(int endOffset)
    {
        this.endOffset = endOffset;
        if (endOffset < startOffset)
            startOffset = endOffset;
    }

    public void setXMLStartOffset(int xmlStartOffset)
    {
        this.xmlStartOffset = xmlStartOffset;
    }

    public int getXMLStartOffset()
    {
        return xmlStartOffset;
    }


    public void setXMLEndOffset(int xmlEndOffset)
    {
        this.xmlEndOffset = xmlEndOffset;
    }

    public int getXMLEndOffset()
    {
        return xmlEndOffset;
    }

    public void debugPrint()
    {
        System.out.println("***** STARTING NODE " + name + " *****");
        System.out.println("Offsets are " + startOffset + "," + endOffset);
        System.out.println("XML offsets are " + xmlStartOffset + "," + xmlEndOffset);
        Iterator<JuxtaXMLNode> it = children.iterator();
        while(it.hasNext())
        {
            it.next().debugPrint();
        }
        System.out.println("***** ENDING NODE " + name + " *****");
    }


    public String getXPath()
    {
        if (parent != null)
        {
            return parent.getXPath() + "/" + name + "[" + index + "]";
        }
        else
        {
            return "/" + name;
        }
    }

    public JuxtaXMLNode getNodeForOffset(int offset)
    {
        if (offset >= startOffset && offset < endOffset) {
            // Figure out which child owns this
            for ( JuxtaXMLNode current : children)
            {
                JuxtaXMLNode result = current.getNodeForOffset(offset);
                if(result != null)
                    return result;
            }
            // if we get here, none of our children had it, but we do, so it must be us
            return this;
        }
        else
        {
            // Not in our subset range
            return null;
        }
    }

    // This only supports very, very simple xpaths, such
    // as the ones produced by getXPath() above
    public JuxtaXMLNode getNodeForXPath(String xpath)
    {
        // has to start with '/'
        if (xpath == null || xpath.equals("") || xpath.charAt(0) != '/') return null;
        int slashIndex = xpath.substring(1).indexOf('/');
        String subsection;
        if (slashIndex > -1) {
            // another slash. Read from 1..slashIndex
            subsection = xpath.substring(1, slashIndex + 1);
        } else {
            subsection = xpath.substring(1);
        }

        int openBracketIndex = subsection.indexOf('[');
        int closeBracketIndex = subsection.indexOf(']');
        int targetIndex = 1;
        String targetName;
        if (openBracketIndex > -1 && closeBracketIndex > -1) {
            targetIndex = Integer.parseInt(subsection.substring(openBracketIndex + 1, closeBracketIndex));
            targetName = subsection.substring(0, openBracketIndex);
        } else {
            targetName = subsection;
        }

        if (this.name.equals(targetName) && this.index == targetIndex) {
            if (slashIndex > -1) {
                JuxtaXMLNode result = null;
                ListIterator<JuxtaXMLNode> it = children.listIterator();
                while(it.hasNext() && result == null) {
                    result = it.next().getNodeForXPath(xpath.substring(slashIndex + 1));
                }
                return result;
            } else {
                return this;
            }
        }

        return null;
    }

    public JuxtaXMLNode getCommonNodeForOffsetPair(int offset1, int offset2)
    {
        JuxtaXMLNode node1 = getNodeForOffset(offset1);
        if (offset1 == offset2)
            return node1;

        if (offset1 > offset2)
            return null;

        JuxtaXMLNode node2 = getNodeForOffset(offset2);
        return findCommonParent(node1, node2);
    }

    // Naive, recursive algorithm to find a common parent between two nodes
    private static JuxtaXMLNode findCommonParent(JuxtaXMLNode node1, JuxtaXMLNode node2)
    {
        if (node1 == null || node2 == null)
            return null;

        if (node1 == node2)
            return node1;

        JuxtaXMLNode result = findCommonParent(node1.getParent(), node2);
        if (result == null)
            result = findCommonParent(node1, node2.getParent());
        
        return result;
    }

    public Set<String> getNotableTags()
    {
        Set<String> result;
        if (getParent() != null)
            result = getParent().getNotableTags();
        else
            result = new HashSet<String>();

        if (this.isNotable())
        {
            result.add(this.getName());
        }

        return result;
    }

    public int getStartOffset()
    {
        return startOffset;
    }

    public int getEndOffset()
    {
        return endOffset;
    }

    public boolean isExcluded()
    {
        return isExcludedElement;
    }

    public void setExcluded(boolean value)
    {
        isExcludedElement = value;
    }

    public boolean isNotable()
    {
        return isNotableElement;
    }

    public void setIsNotable(boolean value)
    {
        isNotableElement = value;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
