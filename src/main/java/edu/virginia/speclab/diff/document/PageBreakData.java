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
import edu.virginia.speclab.diff.OffsetRange.Space;

/**
 * Data necessary to represent a TEI:PB tag 
 * 
 * @author loufoster
 *
 */
public class PageBreakData implements Serializable {
    private final long id;
    private OffsetRange range;
    private String label;
    
    public PageBreakData() {
        this.id = UUID.randomUUID().hashCode();
    }
    
    public PageBreakData(int start, int end, String label) {
        this();
        this.range = new OffsetRange();
        this.range.set(start, end, Space.ORIGINAL);
        this.label = label;
    }

    public final OffsetRange getRange() {
        return range;
    }

    public final void setRange(int start, int end) {
        this.range = new OffsetRange();
        this.range.set(start, end, Space.ORIGINAL);
    }

    public final String getLabel() {
        return label;
    }

    public final void setLabel(String label) {
        this.label = label;
    }

    public final long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
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
        PageBreakData other = (PageBreakData) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PageBreakData [breakRange=" + range + ", label=" + label + "]";
    }
}
