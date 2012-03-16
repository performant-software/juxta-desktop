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
 

package edu.virginia.speclab.ui.histograph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.WrappingTextLabel;
import edu.virginia.speclab.util.SimpleLogger;

public class HistogramPanel extends JPanel
{
    private ColorScale colorScale;
    private Color backgroundColor, markerLineColor, markerDotColor;
    private HistogramGraphModel model;    
	private Font labelFont;
    private Insets insets;
	private String xLabel;
	private Color selectionAreaColor;
	
	private static final int GUTTER_HEIGHT = 4;
	private static final int BORDER_WIDTH = 1;
    
    public HistogramPanel( ColorScale colorScale )
    {
        this.colorScale = colorScale;
        this.backgroundColor = Color.WHITE;
        this.markerLineColor = Color.GREEN;
        this.markerDotColor = Color.GREEN;
		this.labelFont = new Font("Verdana",Font.PLAIN,10);
        this.insets = new Insets(5,5,20,5);
		this.xLabel = "X-AXIS";
	}
    
    public Rectangle getGraphBounds()
    {
        // figure out extents
        final Dimension windowSize = getSize();
        final Dimension graphSize = new Dimension(); 
        graphSize.width = windowSize.width - insets.left - insets.right;
        graphSize.height = windowSize.height - insets.top - insets.bottom;
        
        return new Rectangle(insets.left+BORDER_WIDTH,insets.top+BORDER_WIDTH,graphSize.width,graphSize.height);
    }
    
    private void drawGraph( Graphics2D g2 )
    {
        if( model == null ) return;
        
        Rectangle graphRectangle = getGraphBounds();
        
		// paint the background
        g2.setPaint(backgroundColor);        
        g2.fillRect(graphRectangle.x,
                    graphRectangle.y,
                    graphRectangle.width,
                    graphRectangle.height);
        
		final int dataSetSize = model.getDataSetSize();
		
		double[] accumulatedData = new double[graphRectangle.width]; 

		// calculate the height of the bars of the graph
        for( int i=0; i < dataSetSize; i++ )
        {
			double dataPoint =  model.getScaledDataPoint(i);
			            
            float percentAcrossGraph = (float)i/(float)dataSetSize;
            int index = (int)Math.floor(percentAcrossGraph * graphRectangle.width);            
			
            // if this value is greater than the existing value, replace it
			if( dataPoint > accumulatedData[index] ) accumulatedData[index] = dataPoint; 
        }

		// render the graph
        for( int x=0; x < graphRectangle.width; x++ )
        {
			double dataPoint = accumulatedData[x];			
			if( dataPoint > 1.0 ) dataPoint = 1.0;
			
			int heightOfBar = (int)Math.floor(dataPoint * graphRectangle.height);
			int xPositionOfBar = x + graphRectangle.x;
            int yPositionOfBar = (graphRectangle.height - heightOfBar) + graphRectangle.y;
            
            g2.setPaint(colorScale.getColor(dataPoint));
            g2.drawLine(xPositionOfBar,yPositionOfBar,xPositionOfBar,yPositionOfBar+heightOfBar);
        }

        // add the markers
        LinkedList markers = model.getMarkers();
        for( Iterator i = markers.iterator(); i.hasNext(); )
        {
        	Double marker = (Double) i.next();
        	renderMarker(g2,markerLineColor,marker,graphRectangle);
        }
		
		// render the border
        g2.setPaint(Color.BLACK);
        g2.drawRect(graphRectangle.x,
                    graphRectangle.y,
                    graphRectangle.width,
                    graphRectangle.height);        
    }
    
    private void renderMarker( Graphics2D g2, Color lineColor, Double marker, Rectangle graphRectangle )
    {
    	int xPositionOfBar = (int)Math.floor(marker.doubleValue() * graphRectangle.width) + graphRectangle.x;

    	g2.setPaint(lineColor);
        g2.drawLine(xPositionOfBar,graphRectangle.y,xPositionOfBar,graphRectangle.y+graphRectangle.height);
        
        g2.setPaint(markerDotColor);
        g2.fillOval(xPositionOfBar-1,graphRectangle.y+graphRectangle.height+BORDER_WIDTH+1, 3, 3);
        g2.setPaint(Color.BLACK);
        g2.drawOval(xPositionOfBar-1,graphRectangle.y+graphRectangle.height+BORDER_WIDTH+1, 3, 3);
    }
    
    public void setInsets( Insets insets )
    {
        this.insets = insets;
    }

    public void setGraphBackground( Color backgroundColor )
    {
        this.backgroundColor = backgroundColor;
        super.setBackground(backgroundColor);
    }
    
    public void paint( Graphics g )
    {
        super.paint(g);
		
		Graphics2D g2 = (Graphics2D)g;		
        drawGraph(g2); 
        if( model.isDisplaySelectionArea() ) drawSelectionArea(g2);
		drawXLabel(g2);
    }
    
	private void drawXLabel(Graphics2D g2) 
	{
        if( xLabel == null || xLabel.length() == 0 ) return;
        
		final Dimension windowSize = getSize();
		
		final float maxTextWidth = windowSize.width - insets.left - insets.right;
		WrappingTextLabel textLabelX = new WrappingTextLabel( xLabel, labelFont, Color.BLACK, maxTextWidth, 1, g2.getFontRenderContext());
		
		// discover the actual length of the line in pixels
		final int lineLength = (int)Math.round(textLabelX.getLineLength(0));
		
		// calculate label location
		Point xLabelLocation = new Point();
		xLabelLocation.x = Math.round( windowSize.width/2 - lineLength/2 );
		xLabelLocation.y = Math.round( windowSize.height + GUTTER_HEIGHT - (insets.bottom/2 + textLabelX.getHeight()/2) );
		textLabelX.setLocation(xLabelLocation.x,xLabelLocation.y);
		
		// draw it!
		textLabelX.paint(g2);
	}

	/**
     * @param args
     */
    public static void main(String[] args)
    {
		SimpleLogger.initConsoleLogging();
        SimpleLogger.setLoggingLevel(10);
		 
        JFrame testFrame = new JFrame("Histogram Panel");
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        testFrame.setSize(435,150);
        testFrame.setLocation(500,300);
        
        ColorScale colorScale = new ColorScale(JuxtaUserInterfaceStyle.SECOND_COLOR_SCALE);
        HistogramGraphModel model = new HistogramGraphModel();
        HistogramPanel panel = new HistogramPanel(colorScale);
        panel.setModel(model);
		panel.setXLabel("differences in base document - click to scroll");
				
        testFrame.getContentPane().setLayout( new BorderLayout() );
        testFrame.getContentPane().add(panel, BorderLayout.CENTER );
        testFrame.setVisible(true);
    }

	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}
	
	public void setSelectionAreaColor( Color selectionAreaColor ) {
		this.selectionAreaColor = selectionAreaColor;
	}

    private int getBarWidth()
    {
    	Rectangle graphRectangle = getGraphBounds();
        int barWidth =  Math.round((float)graphRectangle.width * model.getSelectionAreaScale() );
        return barWidth > 0 ? barWidth : 1;
    }
    
    private void drawSelectionArea( Graphics2D g2 )
    {
        Rectangle graphRectangle = getGraphBounds();
        int xPosition = Math.round(graphRectangle.x+((float)graphRectangle.width * model.getSelectionAreaPosition()));
                
        g2.setColor( this.selectionAreaColor ); 
        g2.fillRect(xPosition, graphRectangle.y+1, getBarWidth(), graphRectangle.height-1);
        
        // invert the color of the marker lines under the selected area
        List markers = model.getMarkersInSelectedArea();
        
        for( Iterator i = markers.iterator(); i.hasNext(); )
        {
        	Double marker = (Double) i.next();
        	renderMarker(g2,Color.YELLOW,marker,graphRectangle);
        }
        
    }
    
	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setXLabel(String label) {
		xLabel = label;
	}
	
    public void setModel(HistogramGraphModel model)
    {
        this.model = model;
    }

	public HistogramGraphModel getModel() {
		return model;
	}

	public void setMarkerDotColor(Color color) {
		this.markerDotColor = color;
	}
    
	public void setMarkerLineColor(Color color) {
		this.markerLineColor = color;
	}

}
