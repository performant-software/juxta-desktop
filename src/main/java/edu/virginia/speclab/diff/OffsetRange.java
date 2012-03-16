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
 
package edu.virginia.speclab.diff;

import edu.virginia.speclab.diff.document.DocumentModel;
import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.DocumentManagerAccess;
import java.io.Serializable;

/**
 * This class represents an offset pair (start & end) of character indexes
 * into a particular document. These offsets can be mapped into one of three
 * coordinate Spaces:
 *
 *   ORIGINAL:  The source, pre-processed document, which might be flat text or XML.
 *   PROCESSED: The post-processed output document, which has had tags and markup stripped,
 *              potentially some text removed.
 *   ACTIVE:    The active fragment of text, which could have been manually specified via
 *              the user-interface fragment selector, or via a pseudo-XPath by the user
 *              or by default for certain document types.
 *
 * Most of Juxta cares about offsets in the ACTIVE Space, but these offsets can become out-of-sync
 * if the document processing settings change. Since we want to be able to persist these
 * indexes in some cases (for Moves, for instance, or for annotations)
 * 
 * @author ben
 */
public class OffsetRange implements Serializable {
    public enum Space { ORIGINAL, PROCESSED, ACTIVE };
    protected int _documentID;
    protected transient DocumentModel _document;
    protected int _startOffset;
    protected int _endOffset;

    // Empty constructor, initializes everything to a null state
    public OffsetRange()
    {
        _startOffset = _endOffset = 0;
        _document = null;
        _documentID = 0;
    }

    // OffsetRange tied to a documentID but without coordinates
    public OffsetRange(int documentID)
    {
        _documentID = documentID;
        _document = null;
    }


    // OffsetRange tied to a document but without coordinates. This
    // constructor is preferred if the document is available.
    public OffsetRange(DocumentModel doc)
    {
        this();
        _document = doc;
        _documentID = doc.getID();
    }

    // Complete initializer
    public OffsetRange(int documentID, int startOffset, int endOffset, Space space)
    {
        this(documentID);
        set(startOffset, endOffset, space);
    }

    // Complete initializer. This version is preferred if the document is available. 
    public OffsetRange(DocumentModel doc, int startOffset, int endOffset, Space space)
    {
        this(doc);
        set(startOffset, endOffset, space);
    }

    // Copy constructor
    public OffsetRange(OffsetRange other)
    {
        _document = other._document;
        _documentID = other._documentID;
        _startOffset = other._startOffset;
        _endOffset = other._endOffset;
    }

    // Change the document that this OffsetRange refers to
    public void resetDocument(int documentID)
    {
        _document = null;
        _documentID = documentID;
    }

    // Change the document that this OffsetRange refers to.
    // Preferred to the resetDocument() that only takes
    // documentID.
    public void resetDocument(DocumentModel doc)
    {
        _document = doc;
        _documentID = doc.getID();
    }

    // Fully specify the OffsetRange.
    public void set(DocumentModel doc, int startOffset, int endOffset, Space space)
    {
       resetDocument(doc);
       set(startOffset, endOffset, space);
    }

    // Specify the offset range, apart from the document. All data will be
    // converted to ORIGINAL space when stored in the range
    public void set(int startOffset, int endOffset, Space space)
    {
        if (space == Space.ORIGINAL)
        {
            _startOffset = startOffset;
            _endOffset = endOffset;
        }
        else
        {
            if (getDocument() == null)
            {
                throw new UnsupportedOperationException("Don't know what to do with offsets in non-ORIGINAL space with no document reference.");
            }
            else if (space == Space.PROCESSED)
            {
                _startOffset = convertProcessedToOriginal(startOffset);
                _endOffset = convertProcessedToOriginal(endOffset);
            }
            else if (space == Space.ACTIVE)
            {
                _startOffset = convertActiveToOriginal(startOffset);
                _endOffset = convertActiveToOriginal(endOffset);
            }
        }
    }

    public int getLength(Space space)
    {
        return (this.getEndOffset(space) - this.getStartOffset(space));
    }


    public int getStartOffset(Space space)
    {
        if (space == Space.ORIGINAL)
            return _startOffset;
        else
        {
            if (getDocument() == null)
            {
                throw new UnsupportedOperationException("Don't know what to do with offsets in non-ORIGINAL space with no document reference.");
            }
            else if (space == Space.ACTIVE)
                return convertOriginalToActive(_startOffset);
            else if (space == Space.PROCESSED)
                return convertOriginalToProcessed(_startOffset);
        }
        
        throw new UnsupportedOperationException("Offset requested in unrecognized coordinate space: "+space.toString());
    }

    public int getEndOffset(Space space)
    {
        if (space == Space.ORIGINAL)
            return _endOffset;
        else
        {
            if (getDocument() == null)
            {
                throw new UnsupportedOperationException("Don't know what to do with offsets in non-ORIGINAL space with no document reference.");
            }
            else if (space == Space.ACTIVE)
                return convertOriginalToActive(_endOffset);
            else if (space == Space.PROCESSED)
                return convertOriginalToProcessed(_endOffset);
        }

        throw new UnsupportedOperationException("Offset requested in unrecognized coordinate space: "+space.toString());
    }


    public String getText(Space space)
    {
        if (getDocument() == null)
            throw new UnsupportedOperationException("Can't get the string from an OffsetRange with no document reference.");

        if (space == Space.ACTIVE || space == Space.PROCESSED)
        {
            String result = getDocument().getAllProcessedText();
            int start = this.getStartOffset(Space.PROCESSED);
            int end = this.getEndOffset(Space.PROCESSED);
            result = result.substring(start, end);
            return result;
        }
        else if (space == Space.ORIGINAL)
        {
            return getDocument().getSourceDocument().getRawXMLContent().substring(this.getStartOffset(Space.ORIGINAL), this.getEndOffset(Space.ORIGINAL));
        }
        else
            return null;
    }

    public int getDocumentID()
    {
        return _documentID;
    }

    public DocumentModel getDocument()
    {
        // lazy-load the document
        if (_document == null)
        {
            _document = DocumentManagerAccess.getInstance().getDocumentManager().lookupDocument(_documentID);
        }
        return _document;
    }

    private int convertOriginalToProcessed(int original)
    {
        try {
            return getDocument().getSourceDocument().getOffsetMap().getTargetOffset(original);
        } catch (ReportedException ex) {
           ErrorHandler.handleException(ex);
        }
        return 0;
    }

    private int convertProcessedToOriginal(int processed)
    {
        try {
            return getDocument().getSourceDocument().getOffsetMap().getSourceOffset(processed);
        } catch (ReportedException ex) {
            ErrorHandler.handleException(ex);
        }
        return 0;
    }

    private int convertProcessedToActive(int processed)
    {
        return (processed - getDocument().getActiveTextRange().getStartOffset(Space.PROCESSED));
    }

    private int convertActiveToProcessed(int active)
    {
        return (active + getDocument().getActiveTextRange().getStartOffset(Space.PROCESSED));
    }

    private int convertOriginalToActive(int original)
    {
        return convertProcessedToActive(convertOriginalToProcessed(original));
    }

    private int convertActiveToOriginal(int active)
    {
        return convertProcessedToOriginal(convertActiveToProcessed(active));
    }

    @Override
    public String toString()
    {
        return ("offset: " + this._startOffset + " to " + this._endOffset + " space: ORIGINAL");
    }

    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + _documentID;
        result = prime * result + _endOffset;
        result = prime * result + _startOffset;
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
        OffsetRange other = (OffsetRange) obj;
        if (_documentID != other._documentID) {
            return false;
        }
        if (_endOffset != other._endOffset) {
            return false;
        }
        if (_startOffset != other._startOffset) {
            return false;
        }
        return true;
    }

    static public String spaceToString(Space space)
    {
        if (space == Space.ACTIVE) return "active";
        if (space == Space.PROCESSED) return "processed";
        return "original";
    }

    static public Space stringToSpace(String space)
    {
        if (space.equals(spaceToString(Space.ACTIVE))) return Space.ACTIVE;
        if (space.equals(spaceToString(Space.PROCESSED))) return Space.PROCESSED;
        return Space.ORIGINAL;
    }
}
