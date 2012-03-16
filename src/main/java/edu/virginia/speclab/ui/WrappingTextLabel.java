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
 

package edu.virginia.speclab.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class implements a word wrapped text label rendered with Java 2D. It attempts to render 
 * the text at a given wrap width for a given number of lines. If it runs out of space, it 
 * renders as much of the text as it can and then appends an elipse "..." to the end of the rendered
 * text. 
 * 
 * @author Nick
 */
public class WrappingTextLabel
{
    private Color color;
    private Font font;
    private FontRenderContext fontRenderContext;
    private Point2D location;
    private LinkedList textLines;
    private int maxLines;
    
    private float height; 
    
    public WrappingTextLabel( String text, Font font, Color color, float width, int maxLines, FontRenderContext fontRenderContext )
    {
        this.color = color;
        this.font = font;
        this.fontRenderContext = fontRenderContext;
        this.maxLines = maxLines;
		constructLineList(text,width);        
	}
	
	public double getLineLength( int lineNumber )
	{
		if( textLines == null || textLines.size() <= lineNumber ) return 0;
		TextLayout line = (TextLayout) textLines.get(lineNumber);
		Rectangle2D bounds = line.getBounds();
		return bounds.getWidth();
	}
    
    private void constructLineList( String text, float width )
    {            
        LinkedList lineList = new LinkedList();

        AttributedString as = new AttributedString(text);           
        as.addAttribute( TextAttribute.FONT, font );
        AttributedCharacterIterator aci = as.getIterator();
        
        LineBreakMeasurer lbm = new LineBreakMeasurer(aci,fontRenderContext);
        
        int lineCount=0, lineStart=0, lineEnd=0;
        
        // while there is more text and we have not yet reached max lines
        while (lbm.getPosition() < aci.getEndIndex() && lineCount < maxLines ) 
        {
            // break out next line
            lineStart = lineEnd;
            TextLayout textLayout = lbm.nextLayout(width);
            lineEnd = lbm.getPosition();
            
            // add line to the list
            lineList.add(textLayout);
            lineCount++;
        }

        // if there is still more text
        if( lbm.getPosition() < aci.getEndIndex() )
        {
            // replace the last line with an ellipsed last line
            String ellipse = "...";
            
            // if there is enough room for an ellipse, insert one, otherwise, this 
            // must be a pretty short line so don't worry about it.
            if( lineEnd-ellipse.length() > lineStart)
            {
                String cutText = text.substring(lineStart,lineEnd-3) + ellipse;
                TextLayout singleLine = new TextLayout(cutText,font,fontRenderContext);
                lineList.removeLast();
                lineList.add(singleLine);
            }
	    }
        
        // finished, set the values for painting
        this.height = calculateHeight(lineList);
        this.textLines = lineList;
    }
    
    private float calculateHeight( List lines )
    {
        float height = 0;
        
        for( Iterator i = lines.iterator(); i.hasNext(); )
        {
            TextLayout textLayout = (TextLayout) i.next();
            height += textLayout.getAscent() + textLayout.getDescent() + textLayout.getLeading();
        }
        
        return height;
    }
  
	public void paint(Graphics2D g2)
	{
	    if( textLines == null ) return;
	    
        Color oldColor = g2.getColor();
        
        g2.setColor(color);
        
		float y = (float)location.getY();
		for( Iterator i = textLines.iterator(); i.hasNext(); )
        {			    		    
            TextLayout textLayout = (TextLayout) i.next();

            y += textLayout.getAscent();
            textLayout.draw(g2,(float)location.getX(),y);
            y += textLayout.getDescent() + textLayout.getLeading();
        }	
                
        g2.setColor(oldColor);
	}
    
    public float getHeight()
    {
        return height;
    }

    public void setLocation( float x, float y )
    {
        this.location = new Point2D.Float(x,y);
    }

    public Point2D getLocation()
    {
        return location;
    }
}
