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
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

class AnchorPoint implements RenderingConstants
{
    private boolean highlight;
    private Point anchorPoint, attachPoint;
    private int rightMarginBoundary;
    
    private Rectangle boundingBox; 
    
    private static final float DASH[] = {2.0f}; 

    private static final BasicStroke DASH_STROKE = new BasicStroke(STROKE_WIDTH, BasicStroke.CAP_BUTT,  
            BasicStroke.JOIN_MITER, 10.0f, DASH, 0.0f); 

    private static final BasicStroke HIGHLIGHT_STROKE = new BasicStroke(HIGHLIGHT_STROKE_WIDTH,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND); 
    
    public AnchorPoint( Point anchorPoint, int rightMarginBoundary, Point attachPoint )
    {
        this.attachPoint = attachPoint;
        this.rightMarginBoundary = rightMarginBoundary;
        this.anchorPoint = anchorPoint;
        
        computeBoundingBox();
    }
    
    private void computeBoundingBox()
    {
        boundingBox = new Rectangle();        
        boundingBox.add(attachPoint);
        boundingBox.add(anchorPoint);
        boundingBox.add(getAnchorDot());
    }
    
    private Rectangle2D getAnchorDot()
    {
        return new Rectangle2D.Float( anchorPoint.x, anchorPoint.y - (ANCHOR_POINT_SQUARE_WIDTH/2.0f),
                                      ANCHOR_POINT_SQUARE_WIDTH,ANCHOR_POINT_SQUARE_WIDTH );
    }

    private void drawAnchorPoint(Graphics2D g2)
    {
        Rectangle2D anchorDot = getAnchorDot();
        
        // make it crisp
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.fill(anchorDot);        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private void drawDashedLine( Graphics2D g2, Point lineStart, Point lineEnd )
    {
    	Line2D line = new Line2D.Float( lineStart.x, lineStart.y, lineEnd.x, lineEnd.y ); 
    	g2.draw(line); 		
    }
    
    private void render( Graphics2D g2 )
    {
        // calculate a straight line to the margin
        Point lineStart = anchorPoint;
        Point lineEnd = new Point( rightMarginBoundary + LINE_EXTENSION_BEYOND_MARGIN, lineStart.y );
    
        // set drawing color
        g2.setPaint(DIFF_STROKE_COLOR);
    
        // draw the anchor point dot
        drawAnchorPoint( g2 );
    
        // is box lined up with anchor point?
        if( lineEnd.equals(attachPoint) )
        {
            // if so, draw a single straight line to the box
            drawDashedLine(g2,lineStart,lineEnd);
        }
        else
        {
            // otherwise, draw a horizontal line to the margin 
            lineEnd.x = rightMarginBoundary;
            drawDashedLine(g2,lineStart,lineEnd);
        
            // if the box is too far away, just draw a pair of arrows
            if( Math.abs(attachPoint.y - lineEnd.y) > MAX_BOX_DISTANCE_FROM_LINE )
            {
                drawArrows(g2,lineEnd,attachPoint); 
            }
            // otherwise, draw a diagonal line to the box
            else
            {
                drawDashedLine(g2,lineEnd,attachPoint);
            }
        }
    }

    private void drawArrows(Graphics2D g2, Point lineEnd, Point attachPoint )
    {
        BufferedImage downArrow = DifferenceIconSet.getDownArrow();
        g2.drawImage(downArrow,lineEnd.x,lineEnd.y,null);
        
        BufferedImage upArrow = DifferenceIconSet.getUpArrow();
        g2.drawImage(upArrow,attachPoint.x-upArrow.getWidth(),attachPoint.y-upArrow.getHeight(),null);        
    }

    public void paint( Graphics2D g2 )
    {
        // render 
        g2.setComposite(DRAW_MODE);
        g2.setStroke( (highlight) ? HIGHLIGHT_STROKE : DASH_STROKE );
        render(g2);        
    }

    public void setHighlight(boolean hightlight)
    {
        this.highlight = hightlight;
    }

    public boolean isHighlight()
    {
        return highlight;
    }

    public Rectangle getBoundingBox()
    {
        return boundingBox.getBounds();
    }
    
}
