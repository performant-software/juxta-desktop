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
package edu.virginia.speclab.juxta.author.model.template;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.virginia.speclab.diff.document.TagSet;

/**
 * ParesTemplate is a named collection of behaviors that describe
 * how tags will be handled during a file import parsing
 * 
 * @author lfoster
 *
 */
public final class ParseTemplate implements Cloneable {
    
    private final String guid;
    private String name;
    private final String rootTagName;
    private boolean defaultTemplate;
    private List<Behavior> behaviors;
    
    /**
     * Construct a named parse template for documents with the
     * specified root tag name. It will no be set as the default
     * template for this type of document.
     * 
     * @param rootTag Root tag name of the XML document
     * @param name Name of the parse template
     */
    public ParseTemplate( final String rootTag, final String name ) {
        this(rootTag, name, false);
    }
    
    /**
     * Construct a named parse template for documents with the
     * specified root tag name. 
     * 
     * @param rootTag Root tag name of the XML document
     * @param name Name of the parse template
     * @param isDefault Set to true to make this template the default for
     *        all documents with the root element matching this one
     */
    public ParseTemplate( final String rootTag, final String name, final boolean isDefault) {
        this.name = name;
        this.rootTagName = rootTag;
        this.defaultTemplate = isDefault;
        this.behaviors = new ArrayList<Behavior>();
        this.guid = UUID.randomUUID().toString();
    }
    
    /**
     * Construct a named parse template for documents with the
     * specified settings.
     * 
     * @param guid The template GUID. Can be null. If so, a new guid will be generated
     * @param rootTag Root tag name of the XML document
     * @param name Name of the parse template
     * @param isDefault Set to true to make this template the default for
     *        all documents with the root element matching this one
     */
    public ParseTemplate( final String guid, final String rootTag, final String name, final boolean isDefault) {
        if (guid == null) {
            this.guid = UUID.randomUUID().toString();
        } else {
            this.guid = guid;
        }
        this.name = name;
        this.rootTagName = rootTag;
        this.defaultTemplate = isDefault;
        this.behaviors = new ArrayList<Behavior>();
    }
    
    /**
     * Create a clone of this ParseTemplate.
     */
    @Override
    public ParseTemplate clone() {
        ParseTemplate clone =  new ParseTemplate( getGuid(), getRootTagName(), getName(), isDefault());
        for (Behavior b: getBehaviors()) {
            clone.addBehavior(b.getTagName(), b.getAction(), b.getNewLine());
        }
        return clone;
    }

    /**
     * Give the template a new name
     * @param val
     */
    public void rename( final String newName) {
        this.name = newName;
    }
    
    public final String getGuid() {
        return this.guid;
    }
    
    /**
     * Get the root tag name. This is used to identify document types.
     * @return
     */
    public final String getRootTagName() {
        return this.rootTagName;
    }
    
    /**
     * Get the template Name
     * @return
     */
    public final String getName() {
        return this.name;
    }
    
    /**
     * Check if this template is the default for its root tag name
     * @return
     */
    public final boolean isDefault() {
        return this.defaultTemplate;
    }
    
    /**
     * Set/Unset this template as default for a root tag name
     * @param isDefault
     */
    public void setDefault( boolean isDefault ) {
        this.defaultTemplate = isDefault;
    }
   
    /**
     * Get the list of behaviors for this template
     * @return
     */
    public final List<Behavior> getBehaviors() {
        return this.behaviors;
    }
    
    /**
     * Add the specifed tag behavior to the template
     * 
     * @param tagName
     * @param act
     * @param newLine
     */
    public void addBehavior(final String tagName, final Action act, final boolean newLine) {
        this.behaviors.add(new Behavior(tagName, act, newLine));
    }
    
    /**
     * Get the set of notable tags for this tempate
     * @return
     */
    public TagSet getNotableTagSet() {
        return getTagSet(Action.MAKE_NOTABLE);
    }

    /**
     * Get the set of EXCLUDED tags for this template
     * @return
     */
    public TagSet getExcludedTagSet() {
        return getTagSet(Action.EXCLUDE);
    }

    /**
     * Get the set of tags that include NEWLINE for this template
     * @return
     */
    public TagSet getNewlineTagSet() {
        TagSet result = new TagSet();
        for (Behavior behavior : getBehaviors()) {
            if (behavior.getNewLine() == true) {
                result.includeTag(behavior.getTagName());
            }
        }
        return result;
    }
    
    /**
     * helper to get a specific tag set
     * @param action
     * @return
     */
    private TagSet getTagSet(final Action action) {
        TagSet result = new TagSet();
        for (Behavior behavior : getBehaviors()) {
            if (behavior.getAction().equals(action)) {
                result.includeTag(behavior.getTagName());
            }
        }
        return result;
    }
    
    
    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((guid == null) ? 0 : guid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        
        if (obj == null) {
            return false;
        }
        
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        ParseTemplate other = (ParseTemplate) obj;
        if (guid == null) {
            if (other.guid != null) {
                return false;
            }
        } else if (!guid.equals(other.guid)) {
            return false;
        }
        return true;
    }

    
    /**
     * An enumeration of the possible actions to take
     * upon encountering a tag during parsing
     */
    public enum Action {
      INCLUDE, 
      EXCLUDE,
      MAKE_NOTABLE
    };
    
    /**
     * Define the behavior when a tag is encountered during parsing
     */
    public static final class Behavior implements Comparable<Behavior> {
        private final String tagName;
        private final Action action;
        private final boolean newline;
        
        public Behavior(final String tagName, final Action action, final boolean newLine) {
            this.tagName = tagName;
            this.action = action;
            this.newline = newLine;
        }
        
        public final String getTagName() {
            return this.tagName;
        }
        
        public final Action getAction() {
            return this.action;
        }
        
        public final boolean getNewLine() {
            return this.newline;
        }
        
        @Override
        public String toString() {
            return "Tag: " + tagName+ ", Action: "+getAction()+", newLine: "+getNewLine();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((tagName == null) ? 0 : tagName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj){
                return true;
            }
            
            if (obj == null) {
                return false;
            }
            
            if (getClass() != obj.getClass()) {
                return false;
            }
            
            Behavior other = (Behavior) obj;
            if (tagName == null) {
                if (other.tagName != null) {
                    return false;
                }
            } else if (!tagName.equals(other.tagName)) {
                return false;
            }
            return true;
        }

        public int compareTo(Behavior other) {
            return getTagName().compareTo( other.getTagName() );
        }
    }
}
