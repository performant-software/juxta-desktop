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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;

import javax.swing.JPanel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.collation.DifferenceMap;
import edu.virginia.speclab.juxta.author.model.MovesManager;
import edu.virginia.speclab.juxta.author.model.MovesManager.FragmentPair;
import edu.virginia.speclab.juxta.author.model.MovesManagerListener;
import edu.virginia.speclab.util.IntPair;
import edu.virginia.speclab.util.SimpleLogger;

// This handles highlighting on a single side in the Comparison View.
// The following types of highlighting are supported:
// * Regular Difference: all of the differences in this document. The set of items
// to be highlighted are in differenceMap.
// * Hovered Difference: When the mouse hovers over a difference, it turns a different
// color.
// * Selection (focused): This is the actual control's selection, when the control is focused
// * Selection (unfocused): This is the actual control's selection, when the control is not focused
// * Move: all of the moves in the document
// * Emphasis: This is an area that is explicitly requested to be highlighted, for instance, after a Find.
// It is drawn in the same color as the hovered difference.

class DocumentCompareHighlighter implements DifferenceCompareRenderingConstants, MovesManagerListener
{
	private class RectangleHighlighter implements Highlighter.HighlightPainter
	{
		boolean onlyDrawOutline;
		RectangleHighlighter(boolean onlyDrawOutline)
		{
			this.onlyDrawOutline = onlyDrawOutline;
		}
		private boolean isOnSingleLine(Rectangle r0, Rectangle r1)
		{
			return r0.y == r1.y;
		}
		private boolean isOnAdjacentLines(Rectangle r0, Rectangle r1)
		{
			return r0.y+r0.height+1 >= r1.y;
		}
		private boolean isCompletelyToTheLeftOf(Rectangle r0, Rectangle r1)
		{
			return r0.x >= r1.x+r1.width;
		}
		private void setLineColor(Graphics2D g2)
		{
			g2.setColor(new Color(21,125,0));
		}
		private void setFillColor(Graphics2D g2)
		{
			g2.setColor(new Color(230,255,230));
		}
		public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c)
		{
			Graphics2D g2 = (Graphics2D)g;
			Rectangle r0 = null;
			Rectangle r1 = null;
			Rectangle rbounds = bounds.getBounds();
			
			try
			{
				r0 = c.modelToView(p0);
				r1 = c.modelToView(p1);
			} catch (BadLocationException ex) { return; }
			if ((r0 == null) || (r1 == null)) return;

			// There are different cases depending on how much is highlighted.
			if (isOnSingleLine(r0, r1))	// single line selection
			{
				if (!onlyDrawOutline)
				{
					setFillColor(g2);
					g2.fillRect(r0.x, r0.y, r1.x-r0.x+r1.width, r0.height);
				}
				setLineColor(g2);
				g2.drawRect(r0.x, r0.y, r1.x-r0.x+r1.width, r0.height);
			}
			else if (isOnAdjacentLines(r0, r1))
			{
				if (isCompletelyToTheLeftOf(r0, r1))	// split selection on two lines
				{
					if (!onlyDrawOutline)
					{
						setFillColor(g2);
						g2.fillRect(r0.x, r0.y, rbounds.width-r0.x-1, r0.height);
						g2.fillRect(0, r1.y, r1.x, r1.height);
					}
					setLineColor(g2);
					g2.drawRect(r0.x, r0.y, rbounds.width-r0.x-1, r0.height);
					g2.drawRect(0, r1.y, r1.x, r1.height);
				}
				else	// overlapping selection on two lines
				{
					Area area = new Area();
					area.add(new Area(new Rectangle(r0.x, r0.y, rbounds.width-r0.x-1, r0.height)));
					area.add(new Area(new Rectangle(0, r1.y, r1.x, r1.height)));
					if (!onlyDrawOutline)
					{
						setFillColor(g2);
						g2.fill(area);
					}
					setLineColor(g2);
					g2.draw(area);
				}
			}
			else	// multiple lines
			{
				Area area = new Area();
				area.add(new Area(new Rectangle(r0.x, r0.y, rbounds.width-r0.x-1, r0.height)));
				area.add(new Area(new Rectangle(0, r0.y+r0.height, rbounds.width-1, r1.y-r0.y-r0.height)));
				area.add(new Area(new Rectangle(0, r1.y, r1.x, r1.height)));
				if (!onlyDrawOutline)
				{
					setFillColor(g2);
					g2.fill(area);
				}
				setLineColor(g2);
				g2.draw(area);
			}
		}
	}
	
	// definition of what to highlight
    private DifferenceMap differenceMap;
    private Difference differenceHovered;
	private boolean hasEmphasis;
	private int emphasisStart;
	private int emphasisEnd;
	
	// list of permanent search highlights
	private IntPair[] searchHighlights;

    private int textType;
    private MovesManager.MoveList moveList;
	private int idBase;
	private int idWitness;
    
    private JTextComponent textArea;

    private DefaultHighlighter.DefaultHighlightPainter differencePainter;
    private DefaultHighlighter.DefaultHighlightPainter highlightPainter;
    private DefaultHighlighter.DefaultHighlightPainter searchResultPainter;
    private DefaultHighlighter.DefaultHighlightPainter selectionPainter;
    private DefaultHighlighter.DefaultHighlightPainter selectionPainterFocus;
    private RectangleHighlighter movePainter;
    private RectangleHighlighter movePainterOutline;
    private DefaultHighlighter.DefaultHighlightPainter allSearchResultPainter;
	//private boolean grayedOut;
	//private int fragStart, fragEnd;
	
	public DocumentCompareHighlighter( JTextComponent textArea )
	{
        this.textArea = textArea;
        differencePainter = new DefaultHighlighter.DefaultHighlightPainter(FIRST_COLOR);
        highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(FIRST_COLOR_BRIGHTEST);
        searchResultPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);
        allSearchResultPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
        
        JPanel jp = new JPanel();
        Color b = jp.getBackground();
        selectionPainter = new DefaultHighlighter.DefaultHighlightPainter(b);
        selectionPainterFocus = new DefaultHighlighter.DefaultHighlightPainter(textArea.getSelectionColor());
        //grayedOut = false;
        movePainter =  new RectangleHighlighter(false);
        movePainterOutline =  new RectangleHighlighter(true);

        DefaultHighlighter highlighter = (DefaultHighlighter)textArea.getHighlighter();        
		highlighter.setDrawsLayeredHighlights(false);
	}
    
    private void clear()
    {
        Highlighter highlighter = textArea.getHighlighter();        
        highlighter.removeAllHighlights();
     }

	public void movesChanged(MovesManager movesManager) {
		if (movesManager == null)
			moveList = null;
		else
			moveList = movesManager.getAllMoves(idBase, idWitness);
		renderAllHighlighting();
	}

	private void highlightSelection()
	{
        Highlighter highlighter = textArea.getHighlighter();        
		// We need to draw the selection ourselves or it will disappear.
        // Another reason to draw it ourselves is to put it in the right Z-order.
        int selStart = textArea.getSelectionStart();
        int selEnd = textArea.getSelectionEnd();
        //System.out.println("Sel: " + selStart + "," + selEnd);
        if (selStart < selEnd)
        {
        	try 
        	{
        		if (textArea.hasFocus())
        			highlighter.addHighlight(selStart, selEnd, selectionPainterFocus);
        		else
        			highlighter.addHighlight(selStart, selEnd, selectionPainter);
		    } 
        	catch (BadLocationException e) 
        	{
        		SimpleLogger.logError("Attempted to highlight bad location: "+e);
        	}        
        }
 	}
       
	private void highlightMove(RectangleHighlighter painter)
	{
       if (moveList == null)
    	   return;

		Highlighter highlighter = textArea.getHighlighter();
		try 
		{
			for (int i = 0; i < moveList.size(); ++i)
			{
				FragmentPair fp = moveList.get(i);
				if (textType == Difference.BASE)
					highlighter.addHighlight(fp.first.getStartOffset(OffsetRange.Space.ACTIVE), fp.first.getEndOffset(OffsetRange.Space.ACTIVE)+1, painter);
				else
					highlighter.addHighlight(fp.second.getStartOffset(OffsetRange.Space.ACTIVE), fp.second.getEndOffset(OffsetRange.Space.ACTIVE)+1, painter);
			}
		} 
		catch (BadLocationException e) 
		{
			SimpleLogger.logError("Attempted to highlight bad location: "+e);
		}        
 	}
	
	private void highlightSearches()
	{
       if (searchHighlights == null)
    	   return;

		Highlighter highlighter = textArea.getHighlighter();
		try 
		{
			for (int i = 0; i < searchHighlights.length; ++i)
					highlighter.addHighlight(searchHighlights[i].x, searchHighlights[i].y, allSearchResultPainter);
		} 
		catch (BadLocationException e) 
		{
			SimpleLogger.logError("Attempted to highlight bad location: "+e);
		}        
 	}
	
	private void addHighlight( int start, int end )
	{
	    try 
	    {
	        // Highlight the selected position
            Highlighter highlighter = textArea.getHighlighter();
			highlighter.addHighlight(start, end, differencePainter);
	    } 
	    catch (BadLocationException e) 
	    {
	        SimpleLogger.logError("Attempted to highlight bad location: "+e);
	    }        
	}
	
    public void redrawSelection()
    {
    	renderAllHighlighting();
    }

    public void clearHighlight()
    {
    	differenceHovered = null;
    	renderAllHighlighting();
    }
    
	public void highlightDifference( Difference difference)
	{
		differenceHovered = difference;
		//textTypeHovered =textType;
		renderAllHighlighting();
	}

	private void renderAllHighlighting()
	{
		clear();
		highlightMove(movePainter);
        renderComparisonHighlights();
		highlightSearches();
		renderHighlightedRange();
        highlightSelection();
        renderHoveredDifference();               
		highlightMove(movePainterOutline);
	}
    
	private void renderHoveredDifference()
	{
		if( differenceHovered != null )
        {
            try 
            {
                // get the offsets for the appropriate text
                int start = differenceHovered.getOffset(textType);
                int end = start + differenceHovered.getLength(textType);
                if ( end == start ) {
                    int max = this.textArea.getDocument().getLength();
                    end++;
                    
                    if ( end > max ) {
                        end--;
                        start--;
                    }
                }

                // Highlight the selected position                
                Highlighter highlighter = textArea.getHighlighter();
                highlighter.addHighlight(start, end, highlightPainter);
            } 
            catch (BadLocationException e) 
            {
                SimpleLogger.logError("Attempted to highlight bad location: "+e);
            }
        }
	}
    
	private void renderComparisonHighlights()
	{
        if( differenceMap == null ) return;
        	    
        int start = 0;
        int end = 0;
        boolean inRun = false;
        
        int documentLength = differenceMap.getLength();
        
        // run length encode frequency data to generate highlighting        
	    for( int i=0; i <= documentLength; i++ )
	    {
            // there is something to highlight here
	        if ( differenceMap.differencePresent(i) )
	        {                
                // check to see if we are in the midst of a run
                if( inRun )
                {
				    end++;
                }
                else
                {
                    // if we aren't in the middle of a run, start a new one
                    start = i;
                    end = i+1;
					inRun = true;
                }
            }
            else
            {
                // if we are in the midst of a run, end it
                if( inRun )
                {
                    addHighlight(start,end);
                    start = end = -1;
                    inRun = false;
                }
            }
	    }
	}

    public void updateModel( DifferenceMap map, int textType, int idBase, int idWitness, MovesManager movesManager  )
    {
    	this.idBase = idBase;
    	this.idWitness = idWitness;
    	this.textType = textType;
        this.differenceMap = map;        
    	differenceHovered = null;
    	movesChanged(movesManager);
        renderAllHighlighting();
    }

//    public boolean isHighlightOn()
//    {
//        return highlightOn;
//    }

    public void setSearchHighlights(IntPair[] searchHighlights)
    {
    	this.searchHighlights = searchHighlights;
    }
    
	public void highlightRange(int start, int end, boolean selected) 
	{
		hasEmphasis = selected;	// TODO: currently this is only called with selected=true
		emphasisStart = start;
		emphasisEnd = end;
    	differenceHovered = null;
		renderAllHighlighting();
	}  
	
	public void clearHighlightRange()
	{
		hasEmphasis = false;
    	differenceHovered = null;
		renderAllHighlighting();
	}
	
	private void renderHighlightedRange() {
		if (hasEmphasis) {
			try {
				// Highlight the selected position
				Highlighter highlighter = textArea.getHighlighter();
				highlighter.addHighlight(emphasisStart, emphasisEnd,
						searchResultPainter);
			} catch (BadLocationException e) {
				SimpleLogger.logError("Attempted to highlight bad location: "
						+ e);
			}
		}
//		if (grayedOut) {
//			try {
//				// Highlight the selected position
//				DefaultHighlighter.DefaultHighlightPainter disabledTextPainter = 
//					new DefaultHighlighter.DefaultHighlightPainter(DISABLED_TEXT_COLOR);
//				Highlighter highlighter = textArea.getHighlighter();
//				highlighter.addHighlight(0, fragStart, disabledTextPainter);
//				highlighter.addHighlight(fragEnd, textArea.getText().length(), disabledTextPainter);
//			} catch (BadLocationException e) 
//			{
//				SimpleLogger.logError("Attempted to highlight bad location: " + e);
//			}
//		}
	}
	
//	public void greyOut(int fragStart, int fragEnd) 
//	{
//		grayedOut = true;
//		this.fragStart = fragStart;
//		this.fragEnd = fragEnd;
//		clear();
//		renderAllHighlighting();
//	}
//
//	public void clearGreyOut() 
//	{
//		grayedOut = false;	
//	} 
    
}
