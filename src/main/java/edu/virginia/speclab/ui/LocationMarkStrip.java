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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

public class LocationMarkStrip extends JPanel 
{
	private JTextComponent textArea;
	private int lineHeight;
	private boolean selfScrolling;
	private boolean lineOnLeft;
    private HashMap lineMarkMap;

    public enum Position { LEFT, RIGHT };
    
	private ResizeListener resizeListener;

	public static int DEFAULT_LINE_HEIGHT = 15;
	public static final Font DEFAULT_FONT = new Font("Verdana",Font.PLAIN,10);
	private int scrollPosition;
    private int maxMarkLength;
    private static int LEFT_MARGIN = 3;
	
	public LocationMarkStrip( JTextComponent text, boolean selfScrolling, Position position )
	{
        this.lineMarkMap = new HashMap(10000);
		this.selfScrolling = selfScrolling;
		this.lineOnLeft = (position == Position.LEFT);
		this.resizeListener = new ResizeListener();
        this.maxMarkLength = 0;
		setTextArea(text);
	}
	
	public void setTextArea( JTextComponent newTextArea )
	{
		if( textArea != null )
		{
			textArea.removeComponentListener(resizeListener);	
		}
		
		if( newTextArea != null )
		{
			newTextArea.addComponentListener(resizeListener);	
		}
		
		this.textArea = newTextArea;
	}
	
	// based on number of digits in max line #
	private int getTargetWidth()
	{
		return 10*maxMarkLength+5;
	}
	
	public void paint( Graphics g )
	{
		super.paint(g);
		
		Graphics2D g2 = (Graphics2D) g;
		
		g2.setColor(Color.GRAY);
		g2.setFont(DEFAULT_FONT);
		
		int lineLocation = lineOnLeft?LEFT_MARGIN:getTargetWidth()-1;
		g2.drawLine(lineLocation,0,lineLocation,getHeight());
		
		if( selfScrolling )
			drawSelfScrollingLineNumbers(g2);
		else
			drawLineNumbers(g2);
	}

    private void drawLineNumbers(Graphics2D g2) 
	{		
		int lineCount = getLineCount();
		for( int i = 0; i <= lineCount; i++ )
		{
			int position = (i*lineHeight)-4; 
            
            String lineMark = (String) lineMarkMap.get(new Integer(i));
            if( lineMark != null )
            {
                g2.drawString(lineMark,getLeftEdgeOfText(),position);    
            }				
		}
	}

	private int getLeftEdgeOfText() {
		int x = lineOnLeft?LEFT_MARGIN*2:2;
		return x;
	}

	public void recalculateLineHeight()
    {
		if( textArea == null )
		{
			this.lineHeight = 0;
			return;
		}
		
		Font font = textArea.getFont();
		this.lineHeight = textArea.getFontMetrics(font).getHeight();
        if( this.lineHeight == 0 ) 
            this.lineHeight = DEFAULT_LINE_HEIGHT; // fall back if unable to get line height
    }
    
    /**
     * Obtain the number of display lines of text in the specified panel.
     * @param which either <code>DocumentCompareView.LEFT</code> or <code>DocumentCompareView.RIGHT</code>
     * @return The total number of lines in the document, at the current font and window size.
     */
    public int getLineCount()
    {
		if( textArea == null ) return 0;
		
        int textHeight = textArea.getHeight();
		
		if( lineHeight != 0 )
			return textHeight/lineHeight;
		else return 0;
    }
	
	/**
	 * If the <code>LineNumberStrip</code> was initialized for self-scrolling,
	 * this value controls the scroll position.
	 * @param position
	 */
	public void setScrollPosition( int position )
	{
		this.scrollPosition = position;
	}
	
	private void drawSelfScrollingLineNumbers(Graphics2D g2) 
	{	
		if( textArea == null ) return;
		
		int startingLine = scrollPosition / lineHeight;
		int offset = scrollPosition % lineHeight;
		int numberOfLines = (textArea.getHeight() / lineHeight)+1;

		for( int i = 0; i <= numberOfLines; i++ )
		{
			int position = (i*lineHeight)-offset-4;
            
            String lineMark = (String) lineMarkMap.get(new Integer(i+startingLine));
            if( lineMark != null )
            {
                g2.drawString(lineMark,getLeftEdgeOfText(),position);    
            }           			
		}
	}
    
    public void addLineMark( int lineNumber, String text )
    {
        //TODO
        lineMarkMap.put( new Integer(lineNumber), text );
        
        if( text.length() > maxMarkLength )
        {
            maxMarkLength = text.length();
            if( textArea != null )
                setPreferredSize( new Dimension( getTargetWidth(), textArea.getHeight() ));
        }

    }

	public int getLineHeight() 
	{
		return lineHeight;
	}
	
	private class ResizeListener extends ComponentAdapter 
	{
		public void componentResized(ComponentEvent e) 
		{
			if( textArea != null )
				setPreferredSize( new Dimension( getTargetWidth(), textArea.getHeight() ));
		}
	}

    public void clearLineMarks()
    {
        lineMarkMap.clear();    
        this.maxMarkLength = 0;
    }
}
