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
 

package edu.virginia.speclab.juxta.author.view.compare;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;


class DifferenceConnector implements DifferenceCompareRenderingConstants
{
    private int leftPosition, leftLength;
    private int rightPosition, rightLength;
    private int connectorLaneWidth;
	private int baseTextOffset;
    private Rectangle2D viewRect;
    
    private boolean useDashStroke;
    private boolean useThickStroke;
    private int horizontalBarLength;
    
    public static final float ANCHOR_POINT_SQUARE_WIDTH = 2.0f;
    public static final int DIFFERENCE_STYLE = 0;
    public static final int MOVE_STYLE = 1;
	
    public DifferenceConnector( int baseTextOffset, int leftPosition,  int leftLength, int rightPosition, int rightLength, int style )
    {
		this.baseTextOffset = baseTextOffset;
        this.leftPosition = leftPosition; 
        this.leftLength = leftLength;
        this.rightPosition = rightPosition;
        this.rightLength = rightLength;
        
        useDashStroke = (style == DIFFERENCE_STYLE);
        useThickStroke = (style != DIFFERENCE_STYLE);
        horizontalBarLength = (style == DIFFERENCE_STYLE)?HORIZONTAL_BAR_LENGTH:HORIZONTAL_BAR_LENGTH_MOVE;
        
        // constructed once to save the heap from rapid allocation
        this.viewRect = new Rectangle2D.Float();
    }
    
    
    private boolean isVisible( int connectorLeftPosition, int connectorRightPosition, int viewWidth, int viewHeight )
    {
        int connectorLeftEnd = connectorLeftPosition+leftLength;
        int connectorRightEnd = connectorRightPosition+rightLength;
        
        // update clip rectangle for target view
        viewRect.setRect(0,0,viewWidth,viewHeight);

        if( connectorLeftEnd < connectorRightEnd )
        {
            return viewRect.intersectsLine(0,connectorLeftPosition,viewWidth,connectorRightEnd);
        }
        else
        {
            return viewRect.intersectsLine(0,connectorRightPosition,viewWidth,connectorLeftEnd);   
        }
    }
        
    public void render( Graphics2D g2, int connectorLaneWidth, int viewHeight, int leftViewPosition, int rightViewPosition )
    {
        int connectorLeftPosition = leftPosition - leftViewPosition;
        int connectorRightPosition = rightPosition - rightViewPosition;
        
        // if any part of the connector is visible in the view, render it
        if( isVisible(connectorLeftPosition,connectorRightPosition,connectorLaneWidth,viewHeight) )
        {
            this.connectorLaneWidth = connectorLaneWidth;
            
            // draw brackets if the connector has any length, otherwise draw points
            if( leftLength > 0 ) drawBracket(g2,leftLength,connectorLeftPosition,LEFT);
            else drawAnchorPoint(g2, connectorLeftPosition, LEFT );
            if( rightLength > 0 ) drawBracket(g2,rightLength,connectorRightPosition,RIGHT);
            else drawAnchorPoint(g2, connectorRightPosition, RIGHT );
            
            // draw a line connecting the brackets or points
            drawLine(g2,(leftLength/2)+connectorLeftPosition,(rightLength/2)+connectorRightPosition);
        }
    }
    
    private void drawLine( Graphics2D g2, int leftPosition, int rightPosition )
    {
        Stroke oldStroke = g2.getStroke();
        g2.setColor(FIRST_COLOR_DARKER);
        if (useDashStroke)
        	g2.setStroke(DASH_STROKE);
        g2.drawLine(horizontalBarLength+DIFFERENCE_MARK_MARGIN,
                    leftPosition,
                    connectorLaneWidth - DIFFERENCE_MARK_MARGIN - horizontalBarLength,
                    rightPosition );
        g2.setStroke(oldStroke);
    }
    
    private void drawAnchorPoint(Graphics2D g2, int position, int orientation )
    {
        int horizontalBarX;
        
        if( orientation == LEFT )
        {
            horizontalBarX = DIFFERENCE_MARK_MARGIN +3;                    
        }
        else
        {
            horizontalBarX = connectorLaneWidth - DIFFERENCE_MARK_MARGIN - 4;
        }
        
        Point anchorPoint = new Point(horizontalBarX,position);
        
        Rectangle2D anchorDot = new Rectangle2D.Float( anchorPoint.x, anchorPoint.y - (ANCHOR_POINT_SQUARE_WIDTH/2.0f),
                ANCHOR_POINT_SQUARE_WIDTH,ANCHOR_POINT_SQUARE_WIDTH );
        
        // make it crisp
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.fill(anchorDot);        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    
    private void drawBracket( Graphics2D g2, int height, int location, int orientation )
    {
    	Stroke oldStroke = g2.getStroke();
    	g2.setColor(FIRST_COLOR_DARKER);
    	if (useThickStroke)
    		g2.setStroke(THICK_STROKE);

    	if( orientation == LEFT )
    	{
    		int horizontalBarX = DIFFERENCE_MARK_MARGIN;                    

    		// draw horizontal bars
    		g2.drawLine(horizontalBarX,location,horizontalBarX+horizontalBarLength,location);
    		g2.drawLine(horizontalBarX,location+height,horizontalBarX+horizontalBarLength,location+height);

    		// draw vertical bar
    		g2.drawLine(horizontalBarX+horizontalBarLength,location,horizontalBarX+horizontalBarLength,location+height);
    	}        
    	else
    	{
    		int horizontalBarX = connectorLaneWidth - DIFFERENCE_MARK_MARGIN;                    

    		// draw horizontal bars
    		g2.drawLine(horizontalBarX,location,horizontalBarX-horizontalBarLength,location);
    		g2.drawLine(horizontalBarX,location+height,horizontalBarX-horizontalBarLength,location+height);

    		// draw vertical bar
    		g2.drawLine(horizontalBarX-horizontalBarLength,location,horizontalBarX-horizontalBarLength,location+height);
    	}
    	g2.setStroke(oldStroke);
    }

    public int getLeftPosition()
    {
        return leftPosition;
    }    

    public int getRightPosition()
    {
        return rightPosition;
    }

	public int getLeftLength() 
	{
		return leftLength;
	}
	
	public int getRightLength() 
	{
		return rightLength;
	}


	public int getBaseTextOffset() {
		return baseTextOffset;
	}
	
    
}
