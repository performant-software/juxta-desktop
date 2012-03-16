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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class HistogramGraphModel
{
    private int maxValue;
    private String labelXAxis, labelYAxis;
    private byte data[];
    private LinkedList markers = new LinkedList();
	private float selectionAreaPosition;
	private float selectionAreaScale;
	private boolean displaySelectionArea;

    public boolean isDisplaySelectionArea() {
		return displaySelectionArea;
	}

	public void setDisplaySelectionArea(boolean displaySelectionArea) {
		this.displaySelectionArea = displaySelectionArea;
	}

	public float getSelectionAreaPosition() {
		return selectionAreaPosition;
	}

	public void setSelectionAreaPosition(float selectionAreaPosition) {
		this.selectionAreaPosition = selectionAreaPosition;
	}

	public float getSelectionAreaScale() {
		return selectionAreaScale;
	}

	public void setSelectionAreaScale(float selectionAreaScale) {
		this.selectionAreaScale = selectionAreaScale;
	}

	public HistogramGraphModel( )
    {
        generateTestData();
    }
    
    public HistogramGraphModel( byte histogramData[], int maxValue )
    {
        this.data = histogramData;
        this.maxValue = maxValue;
    }
        
    public void addMarker( int value )
    {
    	if( this.data.length > 0 )
    	{
        	double scaledValue = (double)value / this.data.length;
        	scaledValue = ( scaledValue > 1.0 ) ? 1.0 : scaledValue;
        	markers.add( new Double(scaledValue) );
    	}
    }
    
    public LinkedList getMarkers()
    {
    	return markers;
    }
    
    private void generateTestData()
    {
        maxValue = 100;
        labelXAxis = "X-Axis";
        labelYAxis = "Y-Axis";
        
		int DATA_SIZE = 500;
		
        data = new byte[DATA_SIZE];
        for( int i=0; i < DATA_SIZE; i++ )
        {
            data[i] = (byte)Math.round(Math.random() * 100.0);                
        }               
        
        addMarker(maxValue/2);
    }
    
    public int getDataSetSize()
    {
        if( data == null ) return 0;
        else return data.length;
    }

    public double getScaledDataPoint( int index )
    {
        if( index < 0 || index > data.length || maxValue <= 0 ) return 0;
        double scaledValue = (double)data[index] / (double)maxValue;
        
        if(scaledValue > 1.0) scaledValue = 1.0;
        return scaledValue;
    }

    public String getLabelXAxis()
    {
        return labelXAxis;
    }
    

    public String getLabelYAxis()
    {
        return labelYAxis;
    }
    

    public int getMaxValue()
    {
        return maxValue;
    }

	public void clearMarkers() 
	{
		markers.clear();
	}

	/**
	 * Returns a list of markers within the selection area.
	 * @return
	 */
	public List getMarkersInSelectedArea() 
	{
		LinkedList selectedMarkers = new LinkedList();
		
		for( Iterator i = markers.iterator(); i.hasNext(); )
		{
			Double marker = (Double) i.next();
			if( testMarkerInSelectionArea(marker) )
			{
				selectedMarkers.add(marker);
			}
		}

		return selectedMarkers;
	}

	// determine if this marker falls within the bounds of the current selection area.
	private boolean testMarkerInSelectionArea(Double marker) 
	{
		double markerValue = marker.doubleValue();
		
		return ( markerValue >= selectionAreaPosition && 
				 markerValue < selectionAreaPosition+selectionAreaScale );
	}
    
    
}
