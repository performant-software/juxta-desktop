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

package edu.virginia.speclab.juxta.author.view.collation;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.font.FontRenderContext;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.OffsetRange.Space;
import edu.virginia.speclab.diff.document.NoteData;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.view.collation.NoteBox.Type;

public class NotesManager implements ComponentListener {
    private FontRenderContext fontRenderContext;
    private JuxtaDocument document;
    private List<NoteBox> notes;
    private CollationViewTextArea textPane;
    private boolean enabled = true;
    private boolean adjustSize = false;
    
    public NotesManager(CollationViewTextArea txtPane, FontRenderContext frc) {
        this.textPane = txtPane;
        this.fontRenderContext = frc;
        this.notes = Collections.synchronizedList( new LinkedList<NoteBox>() );
    }
    
    public void setEnabled( boolean vis ) {
        this.enabled = vis;
    }
    
    public void initNotes( JuxtaDocument document ) {
        this.document = document;
        this.notes.clear();
        if ( document == null ) {
            return;
        }
        
        createNotes();
    }

    private void createNotes() {
        int lastY = -1;
        adjustSize = true;
        for ( NoteData note : this.document.getNotes() ) {
            try {
                // extract the range for the source text
                // and convert it to screen coords. use this
                // as the anchor point. End point is right margin
                OffsetRange range = note.getAnchorRange();
                int p0 = range.getStartOffset(Space.ACTIVE);
                int p1 = range.getEndOffset(Space.ACTIVE);
                int mid = (p0+p1)/2;
                Rectangle startRect = this.textPane.modelToView(mid);
                Point startPoint = new Point(startRect.x, startRect.y+startRect.height+1);
                int rightMarginBoundary = this.textPane.getSize().width - RenderingConstants.MARGIN_SIZE;
                Point endPoint = new Point(rightMarginBoundary + 
                    RenderingConstants.LINE_EXTENSION_BEYOND_MARGIN, startPoint.y);
                
                // grab the note text
                range = note.getNoteRange(); 
                p0 = range.getStartOffset(Space.ORIGINAL);
                p1 = range.getEndOffset(Space.ORIGINAL);
                String txt = this.document.getSourceDocument().getRawXMLContent().substring(p0,p1);
                
                // create it at the initial position
                NoteBox box = new NoteBox(txt, note.getAnchorRange(), startPoint, endPoint);
                if ( note.getTargetID() != null ) {
                    box.setType(Type.TARGETED);
                } else {
                    box.setType(Type.MARGIN);
                }
                box.setNoteTextStartOffset( p0 );
                box.initialize(this.fontRenderContext, rightMarginBoundary);
                this.notes.add(box);
                
                // fix overlaps
                Rectangle rect = box.getBoundingBox();
                if ( lastY > -1 ) {
                    if ( rect.y < lastY ) {
                        box.move(0, lastY - rect.y);
                    }
                } 
                lastY = box.getBoundingBox().y+box.getBoundingBox().height+4;

            } catch (Exception e) {
                // TODO something epic
            }
        }
    }
    
    /**
     * Get the note box at the specified point. Null fif none
     * @param pt
     * @return
     */
    public NoteBox getNoteBox( Point pt ) {
        for ( NoteBox box : this.notes ) {
            if ( box.isHit(pt)) {
                return box;
            }
        }
        return null;
    }
    
    /**
     * handles mouse over at the specified point. If it is over a note box or
     * noted lemmata, highlight both.
     * @param point
     * @return Returns true if the rollover changed any highlight status
     */
    public boolean rollover(Point point) {
        if ( this.enabled == false) {
            return false;
        }
        
        boolean changed = false;
        for ( NoteBox box : this.notes ) {
            boolean old = box.isHighlight();
            box.setHighlight( box.isHit(point) );
            if (old != box.isHighlight()) {
                changed = true;
                if ( box.getType().equals(Type.TARGETED)) {
                    this.textPane.setNoteHighlight( box.getSourceRange(), box.isHighlight() );
                } else {
                    this.textPane.repaint();
                }
            }
        }
        
        return changed;
    }
   
    public void paint( Graphics2D g2 ) {
        if ( this.enabled == false ) {
            return;
        }
        
        if ( this.adjustSize ) {
            this.adjustSize = false;
            if ( this.notes.size() > 0) {
                NoteBox n = this.notes.get( this.notes.size()-1 );
                Rectangle tcer = n.getBoundingBox();
                this.textPane.extendDrawingSurfaceHeight( tcer.y+tcer.height+10);
            }
        }
        
        for ( NoteBox box : this.notes ) {
            box.paint(g2);
        }
    }
    
    public boolean isEnabled() {
        return this.enabled;
    }
    
    public void componentResized(ComponentEvent arg0) {
        this.notes.clear();
        createNotes();
        this.textPane.repaint();
    }

    // No-op for all of these....
    public void componentHidden(ComponentEvent arg0) {}
    public void componentMoved(ComponentEvent arg0) {}
    public void componentShown(ComponentEvent arg0) {}
}
