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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.BadLocationException;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.juxta.author.model.AnnotationManager;
import edu.virginia.speclab.juxta.author.model.DocumentManager;
import edu.virginia.speclab.juxta.author.view.InvalidDataException;
import edu.virginia.speclab.util.SimpleLogger;

/**
 * This class is responsible for orchestrating the rendering of the margin boxes for difference
 * display. It can asynchronously render these differences onto the specified <code>MarkableTextPanel</code>
 * @author Nick
 *
 */
class DifferencePainter implements RenderingConstants {
    // model
    private List<DifferenceTag> differenceTagList;
    private int selectedOffset;

    // helpers
    private MarginBoxManager marginBoxManager;

    // drawing surface
    private CollationViewTextArea textArea;

    // tracking surface resize
    private boolean needSurfaceResize;
    private Rectangle drawingAreaBounds;
    private boolean enabled = true;

    public DifferencePainter(CollationViewTextArea drawingPanel, int selectedOffset, FontRenderContext fontRenderContext,
        DocumentManager documentManager, AnnotationManager annotationManager) {
        this.textArea = drawingPanel;
        this.selectedOffset = selectedOffset;

        marginBoxManager = new MarginBoxManager(fontRenderContext, documentManager, annotationManager);

        differenceTagList = Collections.synchronizedList(new LinkedList<DifferenceTag>());
        drawingAreaBounds = new Rectangle();
    }
    
    public void setEnabled(boolean enabled ) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return this.enabled;
    }

    private void extendDrawingSurface() {
        if (needSurfaceResize) {
            textArea.extendDrawingSurfaceHeight((int) drawingAreaBounds.getHeight());
            needSurfaceResize = false;
        }
    }

    public boolean isAnnotationSelected(Point p) {
        if ( this.enabled == false ) {
            return false;
        }
        synchronized (differenceTagList) {
            for (DifferenceTag differenceTag : differenceTagList) {
                if (differenceTag.testAnnotationHit(p)) {
                    return true;
                }
            }
        }

        return false;
    }

    public Difference getDifference(int x, int y) {
        if ( this.enabled == false ) {
            return null;
        }
        
        Difference difference = null;
        synchronized (differenceTagList) {
            for (DifferenceTag differenceTag : differenceTagList) {
                if (differenceTag.testHit(x, y)) {
                    difference = differenceTag.getDifference();
                }
            }
        }

        return difference;
    }

    /**
     * Looks for a difference tag at the given location. If one is found, it is highlighted.
     * All other tags are unhighlighted. 
     * @param x
     * @param y
     */
    public boolean highlightDifference(Difference difference) {
        if ( this.enabled == false ) {
            return false;
        }
        
        boolean changed = false;
        synchronized (differenceTagList) {
            for (DifferenceTag differenceTag : differenceTagList ) {
                if (differenceTag.getDifference().equals(difference)) {
                    if (differenceTag.isHighlighted() == false) {
                        differenceTag.setHighlight(true);
                        changed = true;
                    }
                } else {
                    if (differenceTag.isHighlighted() == true) {
                        differenceTag.setHighlight(false);
                        changed = true;
                    }
                }
            }
        }

        return changed;
    }

    public boolean setAnnotationMark(Difference difference, boolean annotated) {
        if ( this.enabled == false ) {
            return false;
        }
        
        boolean changed = false;
        synchronized (differenceTagList) {
            for (DifferenceTag differenceTag : differenceTagList) {
                if (difference.same(differenceTag.getDifference())) {
                    differenceTag.setAnnotated(annotated);
                    changed = true;
                    break;
                }
            }
        }

        return changed;
    }

    /**
     * Load the difference set to use with this painter. This can only be called once.
     * @param differenceSet
     */
    public void initialize(List<Difference> differenceList) {
        for (Difference diff : differenceList) {
            try {
                DifferenceTag tag = createDifferenceTag(diff);
                if (tag != null) {
                    addDifferenceTag(tag);
                }
            } catch (InvalidDataException e) {
                SimpleLogger.logError(e.getMessage());
            }
        }
        checkSurfaceBounds();
    }

    private DifferenceTag createDifferenceTag(Difference data) throws InvalidDataException {
        Point lineStart;
        try {
            if (selectedOffset >= 0) {
                Rectangle lineStartRect = textArea.modelToView(selectedOffset);
                lineStart = new Point(lineStartRect.x, lineStartRect.y + lineStartRect.height);
            } else {
                Rectangle lineStartRect = textArea.modelToView(data.getOffset(Difference.BASE)
                    + data.getLength(Difference.BASE));
                lineStart = new Point(lineStartRect.x, lineStartRect.y + lineStartRect.height);
            }
        } catch (BadLocationException e) {
            throw new InvalidDataException(e.toString());
        }

        int rightMarginBoundary = textArea.getSize().width - RenderingConstants.MARGIN_SIZE;

        Point lineEnd = new Point(rightMarginBoundary + LINE_EXTENSION_BEYOND_MARGIN, lineStart.y);

        MarginBox marginBox = marginBoxManager.createMarginBox(lineEnd.x, lineEnd.y, data);
        AnchorPoint anchorPoint = new AnchorPoint(lineStart, rightMarginBoundary, marginBox.getAttachPoint());

        // create a new tag
        return new DifferenceTag(data, anchorPoint, marginBox);
    }

    private void addDifferenceTag(DifferenceTag newTag) {
        // add this tag to the total bounds for the drawing area
        drawingAreaBounds.add(newTag.getBoundingBox());

        for (DifferenceTag tag : differenceTagList) {

            // if this tag is past the new tag, insert before
            if (tag.getDifference().getOffset(Difference.BASE) > newTag.getDifference().getOffset(Difference.BASE)) {
                differenceTagList.add(differenceTagList.indexOf(tag), newTag);
                return;
            }
        }

        // still here, this tag must be past all the others, put it on the end 
        differenceTagList.add(newTag);
    }

    private void checkSurfaceBounds() {
        // get the bounds of the target surface
        Dimension surfaceSize = textArea.getSize();
        Rectangle bounds = new Rectangle(0, 0, surfaceSize.width, surfaceSize.height);

        // if this tag is off the surface, don't paint yet, we need to 
        // enlarge the surface
        if (bounds.contains(drawingAreaBounds) == false) {
            needSurfaceResize = true;
        }
    }

    public void paint(Graphics2D g2) {
        if ( this.enabled == false ) {
            return;
        }
        synchronized (differenceTagList) {
            extendDrawingSurface();
            for (DifferenceTag differenceTag : differenceTagList) {
                differenceTag.paint(g2);
            }
        }
    }

    private class DifferenceTag {
        private Difference difference;

        private Rectangle boundingBox;

        private AnchorPoint anchorPoint;
        private MarginBox marginBox;

        public DifferenceTag(Difference difference, AnchorPoint anchorPoint, MarginBox marginBox) {
            this.anchorPoint = anchorPoint;
            this.marginBox = marginBox;
            this.difference = difference;

            computeBoundingBox();
        }

        private void computeBoundingBox() {
            boundingBox = new Rectangle();
            boundingBox.add(anchorPoint.getBoundingBox());
            boundingBox.add(marginBox.getBoundingBox());
        }

        public boolean testHit(int x, int y) {
            return marginBox.contains(x, y);
        }

        public boolean testAnnotationHit(Point p) {
            return marginBox.testAnnotationHit(p);
        }

        public void paint(Graphics2D g2) {
            anchorPoint.paint(g2);
            marginBox.paint(g2);
        }

        public boolean isHighlighted() {
            if (marginBox == null)
                return false;
            else
                return marginBox.isHighlight();
        }

        public void setHighlight(boolean highlight) {
            if (marginBox != null && anchorPoint != null) {
                anchorPoint.setHighlight(highlight);
                marginBox.setHighlight(highlight);
            }
        }

        public void setAnnotated(boolean annotate) {
            if (marginBox != null) {
                marginBox.setAnnotated(annotate);
            }

        }

        public Rectangle getBoundingBox() {
            return boundingBox;
        }

        public Difference getDifference() {
            return difference;
        }
    }
}
