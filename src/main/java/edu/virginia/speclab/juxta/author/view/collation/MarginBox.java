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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.juxta.author.model.AnnotationManager;
import edu.virginia.speclab.juxta.author.model.DocumentManager;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.view.InvalidDataException;
import edu.virginia.speclab.ui.WrappingTextLabel;

class MarginBox implements RenderingConstants {
    private boolean highlight;
    private boolean annotated;

    private int textType;

    private Point attachPoint;

    private Difference difference;
    private DocumentManager documentManager;

    private WrappingTextLabel titleTextBox, bodyTextBox;

    private static final Stroke BOX_STROKE = new BasicStroke(STROKE_WIDTH);
    private static final Stroke HIGHLIGHT_BOX_STROKE = new BasicStroke(HIGHLIGHT_STROKE_WIDTH);

    // assets for rendering
    private RoundRectangle2D marginBox;
    private Line2D titleSeperator;
    private Area bodyBox;

    private Point iconLocation;
    private BufferedImage iconImage;

    private Point annotationIconLocation;

    public MarginBox(int x, int y, Difference difference, FontRenderContext fontRenderContext,
        DocumentManager documentManager, boolean hasAnnotation) throws InvalidDataException {
        this.attachPoint = new Point(x, y);
        this.difference = difference;
        this.documentManager = documentManager;
        this.annotated = hasAnnotation;

        // this is where the box will be drawn
        Point boxLocation = new Point(attachPoint.x, attachPoint.y - (TITLE_HEIGHT / 2));

        if (textType == Difference.BASE) {
            float textHeight = initBaseText(boxLocation, fontRenderContext);
            initBaseBox(boxLocation, textHeight);
        } else {
            float textHeight = initWitnessText(boxLocation, fontRenderContext);
            initWitnessBox(boxLocation, textHeight);
        }
    }

    private void initWitnessBox(Point boxLocation, float textAreaHeight) {
        // rendering: fill the background of the box with the fill color, draw the
        // box lines on top of that, then cut out a white area for the body in the shape of
        // bodyBox, which is a rounded rectangle with square sides on the top.

        // the height of the box is driven the the text area
        float boxHeight = (STROKE_WIDTH * 2) + textAreaHeight;

        this.marginBox = new RoundRectangle2D.Float(boxLocation.x, boxLocation.y, BOX_WIDTH, boxHeight, BOX_ARC,
            BOX_ARC);

        this.titleSeperator = new Line2D.Float(boxLocation.x + SYMBOL_SIDEBAR_WIDTH, boxLocation.y, boxLocation.x
            + SYMBOL_SIDEBAR_WIDTH, boxLocation.y + boxHeight);

        Rectangle2D leftBodyBox = new Rectangle2D.Float(boxLocation.x + SYMBOL_SIDEBAR_WIDTH + STROKE_WIDTH,
            boxLocation.y + STROKE_WIDTH, BOX_WIDTH / 2, boxHeight - (STROKE_WIDTH * 2));

        RoundRectangle2D rightBodyBox = new RoundRectangle2D.Float(boxLocation.x + BOX_WIDTH / 2, boxLocation.y
            + STROKE_WIDTH, BOX_WIDTH / 2, boxHeight - (STROKE_WIDTH * 2), BOX_ARC, BOX_ARC);

        // bodyBox is a union of these two shapes
        this.bodyBox = new Area(leftBodyBox);
        this.bodyBox.add(new Area(rightBodyBox));

        // obtain the correct icon
        this.iconImage = DifferenceIconSet.getDifferenceIcon(difference.getType());

        // determine its location
        float iconY = (boxLocation.y + (boxHeight / 2)) - (iconImage.getHeight() / 2);
        this.iconLocation = new Point(boxLocation.x + DIFF_ICON_OFFSET_X, (int) iconY);
    }

    private void initBaseBox(Point boxLocation, float textAreaHeight) {
        // rendering: fill the background of the box with the fill color, draw the
        // box lines on top of that, then cut out a white area for the body in the shape of
        // bodyBox, which is a rounded rectangle with square sides on the top.

        // the height of the box is driven the the text area
        float boxHeight = TITLE_HEIGHT + (STROKE_WIDTH * 2) + textAreaHeight;

        this.marginBox = new RoundRectangle2D.Float(boxLocation.x, boxLocation.y, BOX_WIDTH, boxHeight, BOX_ARC,
            BOX_ARC);

        this.titleSeperator = new Line2D.Float(boxLocation.x, boxLocation.y + TITLE_HEIGHT, boxLocation.x + BOX_WIDTH,
            boxLocation.y + TITLE_HEIGHT);

        Rectangle2D topBodyBox = new Rectangle2D.Float(boxLocation.x + 1, boxLocation.y + TITLE_HEIGHT + STROKE_WIDTH,
            BOX_WIDTH - 1, (textAreaHeight) / 2.0f);

        RoundRectangle2D bottomBodyBox = new RoundRectangle2D.Float(boxLocation.x + 1, boxLocation.y + TITLE_HEIGHT
            + STROKE_WIDTH, BOX_WIDTH - 1, textAreaHeight, BOX_ARC, BOX_ARC);

        // bodyBox is a union of these two shapes
        this.bodyBox = new Area(topBodyBox);
        this.bodyBox.add(new Area(bottomBodyBox));

        // obtain the correct icon
        this.iconImage = DifferenceIconSet.getDifferenceIcon(difference.getType());

        // determine its location
        this.iconLocation = new Point(boxLocation.x + DIFF_ICON_OFFSET_X, boxLocation.y + DIFF_ICON_OFFSET_Y);

        this.annotationIconLocation = new Point(boxLocation.x + BOX_WIDTH - ANNOTATION_ICON_WIDTH - DIFF_ICON_OFFSET_X,
            boxLocation.y + ANNOTATION_ICON_OFFSET_Y);
    }

    private float initWitnessText(Point boxLocation, FontRenderContext fontRenderContext) throws InvalidDataException {
        float textHeight = 0.0f;

        // pull out the data we need for this difference        
        JuxtaDocument witnessDocument = documentManager.lookupDocument(difference.getWitnessDocumentID());
        String body = "";

        if (witnessDocument == null) {
            throw new InvalidDataException("Unable to look up document: " + difference.getWitnessDocumentID());
        }

        if (difference.getType() == Difference.DELETE || difference.getType() == Difference.CHANGE) {
            JuxtaDocument baseDocument = documentManager.lookupDocument(difference.getBaseDocumentID());
            body = baseDocument.getSubString(difference.getOffset(Difference.BASE),
                difference.getLength(Difference.BASE));
        } else {
            body = witnessDocument.getSubString(difference.getOffset(Difference.WITNESS),
                difference.getLength(Difference.WITNESS));
        }

        body = AnnotationManager.filterText(body);

        if (body.length() > 0) {
            float width = BOX_WIDTH - SYMBOL_SIDEBAR_WIDTH - MARGIN_BOX_TEXT_INSET_LEFT - MARGIN_BOX_TEXT_INSET_RIGHT;

            this.bodyTextBox = new WrappingTextLabel(body, DIFF_BODY_FONT, DIFF_TEXT_COLOR, width,
                MARGIN_BOX_TEXT_MAX_LINES, fontRenderContext);

            this.bodyTextBox.setLocation(boxLocation.x + SYMBOL_SIDEBAR_WIDTH + MARGIN_BOX_TEXT_INSET_LEFT,
                boxLocation.y + MARGIN_BOX_TEXT_INSET_Y);

            textHeight = bodyTextBox.getHeight() + (MARGIN_BOX_TEXT_INSET_Y * 2);
        }

        return textHeight;
    }

    private float initBaseText(Point boxLocation, FontRenderContext fontRenderContext) throws InvalidDataException {
        float textHeight = 0.0f;

        // pull out the data we need for this difference        
        JuxtaDocument witnessDocument = documentManager.lookupDocument(difference.getWitnessDocumentID());

        if (witnessDocument == null) {
            throw new InvalidDataException("Unable to look up document: " + difference.getWitnessDocumentID());
        }

        String title = witnessDocument.getDocumentName();
        String body = "";

        if (difference.getType() == Difference.DELETE) {
            JuxtaDocument baseDocument = documentManager.lookupDocument(difference.getBaseDocumentID());
            body = baseDocument.getSubString(difference.getOffset(Difference.BASE),
                difference.getLength(Difference.BASE));
        } else {
            body = witnessDocument.getSubString(difference.getOffset(Difference.WITNESS),
                difference.getLength(Difference.WITNESS));
        }

        body = AnnotationManager.filterText(body);

        // only display the title for base texts
        if (title.length() > 0) {
            float width = BOX_WIDTH - TITLE_TEXT_INSET_X - MARGIN_BOX_TEXT_INSET_LEFT - MARGIN_BOX_TEXT_INSET_RIGHT;

            this.titleTextBox = new WrappingTextLabel(title, DIFF_TITLE_FONT, DIFF_TEXT_COLOR, width, 1,
                fontRenderContext);

            titleTextBox.setLocation(boxLocation.x + TITLE_TEXT_INSET_X + MARGIN_BOX_TEXT_INSET_LEFT, boxLocation.y
                + MARGIN_BOX_TEXT_INSET_Y);
        }

        if (body.length() > 0) {
            float width = BOX_WIDTH - MARGIN_BOX_TEXT_INSET_LEFT - MARGIN_BOX_TEXT_INSET_RIGHT;

            this.bodyTextBox = new WrappingTextLabel(body, DIFF_BODY_FONT, DIFF_TEXT_COLOR, width,
                MARGIN_BOX_TEXT_MAX_LINES, fontRenderContext);

            this.bodyTextBox.setLocation(boxLocation.x + MARGIN_BOX_TEXT_INSET_LEFT, boxLocation.y + TITLE_HEIGHT
                + MARGIN_BOX_TEXT_INSET_Y);

            textHeight = bodyTextBox.getHeight() + (MARGIN_BOX_TEXT_INSET_Y * 2);
        }

        return textHeight;
    }

    public boolean contains(int x, int y) {
        return marginBox.contains(x, y);
    }

    public Point getLocation() {
        Point location = new Point();
        location.x = marginBox.getBounds().x;
        location.y = marginBox.getBounds().y;
        return location;
    }

    public Point getAttachPoint() {
        return attachPoint;
    }

    private void renderShape(Graphics2D g2) {
        g2.setPaint(( highlight) ? DIFF_HIGHLIGHT_FILL_COLOR : DIFF_FILL_COLOR );
        g2.fill(marginBox);

        g2.setPaint(DIFF_STROKE_COLOR);
        g2.draw(marginBox);
    }

    private void renderContents(Graphics2D g2) {
        // render diff icon
        g2.drawImage(iconImage, iconLocation.x, iconLocation.y, null);

        // render annotation marker
        BufferedImage annotationIcon = DifferenceIconSet.getStarIcon(highlight, annotated);
        g2.drawImage(annotationIcon, annotationIconLocation.x, annotationIconLocation.y, null);

        g2.draw(titleSeperator);

        g2.setPaint(Color.WHITE);
        g2.fill(bodyBox);
        g2.setPaint(DIFF_STROKE_COLOR);

        // render text
        if (titleTextBox != null)
            titleTextBox.paint(g2);
        if (bodyTextBox != null)
            bodyTextBox.paint(g2);
    }

    public void paint(Graphics2D g2) {
        g2.setComposite(DRAW_MODE);
        g2.setStroke((highlight) ? HIGHLIGHT_BOX_STROKE : BOX_STROKE);
        renderShape(g2);
        renderContents(g2);
    }

    public boolean testAnnotationHit(Point p) {
        if (p == null)
            return false;

        BufferedImage annotationIcon = DifferenceIconSet.getStarIcon(true, false);
        Rectangle rect = new Rectangle(annotationIconLocation.x, annotationIconLocation.y, annotationIcon.getWidth(),
            annotationIcon.getHeight());
        return rect.contains(p);
    }

    public void setHighlight(boolean hightlight) {
        this.highlight = hightlight;
    }

    public boolean isHighlight() {
        return highlight;
    }

    public Rectangle getBoundingBox() {
        return marginBox.getBounds();
    }

    public boolean isAnnotated() {
        return annotated;
    }

    public void setAnnotated(boolean annotated) {
        this.annotated = annotated;
    }

    public Difference getDifference() {
        return difference;
    }

}
