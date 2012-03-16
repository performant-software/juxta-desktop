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

package edu.virginia.speclab.juxta.author.view.collation;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.juxta.author.model.MovesManager;

class TextSelectionTracker extends MouseAdapter implements MouseMotionListener, RenderingConstants {
    private CollationViewTextArea textArea;
    private Collation collation;
    private MovesManager movesManager;
    private int docId;

    private boolean dragging;
    private Point dragStart;

    public TextSelectionTracker(CollationViewTextArea collationViewText) {
        this.textArea = collationViewText;
        this.textArea.addMouseListener(this);
        this.textArea.addMouseMotionListener(this);
    }
    

    public void setEnabled(boolean enabled) {
        if ( enabled ) {
            this.textArea.addMouseListener(this);
            this.textArea.addMouseMotionListener(this);
        } else {
            this.textArea.removeMouseListener(this);
            this.textArea.removeMouseMotionListener(this);
        }
    }

    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();

        if (!dragging) {
            if (textArea.selectDifference(p) == false) {
               selectPoint(p);
            }
        }

        dragging = false;
    }

    public void mousePressed(MouseEvent e) {
        dragStart = e.getPoint();
        dragging = false;
    }

    public void mouseReleased(MouseEvent e) {
        if (dragging) {
            Point dragEnd = e.getPoint();
            selectRange( this.dragStart, dragEnd);
            dragging = false;
        }
    }

    public void mouseDragged(MouseEvent e) {
        // if the dragging started on this surface
        if (dragStart != null) {
            this.dragging = true;
            int start = this.textArea.viewToModel(this.dragStart);
            int end = this.textArea.viewToModel(e.getPoint());
            this.textArea.highlightRangeSelection(start, end);
        }
    }

    // update the cursor if we are over a highlighted area
    private void updateCursor(Point p) {
        if (collation == null)
            return;

        int offset = textArea.viewToModel(p);
        if (collation.getDifferenceFrequency(offset) > 0 || 
            textArea.isMarginBoxAnnotationLink(p) ) {
            textArea.setCursor(HOTSPOT_CURSOR);
        } else {
            textArea.setCursor(NORMAL_CURSOR);
        }
    }

    public void mouseMoved(MouseEvent e) {
        textArea.rollOver(e.getPoint());
        updateCursor(e.getPoint());
        dragging = false;
    }

    private void selectRange(Point start, Point end) {
        if (collation == null){
            return;
        }
        
        // figure out where in the text was clicked
        int startOffset = this.textArea.viewToModel(start);
        int endOffset = this.textArea.viewToModel(end);
        this.textArea.highlightRangeSelection(startOffset, endOffset);
        
        // create the selected set (no duplicates)
        HashSet<Difference> differenceSet = new HashSet<Difference>();
        for (int i = startOffset; i <= endOffset; i++) {
            List<Difference> differences = getDifferenceList(i);
            if (differences != null) {
                differenceSet.addAll(differences);
            }
        }

        // transfer the contents of the set to the list
        List<Difference> differenceList = new LinkedList<Difference>();
        differenceList.addAll(differenceSet);

        // if there are differences to paint
        if (differenceList.size() > 0) {
            // select the offset to use as the anchor line        
            int selectedOffset = startOffset + ((endOffset - startOffset) / 2);
            this.textArea.setDifferenceList(differenceList);
            this.textArea.setSelectedOffset(selectedOffset);
            this.textArea.redraw();
        }
    }

    private List<Difference> getDifferenceList(int offset) {
        List<Difference> list = collation.getDifferences(offset);
        list = movesManager.addMoves(list, docId, offset);
        return list;
    }

    private void selectPoint(Point p) {
        if (collation == null)
            return;

        this.textArea.clearRangeSelection();
        
        // figure out where in the text was clicked
        int offset = textArea.viewToModel(p);

        // record the differences found there
        List differenceList = getDifferenceList(offset);
        textArea.setDifferenceList(differenceList);
        textArea.handleClick(offset);
    }

    public void setCollation(Collation collation, MovesManager movesManager, int docId) {
        this.collation = collation;
        this.movesManager = movesManager;
        this.docId = docId;
    }
}
