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

import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import edu.virginia.speclab.diff.OffsetRange;
import edu.virginia.speclab.diff.document.LocationMarker;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.ui.LocationMarkStrip;

public class LineMarkController
{
    private JuxtaDocument documentModel;    
    private LocationMarkStrip lineNumberStrip;
    private JTextComponent textArea;
    private ResizeListener resizer;
        
    public LineMarkController( LocationMarkStrip lineNumberStrip )
    {
        this.lineNumberStrip = lineNumberStrip;
        this.resizer = new ResizeListener();
    }
    
    public void updateLineMarkers( JuxtaDocument document, JTextComponent textArea )
    {
        this.documentModel = document;
        this.textArea = textArea;

        if( this.textArea != null )
        {
            this.textArea.removeComponentListener(resizer);
        }
        
        this.textArea = textArea;
        
        if( textArea != null )
        {
            textArea.addComponentListener(resizer);
        }
        
        updateMarks();        
    }
    
    public void updateLineMarkers( JuxtaDocument document )
    {
        this.documentModel = document;
        updateMarks();        
    }
    
    private void updateMarks() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (lineNumberStrip == null)
                    return;

                lineNumberStrip.clearLineMarks();

                if (textArea == null || documentModel == null)
                    return;

                List locationMarkers = documentModel.getLocationMarkerList();

                if (locationMarkers != null) {
                    int lineHeight = lineNumberStrip.getLineHeight();

                    for (Iterator i = locationMarkers.iterator(); i.hasNext();) {
                        LocationMarker marker = (LocationMarker) i.next();

                        try {
                            Rectangle position = textArea.modelToView(marker.getStartOffset(OffsetRange.Space.ACTIVE));

                            if (position != null) {
                                int lineNumber = ((position.y + (position.height / 2)) / lineHeight) + 1;
                                String mark = marker.getLocationType() + Integer.toString(marker.getNumber());
                                lineNumberStrip.addLineMark(lineNumber, mark);
                            }
                        } catch (BadLocationException e) { /*skip it*/
                        }
                    }
                }

                lineNumberStrip.repaint();
            }
        });
    }
    
    private class ResizeListener extends ComponentAdapter  
    {        
        public void componentResized(ComponentEvent e) 
        {
            updateMarks();
        }
    }
}
