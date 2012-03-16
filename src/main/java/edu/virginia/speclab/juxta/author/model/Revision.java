package edu.virginia.speclab.juxta.author.model;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.OffsetRange.Space;

public class Revision implements Comparable<Revision> {
    public enum Type { ADD, DELETE};
    private final OffsetRange offsetRange;
    private final Type type;
    
    public Revision(Type type, OffsetRange range) {
        this.type = type;
        this.offsetRange = range;
    }

    public final OffsetRange getOffsetRange() {
        return offsetRange;
    }

    public final Type getType() {
        return type;
    }
    
    public int getStartOffset(Space space) {
        return this.offsetRange.getStartOffset(space);
    }
    
    public int getEndOffset(Space space) {
        return this.offsetRange.getEndOffset(space);
    }

    public int compareTo(Revision that) {
        int thisStart = getOffsetRange().getStartOffset(Space.ORIGINAL);
        int thatStart = that.getOffsetRange().getStartOffset(Space.ORIGINAL);
        if (thisStart > thatStart) {
            return 1;
        } else if (thisStart < thatStart ) {
            return -1;
        } else {
            return 0;
        }
    }
    
    @Override
    public String toString() {
        return getType()+" "+getOffsetRange();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((offsetRange == null) ? 0 : offsetRange.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        Revision other = (Revision) obj;
        if (offsetRange == null) {
            if (other.offsetRange != null) {
                return false;
            }
        } else if (!offsetRange.equals(other.offsetRange)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }
    
    
}
