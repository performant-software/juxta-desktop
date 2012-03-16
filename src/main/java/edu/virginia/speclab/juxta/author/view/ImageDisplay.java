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

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JScrollPane;

import edu.virginia.speclab.diff.document.Image;
import edu.virginia.speclab.juxta.author.model.JuxtaDocument;
import edu.virginia.speclab.juxta.author.view.collation.CollationViewTextArea;
import edu.virginia.speclab.juxta.author.view.ui.JuxtaUserInterfaceStyle;
import edu.virginia.speclab.ui.ImagePanel;
import edu.virginia.speclab.util.BackgroundImageLoader;
import edu.virginia.speclab.util.SimpleLogger;

public class ImageDisplay extends ImagePanel implements AdjustmentListener, JuxtaUserInterfaceStyle
{
    private CollationViewTextArea differenceViewer;
    private JScrollPane scrollPane;
    private boolean synchWindows;
    private boolean enabled;
    private String currentImagePath;
    
    private JButton synchButton;
    
    public ImageDisplay( CollationViewTextArea differenceViewer, JScrollPane scrollPane )
    {
        this.synchButton = new JButton(LOCK_WINDOWS);
        synchWindows = true;
        synchButton.setToolTipText("Unlock image from text");
        synchButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                if( toggleSynchWindows() )  
                {
                    synchButton.setIcon(LOCK_WINDOWS);
                    synchButton.setToolTipText("Unlock image from text");
                    recalculateImage(false);
                }
                else
                {
                    synchButton.setIcon(UNLOCK_WINDOWS);
                    synchButton.setToolTipText("Lock image to text");
                }
            } 
        });  
        
        LinkedList buttonList = new LinkedList();
        buttonList.add(synchButton);
        initialize( buttonList );
        
        this.scrollPane = scrollPane;
        this.differenceViewer = differenceViewer;

        // tie the collation viewer scrollbar to the image display
        scrollPane.getVerticalScrollBar().addAdjustmentListener(this);
    }
    
    private boolean toggleSynchWindows()
    {
        synchWindows = !synchWindows;        
        return synchWindows;
    }

    public void adjustmentValueChanged(AdjustmentEvent e)
    {
        if( !synchWindows || !enabled ) return;
        
        int centerOffset = calculateCenterOffset( e.getValue() );
        JuxtaDocument document = differenceViewer.getCurrentText();
                
        if( document != null )
        {
            Image image = document.getImageAt(centerOffset);
            String imagePath = null;
            if( image != null ) imagePath = image.getImageFile().getAbsolutePath();
            
            if( (imagePath == null && currentImagePath != null) || 
                (imagePath != null && currentImagePath == null)   )
            {
                currentImagePath = imagePath;
                BackgroundImageLoader backgroundImageLoader = new BackgroundImageLoader(currentImagePath,this);
                backgroundImageLoader.start();    
            }
            else if( imagePath != null && currentImagePath != null && 
                     imagePath.compareTo(currentImagePath) != 0 )
            {
                currentImagePath = imagePath;
                BackgroundImageLoader backgroundImageLoader = new BackgroundImageLoader(currentImagePath,this);
                backgroundImageLoader.start();    
            }
        }        
    }
    
    public void recalculateImage(boolean isDocumentChange)
    {
        if( !synchWindows || !enabled ) return;
        
        int centerOffset = (isDocumentChange) ? 0: calculateCenterOffset(calculateCenterOffset(scrollPane.getVerticalScrollBar().getValue()));

        JuxtaDocument document = differenceViewer.getCurrentText();
        
        if( document != null )
        {
            Image image = document.getImageAt(centerOffset);
            String imagePath = null;
            if( image != null ) imagePath = image.getImageFile().getAbsolutePath();
            currentImagePath = imagePath;
            SimpleLogger.logInfo("recalculate image loading image: "+currentImagePath);
            BackgroundImageLoader backgroundImageLoader = new BackgroundImageLoader(currentImagePath,this);
            backgroundImageLoader.start();    
        }        
    } 
    
   
    private int calculateCenterOffset(int value)
    {
        int centerOffset = 0;

        if( differenceViewer != null )
        {          
            centerOffset = differenceViewer.viewToModel( new Point( scrollPane.getWidth()/2, 
                                                                (scrollPane.getHeight()/2)+value ));
        }
        
        return centerOffset;
    }

    public boolean isEnabled()
    {
        return enabled;
    }
    

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        super.setEnabled(enabled);
    }
    
    public void resetImage()
    {
    	this.setImage(null);
    }
    

}
