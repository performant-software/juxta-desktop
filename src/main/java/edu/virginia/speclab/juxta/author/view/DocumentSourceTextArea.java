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
 
package edu.virginia.speclab.juxta.author.view;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.util.SimpleLogger;

import java.awt.Color;
import java.awt.Rectangle;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.undo.UndoManager;

/**
 *
 * @author ben
 */
public class DocumentSourceTextArea extends JTextArea implements JuxtaUserInterfaceStyle, UndoableEditListener {

    private static final Color READ_ONLY_COLOR = new Color(255,255,255); 
    private static final Color EDIT_COLOR = new Color(255,250,240); 
    private UndoManager undoMgr = new UndoManager();
    
    public DocumentSourceTextArea()
    {
        this.setEditable(false);
        this.setWrapStyleWord(true);
        this.setLineWrap(true);this.setBackground( READ_ONLY_COLOR );
        this.getDocument().addUndoableEditListener( this );
    }
    
    @Override
    public void setEditable(boolean editable ) {
        super.setEditable(editable);
        if ( editable ) {
            this.setBackground( EDIT_COLOR );
            
        } else {
            this.setBackground( READ_ONLY_COLOR );
        }
    }
    
    public boolean hasEdits() {
        return this.undoMgr.canUndo();
    }
    
    public void undoAllEdits() {
        while ( this.undoMgr.canUndo() ) {
            this.undoMgr.undo();
        }
    }

    public void highlightRanges(OffsetRange innerRange, OffsetRange outerRange)
    {
        try {
            Highlighter h = getHighlighter();
            h.removeAllHighlights();
            if (outerRange != null)
                h.addHighlight(outerRange.getStartOffset(OffsetRange.Space.ORIGINAL), innerRange.getStartOffset(OffsetRange.Space.ORIGINAL),
                        new DefaultHighlighter.DefaultHighlightPainter(FIRST_COLOR));

            h.addHighlight(innerRange.getStartOffset(OffsetRange.Space.ORIGINAL), innerRange.getEndOffset(OffsetRange.Space.ORIGINAL),
                    new DefaultHighlighter.DefaultHighlightPainter(SECOND_COLOR_SCALE[4]));

             if (outerRange != null)
                h.addHighlight(innerRange.getEndOffset(OffsetRange.Space.ORIGINAL), outerRange.getEndOffset(OffsetRange.Space.ORIGINAL),
                        new DefaultHighlighter.DefaultHighlightPainter(FIRST_COLOR));

            centerOffset(innerRange.getStartOffset(OffsetRange.Space.ORIGINAL));

        } catch (BadLocationException ex) {
            // meh
        }
    }

    public void centerOffset(final int offset) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                setCaretPosition(offset);

                try {
                    // scroll to the beginning of the doc
                    Rectangle startOfDoc = modelToView(0);

                    if (startOfDoc != null) {

                        scrollRectToVisible(startOfDoc);

                        // scroll to the selected position
                        Rectangle comparandRect = modelToView(offset);

                        if (comparandRect != null) {
                            Rectangle viewRect = getVisibleRect();
                            comparandRect.y += viewRect.height / 2;
                            scrollRectToVisible(comparandRect);
                        }
                    }
                } catch (Exception e) {
                    SimpleLogger.logError("Unable to scroll DocumentSourceText to offset: " + offset);
                }
            }

        });
    }
    
    public void undoableEditHappened(UndoableEditEvent evt) {
        if ( isEditable() ) {
            this.undoMgr.addEdit(evt.getEdit());
        }
    }


//    // For whatever reason, Container.isVisible() only returns "local" visibility;
//    // that is, it returns a boolean that means whether or not it WOULD be visible if it's
//    // parent container were visible. But this is silly; I just want to know if the
//    // container is actually visible on screen. So I wrote this:
//    public static boolean isActuallyVisible(Container container)
//    {
//        if (container.getParent() == null)
//            return container.isVisible();
//        else if (container.getParent().isVisible() == false)
//            return false;
//        else
//            return isActuallyVisible(container.getParent());
//    }

}
