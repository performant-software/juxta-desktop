/*
 * Created on Feb 14, 2004
 */
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


package edu.virginia.speclab.util;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

/**
 * ImageLoader
 * http://wiki.java.net/bin/view/Games/LoadingSpritesWithImageIO
 * @author ScottWPalmer 
 */
public class ImageLoader {
	final GraphicsConfiguration gc;
	
	public ImageLoader(GraphicsConfiguration gc) {
		if (gc == null) {
			gc =
				GraphicsEnvironment
					.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice()
					.getDefaultConfiguration();
		}
		this.gc = gc;
	}
   
   public BufferedImage createImage( int width, int height )
   {      
      return gc.createCompatibleImage( width, height );
   }

	public BufferedImage loadImage(String resource) {
		try {
		    URL resourceUrl = ClassLoader.getSystemResource(resource);
			return javax.imageio.ImageIO.read(resourceUrl);
			
		} catch (java.io.IOException ioe) {
			return null;
		}
	}
	
	public BufferedImage loadImageFile(String imageFile) {
        try {
            return javax.imageio.ImageIO.read( new File(imageFile) );
        } catch (java.io.IOException ioe) {
            return null;
        }
    }
}
