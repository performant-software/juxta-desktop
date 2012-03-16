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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.virginia.speclab.diff.Difference;
import edu.virginia.speclab.diff.collation.Collation;
import edu.virginia.speclab.exceptions.ErrorHandler;
import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.model.JuxtaSession;
import edu.virginia.speclab.juxta.author.model.JuxtaSessionListener;
import edu.virginia.speclab.juxta.author.view.compare.DocumentCompareViewListener;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.histograph.ColorScale;
import edu.virginia.speclab.ui.histograph.HistogramGraphModel;
import edu.virginia.speclab.ui.histograph.HistogramPanel;

public class DifferenceHistogramDialog extends JDialog implements JuxtaUserInterfaceStyle, 
                                                                  JuxtaSessionListener, 
                                                                  DocumentCompareViewListener,
                                                                  ChangeListener
{   
    private JuxtaAuthorFrame frame;
    private JuxtaSession session;
    private HistogramPanel histogramPanel;    
    
    private BufferedImage surface;
    
    private FilterStrengthSlider filterStengthSlider;
    private SelectionTracker tracker; 
      
    private int currentMode;
    private JuxtaDocument baseDocument,witnessDocument;
    private Collation currentCollation;
    
    public DifferenceHistogramDialog( JuxtaAuthorFrame frame )    
    {
        super(frame);
        this.frame = frame;
        
        currentMode = JuxtaAuthorFrame.VIEW_MODE_COLLATION;
        
        if( frame != null ) frame.addScrollListener(this);
        
        setTitle("Collation Histogram");

        ColorScale colorScale = pickColorScale(); 
        histogramPanel = new HistogramPanel(colorScale);
        histogramPanel.setSelectionAreaColor(FIRST_COLOR_DARKER_WITH_ALPHA);
        filterStengthSlider = new FilterStrengthSlider();
        
        getContentPane().setLayout( new BorderLayout() );
        getContentPane().add(histogramPanel, BorderLayout.CENTER );
        getContentPane().add(filterStengthSlider,BorderLayout.SOUTH );
        setVisible(false);

        tracker = new SelectionTracker();
        histogramPanel.addMouseListener(tracker);
        histogramPanel.addMouseMotionListener(tracker);
        
        addComponentListener(new ComponentAdapter() {
            public void componentHidden(ComponentEvent e) {
                close();
            }
            public void componentResized(ComponentEvent e)
            {
                surface = createOffScreenBuffer( getSize() );
            }
        });
    }

    // create the offscreen buffer
    private BufferedImage createOffScreenBuffer( Dimension size )
    {
        if( size == null ) return null;
        
        GraphicsEnvironment local = 
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice screen = local.getDefaultScreenDevice();
        GraphicsConfiguration conf = screen.getDefaultConfiguration();
        return conf.createCompatibleImage(size.width, size.height, Transparency.TRANSLUCENT);
    }
        
    public void setSession( JuxtaSession session )
    {
        if( this.session != null ) this.session.removeListener(this);
        if( session != null ) session.addListener(this);
        this.session = session;    
        repaint();
    }
    
    public void close()
    {
        setVisible(false);
    }
    
    private void display()
    {
        setBounds(frame.getX()+(frame.getWidth()/4), 
                frame.getY()+(frame.getHeight()/4), 310, 180);
      
        setVisible(true);
    }

    // just get the darker half of the color scale
    private ColorScale pickColorScale()
    {
        int length = JuxtaUserInterfaceStyle.SECOND_COLOR_SCALE.length/2;
        Color[] colorSet = new Color[length];
        
        for( int i=0; i < length; i++ )
        {
            colorSet[i] = JuxtaUserInterfaceStyle.SECOND_COLOR_SCALE[i+length];        
        }
        
        ColorScale colorScale = new ColorScale(colorSet);
        
        return colorScale;
    }

    public void sessionModified()
    {
        // do nothing        
    }
    
    public void paint( Graphics g )
    {
        if( surface != null )
        {
        	// If we're growing, then we might need to resize
        	Dimension size = getSize();
        	if ((size.width > surface.getWidth()) || (size.height > surface.getHeight()))
        	{
        		// make it a little bigger that we asked, because the user might be dragging larger at the moment, and we won't have
        		// to keep reallocating.
        		size.setSize(size.width+100, size.height+100);
        		surface = createOffScreenBuffer(size);
        	}
        	
            // render to offscreen surface and then copy complete image to screen
            Graphics offScreenSurface = surface.getGraphics();
            super.paint(offScreenSurface);            
            g.drawImage(surface,0,0,surface.getWidth(),surface.getHeight(),null);
        }
        else
        {
            // if there is no surface, fall back on standard rendering
            super.paint(g);            
        }
    }
    
    public void currentCollationChanged(Collation collation)
    {
        currentMode = JuxtaAuthorFrame.VIEW_MODE_COLLATION;
        if(frame.getViewMode() == currentMode)
        {
	        HistogramGraphModel histogramModel = null;
	        String labelText = "";
	        if( collation != null ) 
	        {
	        	int numberOfDocuments = session.getDocumentManager().getDocumentList().size();
	            byte[] histogramData = collation.getHistogramData();
	            histogramModel = new HistogramGraphModel(histogramData,numberOfDocuments);
	            labelText = session.getDocumentManager().lookupDocument(collation.getBaseDocumentID()).getDocumentName();
	        }
	        
	        initHistogramPanel(histogramModel,labelText);
	
	        repaint();
        }
    }
    
    public void documentAdded(JuxtaDocument document)
    {
        // do nothing        
    }
        
    private int getBarWidth( Rectangle graphRectangle )
    {
        int barWidth =  Math.round((float)graphRectangle.width * frame.calculateVisibleDocumentAreaPercentage());
        return barWidth > 0 ? barWidth : 1;
    }
    
    private void initHistogramPanel( HistogramGraphModel histogramModel, String labelText )
    {
    	histogramModel.setDisplaySelectionArea(false);
        histogramPanel.setModel(histogramModel);
        histogramPanel.setXLabel(labelText);
        histogramPanel.setMarkerDotColor( FIRST_COLOR );
        histogramPanel.setMarkerLineColor( FIRST_COLOR_DARKER );
    }
    
    public void selectedDocumentsChanged(JuxtaDocument base, JuxtaDocument witness)  
	{
        currentMode = JuxtaAuthorFrame.VIEW_MODE_COMPARISON;
        baseDocument = base;
        witnessDocument = witness;
        
    	if( base != null && witness != null )
    	{
    		currentCollation = null;
			try 
			{				
				currentCollation = (frame.getViewMode() == JuxtaAuthorFrame.VIEW_MODE_COMPARISON) ? 
						session.getComparisonSet().getCollation(base): session.getComparisonSet().getExistingCollation(base);
				HashSet collationFilter = new HashSet(session.getCurrentCollation().getCollationFilter());
				collationFilter.remove(base);
				collationFilter.remove(witness);
				currentCollation.setCollationFilter(collationFilter);
			} 
			catch (ReportedException e) 
			{
				ErrorHandler.handleException(e);	
			}
			
        	if( currentCollation != null )
        	{
	            int numberOfDocuments = session.getDocumentManager().getDocumentList().size();
	            byte[] histogramData = currentCollation.getHistogramData();
	            HistogramGraphModel histogramModel = new HistogramGraphModel(histogramData,numberOfDocuments);
	            String labelText = base.getDocumentName() + " vs. "+ witness.getDocumentName();
	            initHistogramPanel(histogramModel,labelText);

        		List differences = currentCollation.getDifferences(witness);
        		differences = session.getDocumentManager().getMovesManager().addMoves(differences, baseDocument.getID());


        		if( differences != null )
            	{
                	for( Iterator i = differences.iterator(); i.hasNext(); )
                	{
                		Difference difference = (Difference) i.next();
                		int offset = difference.getOffset(Difference.BASE);
               			histogramModel.addMarker(offset);
                	}
            	}
        	}
    	}
    	else
    	{
    		histogramPanel.getModel().clearMarkers();
    	}

    	histogramPanel.getModel().setDisplaySelectionArea(false);

    	repaint();		
	}

	private class SelectionTracker extends MouseAdapter implements MouseMotionListener
    {
        private boolean dragging;
        
        public void mouseDragged(MouseEvent e)
        {   
            dragging = true;
            trackPosition( e.getPoint() );    
        }
        
        public void mouseReleased(MouseEvent e)
        {
            dragging = false;
        }
        
        public void mouseExited(MouseEvent e)
        {
            dragging = false;
        }
        
        public void mouseClicked(MouseEvent e)
        {
            trackPosition( e.getPoint() );    
        }
        
        private void trackPosition( Point p )
        {
            Rectangle graphRectangle = histogramPanel.getGraphBounds();
            Point selectionPoint = SwingUtilities.convertPoint(DifferenceHistogramDialog.this,p.x,p.y,histogramPanel);
            if( graphRectangle.width > 0 )
            {
                int barWidth = getBarWidth(graphRectangle);
                int maxX = graphRectangle.width - barWidth;
                int minX = 0;
                int selectionX = (selectionPoint.x-graphRectangle.x)-barWidth/2;                
                if( selectionX > maxX ) selectionX = maxX;
                if( selectionX < minX ) selectionX = minX;
                
                setSelectionPosition( (float)selectionX/(float)graphRectangle.width );
                if( frame != null ) frame.setScrollPosition(histogramPanel.getModel().getSelectionAreaPosition());
                repaint();
            }
        }

        public void mouseMoved(MouseEvent e)
        {
            // do nothing.
        }

        public boolean isDragging()
        {
            return dragging;
        }
        
        
    }
    
    public void setSelectionPosition(float selectionPosition)
    {
    	HistogramGraphModel model = histogramPanel.getModel();
    	if( model != null )
    	{
    		model.setDisplaySelectionArea(true);
            if( selectionPosition > 1.0 ) selectionPosition = 1.0f;
            if( selectionPosition < 0.0 ) selectionPosition = 0.0f;
            model.setSelectionAreaPosition(selectionPosition);
            model.setSelectionAreaScale(frame.calculateVisibleDocumentAreaPercentage());
    	}
    }

    // handle scroll bar state change
    public void stateChanged(ChangeEvent e)
    {
        if( frame == null ) return;

        // ignore scrollbar while user is dragging to keep the two from fighting
        if( tracker.isDragging() ) return;
        
        float position = frame.getScrollPosition();
        setSelectionPosition(position);
        repaint();
    }
    
    private class FilterStrengthSlider extends JSlider implements ChangeListener
    {
        public FilterStrengthSlider()
        {
            setValue(0);
            setMaximum(20);
            setMinorTickSpacing(1);
            setPaintTicks(true);
            setSnapToTicks(true);
            setToolTipText("Adjust edit distance filter strength.");
            addChangeListener(this);
        }

        public void stateChanged(ChangeEvent e)
        {
        	int value = getValue();
            frame.setFilterStrength(value);
            
            if( currentCollation != null )
            {
            	currentCollation.setMinChangeDistance(value);
            	repaint();
            }
        }
    }

	public void currentCollationFilterChanged(Collation currentCollation) 
	{
		if( currentMode == JuxtaAuthorFrame.VIEW_MODE_COLLATION )
		{
			currentCollationChanged(currentCollation);
		}
		else
		{
			selectedDocumentsChanged(baseDocument,witnessDocument);
		}
	}

	public void display(JuxtaDocument baseDocument, JuxtaDocument witnessDocument) {
		selectedDocumentsChanged(baseDocument, witnessDocument);
		display();
	}

	public void display(Collation collation) {
		currentCollationChanged(collation);
		display();
	}

}
