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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

public class TransparentSurface 
{
    private BufferedImage surface;
    private Graphics2D graphics;
    
    public TransparentSurface( int width, int height )
    {
        // create the offscreen buffer
        GraphicsEnvironment local = 
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice screen = local.getDefaultScreenDevice();
        GraphicsConfiguration conf = screen.getDefaultConfiguration();
        surface = conf.createCompatibleImage(width, height, Transparency.TRANSLUCENT );

        // create the graphics interface
        graphics = (Graphics2D) surface.createGraphics();

        // clear it
        clear();
    }
    
    /**
     * Erase the contents of the surface, returning it to a fully transparent state.
     *
     */
    public void clear()
    {
        if( surface == null ) return;
        
        Paint oldPaint = graphics.getPaint();
        
        // set the background to be transparent
        graphics.setPaint( new Color(0,0,0,0) );
        
        Composite oldComposite = graphics.getComposite();
        Composite newComposite = AlphaComposite.getInstance(AlphaComposite.CLEAR);
        graphics.setComposite(newComposite);

        graphics.fillRect(0,0,surface.getWidth(null),surface.getHeight(null));

        graphics.setComposite(oldComposite);
        graphics.setPaint(oldPaint);
    }
    
    public int getWidth()
    {
        if( surface != null )
        {
            return surface.getWidth();
        }
        else
        {
            return 0;
        }
    }
    
    public int getHeight()
    {
        if( surface != null )
        {
            return surface.getHeight();
        }
        else
        {
            return 0;
        }
    }
    
    public void dispose()
    {
        if( graphics != null )
        {
            graphics.dispose();
        }
    }
    
    /**
     * Obtain a Graphics2D object that references the transparent surface. 
     * @return
     */
    public Graphics2D getGraphics()
    {
        return graphics;
    }
    
    /**
     * Obtain the offscreen buffer for the drawing surface. 
     * @return
     */
    public BufferedImage getSurface()
    {
        return surface;
    }
}
