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

import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.juxta.author.model.Annotation;
import edu.virginia.speclab.juxta.author.model.AnnotationManager;
import edu.virginia.speclab.juxta.author.model.DocumentManager;
import edu.virginia.speclab.juxta.author.view.InvalidDataException;

class MarginBoxManager implements RenderingConstants {
    private FontRenderContext fontRenderContext;
    private DocumentManager documentManager;
    private List<MarginBox> marginBoxList;
    private AnnotationManager annotationManager;

    public MarginBoxManager(FontRenderContext fontRenderContext, DocumentManager documentManager,
        AnnotationManager annotationManager) {
        this.annotationManager = annotationManager;
        this.documentManager = documentManager;
        this.fontRenderContext = fontRenderContext;
        this.marginBoxList = new LinkedList<MarginBox>();
    }

    private MarginBox lookupExistingBox(int x, int y) {
        for (MarginBox box : this.marginBoxList) {
            Rectangle2D boundingBox = box.getBoundingBox();
            Rectangle2D boxPlusGap = new Rectangle2D.Double(boundingBox.getX(), boundingBox.getY(),
                boundingBox.getWidth(), boundingBox.getHeight() + BOX_GAP);
            if (boxPlusGap.contains(x, y)) {
                return box;
            }
        }

        return null;
    }

    /**
     * Attempts to create a margin box at the specified location. If there is already a box
     * at that location, it will find an open space and place it there instead. 
     * @param x Requested X coordinate for the box.
     * @param y Requested Y coordinate for the box.
     * @param differenceType The type of difference this is displaying. 
     * @return A MarginBox object.
     */
    public MarginBox createMarginBox(int x, int y, Difference difference) throws InvalidDataException {
        MarginBox existingBox = lookupExistingBox(x, y);

        if (existingBox == null) {
            Annotation annotation = annotationManager.getAnnotation(difference);
            boolean hasAnnotation = (annotation != null);

            // render the box                
            MarginBox marginBox = new MarginBox(x, y, difference, fontRenderContext, documentManager, hasAnnotation);

            // remember the box
            marginBoxList.add(marginBox);

            // return box location
            return marginBox;
        } else {
            // recurse till we find an opening
            Rectangle2D box = existingBox.getBoundingBox();
            int newY = (int) (box.getY() + box.getHeight() + BOX_GAP + (TITLE_HEIGHT / 2));
            return createMarginBox(x, newY, difference);
        }
    }
}