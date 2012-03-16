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
package edu.virginia.speclab.diff.document;

import java.io.Serializable;
import java.util.UUID;

import edu.virginia.speclab.diff.OffsetRange;

public final class NoteData implements Serializable {
    private static final long serialVersionUID = 8778005761664217695L;
    private final int id;
    private final String type;
    private String targetID;
    private OffsetRange noteRange;
    private OffsetRange anchorRange;
    
    /**
     * Create a note of the specified type
     * @param type
     * @param note
     */
    public NoteData(final String type) {
        this.type = type;
        this.id = UUID.randomUUID().hashCode();
    }
    
    /**
     * Set the target ID for a note that is tied to a specific element
     * @param tgt
     */
    public void setTargetID( final String tgt ) {
        this.targetID = tgt;
    }
    
    public void setNoteContentRange(final OffsetRange range) {
        this.noteRange = range;
    }
    
    public void setAnchorRange(final OffsetRange range) {
        this.anchorRange = range;
    }

    public final String getType() {
        return type;
    }

    public final String getTargetID() {
        return targetID;
    }

    public final OffsetRange getNoteRange() {
        return this.noteRange;
    }

    public final OffsetRange getAnchorRange() {
        return this.anchorRange;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
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
        NoteData other = (NoteData) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }
    
    
}
