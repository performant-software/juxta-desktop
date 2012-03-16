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

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JButton;

import edu.virginia.speclab.util.ImageLoader;

public class ImagePanel extends JPanel 
{
    private boolean noImage;
    private ImageDisplayPanel imageDisplayPanel;
        
    private static final ImageLoader imageLoader = new ImageLoader(null);
    
    private static final ImageIcon zoomInIcon = new ImageIcon( imageLoader.loadImage("icons/zoom.in.button.gif") );
    private static final ImageIcon zoomOutIcon = new ImageIcon( imageLoader.loadImage("icons/zoom.out.button.gif") );
    private static final BufferedImage NO_IMAGE = imageLoader.loadImage("icons/no.image.gif");

    private PanelTitle panelTitle;

    private Color imageBackgroundColor;
    
    private ImageCache imageCache;
    
    public void initialize( LinkedList toolbarButtons )
    {
        init(toolbarButtons);
        imageDisplayPanel.setImageOffset(new Point(0,0));
        displayNoImage();        
        DragTracker tracker = new DragTracker(); 
        addMouseListener( tracker );
        addMouseMotionListener( tracker );
        addComponentListener( new ResizeTracker() );
        imageCache = new ImageCache();
    }
    
    public void clearCache()
    {
    	imageCache.clear();
    }
    
    private void displayNoImage()
    {
        noImage = true;
        imageDisplayPanel.setImage(NO_IMAGE);
        imageDisplayPanel.resetZoom();
        imageDisplayPanel.centerImage();
    }
    
    public void setImage( String imageFile )
    {
        if( imageFile == null )
        {
            displayNoImage();
        }
        else
        {
            noImage = false;
            BufferedImage image = imageCache.getImage(imageFile);
            imageDisplayPanel.setImage(image);
            imageDisplayPanel.resetZoom();
            imageDisplayPanel.setImageOffset(new Point(0,0));
        }
        
        repaint();
    }
    
    public void setTitleBackgroundColor( Color color )
    {
        panelTitle.setBackground(color);
    }
    
    public void setTitleFont( Font font )
    {
        panelTitle.setFont(font);
    }
    
    public void setImageBackgroundColor( Color color )
    {
        this.imageBackgroundColor = color;
    }
    
    private void init( LinkedList toolbarButtons )
    {
        setLayout( new BorderLayout() );        
        
        panelTitle = new PanelTitle();
        panelTitle.setTitleText("Image");        
        panelTitle.setBorder( LineBorder.createGrayLineBorder() );
        add( panelTitle, BorderLayout.NORTH );
        
        imageDisplayPanel = new ImageDisplayPanel();
        add( imageDisplayPanel, BorderLayout.CENTER);
        
        JToolBar leftToolBar = new JToolBar();
        leftToolBar.setFloatable(false);
        
        JToolBar rightToolBar = new JToolBar();
        rightToolBar.setFloatable(false);
        
        loadToolbarButtons(rightToolBar,toolbarButtons);
        
        JButton zoomInButton = new JButton(zoomInIcon);
        JButton zoomOutButton = new JButton(zoomOutIcon);
        
        zoomInButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                if( noImage ) return;
                imageDisplayPanel.zoomIn();
            } 
        });
       
        zoomOutButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                if( noImage ) return;
                imageDisplayPanel.zoomOut();
            } 
        });
        
        rightToolBar.add(zoomInButton);
        rightToolBar.add(zoomOutButton);
        
        JPanel southPanel = new JPanel();        
        southPanel.setLayout( new BorderLayout());
        
        CompoundBorder compoundBorder = new CompoundBorder( LineBorder.createGrayLineBorder(),
                                                            new EmptyBorder(2,0,2,0) );
        
        southPanel.setBorder(compoundBorder);
        southPanel.add(leftToolBar,BorderLayout.WEST);
        southPanel.add(rightToolBar,BorderLayout.EAST);        
        add(southPanel, BorderLayout.SOUTH);
        
    }
    
    private void loadToolbarButtons(JToolBar toolbar, LinkedList toolbarButtons)
    {
        if( toolbarButtons == null ) return;
        
        for( Iterator i = toolbarButtons.iterator(); i.hasNext(); )
        {
            JButton button = (JButton) i.next();
            toolbar.add(button);
        }
    }

    private class ImageDisplayPanel extends JPanel
    {
        private float zoomLevel;
        private Point imageOffset;
        private BufferedImage image;
        
        public ImageDisplayPanel()
        {
            resetZoom();
            setLayout( new BorderLayout() );                        
        }

        public void setImage(BufferedImage image)
        {
            this.image = image;          
        }
        
        public void resetZoom()
        {
            zoomLevel = 1f;
        }
        
        public void zoomIn()
        {
            zoomLevel += .15f;
            repaint();
        }
        
        public void centerImage()
        {
            if( image == null ) return;
            
            imageOffset.x = getWidth()/2 - (image.getWidth()/2); 
            imageOffset.y = getHeight()/2 - (image.getHeight()/2);            
        }
        
        public void zoomOut()
        {
            zoomLevel -= .15f;
            if( zoomLevel < .15f ) zoomLevel = .15f;
            repaint();
        }
        
        public void paint( Graphics g )
        {
            Graphics2D g2 = (Graphics2D) g;
            
            g2.setPaint(imageBackgroundColor);
            g2.fillRect(0,0,getWidth(),getHeight());
            
            if( image == null ) 
            {
                super.paint(g);
                return;
            }
            
            float scaledWidth = (float)image.getWidth() * zoomLevel;
            float scaledHeight = (float)image.getHeight() * zoomLevel;
            
            g2.drawImage(image, imageOffset.x, imageOffset.y, Math.round(scaledWidth), Math.round(scaledHeight), null);
            super.paintBorder(g);
        }

        public Point getImageOffset()
        {
            return imageOffset;
        }
        

        public void setImageOffset(Point imageOffset)
        {
            this.imageOffset = imageOffset;
        }
        

    }
    
    private class DragTracker extends MouseAdapter implements MouseMotionListener 
    {
        private Point lastPoint;
        
        public void mouseReleased(MouseEvent e)
        {
            lastPoint = null;
        }

        public void mouseDragged(MouseEvent e)
        {            
            if( noImage ) 
            {
                lastPoint = null;
                return;
            }
           
            if( lastPoint == null )
            {
                lastPoint = e.getPoint();
            }
            else
            {
                Point currentPoint = new Point();
                
                currentPoint.x = e.getPoint().x;
                currentPoint.y = e.getPoint().y;
                
                int deltaX = currentPoint.x - lastPoint.x;
                int deltaY = currentPoint.y - lastPoint.y;
                
                Point imageOffset = imageDisplayPanel.getImageOffset();
                
                int imageOffsetX = imageOffset.x + deltaX;
                int imageOffsetY = imageOffset.y + deltaY;
                
                imageOffset.x = imageOffsetX;
                imageOffset.y = imageOffsetY;                
                lastPoint = currentPoint;                
                repaint();
            }            
        }

        public void mouseMoved(MouseEvent e)
        {
            // do nothing.
        }
        
    }
    
    private class ResizeTracker extends ComponentAdapter
    {
        public void componentResized(ComponentEvent e)
        {
            if( noImage )
            {
                imageDisplayPanel.centerImage();
                repaint();
            }            
        }
    }

}
