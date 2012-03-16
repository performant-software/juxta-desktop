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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.View;

import edu.virginia.speclab.util.SimpleLogger;

/**
 * Couples a read only text area with a drawing surface for easy drawing over the text surface.
 * Margins can be set to provide drawable space around the text area. 
 * @author Nick
 *
 */
public class MarkableTextPanel extends JTextPane
{  
	private int leftMarginWidth, rightMarginWidth, topMarginHeight, bottomMarginHeight;
    
    public MarkableTextPanel( int leftMarginWidth, int rightMarginWidth, int topMarginHeight, int bottomMarginHeight )
    {        
        this.leftMarginWidth = leftMarginWidth;
        this.rightMarginWidth = rightMarginWidth; 
        this.topMarginHeight = topMarginHeight;
        this.bottomMarginHeight = bottomMarginHeight;
        
        initTextArea();
    }
    
    /**
     * Returns an offset into the text for a given point on the surface.
     * @param p
     * @return
     */
    public int getOffset( Point p )
    {
        return viewToModel(p); 
    }
	
    public void paint( Graphics g )
    {
        super.paint(g);
    }
	
    public Rectangle getLocation( int offset )
    {
        Rectangle rect = null;
        
        try 
        {
            // locate where the difference is found on the drawing surface
            rect = modelToView(offset);
        } 
        catch (BadLocationException e) 
        {
            SimpleLogger.logError("Bad location offset.");
            return null;
        }
        
        return rect;
    }
	
    /**
     * At a minimum, the drawing surface is always the height of the text area plus the height of 
     * the top and bottom margins combined. The height of the drawing surface can be further 
     * extended below the bottom margin with this method. 
     * 
     * @param extension The height of the extension. If less than or equal to zero, the existing
     * extension is removed.
     */
    public void extendDrawingSurfaceHeight( int extendedHeight )
    { 
        // figure out how tall the text is
        View v = getUI().getRootView(this);
        int pw = getParent().getWidth();
        v.setSize(pw, Integer.MAX_VALUE);        
        int preferredHeight = (int)v.getPreferredSpan(View.Y_AXIS)+ this.topMarginHeight + this.bottomMarginHeight;  
        if ( extendedHeight > preferredHeight ) {
            Dimension prefferedSize = new Dimension(pw, extendedHeight);         
    		setPreferredSize(prefferedSize);
    		revalidate();
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
                    SimpleLogger.logError("Unable to scroll MarableTextPanel to offset: " + offset);
                }
            }

        });
    }
    
    /**
     * Get the height of the margin below the text.
     * @return
     */
	public int getBottomMarginHeight() 
    {
		return bottomMarginHeight;
	}

    /**
     * Get the width of the margin to the left of the text.
     * @return
     */
	public int getLeftMarginWidth() 
    {
		return leftMarginWidth;
	}
	
    /**
     * Get the width of the margin to the right of the text.
     * @return
     */
	public int getRightMarginWidth()
    {
		return rightMarginWidth;
	}
	
    /**
     * Get the height of the margin above the text.
     * @return
     */
	public int getTopMarginHeight() 
    {
		return topMarginHeight;
	}
    
    public void scrollToPosition( float position ) 
    {
        if( getText() == null ) return;
        
        int textLength = getText().length();
        int targetPosition = Math.round((float)textLength * position);
                
        try
        {
            Rectangle comparandRect= modelToView(targetPosition);            
            Rectangle viewRect = getVisibleRect();

            if( viewRect != null && comparandRect != null )
            {
                comparandRect.height = viewRect.height;
                if( comparandRect.y < 0 ) comparandRect.y = 0;
                scrollRectToVisible(comparandRect);
            }
        } 
        catch (BadLocationException e)
        {
            SimpleLogger.logError("Unable to scroll to position: "+ position);
        }
    }

    /**
     * Installs the <code>JTextArea</code> component in the center of the panel.
     * @param textArea The <code>JTextArea</code> to install.
     */
    private void initTextArea()
    {
        // no editing!
        setEditable(false);
        setFocusable(false);   
		setOpaque(false);
        
        this.setMargin( new Insets(this.topMarginHeight,
                              this.leftMarginWidth,
                              this.bottomMarginHeight,
                              this.rightMarginWidth    ));        
    }

}
